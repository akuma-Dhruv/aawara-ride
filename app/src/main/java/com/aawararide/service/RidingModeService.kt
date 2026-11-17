package com.aawararide.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.aawararide.R
import com.aawararide.audio.AnnouncementManager
import com.aawararide.contacts.CallerIdentificationManager
import com.aawararide.ride.RideRecord
import com.aawararide.ride.RideTracker
import com.aawararide.settings.AnnouncementSettingsRepository
import com.aawararide.telephony.AawaraCallScreeningService
import com.aawararide.telephony.CallActionManager
import com.aawararide.voice.VoiceCommandManager

class RidingModeService : Service() {
    private var announcementManager: AnnouncementManager? = null
    private var callReceiver: BroadcastReceiver? = null
    private var ringing = false
    private var announcementStarted = false
    private var pendingCallAction: String? = null
    private val handler = Handler(Looper.getMainLooper())
    private var voiceCommandManager: VoiceCommandManager? = null
    private var callActionManager: CallActionManager? = null
    private var rideTracker: RideTracker? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Service created; riding mode is active")
        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Service stopping: READ_PHONE_STATE is not granted")
            stopSelf()
            return
        }
        createNotificationChannel()
        rideTracker = RideTracker(this, object : RideTracker.Listener {
            override fun onStateChanged(state: RideTracker.State, ride: RideRecord?) =
                sendRideUpdate(state, ride)
            override fun onRideCompleted(ride: RideRecord) = sendRideUpdate(null, ride)
        })
        announcementManager = AnnouncementManager(this, AnnouncementSettingsRepository(this))
        callActionManager = CallActionManager(this)
        voiceCommandManager = VoiceCommandManager(this, object : VoiceCommandManager.Listener {
            override fun isCallRinging() = ringing
            override fun onAcceptRequested() {
                Log.i(TAG, "Accept requested by voice command")
                pendingCallAction = "accept"
                if (callActionManager?.acceptRingingCall() == true) {
                    stopVoiceRecognition("accept request")
                    announcementManager?.cancelPending()
                } else {
                    pendingCallAction = null
                    Log.w(TAG, "Accept request was not accepted by TelecomManager")
                }
            }
            override fun onRejectRequested() {
                Log.i(TAG, "Reject requested by voice command")
                pendingCallAction = "reject"
                if (callActionManager?.rejectRingingCall() == true) {
                    stopVoiceRecognition("reject request")
                    announcementManager?.cancelPending()
                } else {
                    pendingCallAction = null
                    Log.w(TAG, "Reject request was not accepted by TelecomManager")
                }
            }
        })
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) = handleCallState(intent)
        }
        callReceiver = receiver
        ContextCompat.registerReceiver(
            this, receiver, IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED),
            ContextCompat.RECEIVER_EXPORTED
        )
        ContextCompat.registerReceiver(
            this, receiver, IntentFilter(AawaraCallScreeningService.ACTION_SCREENED_INCOMING_CALL),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        if (intent?.action == ACTION_END_RIDE) {
            rideTracker?.stopAndComplete()
            stopSelf(startId)
            return START_NOT_STICKY
        }
        rideTracker?.start()
        Log.i(TAG, "Service started in foreground")
        return START_NOT_STICKY
    }

    private fun sendRideUpdate(state: RideTracker.State?, ride: RideRecord?) {
        val update = Intent(ACTION_RIDE_UPDATE).setPackage(packageName)
        state?.let { update.putExtra(EXTRA_RIDE_STATE, it.name) }
        ride?.let { update.putExtra(EXTRA_RIDE, it.rideId) }
        sendBroadcast(update)
    }

    private fun handleCallState(intent: Intent) {
        if (intent.action == AawaraCallScreeningService.ACTION_SCREENED_INCOMING_CALL) {
            Log.i(TAG, "Call-screening broadcast received: ringing=$ringing, announcementStarted=$announcementStarted")
            if (!ringing) ringing = true
            announceCaller(intent.getStringExtra(AawaraCallScreeningService.EXTRA_HANDLE))
            startVoiceRecognition()
            return
        }
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        Log.i(TAG, "Phone state changed: $state, ringing=$ringing, announcementStarted=$announcementStarted")
        if (state == TelephonyManager.EXTRA_STATE_RINGING) {
            if (!ringing) ringing = true
            if (!announcementStarted) announceCaller(
                intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER))
            startVoiceRecognition()
        } else if (state == TelephonyManager.EXTRA_STATE_OFFHOOK ||
            state == TelephonyManager.EXTRA_STATE_IDLE
        ) {
            ringing = false
            announcementStarted = false
            Log.i(TAG, "Call ended or answered; stopping recognition: $state")
            pendingCallAction?.let {
                Log.i(TAG, "Verified call action transition: $it -> $state")
                pendingCallAction = null
            }
            stopVoiceRecognition("phone state $state")
            announcementManager?.cancelPending()
        }
    }

    private fun startVoiceRecognition() {
        if (!ringing) return
        val manager = voiceCommandManager ?: return
        Log.i(TAG, "Voice recognition start attempt: started=${manager.start()}, ringing=$ringing, announcementStarted=$announcementStarted")
    }

    private fun stopVoiceRecognition(reason: String = "unspecified") {
        handler.removeCallbacksAndMessages(null)
        Log.i(TAG, "Stopping voice recognition: $reason")
        voiceCommandManager?.stop()
    }

    private fun announceCaller(number: String?) {
        val manager = announcementManager ?: return
        if (announcementStarted) return
        announcementStarted = true
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            manager.announce(
                CallerIdentificationManager(contentResolver).announcementFor(number),
                ::startVoiceRecognition
            )
        } else {
            Log.w(TAG, "Contacts permission unavailable; announcing unknown caller")
            manager.announce("Incoming call from an unknown number.", ::startVoiceRecognition)
        }
        startVoiceRecognition()
    }

    private fun buildNotification() = Notification.Builder(this, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_menu_call)
        .setContentTitle(getString(R.string.notification_title))
        .setContentText(getString(R.string.notification_text))
        .setOngoing(true)
        .build()

    private fun createNotificationChannel() {
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID, getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
        )
    }

    override fun onDestroy() {
        Log.i(TAG, "Service destroying")
        stopVoiceRecognition("service destroyed")
        rideTracker?.stopWithoutCompleting()
        callReceiver?.let { unregisterReceiver(it) }
        announcementManager?.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_END_RIDE = "com.aawararide.action.END_RIDE"
        const val ACTION_RIDE_UPDATE = "com.aawararide.action.RIDE_UPDATE"
        const val EXTRA_RIDE_STATE = "ride_state"
        const val EXTRA_RIDE = "ride"
        private const val TAG = "AawaraService"
        private const val CHANNEL_ID = "riding_mode"
        private const val NOTIFICATION_ID = 1001
    }
}
