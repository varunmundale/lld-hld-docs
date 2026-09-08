package org.example.temporal;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.util.List;

/**
 * Activities are the only code allowed to touch the outside world: HTTP, DB, the clock,
 * randomness, a push gateway, a PSP.
 *
 * THE CONTRACT YOU MUST INTERNALISE:
 *
 *   Temporal guarantees AT-LEAST-ONCE execution of an activity. Never exactly-once. It cannot
 *   guarantee exactly-once and neither can anything else, because "the activity ran and the
 *   ack was lost" is indistinguishable, from the server's side, from "the activity never ran".
 *   Both look like silence. The server's only correct move on silence is to try again.
 *
 *   Therefore: every activity below that changes the world takes an EXPLICIT KEY, minted by
 *   the workflow, recorded in history, and therefore identical across every retry and every
 *   replay. The activity's job is to make "run twice with the same key" indistinguishable
 *   from "run once".
 *
 * Read the signatures with that in mind. The pure reads (findNearbyDrivers, quoteFare) take
 * no key because re-running them costs nothing. Everything that moves money, buzzes a phone,
 * or claims a driver does.
 */
@ActivityInterface
public interface RideActivities {

    // ---- pure reads: safe to run any number of times ---------------------------------------

    @ActivityMethod
    ValidationResult validateRider(RideRequest request);

    @ActivityMethod
    long quoteFare(RideRequest request);

    /** The supply service. alreadyTried keeps the loop from re-offering to the same drivers. */
    @ActivityMethod
    List<Driver> findNearbyDrivers(RideRequest request, int radiusKm, List<String> alreadyTried);

    /** Added later in the product's life - see the getVersion block in RideWorkflowImpl. */
    @ActivityMethod
    String checkDriverSafetyRating(String driverId);

    // ---- effectful: keyed, and idempotent on that key ---------------------------------------

    /**
     * Buzzes a real phone. offerId is the dedupe key: the driver app has already seen this id
     * if a previous attempt got through before its ack was lost, and drops the duplicate
     * instead of stacking a second identical card on the screen.
     */
    @ActivityMethod
    void pushOffer(String offerId, String driverId, RideRequest request, long fareCents, int ttlSeconds);

    /** Idempotent by construction: withdrawing an already-withdrawn offer is a no-op. */
    @ActivityMethod
    void withdrawOffer(String offerId, String driverId);

    /**
     * Claims the driver in the fleet service. Returns false if the driver was taken by ANOTHER
     * ride's workflow in the moment between their tap and this call.
     *
     * This is the one race the workflow cannot serialize away, because the two contenders are
     * two different workflow executions. The fix is not in Temporal: it is a compare-and-set
     * in the fleet service on (driverId: free -> rideId), idempotent for the same rideId.
     * See README, "the race Temporal does not solve for you".
     */
    @ActivityMethod
    boolean reserveDriver(String rideId, String driverId);

    /** Compensation for reserveDriver. Idempotent on (rideId, driverId). */
    @ActivityMethod
    void releaseDriver(String rideId, String driverId);

    /** Puts a hold on the rider's card. Keyed. Runs on the ride-payments task queue. */
    @ActivityMethod
    PaymentAuth authorizeFare(String idempotencyKey, String riderId, long amountCents);

    /** Compensation for authorizeFare. */
    @ActivityMethod
    void voidAuth(String authId);

    /** Settles the hold for the real fare. Its own key: a capture is not an auth. */
    @ActivityMethod
    String captureFare(String idempotencyKey, String authId, long amountCents);

    /** Its own key again. A cancellation fee must never ride on the auth's key. */
    @ActivityMethod
    String chargeCancellationFee(String idempotencyKey, String riderId, long amountCents);

    @ActivityMethod
    void notifyRider(String rideId, String message);

    @ActivityMethod
    String writeReceipt(String rideId, String captureId, long amountCents);
}
