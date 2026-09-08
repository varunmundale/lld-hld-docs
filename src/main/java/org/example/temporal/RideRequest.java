package org.example.temporal;

/**
 * What the rider tapped. rideId is also the workflowId, which is the first and cheapest
 * idempotency boundary in the whole system: two taps of "Request" send the same rideId, and
 * the second StartWorkflowExecution is rejected by the server with WorkflowExecutionAlreadyStarted.
 * No dedupe table, no Redis SETNX - see README, "four layers of idempotency".
 */
public record RideRequest(String rideId,
                          String riderId,
                          String city,
                          Location pickup,
                          Location dropoff,
                          String product) {

    public double tripKm() {
        return pickup.kmFrom(dropoff);
    }

    public String display() {
        // Locale.ROOT, not the default: this string is built inside workflow code, and a
        // worker with a different default locale would render "1,4km" instead of "1.4km".
        return rideId + " " + riderId + " " + pickup.label() + " -> " + dropoff.label()
                + " (" + String.format(java.util.Locale.ROOT, "%.1f", tripKm())
                + "km, " + product + ")";
    }
}
