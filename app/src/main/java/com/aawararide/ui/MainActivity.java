package com.aawararide.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.app.role.RoleManager;

import com.aawararide.R;
import com.aawararide.service.RidingModeService;
import com.aawararide.settings.AnnouncementSettingsRepository;

public final class MainActivity extends Activity {
    private static final int REQUEST_PERMISSIONS = 10;
    private Switch ridingModeSwitch;
    private TextView serviceStatus;
    private TextView permissionStatus;
    private AnnouncementSettingsRepository settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ridingModeSwitch = findViewById(R.id.riding_mode_switch);
        serviceStatus = findViewById(R.id.service_status);
        permissionStatus = findViewById(R.id.permission_status);
        settings = new AnnouncementSettingsRepository(this);
        ridingModeSwitch.setChecked(settings.isRidingModeEnabled());

        ridingModeSwitch.setOnCheckedChangeListener((button, enabled) -> {
            if (enabled) {
                if (hasPhonePermission()) {
                    settings.setRidingModeEnabled(true);
                    requestCallScreeningRoleIfAvailable();
                    startRidingMode();
                } else {
                    settings.setRidingModeEnabled(false);
                    ridingModeSwitch.setChecked(false);
                    requestRuntimePermissions();
                }
            } else {
                stopService(new Intent(this, RidingModeService.class));
                serviceStatus.setText(R.string.service_inactive);
            }
        });
        Button settings = findViewById(R.id.settings_button);
        settings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        updatePermissionStatus();
    }

    private void requestCallScreeningRoleIfAvailable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roleManager = getSystemService(RoleManager.class);
            if (roleManager != null
                    && roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)
                    && !roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                startActivityForResult(
                        roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING),
                        20);
            }
        }
    }

    private boolean hasPhonePermission() {
        return checkSelfPermission(Manifest.permission.READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void startRidingMode() {
        Intent serviceIntent = new Intent(this, RidingModeService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        serviceStatus.setText(R.string.service_active);
    }

    private void requestRuntimePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissions(new String[]{
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.POST_NOTIFICATIONS
                }, REQUEST_PERMISSIONS);
            } else {
                requestPermissions(new String[]{
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.READ_CONTACTS
                }, REQUEST_PERMISSIONS);
            }
        }
    }

    private void updatePermissionStatus() {
        permissionStatus.setText(hasPhonePermission()
                ? ""
                : getString(R.string.permissions_required));
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            updatePermissionStatus();
            if (hasPhonePermission()) {
                ridingModeSwitch.setChecked(true);
            }
        }
    }
}
