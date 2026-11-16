package com.aawararide.ride;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.TimeUnit;

public final class RideTracker implements LocationListener {
    public enum State {
        WAITING_FOR_GPS,
        ACTIVE_RIDE
    }

    public interface Listener {
        void onStateChanged(State state, RideRecord ride);
        void onRideCompleted(RideRecord ride);
    }

    static final double IDLE_THRESHOLD_KMH = 5.0;
    static final double MOVING_ENTER_THRESHOLD_KMH = 7.0;
    static final double MOVING_EXIT_THRESHOLD_KMH = 3.0;
    private static final float MAX_ACCURACY_METERS = 50f;
    private static final double MAX_SPEED_KMH = 160.0;
    private static final long CURRENT_SPEED_STALE_MS = 15_000L;
    private static final String TAG = "AawaraRideTracker";
    private final Context context;
    private final LocationManager locationManager;
    private final RideStore store;
    private final Listener listener;
    private RideRecord ride;
    private RidePoint previousPoint;
    private boolean moving;
    private State state = State.WAITING_FOR_GPS;

    public RideTracker(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        locationManager = (LocationManager) this.context.getSystemService(Context.LOCATION_SERVICE);
        store = new RideStore(this.context);
        ride = store.activeRide();
        if (ride != null && !ride.points.isEmpty()) {
            previousPoint = ride.points.get(ride.points.size() - 1);
            state = State.ACTIVE_RIDE;
        }
    }

    public State state() {
        return state;
    }

    public RideRecord ride() {
        return ride;
    }

