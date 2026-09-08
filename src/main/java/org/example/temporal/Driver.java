package org.example.temporal;

/** A candidate returned by the supply service. */
public record Driver(String driverId, String name, String vehicle, double distanceKm, double rating) {

    public String display() {
        return driverId + " (" + name + ", " + vehicle + ", "
                + String.format(java.util.Locale.ROOT, "%.1f", distanceKm)
                + "km away, " + rating + "*)";
    }
}
