package com.aawararide.ui;

import android.Manifest;
import android.app.Activity;
import android.app.role.RoleManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;

import com.aawararide.R;
import com.aawararide.ride.RideRecord;
import com.aawararide.ride.RideStore;
import com.aawararide.ride.RideTracker;
import com.aawararide.service.RidingModeService;
import com.aawararide.settings.AnnouncementSettingsRepository;

public final class MainActivity extends Activity {
    private static final int REQUEST_PERMISSIONS = 10;
    private Switch ridingModeSwitch;
    private TextView serviceStatus;
    private TextView permissionStatus;
    private TextView rideMetrics;
    private AnnouncementSettingsRepository settings;
    private RideStore rideStore;

    private final BroadcastReceiver rideUpdates = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (RideTracker.State.WAITING_FOR_GPS.name().equals(
                    intent.getStringExtra(RidingModeService.EXTRA_RIDE_STATE))) {
                rideMetrics.setText(R.string.waiting_for_gps);
            } else {
                updateRideStatus();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ridingModeSwitch = findViewById(R.id.riding_mode_switch);
        serviceStatus = findViewById(R.id.service_status);
        permissionStatus = findViewById(R.id.permission_status);
        rideMetrics = findViewById(R.id.ride_metrics);
        settings = new AnnouncementSettingsRepository(this);
        rideStore = new RideStore(this);
        ridingModeSwitch.setChecked(settings.isRidingModeEnabled());

        ridingModeSwitch.setOnCheckedChangeListener((button, enabled) -> {
            if (enabled) {
                if (hasAllRequestedPermissions()) {
                    settings.setRidingModeEnabled(true);
                    requestCallScreeningRoleIfAvailable();
                    startRidingMode();
                } else {
                    requestRuntimePermissions();
                }
            } else {
                settings.setRidingModeEnabled(false);
                Intent endRide = new Intent(this, RidingModeService.class)
                        .setAction(RidingModeService.ACTION_END_RIDE);
                startService(endRide);
                serviceStatus.setText(R.string.service_inactive);
            }
        });
        Button settingsButton = findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(
                view -> startActivity(new Intent(this, SettingsActivity.class)));
        updatePermissionStatus();
        updateRideStatus();
        androidx.core.content.ContextCompat.registerReceiver(
                this,
                rideUpdates,
                new IntentFilter(RidingModeService.ACTION_RIDE_UPDATE),
                androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(rideUpdates);
        super.onDestroy();
    }

    private void requestCallScreeningRoleIfAvailable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roleManager = getSystemService(RoleManager.class);
            if (roleManager != null
                    && roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)
                    && !roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                startActivityForResult(
                        roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING), 20);
            }
        }
    }

    private boolean hasPhonePermission() {
        return checkSelfPermission(Manifest.permission.READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasMicrophonePermission() {
        return checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasLocationPermission() {
        return hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                || hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
    }

    private boolean hasAllRequestedPermissions() {
        return hasPhonePermission()
                && hasMicrophonePermission()
                && hasPermission(Manifest.permission.READ_CONTACTS)
                && hasPermission(Manifest.permission.ANSWER_PHONE_CALLS)
                && hasLocationPermission()
                && (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || hasPermission(Manifest.permission.POST_NOTIFICATIONS));
    }

    private boolean hasPermission(String permission) {
        return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void startRidingMode() {
        Intent serviceIntent = new Intent(this, RidingModeService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        serviceStatus.setText(R.string.service_active);
        rideMetrics.setText(R.string.waiting_for_gps);
    }

    private void requestRuntimePermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        java.util.ArrayList<String> permissions = new java.util.ArrayList<>();
        String[] requested = {
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ANSWER_PHONE_CALLS,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };
        for (String permission : requested) {
            if (!hasPermission(permission)) {
                permissions.add(permission);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && !hasPermission(Manifest.permission.POST_NOTIFICATIONS)) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (!permissions.isEmpty()) {
            requestPermissions(permissions.toArray(new String[0]), REQUEST_PERMISSIONS);
        }
    }

    private void updatePermissionStatus() {
        if (!hasPhonePermission()) {
            permissionStatus.setText(R.string.permissions_required);
        } else if (!hasMicrophonePermission()) {
            permissionStatus.setText(R.string.voice_permission_required);
        } else if (!hasLocationPermission()) {
            permissionStatus.setText(R.string.location_permission_required);
        } else {
            permissionStatus.setText("");
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_PERMISSIONS) {
            return;
        }
        updatePermissionStatus();
        if (hasAllRequestedPermissions()) {
            settings.setRidingModeEnabled(true);
            requestCallScreeningRoleIfAvailable();
            startRidingMode();
        } else {
            settings.setRidingModeEnabled(false);
            ridingModeSwitch.setChecked(false);
        }
    }

    private void updateRideStatus() {
        if (rideStore == null || rideMetrics == null) {
            return;
        }
        RideRecord active = rideStore.activeRide();
        if (active == null) {
            java.util.List<RideRecord> history = rideStore.history();
            rideMetrics.setText(history.isEmpty()
                    ? getString(R.string.no_ride_data)
                    : formatRideSummary(history.get(0)));
            return;
        }
        if (active.startTimestamp == 0) {
            rideMetrics.setText(R.string.waiting_for_gps);
        } else {
            rideMetrics.setText(formatRideSummary(active));
        }
    }

    private String formatRideSummary(RideRecord ride) {
        Double movingAverage = ride.movingAverageSpeedKmh();
        String movingAverageText = movingAverage == null
                ? getString(R.string.unavailable)
                : String.format(java.util.Locale.US, "%.1f km/h", movingAverage);
        String currentSpeedText = ride.hasFreshSpeed(System.currentTimeMillis())
                ? String.format(java.util.Locale.US, "%.1f km/h", ride.currentSpeedKmh)
                : getString(R.string.unavailable);
        return getString(
                R.string.ride_metrics,
                ride.totalDistanceMeters / 1000.0,
                formatDuration(ride.totalDurationSeconds),
                formatDuration(ride.movingDurationSeconds),
                formatDuration(ride.idleDurationSeconds),
                ride.overallAverageSpeedKmh(),
                movingAverageText,
                ride.topSpeedKmh,
                currentSpeedText);
    }

    private String formatDuration(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return hours > 0
                ? String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
                : String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds);
    }
}
