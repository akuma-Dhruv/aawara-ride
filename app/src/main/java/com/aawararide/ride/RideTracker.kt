package com.aawararide.ride

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import java.util.concurrent.TimeUnit

class RideTracker(context: Context, private val listener: Listener) : LocationListener {
    enum class State { WAITING_FOR_GPS, ACTIVE_RIDE }

    interface Listener {
        fun onStateChanged(state: State, ride: RideRecord?)
        fun onRideCompleted(ride: RideRecord)
    }

    private val context = context.applicationContext
    private val locationManager = this.context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private val store = RideStore(this.context)
    private var ride: RideRecord? = store.activeRide()
    private var previousPoint: RidePoint? = ride?.points?.lastOrNull()
    private var moving = false
    private var consecutiveMovingSamples = 0
    private var state = if (previousPoint != null) State.ACTIVE_RIDE else State.WAITING_FOR_GPS

    fun state() = state
    fun ride() = ride

    fun start() {
        if (locationManager == null || !hasLocationPermission()) {
            Log.w(TAG, "Location tracking not started: location permission unavailable")
            return
        }
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 2_000L, 3f, this, Looper.getMainLooper())
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER, 5_000L, 5f, this, Looper.getMainLooper())
            listener.onStateChanged(state, ride)
        } catch (exception: SecurityException) {
            Log.e(TAG, "Location updates rejected by Android", exception)
        }
    }

    fun stopAndComplete() {
        stopLocationUpdates()
        val currentRide = ride ?: return
        if (currentRide.startTimestamp == 0L) return
        val end = System.currentTimeMillis()
        addDuration(end)
        currentRide.endTimestamp = end
        previousPoint?.let {
            currentRide.endLatitude = it.latitude
            currentRide.endLongitude = it.longitude
        }
        currentRide.totalDurationSeconds = maxOf(
            0,
            TimeUnit.MILLISECONDS.toSeconds(currentRide.endTimestamp - currentRide.startTimestamp)
        )
        normalizeDurations(currentRide)
        currentRide.currentSpeedTimestamp = 0
        store.completeRide(currentRide)
        listener.onRideCompleted(currentRide)
        ride = null
        previousPoint = null
    }

    fun stopWithoutCompleting() = stopLocationUpdates()

    private fun stopLocationUpdates() {
        try {
            locationManager?.removeUpdates(this)
        } catch (exception: SecurityException) {
            Log.w(TAG, "Location updates could not be removed", exception)
        }
    }

    override fun onLocationChanged(location: Location) {
        if (!isAcceptable(location)) return
        val point = pointFrom(location)
        val currentRide = ride
        if (currentRide == null) {
            val newRide = RideRecord()
            newRide.startTimestamp = point.timestamp
            newRide.startLatitude = point.latitude
            newRide.startLongitude = point.longitude
            ride = newRide
            state = State.ACTIVE_RIDE
            previousPoint = point
            newRide.points.add(point)
            store.saveActiveRide(newRide)
            listener.onStateChanged(state, newRide)
            return
        }
        val previous = previousPoint ?: point.also { previousPoint = it }
        val deltaMeters = distanceMeters(previous, point)
        val deltaMillis = point.timestamp - previous.timestamp
        val deltaSeconds = deltaMillis / 1000.0
        if (deltaSeconds <= 0) return
        val observedSpeedKmh = deltaMeters / deltaSeconds * 3.6
        if (observedSpeedKmh > MAX_VALID_SPEED_KMH) {
            Log.w(TAG, "Ignoring impossible GPS jump: $observedSpeedKmh km/h")
            return
        }
        if (hasReliableReportedSpeed(location) &&
            kotlin.math.abs(location.speed * 3.6 - observedSpeedKmh) > MAX_REPORTED_SPEED_DIFFERENCE_KMH
        ) {
            Log.w(TAG, "Ignoring GPS point with contradictory speed readings")
            return
        }
        addDuration(point.timestamp)
        val speedKmh = speedFor(point, observedSpeedKmh)
        val withinAccuracyRadius = deltaMeters <= previous.accuracy + point.accuracy
        if (deltaMeters >= 1.0 && !(speedKmh <= IDLE_THRESHOLD_KMH && withinAccuracyRadius)) {
            currentRide.totalDistanceMeters += deltaMeters
        }
        consecutiveMovingSamples = if (speedKmh > IDLE_THRESHOLD_KMH) {
            consecutiveMovingSamples + 1
        } else {
            0
        }
        if (speedKmh <= IDLE_THRESHOLD_KMH || consecutiveMovingSamples >= 2) {
            currentRide.topSpeedKmh = maxOf(currentRide.topSpeedKmh, speedKmh)
        }
        currentRide.currentSpeedKmh = speedKmh
        currentRide.currentSpeedTimestamp = System.currentTimeMillis()
        moving = updateMovementState(speedKmh)
        previousPoint = point
        currentRide.points.add(point)
        store.saveActiveRide(currentRide)
        listener.onStateChanged(state, currentRide)
    }

    private fun isAcceptable(location: Location): Boolean {
        if (!location.hasAccuracy() || location.accuracy > MAX_ACCURACY_METERS ||
            !location.latitude.isFinite() || !location.longitude.isFinite()
        ) return false
        val now = System.currentTimeMillis()
        if (location.time < now - MAX_LOCATION_AGE_MS ||
            location.time > now + MAX_FUTURE_LOCATION_MS
        ) return false
        return previousPoint?.let { location.time > it.timestamp } ?: true
    }

    private fun pointFrom(location: Location) = RidePoint(
        location.time,
        location.latitude,
        location.longitude,
        location.accuracy,
        if (hasReliableReportedSpeed(location)) location.speed else -1f,
        if (location.hasAltitude()) location.altitude else 0.0
    )

    private fun hasReliableReportedSpeed(location: Location): Boolean =
        location.hasSpeed() && location.speed.isFinite() && location.speed >= 0 &&
            (!location.hasSpeedAccuracy() || location.speedAccuracyMetersPerSecond <= 10.0f)

    private fun speedFor(point: RidePoint, observedSpeedKmh: Double): Double {
        if (point.speedMetersPerSecond < 0) return observedSpeedKmh
        val reported = point.speedMetersPerSecond * 3.6
        if (!reported.isFinite() || reported > MAX_VALID_SPEED_KMH ||
            kotlin.math.abs(reported - observedSpeedKmh) > MAX_REPORTED_SPEED_DIFFERENCE_KMH
        ) return observedSpeedKmh
        return maxOf(0.0, (reported + observedSpeedKmh) / 2)
    }

    private fun updateMovementState(speedKmh: Double) =
        if (moving) speedKmh > MOVING_EXIT_THRESHOLD_KMH else speedKmh > MOVING_ENTER_THRESHOLD_KMH

    private fun addDuration(timestamp: Long) {
        val currentRide = ride ?: return
        val previous = previousPoint ?: return
        if (timestamp <= previous.timestamp) return
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timestamp - previous.timestamp)
        if (seconds <= 0) return
        if (moving && seconds * 1000L <= CURRENT_SPEED_STALE_MS) {
            currentRide.movingDurationSeconds += seconds
        } else {
            currentRide.idleDurationSeconds += seconds
        }
        currentRide.totalDurationSeconds =
            currentRide.movingDurationSeconds + currentRide.idleDurationSeconds
    }

    private fun normalizeDurations(currentRide: RideRecord) {
        currentRide.totalDurationSeconds = maxOf(
            0,
            TimeUnit.MILLISECONDS.toSeconds(currentRide.endTimestamp - currentRide.startTimestamp)
        )
        val accounted = currentRide.movingDurationSeconds + currentRide.idleDurationSeconds
        if (accounted < currentRide.totalDurationSeconds) {
            currentRide.idleDurationSeconds += currentRide.totalDurationSeconds - accounted
        } else if (accounted > currentRide.totalDurationSeconds) {
            currentRide.idleDurationSeconds = maxOf(
                0,
                currentRide.totalDurationSeconds - currentRide.movingDurationSeconds
            )
        }
    }

    private fun distanceMeters(first: RidePoint, second: RidePoint): Double {
        val result = FloatArray(1)
        Location.distanceBetween(
            first.latitude, first.longitude, second.latitude, second.longitude, result)
        return result[0].toDouble()
    }

    private fun hasLocationPermission() =
        context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    override fun onProviderDisabled(provider: String) {
        Log.i(TAG, "Location provider disabled: $provider")
    }

    override fun onProviderEnabled(provider: String) {
        Log.i(TAG, "Location provider enabled: $provider")
    }

    @Suppress("DEPRECATION")
    override fun onStatusChanged(provider: String, status: Int, extras: Bundle?) = Unit

    private companion object {
        const val IDLE_THRESHOLD_KMH = 5.0
        const val MOVING_ENTER_THRESHOLD_KMH = 7.0
        const val MOVING_EXIT_THRESHOLD_KMH = 3.0
        const val MAX_ACCURACY_METERS = 50f
        const val MAX_VALID_SPEED_KMH = 160.0
        const val MAX_REPORTED_SPEED_DIFFERENCE_KMH = 25.0
        const val MAX_LOCATION_AGE_MS = 120_000L
        const val MAX_FUTURE_LOCATION_MS = 120_000L
        const val CURRENT_SPEED_STALE_MS = 15_000L
        const val TAG = "AawaraRideTracker"
    }
}
