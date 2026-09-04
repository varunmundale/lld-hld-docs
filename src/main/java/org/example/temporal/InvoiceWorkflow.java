package org.example.temporal;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * The workflow contract.
 *
 * Three kinds of entry point, and the distinction matters:
 *
 *   @WorkflowMethod - starts the execution. Exactly one per interface.
 *   @SignalMethod   - delivers an external event INTO a running execution. Durable: the
 *                     signal is written to history before the handler runs, so a worker
 *                     crash cannot lose an approval that a human already gave.
 *   @QueryMethod    - reads state OUT of a running execution without mutating it. Served
 *                     by replaying history on a worker; must not block or call activities.
 */
@WorkflowInterface
public interface InvoiceWorkflow {

    /** Task queue name. Real deployments split queues by SLA class - see README. */
    String TASK_QUEUE = "invoice-approval";

    /** Above this, a human has to approve before any money moves. */
    long APPROVAL_THRESHOLD_CENTS = 500_000L;

    @WorkflowMethod
    String process(Invoice invoice);

    /** The human in the loop. Called by InvoiceCli approve/reject. */
    @SignalMethod
    void decide(ApprovalDecision decision);

    /** Cheap operational visibility: "what is this invoice waiting on right now?" */
    @QueryMethod
    String status();
}
