package com.aawararide.ride;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.Comparator;

public final class RideRecord {
    public enum Status {
        IN_PROGRESS,
        COMPLETED
    }

    public final String rideId;
    public long createdAt;
    public Status status;
    public long startTimestamp;
    public long endTimestamp;
    public double totalDistanceMeters;
    public long totalDurationSeconds;
    public long movingDurationSeconds;
    public long idleDurationSeconds;
    public double topSpeedKmh;
    public double currentSpeedKmh;
    public long currentSpeedTimestamp;
    public double startLatitude;
    public double startLongitude;
    public double endLatitude;
    public double endLongitude;
    public final List<RidePoint> points = new ArrayList<>();

    public RideRecord() {
        rideId = UUID.randomUUID().toString();
        createdAt = System.currentTimeMillis();
        status = Status.IN_PROGRESS;
    }

    public RideRecord(String rideId) {
        this.rideId = rideId;
        createdAt = System.currentTimeMillis();
        status = Status.IN_PROGRESS;
    }

    public double overallAverageSpeedKmh() {
        return totalDurationSeconds == 0
                ? 0
                : totalDistanceMeters / totalDurationSeconds * 3.6;
    }

    public Double movingAverageSpeedKmh() {
        return movingDurationSeconds == 0
                ? null
                : totalDistanceMeters / movingDurationSeconds * 3.6;
    }

    public boolean hasFreshSpeed(long nowMillis) {
        return currentSpeedTimestamp > 0 && nowMillis - currentSpeedTimestamp <= 15_000L;
    }

    public List<RidePoint> immutablePoints() {
        return Collections.unmodifiableList(points);
    }

    JSONObject toJson() throws JSONException {
        JSONArray pointArray = new JSONArray();
        for (RidePoint point : points) {
            pointArray.put(point.toJson());
        }
        return new JSONObject()
                .put("rideId", rideId)
                .put("createdAt", createdAt)
                .put("status", status.name())
                .put("startTimestamp", startTimestamp)
                .put("endTimestamp", endTimestamp)
                .put("totalDistanceMeters", totalDistanceMeters)
                .put("totalDurationSeconds", totalDurationSeconds)
                .put("movingDurationSeconds", movingDurationSeconds)
                .put("idleDurationSeconds", idleDurationSeconds)
                .put("topSpeedKmh", topSpeedKmh)
                .put("currentSpeedKmh", currentSpeedKmh)
                .put("currentSpeedTimestamp", currentSpeedTimestamp)
                .put("startLatitude", startLatitude)
                .put("startLongitude", startLongitude)
                .put("endLatitude", endLatitude)
                .put("endLongitude", endLongitude)
                .put("points", pointArray);
    }

    static RideRecord fromJson(JSONObject json) {
        RideRecord record = new RideRecord(json.optString("rideId"));
        record.createdAt = json.optLong("createdAt", record.createdAt);
        try {
            record.status = Status.valueOf(json.optString("status"));
        } catch (IllegalArgumentException exception) {
            record.status = RideRecord.Status.COMPLETED;
        }
        record.startTimestamp = json.optLong("startTimestamp");
        record.endTimestamp = json.optLong("endTimestamp");
        record.totalDistanceMeters = json.optDouble("totalDistanceMeters");
        record.totalDurationSeconds = json.optLong("totalDurationSeconds");
        record.movingDurationSeconds = json.optLong("movingDurationSeconds");
        record.idleDurationSeconds = json.optLong("idleDurationSeconds");
        record.topSpeedKmh = json.optDouble("topSpeedKmh");
        record.currentSpeedKmh = json.optDouble("currentSpeedKmh");
        record.currentSpeedTimestamp = json.optLong("currentSpeedTimestamp");
        record.startLatitude = json.optDouble("startLatitude");
        record.startLongitude = json.optDouble("startLongitude");
        record.endLatitude = json.optDouble("endLatitude");
        record.endLongitude = json.optDouble("endLongitude");
        JSONArray points = json.optJSONArray("points");
        if (points != null) {
            for (int i = 0; i < points.length(); i++) {
                JSONObject point = points.optJSONObject(i);
                if (point != null) {
                    record.points.add(RidePoint.fromJson(point));
                }
            }
        }
        record.points.sort(Comparator.comparingLong(point -> point.timestamp));
        return record;
    }
}
