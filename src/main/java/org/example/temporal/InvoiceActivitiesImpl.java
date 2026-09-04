package org.example.temporal;

import io.temporal.activity.Activity;
import io.temporal.activity.ActivityExecutionContext;
import io.temporal.failure.ApplicationFailure;

/**
 * Activity implementations - the only place allowed to touch the outside world.
 *
 * Failure injection is driven by system properties so the scenarios in the README can be run
 * without editing code:
 *
 *   -Dfail.matchPurchaseOrder=2     first 2 attempts throw a retryable error
 *   -Dfail.validate=true            validation throws a NON-retryable error
 *   -Dfail.afterCharge=1           the gateway charges, then the ack is "lost" and the
 *                                   activity is retried - the in-process version of a crash
 *   -Dcrash.after=chargePayment     the worker halts hard right after the gateway charges,
 *                                   for the real-server demo in the README
 */
public class InvoiceActivitiesImpl implements InvoiceActivities {

    private final PaymentGateway gateway;

    public InvoiceActivitiesImpl(PaymentGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public ValidationResult validateInvoice(Invoice invoice) {
        log("validateInvoice", invoice.invoiceId());
        if (invoice.amountCents() <= 0 || Boolean.getBoolean("fail.validate")) {
            // Non-retryable: a malformed invoice will still be malformed on attempt four.
            // The type string here is what ActivityOptions.setDoNotRetry("InvalidInvoice") matches.
            throw ApplicationFailure.newNonRetryableFailure(
                    "invoice " + invoice.invoiceId() + " failed validation", "InvalidInvoice");
        }
        return new ValidationResult(true, invoice.currency());
    }

    @Override
    public String fraudCheck(Invoice invoice) {
        log("fraudCheck", invoice.invoiceId());
        return "risk=low";
    }

    @Override
    public String matchPurchaseOrder(Invoice invoice) {
        ActivityExecutionContext ctx = Activity.getExecutionContext();
        int attempt = ctx.getInfo().getAttempt();          // 1-based, supplied by the server
        log("matchPurchaseOrder attempt=" + attempt, invoice.invoiceId());

        int failFirst = Integer.getInteger("fail.matchPurchaseOrder", 0);
        if (attempt <= failFirst) {
            // A plain exception is retryable by default; the retry policy lives on the stub,
            // not here. The activity's job is to fail honestly, not to manage backoff.
            throw new RuntimeException("ERP read timeout (attempt " + attempt + ")");
        }
        return "PO-" + Math.abs(invoice.invoiceId().hashCode() % 9000 + 1000);
    }

    @Override
    public ChargeResult chargePayment(String idempotencyKey, Invoice invoice) {
        ActivityExecutionContext ctx = Activity.getExecutionContext();
        log("chargePayment attempt=" + ctx.getInfo().getAttempt()
                + " key=" + idempotencyKey, invoice.invoiceId());
        ctx.heartbeat("calling gateway");   // lets the server detect a wedged worker early

        ChargeResult result = gateway.charge(idempotencyKey, invoice.amountCents());

        // THE LOST ACK: the money moved, then the reply never made it back. Temporal cannot
        // tell this apart from "the activity never ran", so it retries - which is precisely
        // why at-least-once is the honest guarantee and the idempotency key is not optional.
        int loseAck = Integer.getInteger("fail.afterCharge", 0);
        if (ctx.getInfo().getAttempt() <= loseAck) {
            throw new RuntimeException("network dropped the gateway ack (attempt "
                    + ctx.getInfo().getAttempt() + ") - the charge already happened");
        }

        // THE CRASH WINDOW: the money has moved, the server has not been told. Killing the
        // process here is what forces the retry that proves at-least-once execution.
        if ("chargePayment".equals(System.getProperty("crash.after"))) {
            System.out.println();
            System.out.println("  ##  SIMULATED WORKER DEATH  ##");
            System.out.println("  The gateway was charged. Temporal has NOT been told.");
            System.out.println("  Restart the worker WITHOUT -Dcrash.after and watch what happens:");
            System.out.println("  the StartToClose timeout fires, the activity is retried, and the");
            System.out.println("  same idempotency key means the gateway does not charge again.");
            System.out.println();
            System.out.flush();
            Runtime.getRuntime().halt(1);         // no shutdown hooks: a real kill -9
        }
        return result;
    }

    @Override
    public void refundPayment(String authId, Invoice invoice) {
        log("refundPayment (compensation)", invoice.invoiceId());
        gateway.refund(authId);
    }

    @Override
    public String postToLedger(String authId, String purchaseOrder, Invoice invoice) {
        log("postToLedger", invoice.invoiceId());
        if (Boolean.getBoolean("fail.ledger")) {
            throw ApplicationFailure.newNonRetryableFailure("ledger period is closed", "PeriodClosed");
        }
        return "JE-" + Math.abs((authId + purchaseOrder).hashCode() % 90000 + 10000);
    }

    @Override
    public void reverseJournal(String journalId) {
        log("reverseJournal (compensation)", journalId);
    }

    @Override
    public void notifyVendor(Invoice invoice, String outcome) {
        log("notifyVendor outcome=" + outcome, invoice.invoiceId());
    }

    @Override
    public void escalateToController(Invoice invoice, String waited) {
        log("escalateToController waited=" + waited, invoice.invoiceId());
    }

    @Override
    public void voidInvoice(Invoice invoice, String reason) {
        log("voidInvoice reason=" + reason, invoice.invoiceId());
    }

    private static void log(String what, String subject) {
        System.out.println("  [activity] " + what + "  <" + subject + ">");
    }
}
