package com.aawararide.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

class VoiceCommandManager(context: Context, private val listener: Listener) {
    interface Listener {
        fun isCallRinging(): Boolean
        fun onAcceptRequested()
        fun onRejectRequested()
    }

    private val context = context.applicationContext
    private var recognizer: SpeechRecognizer? = null
    private var listening = false
    private var actionHandled = false
    private var sessionActive = false
    private var sessionId = 0
    private val mainHandler = Handler(Looper.getMainLooper())

    fun start(): Boolean {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Log.e(TAG, "start() called off the main thread")
            return false
        }
        val available = SpeechRecognizer.isRecognitionAvailable(context)
        val microphoneGranted = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        Log.i(TAG, "start() entered: listening=$listening, sessionActive=$sessionActive, available=$available, microphoneGranted=$microphoneGranted")
        if (sessionActive || !available || !microphoneGranted) {
            Log.w(TAG, "Voice recognition start skipped: active session, unavailable service, or microphone permission denied")
            return false
        }
        val currentSession = ++sessionId
        recognizer = try {
            SpeechRecognizer.createSpeechRecognizer(context)
        } catch (exception: RuntimeException) {
            Log.e(TAG, "Recognizer creation failed", exception)
            null
        } ?: return false
        sessionActive = true
        Log.i(TAG, "Recognizer created for session $currentSession")
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                if (isCurrentSession(currentSession)) Log.i(TAG, "onReadyForSpeech: session $currentSession")
            }
            override fun onBeginningOfSpeech() {
                if (isCurrentSession(currentSession)) Log.i(TAG, "onBeginningOfSpeech: session $currentSession")
            }
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() {
                if (isCurrentSession(currentSession)) {
                    listening = false
                    Log.i(TAG, "onEndOfSpeech: session $currentSession")
                }
            }
            override fun onError(error: Int) {
                if (isCurrentSession(currentSession)) {
                    listening = false
                    Log.w(TAG, "onError: session $currentSession, code=$error (${errorDescription(error)})")
                }
            }
            override fun onResults(results: Bundle?) {
                if (!isCurrentSession(currentSession)) {
                    Log.i(TAG, "Ignoring stale onResults for session $currentSession")
                    return
                }
                listening = false
                Log.i(TAG, "onResults: session $currentSession")
                if (results != null) handleResults(results)
            }
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        actionHandled = false
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        listening = true
        return try {
            Log.i(TAG, "startListening() invoked for session $currentSession")
            recognizer?.startListening(intent)
            true
        } catch (exception: RuntimeException) {
            Log.e(TAG, "startListening() failed for session $currentSession", exception)
            listening = false
            sessionActive = false
            destroyRecognizer()
            false
        }
    }

    private fun handleResults(results: Bundle) {
        if (!sessionActive || actionHandled || !listener.isCallRinging()) return
        val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return
        for (match in matches) {
            when (match.lowercase(Locale.ROOT).trim()) {
                "accept call", "pick up", "answer call" -> {
                    actionHandled = true
                    listener.onAcceptRequested()
                    return
                }
                "reject call", "decline call", "end call" -> {
                    actionHandled = true
                    listener.onRejectRequested()
                    return
                }
            }
        }
    }

    fun stop() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { stop() }
            return
        }
        Log.i(TAG, "stop(): listening=$listening, sessionActive=$sessionActive")
        listening = false
        actionHandled = true
        sessionActive = false
        sessionId++
        destroyRecognizer()
    }

    private fun isCurrentSession(currentSession: Int) =
        sessionActive && currentSession == sessionId

    private fun destroyRecognizer() {
        val current = recognizer ?: return
        try {
            try {
                current.cancel()
            } catch (exception: RuntimeException) {
                Log.w(TAG, "Recognizer cancellation failed", exception)
            }
            try {
                current.destroy()
            } catch (exception: RuntimeException) {
                Log.w(TAG, "Recognizer destruction failed", exception)
            }
        } finally {
            recognizer = null
            Log.i(TAG, "Recognizer destroyed")
        }
    }

    private fun errorDescription(error: Int) = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "audio"
        SpeechRecognizer.ERROR_CLIENT -> "client"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "permissions"
        SpeechRecognizer.ERROR_NETWORK -> "network"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "network_timeout"
        SpeechRecognizer.ERROR_NO_MATCH -> "no_match"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "recognizer_busy"
        SpeechRecognizer.ERROR_SERVER -> "server"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "speech_timeout"
        else -> "unknown"
    }

    private companion object {
        const val TAG = "AawaraVoice"
    }
}
