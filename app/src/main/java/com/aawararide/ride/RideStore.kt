package com.aawararide.ride

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class RideStore(context: Context) : RideDao {
    private val preferences: SharedPreferences = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized
    override fun activeRide(): RideRecord? {
        val value = preferences.getString(ACTIVE, null) ?: return null
        return try {
            RideRecord.fromJson(JSONObject(value))
        } catch (exception: JSONException) {
            Log.e(TAG, "Discarding unreadable active ride", exception)
            preferences.edit().remove(ACTIVE).apply()
            null
        }
    }

    @Synchronized
    override fun saveActiveRide(ride: RideRecord) {
        try {
            preferences.edit().putString(ACTIVE, ride.toJson().toString()).apply()
        } catch (exception: JSONException) {
            Log.e(TAG, "Unable to persist active ride", exception)
        }
    }

    @Synchronized
    override fun completeRide(ride: RideRecord) {
        val rides = history().toMutableList()
        if (rides.any { it.rideId == ride.rideId }) {
            preferences.edit().remove(ACTIVE).apply()
            return
        }
        ride.status = RideRecord.Status.COMPLETED
        rides.add(0, ride)
        val history = JSONArray()
        try {
            rides.forEach { history.put(it.toJson()) }
            preferences.edit().putString(HISTORY, history.toString()).remove(ACTIVE).apply()
        } catch (exception: JSONException) {
            Log.e(TAG, "Unable to persist completed ride", exception)
        }
    }

    @Synchronized
    override fun completedRides(): List<RideRecord> {
        val value = preferences.getString(HISTORY, null) ?: return ArrayList()
        return try {
            val history = JSONArray(value)
            val rides = ArrayList<RideRecord>()
            for (index in 0 until history.length()) {
                history.optJSONObject(index)?.let { rides.add(RideRecord.fromJson(it)) }
            }
            rides.sortByDescending { it.startTimestamp }
            rides
        } catch (exception: JSONException) {
            Log.e(TAG, "Unable to read ride history", exception)
            emptyList()
        }
    }

    @Synchronized
    fun history(): List<RideRecord> = completedRides()

    @Synchronized
    override fun findCompletedRide(rideId: String): RideRecord? =
        completedRides().firstOrNull { it.rideId == rideId }

    @Synchronized
    override fun pointsForRide(rideId: String): List<RidePoint> =
        findCompletedRide(rideId)?.immutablePoints() ?: emptyList()

    private companion object {
        const val TAG = "AawaraRideStore"
        const val PREFS = "ride_store"
        const val ACTIVE = "active_ride"
        const val HISTORY = "ride_history"
    }
}
