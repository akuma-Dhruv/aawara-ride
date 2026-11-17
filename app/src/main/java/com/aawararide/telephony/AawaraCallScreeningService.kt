package com.aawararide.telephony

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import com.aawararide.settings.AnnouncementSettingsRepository

class AawaraCallScreeningService : CallScreeningService() {
    override fun onScreenCall(details: Call.Details) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            details.callDirection != Call.Details.DIRECTION_INCOMING
        ) return
        respondToCall(
            details,
            CallResponse.Builder().setDisallowCall(false).setRejectCall(false)
                .setSilenceCall(false).build()
        )
        if (!AnnouncementSettingsRepository(this).isRidingModeEnabled()) return
        val handle: Uri? = details.handle
        sendBroadcast(
            Intent(ACTION_SCREENED_INCOMING_CALL).setPackage(packageName)
                .putExtra(EXTRA_HANDLE, handle?.schemeSpecificPart)
        )
    }

    companion object {
        const val ACTION_SCREENED_INCOMING_CALL =
            "com.aawararide.action.SCREENED_INCOMING_CALL"
        const val EXTRA_HANDLE = "handle"
    }
}
