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
import android.os.Handler;
import android.os.Looper;

import androidx.core.content.ContextCompat;

import com.aawararide.R;
import com.aawararide.audio.AnnouncementManager;
import com.aawararide.contacts.CallerIdentificationManager;
import com.aawararide.settings.AnnouncementSettingsRepository;
import com.aawararide.telephony.CallActionManager;
import com.aawararide.voice.VoiceCommandManager;
import com.aawararide.ride.RideRecord;
import com.aawararide.ride.RideTracker;

public final class RidingModeService extends Service {
    public static final String ACTION_END_RIDE = "com.aawararide.action.END_RIDE";
    public static final String ACTION_RIDE_UPDATE = "com.aawararide.action.RIDE_UPDATE";
    public static final String EXTRA_RIDE_STATE = "ride_state";
    public static final String EXTRA_RIDE = "ride";
    private static final String TAG = "AawaraService";
    private static final String CHANNEL_ID = "riding_mode";
    private static final int NOTIFICATION_ID = 1001;
    private AnnouncementManager announcementManager;
    private BroadcastReceiver callReceiver;
    private boolean ringing;
    private boolean announcementStarted;
    private String pendingCallAction;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private VoiceCommandManager voiceCommandManager;
    private CallActionManager callActionManager;
    private RideTracker rideTracker;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Service created; riding mode is active");
        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Service stopping: READ_PHONE_STATE is not granted");
            stopSelf();
            return;
        }
        createNotificationChannel();
        rideTracker = new RideTracker(this, new RideTracker.Listener() {
            @Override
            public void onStateChanged(RideTracker.State state, RideRecord ride) {
                sendRideUpdate(state, ride);
            }

            @Override
            public void onRideCompleted(RideRecord ride) {
                sendRideUpdate(null, ride);
            }
        });
        announcementManager = new AnnouncementManager(
                this, new AnnouncementSettingsRepository(this));
        callActionManager = new CallActionManager(this);
        voiceCommandManager = new VoiceCommandManager(this, new VoiceCommandManager.Listener() {
            @Override
            public boolean isCallRinging() {
                return ringing;
            }

            @Override
            public void onAcceptRequested() {
                Log.i(TAG, "Accept requested by voice command");
                pendingCallAction = "accept";
                if (callActionManager.acceptRingingCall()) {
                    stopVoiceRecognition("accept request");
                    announcementManager.cancelPending();
                } else {
                    pendingCallAction = null;
                    Log.w(TAG, "Accept request was not accepted by TelecomManager");
                }
            }

            @Override
            public void onRejectRequested() {
                Log.i(TAG, "Reject requested by voice command");
                pendingCallAction = "reject";
                if (callActionManager.rejectRingingCall()) {
                    stopVoiceRecognition("reject request");
                    announcementManager.cancelPending();
                } else {
                    pendingCallAction = null;
                    Log.w(TAG, "Reject request was not accepted by TelecomManager");
                }
            }
        });
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
        if (intent != null && ACTION_END_RIDE.equals(intent.getAction())) {
            if (rideTracker != null) {
                rideTracker.stopAndComplete();
            }
            stopSelf(startId);
            return START_NOT_STICKY;
        }
        if (rideTracker != null) {
            rideTracker.start();
        }
        Log.i(TAG, "Service started in foreground");
        return START_NOT_STICKY;
    }

    private void sendRideUpdate(RideTracker.State state, RideRecord ride) {
        Intent update = new Intent(ACTION_RIDE_UPDATE);
        update.setPackage(getPackageName());
        if (state != null) {
            update.putExtra(EXTRA_RIDE_STATE, state.name());
        }
        if (ride != null) {
            update.putExtra(EXTRA_RIDE, ride.rideId);
        }
        sendBroadcast(update);
    }

    private void handleCallState(Intent intent) {
        if (com.aawararide.telephony.AawaraCallScreeningService
                .ACTION_SCREENED_INCOMING_CALL.equals(intent.getAction())) {
            Log.i(TAG, "Call-screening broadcast received: ringing=" + ringing
                    + ", announcementStarted=" + announcementStarted);
            if (!ringing) {
                ringing = true;
                Log.i(TAG, "Incoming call state established from call-screening broadcast");
            }
            announceCaller(intent.getStringExtra(
                    com.aawararide.telephony.AawaraCallScreeningService.EXTRA_HANDLE));
            startVoiceRecognition();
            return;
        }
        if (!TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intent.getAction())) {
            return;
        }
        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        Log.i(TAG, "Phone state changed: " + state + ", ringing=" + ringing
                + ", announcementStarted=" + announcementStarted);
        if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
            if (!ringing) {
                ringing = true;
                Log.i(TAG, "Incoming call state established from phone-state broadcast");
            }
            String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            if (!announcementStarted) {
                announceCaller(number);
            }
            startVoiceRecognition();
        } else if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)
                || TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
            ringing = false;
            announcementStarted = false;
            Log.i(TAG, "Call ended or answered; stopping recognition: " + state);
            if (pendingCallAction != null) {
                Log.i(TAG, "Verified call action transition: " + pendingCallAction
                        + " -> " + state);
                pendingCallAction = null;
            }
            stopVoiceRecognition("phone state " + state);
            announcementManager.cancelPending();
        }

    }

    private void startVoiceRecognition() {
        if (!ringing) {
            Log.i(TAG, "Voice recognition skipped: call is not ringing");
            return;
        }
        if (voiceCommandManager == null) {
            Log.w(TAG, "Voice recognition skipped: manager is unavailable");
            return;
        }
        boolean started = voiceCommandManager.start();
        Log.i(TAG, "Voice recognition start attempt: started=" + started
                + ", ringing=" + ringing + ", announcementStarted=" + announcementStarted);
    }

    private void stopVoiceRecognition() {
        stopVoiceRecognition("unspecified");
    }

    private void stopVoiceRecognition(String reason) {
        handler.removeCallbacksAndMessages(null);
        if (voiceCommandManager != null) {
            Log.i(TAG, "Stopping voice recognition: " + reason);
            voiceCommandManager.stop();
        }
    }

    private void announceCaller(String number) {
        if (announcementStarted || announcementManager == null) {
            Log.i(TAG, "Caller announcement skipped: announcementStarted="
                    + announcementStarted + ", managerAvailable="
                    + (announcementManager != null));
            return;
        }
        announcementStarted = true;
        Log.i(TAG, "Starting caller announcement; ringing=" + ringing);
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED) {
            announcementManager.announce(
                    new CallerIdentificationManager(getContentResolver()).announcementFor(number),
                    this::startVoiceRecognition);
        } else {
            Log.w(TAG, "Contacts permission unavailable; announcing unknown caller");
            announcementManager.announce(
                    "Incoming call from an unknown number.", this::startVoiceRecognition);
        }
        startVoiceRecognition();
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
        Log.i(TAG, "Service destroying");
        stopVoiceRecognition("service destroyed");
        if (rideTracker != null) {
            rideTracker.stopWithoutCompleting();
        }
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
