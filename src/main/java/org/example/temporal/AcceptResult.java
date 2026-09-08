package org.example.temporal;

/**
 * The answer the driver's phone gets back, synchronously, from acceptOffer.
 *
 * This is why accept is a Workflow UPDATE and not a signal. A signal is fire-and-forget: the
 * driver would tap ACCEPT, get an optimistic green screen, and find out 400ms later over some
 * other channel that another driver got there first. An update is a durable request with a
 * return value, so the phone can render the truth on the first frame.
 */
public record AcceptResult(Outcome outcome, String rideId, String pickupLabel,
                           String dropoffLabel, long fareCents, String note) {

    public enum Outcome {
        /** You have the ride. Drive to the pickup. */
        WON,
        /** Another driver accepted first. This is the normal loser branch of the race. */
        TOO_LATE,
        /** The offer had already expired and been re-dispatched before your tap landed. */
        EXPIRED,
        /** The rider cancelled while the offer was open. */
        RIDE_CANCELLED
    }

    public static AcceptResult won(RideRequest r, long fareCents) {
        return new AcceptResult(Outcome.WON, r.rideId(), r.pickup().label(),
                r.dropoff().label(), fareCents, "head to pickup");
    }

    public static AcceptResult lost(Outcome outcome, String rideId, String note) {
        return new AcceptResult(outcome, rideId, null, null, 0L, note);
    }

    public boolean won() {
        return outcome == Outcome.WON;
    }
}
