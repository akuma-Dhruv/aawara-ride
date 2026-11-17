package com.aawararide.ride

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Collections
import java.util.UUID

class RideRecord {
    enum class Status { IN_PROGRESS, COMPLETED }

    @JvmField val rideId: String
    @JvmField var createdAt: Long
    @JvmField var status: Status
    @JvmField var startTimestamp: Long = 0
    @JvmField var endTimestamp: Long = 0
    @JvmField var totalDistanceMeters: Double = 0.0
    @JvmField var totalDurationSeconds: Long = 0
    @JvmField var movingDurationSeconds: Long = 0
    @JvmField var idleDurationSeconds: Long = 0
    @JvmField var topSpeedKmh: Double = 0.0
    @JvmField var currentSpeedKmh: Double = 0.0
    @JvmField var currentSpeedTimestamp: Long = 0
    @JvmField var startLatitude: Double = 0.0
    @JvmField var startLongitude: Double = 0.0
    @JvmField var endLatitude: Double = 0.0
    @JvmField var endLongitude: Double = 0.0
    @JvmField val points: MutableList<RidePoint> = ArrayList()

    constructor() {
        rideId = UUID.randomUUID().toString()
        createdAt = System.currentTimeMillis()
        status = Status.IN_PROGRESS
    }

    constructor(rideId: String) {
        this.rideId = rideId
        createdAt = System.currentTimeMillis()
        status = Status.IN_PROGRESS
    }

    fun overallAverageSpeedKmh(): Double =
        if (totalDurationSeconds == 0L) 0.0
        else totalDistanceMeters / totalDurationSeconds * 3.6

    fun movingAverageSpeedKmh(): Double? =
        if (movingDurationSeconds == 0L) null
        else totalDistanceMeters / movingDurationSeconds * 3.6

    fun hasFreshSpeed(nowMillis: Long): Boolean =
        currentSpeedTimestamp > 0 && nowMillis - currentSpeedTimestamp <= 15_000L

    fun immutablePoints(): List<RidePoint> = Collections.unmodifiableList(points)

    @Throws(JSONException::class)
    internal fun toJson(): JSONObject {
        val pointArray = JSONArray()
        points.forEach { pointArray.put(it.toJson()) }
        return JSONObject()
            .put("rideId", rideId)
            .put("createdAt", createdAt)
            .put("status", status.name)
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
            .put("points", pointArray)
    }

    companion object {
        internal fun fromJson(json: JSONObject): RideRecord {
            val record = RideRecord(json.optString("rideId"))
            record.createdAt = json.optLong("createdAt", record.createdAt)
            record.status = try {
                Status.valueOf(json.optString("status"))
            } catch (_: IllegalArgumentException) {
                Status.COMPLETED
            }
            record.startTimestamp = json.optLong("startTimestamp")
            record.endTimestamp = json.optLong("endTimestamp")
            record.totalDistanceMeters = json.optDouble("totalDistanceMeters")
            record.totalDurationSeconds = json.optLong("totalDurationSeconds")
            record.movingDurationSeconds = json.optLong("movingDurationSeconds")
            record.idleDurationSeconds = json.optLong("idleDurationSeconds")
            record.topSpeedKmh = json.optDouble("topSpeedKmh")
            record.currentSpeedKmh = json.optDouble("currentSpeedKmh")
            record.currentSpeedTimestamp = json.optLong("currentSpeedTimestamp")
            record.startLatitude = json.optDouble("startLatitude")
            record.startLongitude = json.optDouble("startLongitude")
            record.endLatitude = json.optDouble("endLatitude")
            record.endLongitude = json.optDouble("endLongitude")
            json.optJSONArray("points")?.let { points ->
                for (index in 0 until points.length()) {
                    points.optJSONObject(index)?.let { record.points.add(RidePoint.fromJson(it)) }
                }
            }
            record.points.sortBy { it.timestamp }
            return record
        }
    }
}
