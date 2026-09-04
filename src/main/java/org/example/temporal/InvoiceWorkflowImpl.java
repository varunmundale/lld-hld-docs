package org.example.temporal;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.workflow.Saga;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;

/**
 * The business process, as ordinary sequential code.
 *
 * Notice what is NOT here: no status column, no resume logic, no retry loop, no timer table,
 * no approval table, no "which step was I on" reconstruction. Temporal supplies all of it.
 * That absence is the whole value proposition.
 *
 * The one rule: this method must be DETERMINISTIC. Given the same history it must make the
 * same calls in the same order. So:
 *
 *   NO  System.currentTimeMillis(), new Random(), UUID.randomUUID(), Thread.sleep(),
 *       new Thread(), direct HTTP/JDBC/file access, unordered map iteration, env reads
 *   YES Workflow.currentTimeMillis(), Workflow.newRandom(), Workflow.randomUUID(),
 *       Workflow.sleep(), Async.function(), activities for anything that touches the world
 *
 * Break that rule and the failure does not appear at the point of the bug - it appears months
 * later, on a replay, as a NonDeterministicException on an execution that is now stuck.
 */
public class InvoiceWorkflowImpl implements InvoiceWorkflow {

    private static final Logger log = Workflow.getLogger(InvoiceWorkflowImpl.class);

    /**
     * Fast, safe reads. Aggressive-ish retries are fine because these are cheap and idempotent.
     * doNotRetry("InvalidInvoice") matters: a 400 is not a blip, and retrying it 4 times just
     * makes the failure slower.
     */
    private final InvoiceActivities fast = Workflow.newActivityStub(
            InvoiceActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(30))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setInitialInterval(Duration.ofSeconds(1))
                            .setBackoffCoefficient(2.0)
                            .setMaximumInterval(Duration.ofSeconds(30))
                            .setMaximumAttempts(4)
                            .setDoNotRetry("InvalidInvoice")
                            .build())
                    .build());

    /**
     * A separate stub for money movement: longer timeouts, fewer attempts, a schedule-to-close
     * ceiling so a wedged PSP cannot be retried into next week. In production this would also
     * get its own task queue so a slow payment worker cannot starve the fast ones.
     */
    private final InvoiceActivities payments = Workflow.newActivityStub(
            InvoiceActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofMinutes(2))
                    .setScheduleToCloseTimeout(Duration.ofMinutes(30))
                    .setHeartbeatTimeout(Duration.ofSeconds(20))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setInitialInterval(Duration.ofSeconds(2))
                            .setBackoffCoefficient(2.0)
                            .setMaximumAttempts(3)
                            .setDoNotRetry("CardDeclined")
                            .build())
                    .build());

    private ApprovalDecision decision;          // set by the signal handler
    private volatile String status = "STARTED";

    @Override
    public String process(Invoice invoice) {
        log.info("processing {}", invoice.display());

        status = "VALIDATING";
        fast.validateInvoice(invoice);

        // --- VERSIONING -----------------------------------------------------------------
        // Inserting a step into a workflow that already has executions in flight would make
        // their replay diverge. getVersion writes a marker into history the first time it runs,
        // so old executions keep taking the old branch forever and new ones take the new one.
        // The cost: this branch is now permanent archaeology until every old run has drained.
        int v = Workflow.getVersion("add-fraud-check", Workflow.DEFAULT_VERSION, 1);
        if (v >= 1) {
            status = "FRAUD_CHECK";
            fast.fraudCheck(invoice);
        }

        status = "MATCHING_PO";
        String purchaseOrder = fast.matchPurchaseOrder(invoice);

        // --- IDEMPOTENCY KEY ------------------------------------------------------------
        // Workflow.randomUUID() is deterministic: the value is recorded in history, so every
        // retry of the charge AND every replay after a crash sees the SAME key. Using
        // java.util.UUID.randomUUID() here instead is the single most expensive one-line bug
        // in this file - a crash between the charge and its ack would mint a new key and the
        // vendor would be charged twice. See README, "the double-charge you can reproduce".
        String idempotencyKey = "idem-" + Workflow.randomUUID();

        // --- HUMAN IN THE LOOP ----------------------------------------------------------
        if (invoice.amountCents() > APPROVAL_THRESHOLD_CENTS) {
            status = "AWAITING_APPROVAL";
            // Blocks for up to 72 hours. Costs one timer row and zero CPU - no held thread,
            // no polling job, no pending_approvals table with a cron scanning it.
            boolean answered = Workflow.await(Duration.ofHours(72), () -> decision != null);

            if (!answered) {
                status = "ESCALATING";
                fast.escalateToController(invoice, "72h");
                answered = Workflow.await(Duration.ofHours(24), () -> decision != null);
                if (!answered) {
                    status = "PARKED";
                    // A deliberate outcome, not a stuck execution. The history shows exactly
                    // who was asked, when, and that nobody replied.
                    return "PARKED: no approval 96h after submission, escalated and still silent";
                }
            }

            if (!decision.approved()) {
                status = "REJECTED";
                fast.voidInvoice(invoice, decision.reason());
                fast.notifyVendor(invoice, "rejected");
                return "REJECTED by " + decision.by() + " (" + decision.reason() + ")";
            }
        }

        // --- SAGA -----------------------------------------------------------------------
        // Compensation is just a stack of undo calls. No orchestrator, no compensation table.
        Saga saga = new Saga(new Saga.Options.Builder().setParallelCompensation(false).build());
        try {
            status = "CHARGING";
            ChargeResult charge = payments.chargePayment(idempotencyKey, invoice);
            saga.addCompensation(payments::refundPayment, charge.authId(), invoice);
            if (charge.duplicateSuppressed()) {
                log.warn("gateway suppressed a duplicate charge for key {} - the activity ran "
                        + "more than once and the money moved exactly once", idempotencyKey);
            }

            // A durable timer measured in days. The worker can be redeployed, scaled to zero,
            // or crash repeatedly during this window; the wait is server-side.
            status = "AWAITING_SETTLEMENT";
            Workflow.sleep(Duration.ofDays(2));

            status = "POSTING_TO_LEDGER";
            String journalId = fast.postToLedger(charge.authId(), purchaseOrder, invoice);
            saga.addCompensation(fast::reverseJournal, journalId);

            fast.notifyVendor(invoice, "paid");
            status = "PAID";
            return "PAID " + invoice.display() + " auth=" + charge.authId()
                    + " po=" + purchaseOrder + " journal=" + journalId;

        } catch (ActivityFailure e) {
            status = "COMPENSATING";
            log.error("failing after the charge - compensating", e);
            saga.compensate();           // refund, reverse the journal, in reverse order
            status = "COMPENSATED";
            throw e;
        }
    }

    @Override
    public void decide(ApprovalDecision d) {
        // Signal handlers must be fast and must not block. Just record the fact; the main
        // method is waiting on it via Workflow.await.
        this.decision = d;
        log.info("decision received: {} by {}", d.approved() ? "APPROVE" : "REJECT", d.by());
    }

    @Override
    public String status() {
        return status;
    }
}
