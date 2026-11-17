package com.aawararide.ui

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import com.aawararide.R
import com.aawararide.ride.RideRecord
import com.aawararide.ride.RideStore
import com.aawararide.ride.RideTracker
import com.aawararide.service.RidingModeService
import com.aawararide.settings.AnnouncementSettingsRepository
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var ridingModeSwitch: Switch
    private lateinit var serviceStatus: TextView
    private lateinit var permissionStatus: TextView
    private lateinit var rideMetrics: TextView
    private lateinit var settings: AnnouncementSettingsRepository
    private lateinit var rideStore: RideStore

    private val rideUpdates = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (RideTracker.State.WAITING_FOR_GPS.name ==
                intent.getStringExtra(RidingModeService.EXTRA_RIDE_STATE)
            ) rideMetrics.setText(R.string.waiting_for_gps) else updateRideStatus()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ridingModeSwitch = findViewById(R.id.riding_mode_switch)
        serviceStatus = findViewById(R.id.service_status)
        permissionStatus = findViewById(R.id.permission_status)
        rideMetrics = findViewById(R.id.ride_metrics)
        settings = AnnouncementSettingsRepository(this)
        rideStore = RideStore(this)
        ridingModeSwitch.isChecked = settings.isRidingModeEnabled()
        ridingModeSwitch.setOnCheckedChangeListener { _, enabled ->
            if (enabled) {
                if (hasAllRequestedPermissions()) {
                    settings.setRidingModeEnabled(true)
                    requestCallScreeningRoleIfAvailable()
                    startRidingMode()
                } else requestRuntimePermissions()
            } else {
                settings.setRidingModeEnabled(false)
                startService(Intent(this, RidingModeService::class.java)
                    .setAction(RidingModeService.ACTION_END_RIDE))
                serviceStatus.setText(R.string.service_inactive)
            }
        }
        findViewById<Button>(R.id.settings_button).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<Button>(R.id.history_button).setOnClickListener {
            startActivity(Intent(this, RideHistoryActivity::class.java))
        }
        updatePermissionStatus()
        updateRideStatus()
        androidx.core.content.ContextCompat.registerReceiver(
            this, rideUpdates, IntentFilter(RidingModeService.ACTION_RIDE_UPDATE),
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onDestroy() {
        unregisterReceiver(rideUpdates)
        super.onDestroy()
    }

    private fun requestCallScreeningRoleIfAvailable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager?.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) == true &&
                !roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
            ) startActivityForResult(
                roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING), 20)
        }
    }

    private fun hasPhonePermission() = hasPermission(Manifest.permission.READ_PHONE_STATE)
    private fun hasMicrophonePermission() = hasPermission(Manifest.permission.RECORD_AUDIO)
    private fun hasLocationPermission() = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
        hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    private fun hasPermission(permission: String) =
        checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    private fun hasAllRequestedPermissions() =
        hasPhonePermission() && hasMicrophonePermission() &&
            hasPermission(Manifest.permission.READ_CONTACTS) &&
            hasPermission(Manifest.permission.ANSWER_PHONE_CALLS) &&
            hasLocationPermission() &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                hasPermission(Manifest.permission.POST_NOTIFICATIONS))

    private fun startRidingMode() {
        val serviceIntent = Intent(this, RidingModeService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(serviceIntent)
        else startService(serviceIntent)
        serviceStatus.setText(R.string.service_active)
        rideMetrics.setText(R.string.waiting_for_gps)
    }

    private fun requestRuntimePermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val requested = arrayOf(
            Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_CONTACTS,
            Manifest.permission.RECORD_AUDIO, Manifest.permission.ANSWER_PHONE_CALLS,
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        val permissions = requested.filterNot(::hasPermission).toMutableList()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        ) permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        if (permissions.isNotEmpty()) requestPermissions(permissions.toTypedArray(), REQUEST_PERMISSIONS)
    }

    private fun updatePermissionStatus() {
        when {
            !hasPhonePermission() -> permissionStatus.setText(R.string.permissions_required)
            !hasMicrophonePermission() -> permissionStatus.setText(R.string.voice_permission_required)
            !hasLocationPermission() -> permissionStatus.setText(R.string.location_permission_required)
            else -> permissionStatus.text = ""
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQUEST_PERMISSIONS) return
        updatePermissionStatus()
        if (hasAllRequestedPermissions()) {
            settings.setRidingModeEnabled(true)
            requestCallScreeningRoleIfAvailable()
            startRidingMode()
        } else {
            settings.setRidingModeEnabled(false)
            ridingModeSwitch.isChecked = false
        }
    }

    private fun updateRideStatus() {
        val active = rideStore.activeRide()
        if (active == null) {
            val history = rideStore.history()
            rideMetrics.text = if (history.isEmpty()) getString(R.string.no_ride_data)
            else formatRideSummary(history[0])
        } else if (active.startTimestamp == 0L) rideMetrics.setText(R.string.waiting_for_gps)
        else rideMetrics.text = formatRideSummary(active)
    }

    private fun formatRideSummary(ride: RideRecord): String {
        val movingAverageText = ride.movingAverageSpeedKmh()?.let {
            String.format(Locale.US, "%.1f km/h", it)
        } ?: getString(R.string.unavailable)
        val currentSpeedText = if (ride.hasFreshSpeed(System.currentTimeMillis())) {
            String.format(Locale.US, "%.1f km/h", ride.currentSpeedKmh)
        } else getString(R.string.unavailable)
        return getString(
            R.string.ride_metrics, ride.totalDistanceMeters / 1000.0,
            formatDuration(ride.totalDurationSeconds), formatDuration(ride.movingDurationSeconds),
            formatDuration(ride.idleDurationSeconds), ride.overallAverageSpeedKmh(),
            movingAverageText, ride.topSpeedKmh, currentSpeedText)
    }

    private fun formatDuration(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = totalSeconds % 3600 / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        else String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    private companion object {
        const val REQUEST_PERMISSIONS = 10
    }
}
