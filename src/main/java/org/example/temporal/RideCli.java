package org.example.temporal;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowExecutionAlreadyStarted;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.serviceclient.WorkflowServiceStubs;

import java.time.Duration;
import java.util.List;

/**
 * Drives a ride against a real `temporal server start-dev`, so you can watch it in the web UI
 * and kill -9 a worker in the middle.
 *
 *   temporal server start-dev                      # https://temporal.io/cli, UI on :8233
 *   RideWorker all
 *
 *   RideCli request RIDE-1
 *   RideCli status  RIDE-1
 *   RideCli log     RIDE-1                          # every offer this ride has made
 *   RideCli accept  RIDE-1 D-101 offer-....         # offerId is printed by the worker
 *   RideCli decline RIDE-1 D-101 offer-....
 *   RideCli arrive  RIDE-1
 *   RideCli start   RIDE-1
 *   RideCli end     RIDE-1 4390
 *   RideCli cancel  RIDE-1 "changed my mind"
 *
 * Try this: `RideCli request RIDE-1` twice. The second one is refused by the SERVER, not by
 * your code, because workflowId is the rideId. That is idempotency layer one, and it costs a
 * single WorkflowOptions line.
 */
public final class RideCli {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("usage: RideCli <request|status|log|accept|decline|arrive|start|"
                    + "end|cancel> <rideId> [args]");
            return;
        }
        String command = args[0];
        String rideId = args[1];

        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);

        if (command.equals("request")) {
            String city = args.length > 2 ? args[2] : "sfo";
            RideRequest request = new RideRequest(rideId, "R-4471", city,
                    new Location("Ferry Building", 37.7955, -122.3937),
                    new Location("SFO Terminal 2", 37.6213, -122.3790),
                    "UberX");

            RideWorkflow workflow = client.newWorkflowStub(RideWorkflow.class,
                    WorkflowOptions.newBuilder()
                            // The routing decision: which pool will run this ride, forever.
                            .setTaskQueue(TaskQueues.dispatch(city))
                            // The dedupe key: a double tap cannot start a second ride.
                            .setWorkflowId(rideId)
                            .setWorkflowExecutionTimeout(Duration.ofHours(6))
                            .build());
            try {
                WorkflowClient.start(workflow::requestRide, request);
                System.out.println("started " + rideId + " on queue "
                        + TaskQueues.dispatch(city));
            } catch (WorkflowExecutionAlreadyStarted e) {
                System.out.println(rideId + " is already running - the SERVER rejected the "
                        + "duplicate start. No dedupe table needed.");
            }
            return;
        }

        RideWorkflow workflow = client.newWorkflowStub(RideWorkflow.class, rideId);

        switch (command) {
            case "status" -> {
                System.out.println("status: " + workflow.status());
                System.out.println("driver: " + workflow.assignedDriver());
            }
            case "log" -> {
                List<String> entries = workflow.dispatchLog();
                entries.forEach(l -> System.out.println("  " + l));
                if (entries.isEmpty()) System.out.println("  (no offers yet)");
            }
            case "accept" -> {
                // Synchronous, because the phone needs the answer on this call.
                AcceptResult r = workflow.acceptOffer(DriverResponse.of(args[3], args[2]));
                System.out.println(r.outcome() + ": " + r.note());
                if (r.won()) {
                    System.out.println("  pickup " + r.pickupLabel()
                            + " -> " + r.dropoffLabel() + "  " + PaymentGateway.money(r.fareCents()));
                }
            }
            case "decline" -> {
                workflow.declineOffer(new DriverResponse(args[3], args[2], "too far"));
                System.out.println("declined");
            }
            case "arrive" -> { workflow.driverArrived(); System.out.println("arrival recorded"); }
            case "start"  -> { workflow.tripStarted();  System.out.println("trip started"); }
            case "end"    -> {
                workflow.tripEnded(Long.parseLong(args[2]));
                System.out.println("trip ended, waiting for the final result...");
                System.out.println(WorkflowStub.fromTyped(workflow).getResult(String.class));
            }
            case "cancel" -> {
                workflow.riderCancel(args.length > 2 ? args[2] : "no reason given");
                System.out.println("cancel signalled");
            }
            default -> System.out.println("unknown command " + command);
        }
        System.exit(0);
    }
}
