package org.example.temporal;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.workflow.Async;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Saga;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Rider requests a ride, drivers are offered it, one accepts, the trip runs, the card is
 * settled - written as ordinary sequential code.
 *
 * What is NOT in this file, and would be in every hand-rolled version of it: a ride_state
 * column, a dispatch_offers table with a cron scanning it for expiry, a Redis lock keyed on
 * driverId, an outbox, a "resume from step N" switch, a retry loop, a dead-letter consumer.
 * The absence of all of that is the point.
 *
 * THE ONE RULE: this method must be DETERMINISTIC. Replay must make the same calls in the
 * same order given the same history.
 *
 *   NO   System.currentTimeMillis(), new Random(), UUID.randomUUID(), Thread.sleep(),
 *        new Thread(), HTTP/JDBC/file access, HashMap/HashSet iteration, env or config reads,
 *        String.format with the default locale
 *   YES  Workflow.currentTimeMillis(), Workflow.newRandom(), Workflow.randomUUID(),
 *        Workflow.sleep(), Workflow.await(), Async.function(), LinkedHashMap/LinkedHashSet,
 *        and an activity for anything that touches the world
 *
 * Break it and the bug does not surface where you wrote it. It surfaces months later on a
 * replay, as a NonDeterministicException, on a ride that is now wedged.
 */
public class RideWorkflowImpl implements RideWorkflow {

    private static final Logger log = Workflow.getLogger(RideWorkflowImpl.class);

    private static final long CANCELLATION_FEE_CENTS = 500L;
    private static final int MAX_RADIUS_KM = 16;

