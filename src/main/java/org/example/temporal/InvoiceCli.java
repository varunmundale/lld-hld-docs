package org.example.temporal;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.enums.v1.WorkflowIdReusePolicy;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowExecutionAlreadyStarted;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;

import java.time.Duration;

/**
 * The client side. Starting, signalling and querying an execution.
 *
 *   exec:java -Dexec.mainClass=org.example.temporal.InvoiceCli -Dexec.args="start INV-1001 12500"
 *   ... -Dexec.args="status INV-1001"
 *   ... -Dexec.args="approve INV-1001"
 *   ... -Dexec.args="reject INV-1001 PO amount mismatch"
 *   ... -Dexec.args="wait INV-1001"
 */
public final class InvoiceCli {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("usage: start <invoiceId> <amountCents> | status <id> | "
                    + "approve <id> | reject <id> [reason] | wait <id>");
            return;
        }

        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);
        String command = args[0];
        String invoiceId = args.length > 1 ? args[1] : "INV-1001";

        switch (command) {
            case "start" -> start(client, invoiceId,
                    args.length > 2 ? Long.parseLong(args[2]) : 1_250_000L);
            case "status" -> System.out.println("status: " + stub(client, invoiceId).status());
            case "approve" -> {
                stub(client, invoiceId).decide(
                        new ApprovalDecision(true, "controller@acme.test", "within budget"));
                System.out.println("approval signalled to " + invoiceId);
            }
            case "reject" -> {
                String reason = args.length > 2
                        ? String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length))
                        : "rejected by approver";
                stub(client, invoiceId).decide(
                        new ApprovalDecision(false, "controller@acme.test", reason));
                System.out.println("rejection signalled to " + invoiceId);
            }
            case "wait" -> {
                System.out.println("blocking on the final result of " + invoiceId + " ...");
                String result = client.newUntypedWorkflowStub(invoiceId)
                        .getResult(String.class);
                System.out.println("result: " + result);
            }
            default -> System.out.println("unknown command: " + command);
        }
        System.exit(0);
    }

    private static void start(WorkflowClient client, String invoiceId, long amountCents) {
        Invoice invoice = new Invoice(invoiceId, "Acme Cloud Ltd", amountCents, "USD");

        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setTaskQueue(InvoiceWorkflow.TASK_QUEUE)
                // THE client-side idempotency handle. The workflow id is the business key, so
                // submitting the same invoice twice cannot start two executions - a duplicate
                // POST from a retrying caller is rejected by the server, not by your code.
                .setWorkflowId(invoiceId)
                .setWorkflowIdReusePolicy(
                        WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_ALLOW_DUPLICATE_FAILED_ONLY)
                // A ceiling on the whole business process, including the human wait. Without
                // one, a forgotten approval sits open forever and shows up as storage.
                .setWorkflowExecutionTimeout(Duration.ofDays(30))
                // How long ONE workflow task (a replay + decide) may take. Small on purpose:
                // it is CPU-only work, and a slow one usually means someone did I/O in the
                // workflow method.
                .setWorkflowTaskTimeout(Duration.ofSeconds(10))
                .build();

        InvoiceWorkflow workflow = client.newWorkflowStub(InvoiceWorkflow.class, options);
        try {
            // Async start: returns as soon as the server has DURABLY recorded the execution.
            // From this moment the process is the server's responsibility, not this JVM's -
            // kill this CLI, kill the worker, redeploy: it still completes.
            WorkflowExecution execution = WorkflowClient.start(workflow::process, invoice);
            System.out.println("started " + execution.getWorkflowId()
                    + " runId=" + execution.getRunId());
            System.out.println("  amount " + (amountCents / 100.0) + " USD"
                    + (amountCents > InvoiceWorkflow.APPROVAL_THRESHOLD_CENTS
                        ? "  -> needs approval, it will block on a human"
                        : "  -> under threshold, it will pay straight through"));
            System.out.println("  watch it: http://localhost:8233/namespaces/default/workflows/"
                    + execution.getWorkflowId());
        } catch (WorkflowExecutionAlreadyStarted e) {
            System.out.println("already running: " + invoiceId
                    + "  (the workflow id is the dedup key - this is the server refusing a "
                    + "duplicate submission)");
        }
    }

    private static InvoiceWorkflow stub(WorkflowClient client, String invoiceId) {
        // Attaching to a RUNNING execution by workflow id: no options, no start.
        return client.newWorkflowStub(InvoiceWorkflow.class, invoiceId);
    }

    // Two operations worth knowing about, not wired to a command here:
    //
    //   client.newUntypedWorkflowStub(id).cancel()
    //       Cooperative. Unblocks the workflow with a CanceledFailure so the Saga can run its
    //       compensation. This is what you want almost always.
    //
    //   client.newUntypedWorkflowStub(id).terminate("reason")
    //       Hard kill. NO compensation runs, no further history is written. Use it on a
    //       poisoned execution that cannot make progress - the stuck-on-non-determinism case.
}
