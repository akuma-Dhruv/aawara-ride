package com.aawararide.contacts;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;

public final class CallerIdentificationManager {
    private static final String TAG = "AawaraContacts";
    private final ContentResolver resolver;

    public CallerIdentificationManager(ContentResolver resolver) {
        this.resolver = resolver;
    }

    public String announcementFor(String number) {
        if (TextUtils.isEmpty(number)) {
            return "Incoming call from a private number.";
        }
        String name = findDisplayName(number);
        return name == null
                ? "Incoming call from an unknown number."
                : "Incoming call from " + name + ".";
    }

    private String findDisplayName(String number) {
        Uri uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number));
        try (Cursor cursor = resolver.query(
                uri,
                new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME},
                null,
                null,
                null)) {
            if (cursor != null && cursor.moveToFirst()) {
                String name = cursor.getString(0);
                return TextUtils.isEmpty(name) ? null : name;
            }
        } catch (SecurityException exception) {
            Log.w(TAG, "Contacts permission unavailable during caller lookup");
        }
        String normalized = normalize(number);
        if (!normalized.equals(number)) {
            Uri normalizedUri = Uri.withAppendedPath(
                    ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                    Uri.encode(normalized));
            try (Cursor cursor = resolver.query(
                    normalizedUri,
                    new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME},
                    null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    String name = cursor.getString(0);
                    return TextUtils.isEmpty(name) ? null : name;
                }
            } catch (SecurityException exception) {
                Log.w(TAG, "Contacts permission unavailable during normalized lookup");
            }
        }
        return null;
    }

    private static String normalize(String number) {
        String trimmed = number.trim();
        boolean plus = trimmed.startsWith("+");
        String digits = trimmed.replaceAll("[^0-9]", "");
        return plus ? "+" + digits : digits;
    }
}
