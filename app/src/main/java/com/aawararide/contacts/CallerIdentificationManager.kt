package com.aawararide.contacts

import android.content.ContentResolver
import android.net.Uri
import android.provider.ContactsContract
import android.text.TextUtils
import android.util.Log

class CallerIdentificationManager(private val resolver: ContentResolver) {
    fun announcementFor(number: String?): String {
        if (number.isNullOrEmpty()) return "Incoming call from a private number."
        val name = findDisplayName(number)
        return if (name == null) "Incoming call from an unknown number."
        else "Incoming call from $name."
    }

    private fun findDisplayName(number: String): String? {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
        try {
            resolver.query(
                uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val name = cursor.getString(0)
                    return if (TextUtils.isEmpty(name)) null else name
                }
            }
        } catch (_: SecurityException) {
            Log.w(TAG, "Contacts permission unavailable during caller lookup")
        }
        val normalized = normalize(number)
        if (normalized != number) {
            val normalizedUri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(normalized))
            try {
                resolver.query(
                    normalizedUri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                    null, null, null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val name = cursor.getString(0)
                        return if (TextUtils.isEmpty(name)) null else name
                    }
                }
            } catch (_: SecurityException) {
                Log.w(TAG, "Contacts permission unavailable during normalized lookup")
            }
        }
        return null
    }

    private fun normalize(number: String): String {
        val trimmed = number.trim()
        val plus = trimmed.startsWith("+")
        val digits = trimmed.replace(Regex("[^0-9]"), "")
        return if (plus) "+$digits" else digits
    }

    private companion object {
        const val TAG = "AawaraContacts"
    }
}
