package com.aawararide.voice;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;
import java.util.Locale;

public final class VoiceCommandManager {
    private static final String TAG = "AawaraVoice";

    public interface Listener {
        boolean isCallRinging();
        void onAcceptRequested();
        void onRejectRequested();
    }

    private final Context context;
    private final Listener listener;
    private SpeechRecognizer recognizer;
    private boolean listening;
    private boolean actionHandled;
    private boolean sessionActive;
    private int sessionId;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public VoiceCommandManager(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
    }

    public boolean start() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Log.e(TAG, "start() called off the main thread");
            return false;
        }
        boolean available = SpeechRecognizer.isRecognitionAvailable(context);
        boolean microphoneGranted = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
        Log.i(TAG, "start() entered: listening=" + listening
                + ", sessionActive=" + sessionActive
                + ", available=" + available
                + ", microphoneGranted=" + microphoneGranted);
        if (sessionActive || !available || !microphoneGranted) {
            Log.w(TAG, "Voice recognition start skipped: active session, unavailable service, "
                    + "or microphone permission denied");
            return false;
        }
        final int currentSession = ++sessionId;
        try {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context);
        } catch (RuntimeException exception) {
            Log.e(TAG, "Recognizer creation failed", exception);
            recognizer = null;
            return false;
        }
        sessionActive = true;
        Log.i(TAG, "Recognizer created for session " + currentSession);
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {
                if (isCurrentSession(currentSession)) {
                    Log.i(TAG, "onReadyForSpeech: session " + currentSession);
                }
            }
            @Override public void onBeginningOfSpeech() {
                if (isCurrentSession(currentSession)) {
                    Log.i(TAG, "onBeginningOfSpeech: session " + currentSession);
                }
            }
            @Override public void onRmsChanged(float rmsdB) { }
            @Override public void onBufferReceived(byte[] buffer) { }
            @Override public void onEndOfSpeech() {
                if (isCurrentSession(currentSession)) {
                    listening = false;
                    Log.i(TAG, "onEndOfSpeech: session " + currentSession);
                }
            }
            @Override public void onError(int error) {
                if (isCurrentSession(currentSession)) {
                    listening = false;
                    Log.w(TAG, "onError: session " + currentSession + ", code=" + error
                            + " (" + errorDescription(error) + ")");
                }
            }
            @Override public void onResults(Bundle results) {
                if (!isCurrentSession(currentSession)) {
                    Log.i(TAG, "Ignoring stale onResults for session " + currentSession);
                    return;
                }
                listening = false;
                ArrayList<String> matches = results.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION);
                Log.i(TAG, "onResults: session " + currentSession + ", matches=" + matches);
                handleResults(results);
            }
            @Override public void onPartialResults(Bundle partialResults) { }
            @Override public void onEvent(int eventType, Bundle params) { }
        });
        actionHandled = false;
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
        listening = true;
        try {
            Log.i(TAG, "startListening() invoked for session " + currentSession);
            recognizer.startListening(intent);
            return true;
        } catch (RuntimeException exception) {
            Log.e(TAG, "startListening() failed for session " + currentSession, exception);
            listening = false;
            sessionActive = false;
            destroyRecognizer();
            return false;
        }
    }

    private void handleResults(Bundle results) {
        if (!sessionActive || actionHandled || !listener.isCallRinging()) {
            return;
        }
        ArrayList<String> matches = results.getStringArrayList(
                SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches == null) {
            return;
        }
        for (String match : matches) {
            String command = match.toLowerCase(Locale.ROOT).trim();
            if (command.equals("accept call") || command.equals("pick up")
                    || command.equals("answer call")) {
                actionHandled = true;
                listener.onAcceptRequested();
                return;
            }
            if (command.equals("reject call") || command.equals("decline call")
                    || command.equals("end call")) {
                actionHandled = true;
                listener.onRejectRequested();
                return;
            }
        }
    }

    public void stop() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post(this::stop);
            return;
        }
        Log.i(TAG, "stop(): listening=" + listening + ", sessionActive=" + sessionActive);
        listening = false;
        actionHandled = true;
        sessionActive = false;
        sessionId++;
        destroyRecognizer();
    }

    private boolean isCurrentSession(int currentSession) {
        return sessionActive && currentSession == sessionId;
    }

    private void destroyRecognizer() {
        if (recognizer == null) {
            return;
        }
        try {
            try {
                recognizer.cancel();
            } catch (RuntimeException exception) {
                Log.w(TAG, "Recognizer cancellation failed", exception);
            }
            try {
                recognizer.destroy();
            } catch (RuntimeException exception) {
                Log.w(TAG, "Recognizer destruction failed", exception);
            }
        } finally {
            recognizer = null;
            Log.i(TAG, "Recognizer destroyed");
        }
    }

    private static String errorDescription(int error) {
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO: return "audio";
            case SpeechRecognizer.ERROR_CLIENT: return "client";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: return "permissions";
            case SpeechRecognizer.ERROR_NETWORK: return "network";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: return "network_timeout";
            case SpeechRecognizer.ERROR_NO_MATCH: return "no_match";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: return "recognizer_busy";
            case SpeechRecognizer.ERROR_SERVER: return "server";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: return "speech_timeout";
            default: return "unknown";
        }
    }
}
