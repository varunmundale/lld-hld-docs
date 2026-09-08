package org.example.temporal;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.common.WorkflowExecutionHistory;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.testing.WorkflowReplayer;
import io.temporal.worker.Worker;

import java.time.Duration;

/**
 * The test you actually need in CI, and the one most teams discover they needed the hard way.
 *
 * A workflow's source code is not the source of truth. HISTORY is. When a worker picks up a
 * ride that started an hour ago, it rebuilds the workflow's state by re-running your code
 * against the recorded history and checking that every command still lines up. If your new
 * deploy calls activities in a different order, or skips one, or adds one, replay diverges and
 * that ride wedges with a NonDeterministicException.
 *
 * So: keep a corpus of real histories - from production, per workflow type - and replay it
 * against the candidate build before shipping. This class shows the mechanic on a history it
 * generates itself.
 *
 *   mvn -q compile exec:java -Dexec.mainClass=org.example.temporal.ReplayCheck
 *
 * To watch it FAIL the way it is supposed to, delete the getVersion block in
 * RideWorkflowImpl (or move the checkDriverSafetyRating call above reserveDriver) and run it
 * again against a history captured before the change.
 */
public final class ReplayCheck {

    public static void main(String[] args) throws Exception {
        System.setProperty("gateway.ledger", "target/replay-gateway-ledger.txt");
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "error");
        PaymentGateway.reset();

        WorkflowExecutionHistory history = captureHistory();
        System.out.println("captured a completed ride: " + history.getEvents().size() + " events");

        try {
            WorkflowReplayer.replayWorkflowExecution(history, RideWorkflowImpl.class);
            System.out.println("REPLAY PASSES - this build can safely take over rides that are "
                    + "already in flight");
        } catch (Exception e) {
            System.out.println("REPLAY FAILS - shipping this build would wedge every ride that "
                    + "is mid-flight:");
            System.out.println("  " + e.getMessage());
            System.exit(1);
        }
        System.exit(0);
    }

    /** Runs one ride end to end on the test server and hands back its history. */
    private static WorkflowExecutionHistory captureHistory() {
        TestWorkflowEnvironment env = TestWorkflowEnvironment.newInstance();
        try {
            DriverFleet fleet = new DriverFleet();
            PaymentGateway psp = new PaymentGateway();
            RideActivitiesImpl activities = new RideActivitiesImpl(fleet, psp);
            String queue = TaskQueues.dispatch("sfo");

            Worker dispatch = env.newWorker(queue);
            dispatch.registerWorkflowImplementationTypes(RideWorkflowImpl.class);
            dispatch.registerActivitiesImplementations(activities);
            env.newWorker(TaskQueues.NOTIFICATIONS).registerActivitiesImplementations(activities);
            env.newWorker(TaskQueues.PAYMENTS).registerActivitiesImplementations(activities);
            env.start();

            WorkflowClient client = env.getWorkflowClient();
            String rideId = "RIDE-REPLAY";
            RideWorkflow workflow = client.newWorkflowStub(RideWorkflow.class,
                    WorkflowOptions.newBuilder()
                            .setTaskQueue(queue)
                            .setWorkflowId(rideId)
                            .setWorkflowExecutionTimeout(Duration.ofHours(6))
                            .build());

            env.registerDelayedCallback(Duration.ofSeconds(6), () -> {
                RideWorkflow stub = client.newWorkflowStub(RideWorkflow.class, rideId);
                stub.acceptOffer(DriverResponse.of(fleet.openOfferFor("D-101"), "D-101"));
            });
            env.registerDelayedCallback(Duration.ofSeconds(90), () ->
                    client.newWorkflowStub(RideWorkflow.class, rideId).driverArrived());
            env.registerDelayedCallback(Duration.ofSeconds(120), () ->
                    client.newWorkflowStub(RideWorkflow.class, rideId).tripStarted());
            env.registerDelayedCallback(Duration.ofSeconds(900), () ->
                    client.newWorkflowStub(RideWorkflow.class, rideId).tripEnded(4390));

            WorkflowClient.start(workflow::requestRide,
                    new RideRequest(rideId, "R-4471", "sfo",
                            new Location("Ferry Building", 37.7955, -122.3937),
                            new Location("SFO Terminal 2", 37.6213, -122.3790), "UberX"));

            WorkflowStub untyped = WorkflowStub.fromTyped(workflow);
            System.out.println("result: " + untyped.getResult(String.class));
            return env.getWorkflowExecutionHistory(untyped.getExecution());
        } finally {
            env.close();
        }
    }
}
