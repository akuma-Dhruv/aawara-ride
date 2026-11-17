package com.aawararide.ride

import org.json.JSONException
import org.json.JSONObject

class RidePoint(
    @JvmField val timestamp: Long,
    @JvmField val latitude: Double,
    @JvmField val longitude: Double,
    @JvmField val accuracy: Float,
    @JvmField val speedMetersPerSecond: Float,
    @JvmField val altitude: Double
) {
    @Throws(JSONException::class)
    internal fun toJson(): JSONObject = JSONObject()
        .put("timestamp", timestamp)
        .put("latitude", latitude)
        .put("longitude", longitude)
        .put("accuracy", accuracy)
        .put("speed", speedMetersPerSecond)
        .put("altitude", altitude)

    companion object {
        internal fun fromJson(json: JSONObject) = RidePoint(
            json.optLong("timestamp"),
            json.optDouble("latitude"),
            json.optDouble("longitude"),
            json.optDouble("accuracy").toFloat(),
            json.optDouble("speed", -1.0).toFloat(),
            json.optDouble("altitude")
        )
    }
}