    public void start() {
        if (locationManager == null || !hasLocationPermission()) {
            Log.w(TAG, "Location tracking not started: location permission unavailable");
            return;
        }
        try {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 2_000L, 3f, this, Looper.getMainLooper());
            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, 5_000L, 5f, this, Looper.getMainLooper());
            listener.onStateChanged(state, ride);
        } catch (SecurityException exception) {
            Log.e(TAG, "Location updates rejected by Android", exception);
        }
    }

    public void stopAndComplete() {
        stopLocationUpdates();
        if (ride == null || ride.startTimestamp == 0) {
            return;
        }
        long end = System.currentTimeMillis();
        addDuration(end);
        ride.endTimestamp = end;
        if (previousPoint != null) {
            ride.endLatitude = previousPoint.latitude;
            ride.endLongitude = previousPoint.longitude;
        }
        ride.totalDurationSeconds = Math.max(0,
                TimeUnit.MILLISECONDS.toSeconds(ride.endTimestamp - ride.startTimestamp));
        normalizeDurations();
        store.completeRide(ride);
        listener.onRideCompleted(ride);
        ride = null;
        previousPoint = null;
    }

    public void stopWithoutCompleting() {
        stopLocationUpdates();
    }

    private void stopLocationUpdates() {
        if (locationManager != null) {
            try {
                locationManager.removeUpdates(this);
            } catch (SecurityException exception) {
                Log.w(TAG, "Location updates could not be removed", exception);
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (!isAcceptable(location)) {
            return;
        }
        RidePoint point = pointFrom(location);
        if (ride == null) {
            ride = new RideRecord();
            ride.startTimestamp = point.timestamp;
            ride.startLatitude = point.latitude;
            ride.startLongitude = point.longitude;
            state = State.ACTIVE_RIDE;
            previousPoint = point;
            ride.points.add(point);
            store.saveActiveRide(ride);
            listener.onStateChanged(state, ride);
            return;
        }
        if (previousPoint == null) {
            previousPoint = point;
        }
        double deltaMeters = distanceMeters(previousPoint, point);
        long deltaSeconds = Math.max(1,
                TimeUnit.MILLISECONDS.toSeconds(point.timestamp - previousPoint.timestamp));
        double observedSpeedKmh = deltaMeters / deltaSeconds * 3.6;
        if (observedSpeedKmh > MAX_SPEED_KMH) {
            Log.w(TAG, "Ignoring impossible GPS jump: " + observedSpeedKmh + " km/h");
            return;
        }
        addDuration(point.timestamp);
        if (deltaMeters >= 1.0) {
            ride.totalDistanceMeters += deltaMeters;
        }
        double speedKmh = speedFor(point, observedSpeedKmh);
        if (speedKmh <= MAX_SPEED_KMH) {
            ride.topSpeedKmh = Math.max(ride.topSpeedKmh, speedKmh);
        }
        ride.currentSpeedKmh = speedKmh;
        ride.currentSpeedTimestamp = System.currentTimeMillis();
        moving = updateMovementState(speedKmh);
        previousPoint = point;
        ride.points.add(point);
        store.saveActiveRide(ride);
        listener.onStateChanged(state, ride);
    }

    private boolean isAcceptable(Location location) {
        if (!location.hasAccuracy() || location.getAccuracy() > MAX_ACCURACY_METERS
                || !Double.isFinite(location.getLatitude())
                || !Double.isFinite(location.getLongitude())) {
            return false;
        }
        if (previousPoint != null && location.getTime() <= previousPoint.timestamp) {
            return false;
        }
        return true;
    }

    private RidePoint pointFrom(Location location) {
        return new RidePoint(
                location.getTime(),
                location.getLatitude(),
                location.getLongitude(),
                location.getAccuracy(),
                location.hasSpeed() ? location.getSpeed() : -1,
                location.hasAltitude() ? location.getAltitude() : 0);
    }

    private double speedFor(RidePoint point, double observedSpeedKmh) {
        double reported = point.speedMetersPerSecond >= 0
                ? point.speedMetersPerSecond * 3.6 : observedSpeedKmh;
        if (reported > MAX_SPEED_KMH || (reported > 0 && observedSpeedKmh > MAX_SPEED_KMH)) {
            return observedSpeedKmh;
        }
        return Math.min(MAX_SPEED_KMH, Math.max(0, (reported + observedSpeedKmh) / 2));
    }

    private boolean updateMovementState(double speedKmh) {
        if (moving) {
            return speedKmh > MOVING_EXIT_THRESHOLD_KMH;
        }
        return speedKmh > MOVING_ENTER_THRESHOLD_KMH;
    }

    private void addDuration(long timestamp) {
        if (ride == null || previousPoint == null || timestamp <= previousPoint.timestamp) {
            return;
        }
        long seconds = TimeUnit.MILLISECONDS.toSeconds(timestamp - previousPoint.timestamp);
        if (seconds <= 0) {
            return;
        }
        if (moving && seconds * 1000L <= CURRENT_SPEED_STALE_MS) {
            ride.movingDurationSeconds += seconds;
        } else {
            ride.idleDurationSeconds += seconds;
        }
        ride.totalDurationSeconds = ride.movingDurationSeconds + ride.idleDurationSeconds;
    }

    private void normalizeDurations() {
        ride.totalDurationSeconds = Math.max(0,
                TimeUnit.MILLISECONDS.toSeconds(ride.endTimestamp - ride.startTimestamp));
        long accounted = ride.movingDurationSeconds + ride.idleDurationSeconds;
        if (accounted < ride.totalDurationSeconds) {
            ride.idleDurationSeconds += ride.totalDurationSeconds - accounted;
        } else if (accounted > ride.totalDurationSeconds) {
            ride.idleDurationSeconds = Math.max(0,
                    ride.totalDurationSeconds - ride.movingDurationSeconds);
        }
    }

    private double distanceMeters(RidePoint first, RidePoint second) {
        float[] result = new float[1];
        Location.distanceBetween(
                first.latitude, first.longitude, second.latitude, second.longitude, result);
        return result[0];
    }

    private boolean hasLocationPermission() {
        return context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.i(TAG, "Location provider disabled: " + provider);
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.i(TAG, "Location provider enabled: " + provider);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // Deprecated by Android, retained for API 29 compatibility.
    }
}
