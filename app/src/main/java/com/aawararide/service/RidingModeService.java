package com.aawararide.service;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.os.Build;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.aawararide.R;
import com.aawararide.audio.AnnouncementManager;
import com.aawararide.contacts.CallerIdentificationManager;
import com.aawararide.settings.AnnouncementSettingsRepository;

public final class RidingModeService extends Service {
    private static final String TAG = "AawaraService";
    private static final String CHANNEL_ID = "riding_mode";
    private static final int NOTIFICATION_ID = 1001;
    private AnnouncementManager announcementManager;
    private BroadcastReceiver callReceiver;
    private boolean ringing;
    private boolean announcementStarted;

    @Override
    public void onCreate() {
        super.onCreate();
        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            stopSelf();
            return;
        }
        createNotificationChannel();
        announcementManager = new AnnouncementManager(
                this, new AnnouncementSettingsRepository(this));
        callReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handleCallState(intent);
            }
        };
        IntentFilter filter = new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        ContextCompat.registerReceiver(
                this, callReceiver, filter, ContextCompat.RECEIVER_EXPORTED);
        IntentFilter screenedFilter = new IntentFilter(
                com.aawararide.telephony.AawaraCallScreeningService
                        .ACTION_SCREENED_INCOMING_CALL);
        ContextCompat.registerReceiver(
                this, callReceiver, screenedFilter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, buildNotification());
        return START_NOT_STICKY;
    }

    private void handleCallState(Intent intent) {
        if (com.aawararide.telephony.AawaraCallScreeningService
                .ACTION_SCREENED_INCOMING_CALL.equals(intent.getAction())) {
            announceCaller(intent.getStringExtra(
                    com.aawararide.telephony.AawaraCallScreeningService.EXTRA_HANDLE));
            return;
        }
        if (!TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intent.getAction())) {
            return;
        }
        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        Log.i(TAG, "Phone state changed: " + state);
        if (TelephonyManager.EXTRA_STATE_RINGING.equals(state) && !ringing) {
            ringing = true;
            String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            announceCaller(number);
        } else if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)
                || TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
            ringing = false;
            announcementStarted = false;
            announcementManager.cancelPending();
        }
    }

    private void announceCaller(String number) {
        if (announcementStarted || announcementManager == null) {
            return;
        }
        announcementStarted = true;
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED) {
            announcementManager.announce(
                    new CallerIdentificationManager(getContentResolver()).announcementFor(number));
        } else {
            Log.w(TAG, "Contacts permission unavailable; announcing unknown caller");
            announcementManager.announce("Incoming call from an unknown number.");
        }
    }

    private Notification buildNotification() {
        return new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_call)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_text))
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW));
        }
    }

    @Override
    public void onDestroy() {
        if (callReceiver != null) {
            unregisterReceiver(callReceiver);
        }
        if (announcementManager != null) {
            announcementManager.release();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
