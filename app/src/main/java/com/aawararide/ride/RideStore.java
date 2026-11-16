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

public final class RideStore {
    private static final String TAG = "AawaraRideStore";
    private static final String PREFS = "ride_store";
    private static final String ACTIVE = "active_ride";
    private static final String HISTORY = "ride_history";
    private final SharedPreferences preferences;

    public RideStore(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

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

    public synchronized void saveActiveRide(RideRecord ride) {
        try {
            preferences.edit().putString(ACTIVE, ride.toJson().toString()).apply();
        } catch (JSONException exception) {
            Log.e(TAG, "Unable to persist active ride", exception);
        }
    }

    public synchronized void completeRide(RideRecord ride) {
        List<RideRecord> rides = history();
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

    public synchronized List<RideRecord> history() {
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
            return rides;
        } catch (JSONException exception) {
            Log.e(TAG, "Unable to read ride history", exception);
            return Collections.emptyList();
        }
    }
}
