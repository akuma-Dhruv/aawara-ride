package com.aawararide.ui

import android.app.Activity
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import com.aawararide.R
import com.aawararide.ride.DurationFormatter
import com.aawararide.ride.RideStore
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class RideDetailsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.ride_details)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }
        setContentView(content)
        val rideId = intent.getStringExtra(EXTRA_RIDE_ID)
        val store = RideStore(this)
        val ride = rideId?.let { store.findCompletedRide(it) }
        if (ride == null) {
            content.addView(text(getString(R.string.ride_not_found)))
            return
        }
        val format = DateFormat.getDateTimeInstance(
            DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
        val movingAverage = ride.movingAverageSpeedKmh()
        content.addView(text(getString(
            R.string.ride_details_text,
            format.format(Date(ride.startTimestamp)),
            format.format(Date(ride.endTimestamp)),
            ride.totalDistanceMeters / 1000.0,
            DurationFormatter.format(ride.totalDurationSeconds),
            DurationFormatter.format(ride.movingDurationSeconds),
            DurationFormatter.format(ride.idleDurationSeconds),
            ride.overallAverageSpeedKmh(),
            movingAverage?.let { String.format(Locale.US, "%.1f km/h", it) }
                ?: getString(R.string.unavailable),
            ride.topSpeedKmh, ride.startLatitude, ride.startLongitude,
            ride.endLatitude, ride.endLongitude, store.pointsForRide(ride.rideId).size
        )))
    }

    private fun text(value: String) = TextView(this).apply {
        text = value
        textSize = 16f
    }

    companion object {
        const val EXTRA_RIDE_ID = "ride_id"
    }
}
