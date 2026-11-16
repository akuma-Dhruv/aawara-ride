package com.aawararide.ride;

import org.json.JSONException;
import org.json.JSONObject;

public final class RidePoint {
    public final long timestamp;
    public final double latitude;
    public final double longitude;
    public final float accuracy;
    public final float speedMetersPerSecond;
    public final double altitude;

    public RidePoint(
            long timestamp,
            double latitude,
            double longitude,
            float accuracy,
            float speedMetersPerSecond,
            double altitude) {
        this.timestamp = timestamp;
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
        this.speedMetersPerSecond = speedMetersPerSecond;
        this.altitude = altitude;
    }

    JSONObject toJson() throws JSONException {
        return new JSONObject()
                .put("timestamp", timestamp)
                .put("latitude", latitude)
                .put("longitude", longitude)
                .put("accuracy", accuracy)
                .put("speed", speedMetersPerSecond)
                .put("altitude", altitude);
    }

    static RidePoint fromJson(JSONObject json) {
        return new RidePoint(
                json.optLong("timestamp"),
                json.optDouble("latitude"),
                json.optDouble("longitude"),
                (float) json.optDouble("accuracy"),
                (float) json.optDouble("speed", -1),
                json.optDouble("altitude"));
    }
}
