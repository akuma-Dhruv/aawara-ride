package com.aawararide.telephony

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.TelecomManager
import android.util.Log

class CallActionManager(context: Context) {
    private val context = context.applicationContext
    private val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager

    fun acceptRingingCall(): Boolean {
        Log.i(TAG, "Accept request: api=${Build.VERSION.SDK_INT}, permissionGranted=${!contextPermissionDenied()}")
        if (telecomManager == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Log.w(TAG, "Accept unavailable: TelecomManager or API level")
            return false
        }
        if (contextPermissionDenied()) {
            Log.w(TAG, "Accept unavailable: ANSWER_PHONE_CALLS denied")
            return false
        }
        return try {
            Log.i(TAG, "Invoking TelecomManager.acceptRingingCall()")
            telecomManager.acceptRingingCall()
            Log.i(TAG, "acceptRingingCall() invocation completed; awaiting phone state")
            true
        } catch (exception: SecurityException) {
            Log.w(TAG, "Answer denied by Android; native call controls remain available", exception)
            false
        }
    }

    fun rejectRingingCall(): Boolean {
        Log.i(TAG, "Reject request: api=${Build.VERSION.SDK_INT}, permissionGranted=${!contextPermissionDenied()}")
        if (telecomManager == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            Log.w(TAG, "Reject unavailable: TelecomManager or API level")
            return false
        }
        if (contextPermissionDenied()) {
            Log.w(TAG, "Reject unavailable: ANSWER_PHONE_CALLS denied")
            return false
        }
        return try {
            Log.i(TAG, "Invoking TelecomManager.endCall()")
            telecomManager.endCall()
            Log.i(TAG, "endCall() invocation completed; awaiting phone state")
            true
        } catch (exception: SecurityException) {
            Log.w(TAG, "Reject denied by Android; native call controls remain available", exception)
            false
        }
    }

    private fun contextPermissionDenied() =
        context.checkSelfPermission(Manifest.permission.ANSWER_PHONE_CALLS) !=
            PackageManager.PERMISSION_GRANTED

    private companion object {
        const val TAG = "AawaraCallAction"
    }
}
