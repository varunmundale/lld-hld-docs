package org.example.temporal;

import io.temporal.common.WorkflowExecutionHistory;
import io.temporal.testing.WorkflowReplayer;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The safety net for the versioning pitfall.
 *
 * Export a real execution's history and replay it against the CURRENT workflow code. If the
 * code has drifted in a way that would break executions already in flight, this throws
 * instead of production throwing at 3am on an execution that then cannot progress.
 *
 *   temporal workflow show --workflow-id INV-1001 --output json > history.json
 *   mvn -q compile exec:java \
 *       -Dexec.mainClass=org.example.temporal.ReplayCheck \
 *       -Dexec.args="history.json"
 *
 * In a real repo this is a JUnit test over a directory of archived histories, wired into CI,
 * and it is the single highest-value test you can have on a Temporal codebase. Try it: run it
 * once against a history recorded before the "add-fraud-check" patch, then delete the
 * Workflow.getVersion() guard in InvoiceWorkflowImpl and run it again.
 */
public final class ReplayCheck {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("usage: ReplayCheck <history.json> [more.json ...]");
            return;
        }

        int failures = 0;
        for (String arg : args) {
            Path path = Path.of(arg);
            try {
                WorkflowExecutionHistory history =
                        WorkflowExecutionHistory.fromJson(Files.readString(path));
                WorkflowReplayer.replayWorkflowExecution(history, InvoiceWorkflowImpl.class);
                System.out.println("OK        " + path
                        + "  - current code is replay-compatible with this execution");
            } catch (Exception e) {
                failures++;
                System.out.println("BROKEN    " + path);
                System.out.println("          " + e.getClass().getSimpleName() + ": " + e.getMessage());
                System.out.println("          Deploying this code would STICK that execution. Guard the");
                System.out.println("          change with Workflow.getVersion(), or pin old runs to old");
                System.out.println("          workers with build-id versioning, and re-run this check.");
            }
        }
        System.exit(failures == 0 ? 0 : 1);
    }
}
