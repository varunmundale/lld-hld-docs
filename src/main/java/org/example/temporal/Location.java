package org.example.temporal;

/** A point on the map. Kilometres here are toy Euclidean distance, not haversine. */
public record Location(String label, double lat, double lon) {

    public double kmFrom(Location other) {
        double dLat = (lat - other.lat) * 111.0;
        double dLon = (lon - other.lon) * 88.0;   // roughly right at SF/NY latitudes
        return Math.sqrt(dLat * dLat + dLon * dLon);
    }
}
