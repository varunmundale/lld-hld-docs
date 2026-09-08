package org.example.temporal;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.UpdateMethod;
import io.temporal.workflow.UpdateValidatorMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.time.Duration;

/**
 * One workflow execution per ride request. workflowId = rideId.
 *
 * FOUR kinds of entry point, and picking the right one for each real-world event is most of
 * the design:
 *
 *   @WorkflowMethod  Starts the execution. Exactly one. The rider tapping "Request".
 *
 *   @UpdateMethod    A durable request that RETURNS A VALUE. The driver tapping ACCEPT: the
 *                    phone has to be told, on that same call, whether it won the ride or
 *                    whether someone else got there first. The update is written to history
 *                    before the handler runs, so an accept cannot be lost by a worker crash,
 *                    and the caller still gets an answer.
 *
 *   @SignalMethod    A durable fire-and-forget event. DECLINE, the rider cancelling, the trip
 *                    starting and ending. Nobody is waiting on a return value, so do not pay
 *                    for one.
 *
 *   @QueryMethod     A read of live state with no history write at all. "Where is my driver?"
 *                    is asked by the rider's app every two seconds, by millions of riders.
 *                    Served by replaying history on any worker; must not block, must not call
 *                    an activity, must not mutate anything.
 *
 * The single most valuable property of this interface: every one of these runs on ONE workflow
 * thread, serialized by the server into ONE history. Two drivers accepting the same ride in
 * the same millisecond are not a distributed-locking problem here. They are two entries in a
 * list, processed one after the other, and the second one sees the state the first one wrote.
 */
@WorkflowInterface
public interface RideWorkflow {

    /** How long an offer card stays live on a driver's phone before it is withdrawn. */
    Duration OFFER_TTL = Duration.ofSeconds(15);

    /** How many drivers get the same ride offered at once. Uber calls this a broadcast round. */
    int BROADCAST_FANOUT = 3;

    /** Give up and tell the rider there are no cars after this. */
    Duration DISPATCH_DEADLINE = Duration.ofMinutes(5);

    /** Start radius for the supply query, doubled each round that comes up empty. */
    int INITIAL_RADIUS_KM = 2;

    @WorkflowMethod
    String requestRide(RideRequest request);

    /**
     * The driver tapped ACCEPT. Returns WON / TOO_LATE / EXPIRED / RIDE_CANCELLED.
     *
     * TOO_LATE is a normal return value, not an exception: losing the race is an expected
     * business outcome that the phone needs to render, not an error anyone should page on.
     */
    @UpdateMethod
    AcceptResult acceptOffer(DriverResponse response);

    /**
     * Runs BEFORE the update is written to history. A rejection here costs nothing and leaves
     * no trace - which is exactly what you want for garbage input from a stale app build.
     * It must be deterministic and side-effect free, and it must not mutate workflow state.
     *
     * Note what is deliberately NOT rejected here: an accept for an expired offer. That is a
     * real event, from a real driver, that we want on the record and want to answer properly.
     */
    @UpdateValidatorMethod(updateName = "acceptOffer")
    void validateAcceptOffer(DriverResponse response);

    /** The driver tapped DECLINE, or swiped the card away. */
    @SignalMethod
    void declineOffer(DriverResponse response);

    /** The rider tapped Cancel. Legal at any point; what it costs depends on where we are. */
    @SignalMethod
    void riderCancel(String reason);

    /** Driver's phone crossed the pickup geofence. */
    @SignalMethod
    void driverArrived();

    /** Rider is in the car. */
    @SignalMethod
    void tripStarted();

    /** Car stopped at the destination. The real fare is known only now. */
    @SignalMethod
    void tripEnded(long actualFareCents);

    /** What the rider's map is showing right now. */
    @QueryMethod
    String status();

    /** null until a driver has won the race. */
    @QueryMethod
    String assignedDriver();

    /** Every offer this ride has made, in order. Useful when someone asks "why 4 minutes?". */
    @QueryMethod
    java.util.List<String> dispatchLog();
}
