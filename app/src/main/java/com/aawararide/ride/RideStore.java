package com.aawararide.ride;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RideStore implements RideDao {
    private static final String TAG = "AawaraRideStore";
    private static final String PREFS = "ride_store";
    private static final String ACTIVE = "active_ride";
    private static final String HISTORY = "ride_history";
    private final SharedPreferences preferences;

    public RideStore(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @Override
    public synchronized RideRecord activeRide() {
        String value = preferences.getString(ACTIVE, null);
        if (value == null) {
            return null;
        }
        try {
            return RideRecord.fromJson(new JSONObject(value));
        } catch (JSONException exception) {
            Log.e(TAG, "Discarding unreadable active ride", exception);
            preferences.edit().remove(ACTIVE).apply();
            return null;
        }
    }

    @Override
    public synchronized void saveActiveRide(RideRecord ride) {
        try {
            preferences.edit().putString(ACTIVE, ride.toJson().toString()).apply();
        } catch (JSONException exception) {
            Log.e(TAG, "Unable to persist active ride", exception);
        }
    }

    @Override
    public synchronized void completeRide(RideRecord ride) {
        List<RideRecord> rides = history();
        for (RideRecord existing : rides) {
            if (existing.rideId.equals(ride.rideId)) {
                preferences.edit().remove(ACTIVE).apply();
                return;
            }
        }
        ride.status = RideRecord.Status.COMPLETED;
        rides.add(0, ride);
        JSONArray history = new JSONArray();
        try {
            for (RideRecord item : rides) {
                history.put(item.toJson());
            }
            preferences.edit()
                    .putString(HISTORY, history.toString())
                    .remove(ACTIVE)
                    .apply();
        } catch (JSONException exception) {
            Log.e(TAG, "Unable to persist completed ride", exception);
        }
    }

    @Override
    public synchronized List<RideRecord> completedRides() {
        String value = preferences.getString(HISTORY, null);
        if (value == null) {
            return new ArrayList<>();
        }
        try {
            JSONArray history = new JSONArray(value);
            List<RideRecord> rides = new ArrayList<>();
            for (int i = 0; i < history.length(); i++) {
                JSONObject ride = history.optJSONObject(i);
                if (ride != null) {
                    rides.add(RideRecord.fromJson(ride));
                }
            }
            rides.sort((first, second) ->
                    Long.compare(second.startTimestamp, first.startTimestamp));
            return rides;
        } catch (JSONException exception) {
            Log.e(TAG, "Unable to read ride history", exception);
            return Collections.emptyList();
        }
    }

    public synchronized List<RideRecord> history() {
        return completedRides();
    }

    @Override
    public synchronized RideRecord findCompletedRide(String rideId) {
        for (RideRecord ride : completedRides()) {
            if (ride.rideId.equals(rideId)) {
                return ride;
            }
        }
        return null;
    }

    @Override
    public synchronized List<RidePoint> pointsForRide(String rideId) {
        RideRecord ride = findCompletedRide(rideId);
        return ride == null ? Collections.emptyList() : ride.immutablePoints();
    }
}
