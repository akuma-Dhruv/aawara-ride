package com.aawararide.audio;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import com.aawararide.settings.AnnouncementSettingsRepository;

import java.util.Locale;

public final class AnnouncementManager implements TextToSpeech.OnInitListener {
    private static final String TAG = "AawaraAudio";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final AnnouncementSettingsRepository settings;
    private TextToSpeech textToSpeech;
    private final AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;
    private boolean ready;
    private String currentAnnouncement;
    private int repeatCount;

    public AnnouncementManager(Context context, AnnouncementSettingsRepository settings) {
        this.settings = settings;
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        textToSpeech = new TextToSpeech(context.getApplicationContext(), this);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS && textToSpeech != null) {
            int languageStatus = textToSpeech.setLanguage(Locale.getDefault());
            ready = languageStatus != TextToSpeech.LANG_MISSING_DATA
                    && languageStatus != TextToSpeech.LANG_NOT_SUPPORTED;
            if (ready) {
                textToSpeech.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build());
                textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override public void onStart(String utteranceId) { }
                    @Override public void onDone(String utteranceId) { abandonAudioFocus(); }
                    @Override public void onError(String utteranceId) { abandonAudioFocus(); }
                });
                Log.i(TAG, "TTS initialized");
            }
        }
    }

    public void announce(String announcement) {
        cancelPending();
        currentAnnouncement = announcement;
        repeatCount = 0;
        if (settings.isInitialEnabled()) {
            speakCurrent(false);
        }
        if (settings.isRepeatEnabled() && settings.getMaximumRepeatCount() > 0) {
            scheduleNextRepeat();
        }
    }

    private void speakCurrent(boolean repeat) {
        if (ready && textToSpeech != null && currentAnnouncement != null) {
            requestAudioFocus();
            textToSpeech.speak(currentAnnouncement, TextToSpeech.QUEUE_FLUSH, null, "incoming-caller");
            if (repeat) {
                repeatCount++;
            }
        }
    }

    private void scheduleNextRepeat() {
        handler.postDelayed(() -> {
            if (repeatCount < settings.getMaximumRepeatCount()) {
                speakCurrent(true);
                if (repeatCount < settings.getMaximumRepeatCount()) {
                    scheduleNextRepeat();
                }
            }
        }, settings.getRepeatDelaySeconds() * 1000L);
    }

    public void cancelPending() {
        handler.removeCallbacksAndMessages(null);
        if (textToSpeech != null) {
            textToSpeech.stop();
        }
        abandonAudioFocus();
        currentAnnouncement = null;
        repeatCount = 0;
    }

    public void release() {
        cancelPending();
        if (textToSpeech != null) {
            textToSpeech.shutdown();
            textToSpeech = null;
        }
        ready = false;
    }

    private void requestAudioFocus() {
        if (audioManager == null) return;
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            audioFocusRequest = new AudioFocusRequest.Builder(
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(attributes)
                    .setWillPauseWhenDucked(false)
                    .setOnAudioFocusChangeListener(focus -> {
                        if (focus == AudioManager.AUDIOFOCUS_LOSS) {
                            Log.i(TAG, "Audio focus lost; preserving native call audio");
                        }
                    })
                    .build();
            audioManager.requestAudioFocus(audioFocusRequest);
        }
    }

    private void abandonAudioFocus() {
        if (audioManager != null && audioFocusRequest != null
                && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest);
            audioFocusRequest = null;
        }
    }
}
