package org.example.temporal;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerOptions;

import java.util.List;

/**
 * The worker: YOUR process, hosting YOUR code, long-polling the Temporal server for tasks.
 *
 * The direction of that arrow is the architectural point. The server never calls into this
 * process. It has no address for it, no dependency on its language, and no route to it through
 * a firewall. Workers PULL. That is what makes the SDK polyglot, makes backpressure automatic
 * (nobody polling -> tasks just queue), and keeps your dependencies out of the engine.
 *
 * Run one of these per pool. In production these are three separate deployments; here they are
 * three main() invocations so you can kill one and watch what happens to the others.
 *
 *   mvn -q compile exec:java -Dexec.mainClass=org.example.temporal.RideWorker -Dexec.args=all
 *   mvn -q compile exec:java -Dexec.mainClass=org.example.temporal.RideWorker -Dexec.args="dispatch sfo"
 *   mvn -q compile exec:java -Dexec.mainClass=org.example.temporal.RideWorker -Dexec.args=payments
 *   mvn -q compile exec:java -Dexec.mainClass=org.example.temporal.RideWorker -Dexec.args=notifications
 */
public final class RideWorker {

    public static void main(String[] args) {
        String role = args.length > 0 ? args[0] : "all";
        String city = args.length > 1 ? args[1] : "sfo";

        // newLocalServiceStubs() targets 127.0.0.1:7233, which is what `temporal server
        // start-dev` listens on. For a real cluster, build WorkflowServiceStubsOptions with
        // the target endpoint, TLS and the namespace.
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);
        WorkerFactory factory = WorkerFactory.newInstance(client);

        DriverFleet fleet = new DriverFleet();
        PaymentGateway psp = new PaymentGateway();
        RideActivitiesImpl activities = new RideActivitiesImpl(fleet, psp);
        List<String> polling = new java.util.ArrayList<>();

        if (role.equals("all") || role.equals("dispatch")) {
            String queue = TaskQueues.dispatch(city);
            Worker w = factory.newWorker(queue, WorkerOptions.newBuilder()
                    // Two independent knobs. Sizing them together is a common mistake: a slow
                    // activity must not be able to starve workflow tasks, which are short,
                    // CPU-only replays and are on the critical path of every ride.
                    .setMaxConcurrentWorkflowTaskExecutionSize(200)
                    .setMaxConcurrentActivityExecutionSize(100)
                    .build());
            // The workflow is registered as a CLASS: the SDK constructs one instance per
            // execution and replays it. Activities are registered as an INSTANCE, shared across
            // every execution on this worker, so keep them thread-safe and stateless.
            w.registerWorkflowImplementationTypes(RideWorkflowImpl.class);
            w.registerActivitiesImplementations(activities);
            polling.add(queue);
        }

        if (role.equals("all") || role.equals("payments")) {
            Worker w = factory.newWorker(TaskQueues.PAYMENTS, WorkerOptions.newBuilder()
                    // Small pool: the PSP is the bottleneck, not us, and this is the per-worker
                    // ceiling that stops a downstream blip becoming a self-inflicted retry storm.
                    .setMaxConcurrentActivityExecutionSize(20)
                    .setMaxTaskQueueActivitiesPerSecond(50)
                    .build());
            w.registerActivitiesImplementations(activities);
            polling.add(TaskQueues.PAYMENTS);
        }

        if (role.equals("all") || role.equals("notifications")) {
            Worker w = factory.newWorker(TaskQueues.NOTIFICATIONS, WorkerOptions.newBuilder()
                    // Huge pool: each task is a few milliseconds of HTTP, and falling behind
                    // here delays offer cards, which is the one thing dispatch cannot tolerate.
                    .setMaxConcurrentActivityExecutionSize(500)
                    .build());
            w.registerActivitiesImplementations(activities);
            polling.add(TaskQueues.NOTIFICATIONS);
        }

        if (polling.isEmpty()) {
            System.out.println("usage: RideWorker [all|dispatch <city>|payments|notifications]");
            return;
        }

        System.out.println("polling: " + polling);
        System.out.println("web UI:  http://localhost:8233");
        factory.start();     // blocks
    }
}
