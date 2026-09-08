package org.example.temporal;

import io.temporal.activity.Activity;
import io.temporal.activity.ActivityExecutionContext;
import io.temporal.activity.ActivityInfo;
import io.temporal.failure.ApplicationFailure;

import java.util.List;

/**
 * The activity implementations. One shared instance across every execution on a worker, so
 * keep it thread-safe and stateless - all the mutable state lives in the injected services.
 *
 * FAULT INJECTION, driven by -D system properties, because the failures are the whole point:
 *
 *   -Dfail.<activity>=N         throw on attempts 1..N BEFORE doing the work
 *                               ("the call never landed")
 *   -Dfail.after.<activity>=N   do the work, THEN throw, on attempts 1..N
 *                               ("the call landed and the ack was lost" - the dangerous one)
 *   -Dfail.only.driver=D-101    scope push failures to one driver, so the trace stays readable
 *   -Dfail.declined=true        the PSP declines the card, non-retryably
 *
 * The distinction between the first two is the entire reason idempotency keys exist. Temporal
 * sees the same thing in both cases - silence - and its only correct response to silence is to
 * try again.
 */
public class RideActivitiesImpl implements RideActivities {

    private final DriverFleet fleet;
    private final PaymentGateway psp;

    public RideActivitiesImpl(DriverFleet fleet, PaymentGateway psp) {
        this.fleet = fleet;
        this.psp = psp;
    }

    // ---- pure reads ---------------------------------------------------------------------------

    @Override
    public ValidationResult validateRider(RideRequest r) {
        trace("validateRider", r.riderId());
        if (r.riderId().startsWith("BLOCKED")) {
            // Non-retryable on purpose: a blocked account is not a transient condition, and
            // the workflow's retry policy names this exact type in doNotRetry.
            throw ApplicationFailure.newNonRetryableFailure(
                    "rider " + r.riderId() + " is blocked", "RiderBlocked");
        }
        if (Boolean.getBoolean("rider.nopayment")) {
            return new ValidationResult(false, "no payment method on file");
        }
        return new ValidationResult(true, null);
    }

    @Override
    public long quoteFare(RideRequest r) {
        maybeFail("quoteFare");
        long fare = 350 + Math.round(r.tripKm() * 210);   // base + per km, in cents
        trace("quoteFare", PaymentGateway.money(fare));
        return fare;
    }

    @Override
    public List<Driver> findNearbyDrivers(RideRequest r, int radiusKm, List<String> alreadyTried) {
        maybeFail("findNearbyDrivers");
        List<Driver> found = fleet.nearby(r.pickup(), radiusKm, alreadyTried);
        trace("findNearbyDrivers", "radius=" + radiusKm + "km tried=" + alreadyTried.size()
                + " -> " + found.size() + " candidates");
        return found;
    }

    @Override
    public String checkDriverSafetyRating(String driverId) {
        trace("checkDriverSafetyRating", driverId);
        return "OK";
    }

    // ---- effectful, keyed ---------------------------------------------------------------------

    @Override
    public void pushOffer(String offerId, String driverId, RideRequest r, long fareCents, int ttl) {
        String only = System.getProperty("fail.only.driver");
        boolean scoped = only == null || only.equals(driverId);

        trace("pushOffer", driverId + " " + offerId);
        if (scoped) maybeFail("pushOffer");

        fleet.deliver(offerId, driverId, r, fareCents);

        // "the push landed, then the worker died before it could report success"
        if (scoped) maybeFailAfter("pushOffer");
    }

    @Override
    public void withdrawOffer(String offerId, String driverId) {
        trace("withdrawOffer", driverId + " " + offerId);
        fleet.withdraw(offerId, driverId);
    }

    @Override
    public boolean reserveDriver(String rideId, String driverId) {
        trace("reserveDriver", rideId + " " + driverId);
        return fleet.reserve(rideId, driverId);
    }

    @Override
    public void releaseDriver(String rideId, String driverId) {
        trace("releaseDriver", rideId + " " + driverId);
        fleet.release(rideId, driverId);
    }

    @Override
    public PaymentAuth authorizeFare(String key, String riderId, long amountCents) {
        trace("authorizeFare", "key=" + key);
        maybeFail("authorizeFare");
        if (Boolean.getBoolean("fail.declined")) {
            throw ApplicationFailure.newNonRetryableFailure("card declined", "CardDeclined");
        }
        PaymentAuth auth = psp.authorize(key, amountCents);
        // The money has moved. Now lose the ack.
        maybeFailAfter("authorizeFare");
        return auth;
    }

    @Override
    public void voidAuth(String authId) {
        trace("voidAuth", authId);
        psp.voidAuth(authId);
    }

    @Override
    public String captureFare(String key, String authId, long amountCents) {
        trace("captureFare", "key=" + key);
        maybeFail("captureFare");
        String id = psp.capture(key, authId, amountCents);
        maybeFailAfter("captureFare");
        return id;
    }

    @Override
    public String chargeCancellationFee(String key, String riderId, long amountCents) {
        trace("chargeCancellationFee", "key=" + key);
        return psp.charge(key, amountCents, "cancellation fee");
    }

    @Override
    public void notifyRider(String rideId, String message) {
        trace("notifyRider", message);
    }

    @Override
    public String writeReceipt(String rideId, String captureId, long amountCents) {
        trace("writeReceipt", PaymentGateway.money(amountCents));
        return "rcpt_" + Integer.toHexString((rideId + captureId).hashCode());
    }

    // ---- plumbing -----------------------------------------------------------------------------

    /**
     * Prints the activity, its attempt number, and - the part worth watching - the TASK QUEUE
     * it was dispatched on. Three different worker pools show up in this trace, and which one
     * ran a given activity was decided by one line of ActivityOptions.
     */
    private void trace(String activity, String detail) {
        ActivityInfo info = Activity.getExecutionContext().getInfo();
        System.out.printf("    [%-20s] %-24s attempt=%d  %s%n",
                info.getActivityTaskQueue(), activity, info.getAttempt(), detail);
    }

    private void maybeFail(String activity) {
        int n = Integer.getInteger("fail." + activity, 0);
        if (attempt() <= n) {
            System.out.println("      !! " + activity + " attempt " + attempt()
                    + " fails BEFORE doing anything (the call never landed)");
            throw ApplicationFailure.newFailure(activity + " upstream timeout", "Transient");
        }
    }

    private void maybeFailAfter(String activity) {
        int n = Integer.getInteger("fail.after." + activity, 0);
        if (attempt() <= n) {
            System.out.println("      !! " + activity + " attempt " + attempt()
                    + " DID THE WORK, then the ack was lost -> Temporal will retry it");
            throw ApplicationFailure.newFailure(activity + " ack lost", "Transient");
        }
    }

    private int attempt() {
        ActivityExecutionContext ctx = Activity.getExecutionContext();
        return ctx.getInfo().getAttempt();
    }
}