    /**
     * Dispatch activities. No task queue set, so these INHERIT the workflow's queue -
     * ride-dispatch-<city>. That is what you want: matching is regional, latency-critical,
     * and reads a fleet cache that only the regional workers have warm.
     */
    private final RideActivities dispatch = Workflow.newActivityStub(
            RideActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(10))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setInitialInterval(Duration.ofMillis(200))
                            .setBackoffCoefficient(2.0)
                            .setMaximumInterval(Duration.ofSeconds(5))
                            .setMaximumAttempts(5)
                            // A blocked rider is not a blip. Retrying a 403 four times just
                            // makes the rejection slower and the on-call noisier.
                            .setDoNotRetry("RiderBlocked")
                            .build())
                    .build());

    /**
     * Push notifications, routed to their own queue by name.
     *
     * Note setScheduleToStartTimeout. It fires only when a task SAT IN THE QUEUE with nobody
     * polling. It is the one timeout that unambiguously means "not enough workers" and never
     * "the code is slow", which makes it the one worth paging on. Five seconds, because the
     * offer card is only live for fifteen: a push that arrives later than that is delivering
     * an offer the ride has already moved past. Failing fast is strictly better.
     */
    private final RideActivities notifications = Workflow.newActivityStub(
            RideActivities.class,
            ActivityOptions.newBuilder()
                    .setTaskQueue(TaskQueues.NOTIFICATIONS)
                    .setStartToCloseTimeout(Duration.ofSeconds(5))
                    .setScheduleToStartTimeout(Duration.ofSeconds(5))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setInitialInterval(Duration.ofMillis(200))
                            .setMaximumAttempts(3)
                            .build())
                    .build());

    /**
     * Money. Its own queue, its own pool, its own credentials, its own deploy cadence.
     * Longer timeouts because a PSP takes seconds; fewer attempts because a wedged PSP must
     * not be hammered; a scheduleToClose ceiling so a hold cannot be retried into next week.
     * Deliberately NO scheduleToStart timeout: if the payments pool is down we would rather
     * the ride wait than fail, because dispatch is already done and a driver is en route.
     */
    private final RideActivities payments = Workflow.newActivityStub(
            RideActivities.class,
            ActivityOptions.newBuilder()
                    .setTaskQueue(TaskQueues.PAYMENTS)
                    .setStartToCloseTimeout(Duration.ofSeconds(30))
                    .setScheduleToCloseTimeout(Duration.ofMinutes(10))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setInitialInterval(Duration.ofSeconds(1))
                            .setBackoffCoefficient(2.0)
                            .setMaximumAttempts(4)
                            .setDoNotRetry("CardDeclined")
                            .build())
                    .build());

    // ---- state. All of it lives here, in memory, and is reconstructed by replay. -----------

    /** offerId -> driverId for the round that is open RIGHT NOW. Ordered, for determinism. */
    private final Map<String, String> liveOffers = new LinkedHashMap<>();
    private final Set<String> declinedOffers = new LinkedHashSet<>();
    private final List<String> dispatchLog = new ArrayList<>();

    private RideRequest request;
    private long fareCents;
    private String acceptedDriverId;
    private String acceptedOfferId;
    private String cancelReason;
    private boolean driverArrived;
    private boolean tripStarted;
    private boolean tripEnded;
    private long actualFareCents;
    private int offersMade;
    private String status = "REQUESTED";

    // ========================================================================================

    @Override
    public String requestRide(RideRequest req) {
        this.request = req;
        log.info("ride requested: {}", req.display());

        status = "VALIDATING";
        ValidationResult v = dispatch.validateRider(req);
        if (!v.ok()) {
            status = "REJECTED";
            return "REJECTED " + req.rideId() + ": " + v.reason();
        }

        status = "QUOTING";
        this.fareCents = dispatch.quoteFare(req);

        // --- THE IDEMPOTENCY KEYS ----------------------------------------------------------
        // Minted HERE, once, before anything can fail. Workflow.randomUUID() is deterministic:
        // the value is written into history the first time this line runs, and every retry of
        // the activity and every replay after a crash reads the same value back.
        //
        // Writing java.util.UUID.randomUUID() here instead is the most expensive one-line bug
        // available in this file. A worker crash between the hold and its ack would mint a
        // fresh key on replay and the rider's card would be held twice.
        //
        // Three separate keys, because they are three separate effects. Reusing the auth key
        // for the capture would let the gateway suppress a real capture as a duplicate.
        String authKey = "auth-" + Workflow.randomUUID();
        String captureKey = "cap-" + Workflow.randomUUID();
        String cancelFeeKey = "fee-" + Workflow.randomUUID();

        // --- DISPATCH ----------------------------------------------------------------------
        String winner = findDriver(req);
        if (winner == null) {
            if (cancelReason != null) {
                status = "CANCELLED";
                notifications.notifyRider(req.rideId(), "ride cancelled: " + cancelReason);
                // Nothing was reserved and no money moved, so there is nothing to undo and
                // no fee to charge. Cancelling while still searching is free, by design.
                return "CANCELLED_BEFORE_MATCH " + req.rideId() + ": " + cancelReason;
            }
            status = "NO_DRIVERS";
            notifications.notifyRider(req.rideId(), "no cars available right now");
            return "NO_DRIVERS " + req.rideId() + " after " + DISPATCH_DEADLINE.toMinutes()
                    + " minutes and " + offersMade + " offers";
        }

        // --- VERSIONING --------------------------------------------------------------------
        // Adding a step to a workflow that has executions in flight would make their replay
        // diverge. getVersion writes a marker into history the first time it runs, so rides
        // that started before the deploy keep taking the old branch forever and new ones take
        // the new one. The cost is that this branch is permanent archaeology until every
        // pre-deploy ride has drained - which for a 20-minute ride is fast, and for a 90-day
        // subscription workflow is not.
        int version = Workflow.getVersion("add-safety-rating-check", Workflow.DEFAULT_VERSION, 1);
        if (version >= 1) {
            status = "SAFETY_CHECK";
            dispatch.checkDriverSafetyRating(winner);
        }

        // --- SAGA --------------------------------------------------------------------------
        // Compensation is a stack of undo calls, not a compensation table with a state machine.
        Saga saga = new Saga(new Saga.Options.Builder().setParallelCompensation(false).build());
        saga.addCompensation(dispatch::releaseDriver, req.rideId(), winner);

        try {
            status = "AUTHORIZING";
            PaymentAuth auth = payments.authorizeFare(authKey, req.riderId(), fareCents);
            saga.addCompensation(payments::voidAuth, auth.authId());
            if (auth.duplicateSuppressed()) {
                // Seeing this in the logs is a SUCCESS, not an incident. It means the activity
                // ran more than once - at-least-once, exactly as documented - and the pinned
                // key made the second run a no-op. One hold on the card.
                log.warn("gateway suppressed a duplicate hold for key {}: the activity ran more "
                        + "than once and the money moved exactly once", authKey);
            }

            status = "EN_ROUTE";
            notifications.notifyRider(req.rideId(), winner + " is on the way");

            // Each of these is a durable server-side timer plus a durable condition. No held
            // thread, no polling job, no ride_timeouts table with a cron over it. The worker
            // can be redeployed or scaled to zero in the middle of any of them.
            if (!Workflow.await(Duration.ofMinutes(15), () -> driverArrived || cancelReason != null)) {
                return unwind(saga, req, "DRIVER_NO_SHOW", "driver never arrived", null);
            }
            if (cancelReason != null) {
                return unwind(saga, req, "CANCELLED_AFTER_MATCH", cancelReason, cancelFeeKey);
            }

            status = "AWAITING_PICKUP";
            if (!Workflow.await(Duration.ofMinutes(10), () -> tripStarted || cancelReason != null)) {
                return unwind(saga, req, "RIDER_NO_SHOW", "rider never boarded", cancelFeeKey);
            }
            if (cancelReason != null) {
                return unwind(saga, req, "CANCELLED_AFTER_MATCH", cancelReason, cancelFeeKey);
            }

            status = "ON_TRIP";
            // Four hours is a ceiling, not an expectation. It costs one timer row.
            Workflow.await(Duration.ofHours(4), () -> tripEnded);

            status = "CAPTURING";
            long finalFare = tripEnded ? actualFareCents : fareCents;
            String captureId = payments.captureFare(captureKey, auth.authId(), finalFare);

            // Idempotent, so it is safe here even though the saga would also have called it.
            dispatch.releaseDriver(req.rideId(), winner);

            String receipt = dispatch.writeReceipt(req.rideId(), captureId, finalFare);
            notifications.notifyRider(req.rideId(), "trip complete, receipt " + receipt);

            status = "COMPLETED";
            return "COMPLETED " + req.rideId() + " driver=" + winner
                    + " fare=" + money(finalFare) + " capture=" + captureId + " receipt=" + receipt;

        } catch (ActivityFailure e) {
            status = "COMPENSATING";
            log.error("failed after a driver was reserved - unwinding", e);
            saga.compensate();
            status = "FAILED";
            throw e;      // a failed workflow is a real, queryable, permanent outcome
        }
    }

    /**
     * The dispatch loop: broadcast the ride to a handful of nearby drivers, wait out the offer
     * TTL, widen the radius, repeat until someone accepts or the deadline passes.
     *
     * Returns the winning driverId, or null if we gave up or the rider cancelled.
     */
    private String findDriver(RideRequest req) {
        long deadline = Workflow.currentTimeMillis() + DISPATCH_DEADLINE.toMillis();
        int radiusKm = INITIAL_RADIUS_KM;
        List<String> tried = new ArrayList<>();
        int round = 0;

        while (cancelReason == null && Workflow.currentTimeMillis() < deadline) {

            List<Driver> candidates = dispatch.findNearbyDrivers(req, radiusKm, tried);
            if (candidates.isEmpty()) {
                radiusKm = Math.min(radiusKm * 2, MAX_RADIUS_KM);
                status = "SEARCHING_" + radiusKm + "KM";
                dispatchLog.add("no candidates, widening to " + radiusKm + "km");
                // A cancellable sleep: an await with a condition is a sleep you can interrupt.
                Workflow.await(Duration.ofSeconds(10), () -> cancelReason != null);
                continue;
            }

            round++;
            List<Driver> batch = candidates.subList(0, Math.min(BROADCAST_FANOUT, candidates.size()));
            liveOffers.clear();

            // Fire the whole broadcast round in parallel. Async.procedure returns immediately
            // with a Promise; the SDK schedules all of them on one workflow task. Doing this
            // sequentially would add a full activity round-trip per driver to the time-to-match,
            // which is the single number Uber's dispatch team is judged on.
            List<Promise<Void>> pushes = new ArrayList<>();
            for (Driver d : batch) {
                String offerId = "offer-" + Workflow.randomUUID();   // pinned, replay-stable
                liveOffers.put(offerId, d.driverId());
                tried.add(d.driverId());
                dispatchLog.add("round " + round + " offered " + offerId + " to " + d.display());
                offersMade++;
                pushes.add(Async.procedure(notifications::pushOffer,
                        offerId, d.driverId(), req, fareCents, (int) OFFER_TTL.getSeconds()));
            }
            Promise.allOf(pushes).get();

            status = "OFFERED_ROUND_" + round;

            // Park until someone accepts, everyone declines, the rider cancels, or the offer
            // card expires. All four in one line, and every one of them survives a crash.
            Workflow.await(OFFER_TTL, () -> acceptedDriverId != null
                    || cancelReason != null
                    || declinedOffers.containsAll(liveOffers.keySet()));

            // Take the card off every phone that did not win. Skipping this is how drivers end
            // up staring at an offer for a ride that someone else is already driving.
            for (Map.Entry<String, String> e : liveOffers.entrySet()) {
                if (!e.getKey().equals(acceptedOfferId)) {
                    notifications.withdrawOffer(e.getKey(), e.getValue());
                }
            }
            // Emptying this is what turns a late accept into EXPIRED instead of a second winner.
            liveOffers.clear();

            if (acceptedDriverId != null) {
                status = "RESERVING";
                // The accept won the race INSIDE this workflow. The driver may still have been
                // claimed by a DIFFERENT ride's workflow in the same instant - see the comment
                // on RideActivities.reserveDriver. If so, drop them and keep dispatching.
                if (dispatch.reserveDriver(req.rideId(), acceptedDriverId)) {
                    dispatchLog.add("reserved " + acceptedDriverId);
                    return acceptedDriverId;
                }
                dispatchLog.add("lost " + acceptedDriverId + " to another ride at reserve time");
                acceptedDriverId = null;
                acceptedOfferId = null;
            }

            radiusKm = Math.min(radiusKm * 2, MAX_RADIUS_KM);
        }
        return null;
    }

    /** Unwind the saga, optionally charge a cancellation fee, and end deliberately. */
    private String unwind(Saga saga, RideRequest req, String outcome, String reason, String feeKey) {
        status = "COMPENSATING";
        saga.compensate();          // voidAuth, then releaseDriver - reverse order

        String fee = "none";
        if (feeKey != null) {
            // Its own key, so it can never be confused with the fare by the gateway.
            fee = payments.chargeCancellationFee(feeKey, req.riderId(), CANCELLATION_FEE_CENTS);
        }
        notifications.notifyRider(req.rideId(), outcome.toLowerCase() + ": " + reason);
        status = outcome;
        return outcome + " " + req.rideId() + " (" + reason + ") fee=" + fee;
    }

    private static String money(long cents) {
        return "$" + (cents / 100) + "." + (cents % 100 < 10 ? "0" : "") + (cents % 100);
    }

    // ---- update, signals, queries -----------------------------------------------------------

    @Override
    public void validateAcceptOffer(DriverResponse r) {
        // Runs before the update is written to history. Rejecting here leaves NO trace, which
        // is what you want for junk from a stale app build. Must not mutate state.
        if (r == null || r.offerId() == null || r.offerId().isBlank()
                || r.driverId() == null || r.driverId().isBlank()) {
            throw new IllegalArgumentException("offerId and driverId are required");
        }
    }

    /**
     * THE RACE, AND WHY THERE IS NO LOCK.
     *
     * Three drivers hold the same broadcast round. Two of them tap ACCEPT in the same
     * millisecond, from two phones, hitting two different frontend hosts, in two different
     * availability zones.
     *
     * The server writes both updates into ONE history, in some order, and hands them to ONE
     * workflow thread, which runs them ONE AT A TIME. The second one to run reads the field
     * the first one wrote. There is no lock to acquire, no CAS to get wrong, no lease to
     * renew, no lock to leak when a worker dies mid-critical-section - because there is no
     * critical section. Serialization is a property of the execution model.
     *
     * Everything below is just the ordinary business logic you would write single-threaded.
     */
    @Override
    public AcceptResult acceptOffer(DriverResponse r) {
        if (cancelReason != null) {
            return AcceptResult.lost(AcceptResult.Outcome.RIDE_CANCELLED,
                    request.rideId(), "rider cancelled");
        }
        if (acceptedDriverId != null) {
            // The loser branch. A normal, expected, high-frequency outcome - not an error.
            return AcceptResult.lost(AcceptResult.Outcome.TOO_LATE,
                    request.rideId(), "taken by " + acceptedDriverId);
        }
        String offeredTo = liveOffers.get(r.offerId());
        if (offeredTo == null || !offeredTo.equals(r.driverId())) {
            // Either the round already rolled over, or this driver is echoing an id that was
            // never theirs. Both answer the same way; only the first one actually happens.
            return AcceptResult.lost(AcceptResult.Outcome.EXPIRED,
                    request.rideId(), "offer no longer open");
        }

        acceptedDriverId = r.driverId();
        acceptedOfferId = r.offerId();
        dispatchLog.add("ACCEPTED " + r.offerId() + " by " + r.driverId());
        log.info("{} accepted {}", r.driverId(), r.offerId());
        return AcceptResult.won(request, fareCents);
    }

    @Override
    public void declineOffer(DriverResponse r) {
        // Fire-and-forget: nothing is waiting on a return value, so do not pay for one. The
        // signal is still durable - it is in history before this line runs.
        declinedOffers.add(r.offerId());
        dispatchLog.add("declined " + r.offerId() + " by " + r.driverId()
                + (r.reason() == null ? "" : " (" + r.reason() + ")"));
    }

    @Override
    public void riderCancel(String reason) {
        if (cancelReason == null) cancelReason = reason;
    }

    @Override
    public void driverArrived() {
        driverArrived = true;
    }

    @Override
    public void tripStarted() {
        tripStarted = true;
    }

    @Override
    public void tripEnded(long fare) {
        actualFareCents = fare;
        tripEnded = true;
    }

    @Override
    public String status() {
        return status;
    }

    @Override
    public String assignedDriver() {
        return acceptedDriverId;
    }

    @Override
    public List<String> dispatchLog() {
        return List.copyOf(dispatchLog);
    }
}
