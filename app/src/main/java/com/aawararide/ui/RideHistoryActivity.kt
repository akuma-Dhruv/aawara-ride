package com.aawararide.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.aawararide.R
import com.aawararide.ride.DurationFormatter
import com.aawararide.ride.RideRecord
import com.aawararide.ride.RideStore
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class RideHistoryActivity : Activity() {
    private val dateFormat = DateFormat.getDateTimeInstance(
        DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.ride_history)
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }
        setContentView(list)
        val rides = RideStore(this).completedRides()
        if (rides.isEmpty()) {
            list.addView(TextView(this).apply {
                setText(R.string.no_ride_history)
                gravity = Gravity.CENTER
            }, LinearLayout.LayoutParams(-1, -1))
            return
        }
        rides.forEach { ride ->
            list.addView(Button(this).apply {
                isAllCaps = false
                text = historyText(ride)
                setOnClickListener {
                    startActivity(Intent(this@RideHistoryActivity, RideDetailsActivity::class.java)
                        .putExtra(RideDetailsActivity.EXTRA_RIDE_ID, ride.rideId))
                }
            }, LinearLayout.LayoutParams(-1, -2))
        }
    }

    private fun historyText(ride: RideRecord): String {
        val movingAverage = ride.movingAverageSpeedKmh()
        return dateFormat.format(Date(ride.startTimestamp)) + "\n" + getString(
            R.string.history_row, ride.totalDistanceMeters / 1000.0,
            DurationFormatter.format(ride.totalDurationSeconds),
            DurationFormatter.format(ride.movingDurationSeconds),
            DurationFormatter.format(ride.idleDurationSeconds),
            ride.overallAverageSpeedKmh(),
            movingAverage?.let { String.format(Locale.US, "%.1f km/h", it) }
                ?: getString(R.string.unavailable),
            ride.topSpeedKmh
        )
    }
}
