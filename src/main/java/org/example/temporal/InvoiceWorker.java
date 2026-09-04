package org.example.temporal;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerOptions;

/**
 * The worker: YOUR process, hosting YOUR code, long-polling the Temporal server for tasks.
 *
 * The direction of that arrow is the architectural point. The server never calls into this
 * process - it has no address for it, no dependency on its language, and no way to reach it
 * through a firewall. Workers pull. That is what makes the SDK polyglot, makes backpressure
 * automatic (no pollers -> tasks just queue), and keeps your dependencies out of the engine.
 *
 * Run:  mvn -q compile exec:java -Dexec.mainClass=org.example.temporal.InvoiceWorker
 */
public final class InvoiceWorker {

    public static void main(String[] args) {
        // newLocalServiceStubs() targets 127.0.0.1:7233, which is what `temporal server
        // start-dev` listens on. For a real cluster, build WorkflowServiceStubsOptions with
        // the target endpoint, TLS and the namespace.
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);
        WorkerFactory factory = WorkerFactory.newInstance(client);

        Worker worker = factory.newWorker(
                InvoiceWorkflow.TASK_QUEUE,
                WorkerOptions.newBuilder()
                        // Two independent knobs. Sizing them together is a common mistake:
                        // a slow activity should not be able to starve workflow tasks, which
                        // are short, CPU-only replays.
                        .setMaxConcurrentWorkflowTaskExecutionSize(50)
                        .setMaxConcurrentActivityExecutionSize(20)
                        // A per-worker ceiling on activity dispatch rate. This, plus the
                        // retry policy on the stub, is what stops a downstream blip from
                        // becoming a self-inflicted retry storm.
                        .setMaxTaskQueueActivitiesPerSecond(50)
                        .build());

        // The workflow is registered as a CLASS - the SDK constructs one instance per
        // execution and replays it. The activities are registered as an INSTANCE, shared
        // across executions, so keep them thread-safe and stateless.
        worker.registerWorkflowImplementationTypes(InvoiceWorkflowImpl.class);
        worker.registerActivitiesImplementations(new InvoiceActivitiesImpl(new PaymentGateway()));

        System.out.println("worker polling task queue '" + InvoiceWorkflow.TASK_QUEUE + "'");
        System.out.println("web UI: http://localhost:8233");
        if (System.getProperty("crash.after") != null) {
            System.out.println("!! crash.after=" + System.getProperty("crash.after")
                    + " - this worker will halt mid-activity on purpose");
        }

        factory.start();     // blocks
    }
}
