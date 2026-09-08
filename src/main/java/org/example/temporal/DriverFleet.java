package org.example.temporal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stands in for two real services and one real phone:
 *
 *   - the SUPPLY service: which drivers are near this pickup, and free
 *   - the FLEET service: who is currently assigned to whom
 *   - the DRIVER APP: the thing that actually receives the offer card
 *
 * Two behaviours here are the load-bearing ones for the demo:
 *
 *   reserve() is a COMPARE-AND-SET, not a read-then-write, and it is idempotent for the same
 *   rideId. That is the answer to the one race Temporal cannot serialize for you: two
 *   different ride workflows offering the same driver at the same moment. Inside one workflow
 *   the execution model gives you serialization for free; across two workflows it cannot, and
 *   something outside has to be the arbiter. Here that is one atomic map operation; in
 *   production it is a conditional write in the fleet store.
 *
 *   deliver() DEDUPES ON offerId, like a real push pipeline keyed on a message id. Temporal
 *   will re-run pushOffer when an ack goes missing; this is what stops the driver's phone
 *   from stacking two identical cards for one ride.
 */
public final class DriverFleet {

    /** offerId -> driverId, for offers the app is currently showing. */
    private final Map<String, String> openOffers = new LinkedHashMap<>();
    /** every offerId ever delivered, for dedupe. */
    private final Set<String> deliveredOffers = new LinkedHashSet<>();
    /** offerId -> driverId for every offer ever pushed, open or withdrawn. */
    private final Map<String, String> everOffered = new LinkedHashMap<>();
    /** driverId -> rideId holding them. */
    private final Map<String, String> assignments = new LinkedHashMap<>();
    private final List<Driver> roster = new ArrayList<>();
    private final Map<String, Location> positions = new LinkedHashMap<>();

    private int pushAttempts;
    private int cardsShown;
    private int duplicatePushesSuppressed;

    public DriverFleet() {
        // A fixed roster, laid out so that nearest-first from the Ferry Building is exactly
        // D-101, D-102, ... D-106. Reproducible runs make the scenario scripts readable.
        add("D-101", "Ana",    "Prius",   37.8000, -122.3937, 4.92);   // 0.5km
        add("D-102", "Bilal",  "Camry",   37.7955, -122.4039, 4.88);   // 0.9km
        add("D-103", "Chen",   "Model 3", 37.7829, -122.3937, 4.97);   // 1.4km
        add("D-104", "Dee",    "Sienna",  37.8225, -122.3937, 4.71);   // 3.0km
        add("D-105", "Emeka",  "Civic",   37.7955, -122.3528, 4.83);   // 3.6km
        add("D-106", "Farida", "Leaf",    37.7505, -122.3937, 4.90);   // 5.0km
    }

    private void add(String id, String name, String vehicle, double lat, double lon, double rating) {
        roster.add(new Driver(id, name, vehicle, 0.0, rating));
        positions.put(id, new Location(id + "-pos", lat, lon));
    }

    // ---- supply service ---------------------------------------------------------------------

    /**
     * Nearest-first, free drivers only, excluding anyone this ride has already tried.
     * Sorted deterministically: the workflow calls this through an activity, so a wobbly
     * ordering here would show up as a wobbly dispatch order, which is merely bad. If this
     * ran INSIDE the workflow it would show up as a NonDeterministicException, which is worse.
     */
    public synchronized List<Driver> nearby(Location pickup, int radiusKm, List<String> exclude) {
        List<Driver> out = new ArrayList<>();
        for (Driver d : roster) {
            if (exclude.contains(d.driverId())) continue;
            if (assignments.containsKey(d.driverId())) continue;
            double km = positions.get(d.driverId()).kmFrom(pickup);
            if (km <= radiusKm) {
                out.add(new Driver(d.driverId(), d.name(), d.vehicle(), km, d.rating()));
            }
        }
        out.sort(Comparator.comparingDouble(Driver::distanceKm).thenComparing(Driver::driverId));
        return out;
    }

    // ---- the driver app ---------------------------------------------------------------------

    /** Returns true if a card was actually shown; false if this was a suppressed duplicate. */
    public synchronized boolean deliver(String offerId, String driverId, RideRequest r, long fareCents) {
        pushAttempts++;
        if (!deliveredOffers.add(offerId)) {
            duplicatePushesSuppressed++;
            System.out.println("      [app] offer " + offerId + " already on " + driverId
                    + "'s screen -> duplicate push dropped");
            return false;
        }
        openOffers.put(offerId, driverId);
        everOffered.put(offerId, driverId);
        cardsShown++;
        System.out.println("      [app] " + driverId + " buzzed: " + r.pickup().label()
                + " -> " + r.dropoff().label() + ", " + PaymentGateway.money(fareCents)
                + "  offer=" + offerId);
        return true;
    }

    public synchronized void withdraw(String offerId, String driverId) {
        if (openOffers.remove(offerId) != null) {
            System.out.println("      [app] offer " + offerId + " pulled from " + driverId);
        }
    }

    /** offerId currently on this driver's screen, or null. Used by the demo to script taps. */
    public synchronized String openOfferFor(String driverId) {
        for (Map.Entry<String, String> e : openOffers.entrySet()) {
            if (e.getValue().equals(driverId)) return e.getKey();
        }
        return null;
    }

    /** The last offer ever pushed to this driver, open or not. Lets the demo tap a stale card. */
    public synchronized String lastOfferTo(String driverId) {
        String last = null;
        for (Map.Entry<String, String> e : everOffered.entrySet()) {
            if (e.getValue().equals(driverId)) last = e.getKey();
        }
        return last;
    }

    public synchronized List<String> openOfferIds() {
        return List.copyOf(openOffers.keySet());
    }

    public synchronized List<String> driversHoldingOffers() {
        return List.copyOf(new LinkedHashSet<>(openOffers.values()));
    }

    // ---- fleet service ----------------------------------------------------------------------

    /**
     * Compare-and-set. Free -> held by rideId. Returns false if someone else holds them.
     * Re-running it for the same (rideId, driverId) returns true, so a Temporal retry of
     * reserveDriver is harmless.
     */
    public synchronized boolean reserve(String rideId, String driverId) {
        String holder = assignments.putIfAbsent(driverId, rideId);
        if (holder == null) {
            System.out.println("      [fleet] " + driverId + " reserved for " + rideId);
            return true;
        }
        boolean mine = holder.equals(rideId);
        System.out.println("      [fleet] " + driverId + " already held by " + holder
                + (mine ? "  (that is us - idempotent retry)" : "  <-- LOST THE CROSS-RIDE RACE"));
        return mine;
    }

    public synchronized void release(String rideId, String driverId) {
        if (assignments.remove(driverId, rideId)) {
            System.out.println("      [fleet] " + driverId + " released by " + rideId);
        }
    }

    /** Simulates another ride grabbing a driver out from under us. Demo only. */
    public synchronized void stealDriver(String driverId, String otherRideId) {
        assignments.put(driverId, otherRideId);
        System.out.println("      [fleet] " + driverId + " taken by " + otherRideId
                + " (a different ride workflow)");
    }

    // ---- counters the demo prints ------------------------------------------------------------

    public synchronized int pushAttempts() { return pushAttempts; }
    public synchronized int cardsShown() { return cardsShown; }
    public synchronized int duplicatePushesSuppressed() { return duplicatePushesSuppressed; }
}
