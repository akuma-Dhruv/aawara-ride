package com.aawararide.telephony;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.telecom.Call;
import android.telecom.CallScreeningService;

import com.aawararide.settings.AnnouncementSettingsRepository;

public final class AawaraCallScreeningService extends CallScreeningService {
    public static final String ACTION_SCREENED_INCOMING_CALL =
            "com.aawararide.action.SCREENED_INCOMING_CALL";
    public static final String EXTRA_HANDLE = "handle";

    @Override
    public void onScreenCall(Call.Details details) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                || details.getCallDirection() != Call.Details.DIRECTION_INCOMING) {
            return;
        }
        respondToCall(details, new CallResponse.Builder()
                .setDisallowCall(false)
                .setRejectCall(false)
                .setSilenceCall(false)
                .build());

        if (!new AnnouncementSettingsRepository(this).isRidingModeEnabled()) {
            return;
        }
        Uri handle = details.getHandle();
        Intent intent = new Intent(ACTION_SCREENED_INCOMING_CALL)
                .setPackage(getPackageName())
                .putExtra(EXTRA_HANDLE, handle == null ? null : handle.getSchemeSpecificPart());
        sendBroadcast(intent);
    }
}
