package org.example.temporal;

/**
 * A driver tapping ACCEPT or DECLINE on an offer card.
 *
 * offerId is the important field. The driver's phone echoes back the id it was pushed, so the
 * workflow can tell "accepting the offer I am currently holding open" from "accepting an offer
 * that timed out 40 seconds ago and has already been re-dispatched to someone else".
 */
public record DriverResponse(String offerId, String driverId, String reason) {

    public static DriverResponse of(String offerId, String driverId) {
        return new DriverResponse(offerId, driverId, null);
    }
}
