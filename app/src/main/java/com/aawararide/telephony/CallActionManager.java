package com.aawararide.telephony;

import android.content.Context;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telecom.TelecomManager;
import android.util.Log;

public final class CallActionManager {
    private static final String TAG = "AawaraCallAction";
    private final Context context;
    private final TelecomManager telecomManager;

    public CallActionManager(Context context) {
        this.context = context.getApplicationContext();
        telecomManager = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
    }

    public boolean acceptRingingCall() {
        Log.i(TAG, "Accept request: api=" + Build.VERSION.SDK_INT
                + ", permissionGranted=" + !contextPermissionDenied());
        if (telecomManager == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Log.w(TAG, "Accept unavailable: TelecomManager or API level");
            return false;
        }
        if (contextPermissionDenied()) {
            Log.w(TAG, "Accept unavailable: ANSWER_PHONE_CALLS denied");
            return false;
        }
        try {
            Log.i(TAG, "Invoking TelecomManager.acceptRingingCall()");
            telecomManager.acceptRingingCall();
            Log.i(TAG, "acceptRingingCall() invocation completed; awaiting phone state");
            return true;
        } catch (SecurityException exception) {
            Log.w(TAG, "Answer denied by Android; native call controls remain available",
                    exception);
            return false;
        }
    }

    public boolean rejectRingingCall() {
        Log.i(TAG, "Reject request: api=" + Build.VERSION.SDK_INT
                + ", permissionGranted=" + !contextPermissionDenied());
        if (telecomManager == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            Log.w(TAG, "Reject unavailable: TelecomManager or API level");
            return false;
        }
        if (contextPermissionDenied()) {
            Log.w(TAG, "Reject unavailable: ANSWER_PHONE_CALLS denied");
            return false;
        }
        try {
            Log.i(TAG, "Invoking TelecomManager.endCall()");
            telecomManager.endCall();
            Log.i(TAG, "endCall() invocation completed; awaiting phone state");
            return true;
        } catch (SecurityException exception) {
            Log.w(TAG, "Reject denied by Android; native call controls remain available",
                    exception);
            return false;
        }
    }

    private boolean contextPermissionDenied() {
        return context.checkSelfPermission(Manifest.permission.ANSWER_PHONE_CALLS)
                != PackageManager.PERMISSION_GRANTED;
    }
}
