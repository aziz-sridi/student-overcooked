package com.student.overcooked.ui.focus;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;

import androidx.annotation.Nullable;

import java.util.Random;

/**
 * Very small offline "white noise" generator using AudioTrack.
 * This avoids bundling audio assets while still providing an always-available ambient option.
 */
public class WhiteNoisePlayer {

    private static final int SAMPLE_RATE_HZ = 22050;

    private final Random random = new Random();
    private @Nullable AudioTrack audioTrack;
    private @Nullable Thread thread;
    private volatile boolean playing;
    private volatile float volume = 0.6f;

    public void setVolume(float volume) {
        this.volume = clamp01(volume);
        AudioTrack track = audioTrack;
        if (track != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                track.setVolume(this.volume);
            } else {
                //noinspection deprecation
                track.setStereoVolume(this.volume, this.volume);
            }
        }
    }

    public boolean isPlaying() {
        return playing;
    }

    public void start() {
        if (playing) return;
        playing = true;

        int minBuffer = AudioTrack.getMinBufferSize(
                SAMPLE_RATE_HZ,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
        );
        int bufferSize = Math.max(minBuffer, SAMPLE_RATE_HZ / 2);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            audioTrack = new AudioTrack(
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build(),
                    new AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE_HZ)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build(),
                    bufferSize,
                    AudioTrack.MODE_STREAM,
                    AudioManager.AUDIO_SESSION_ID_GENERATE
            );
        } else {
            //noinspection deprecation
            audioTrack = new AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    SAMPLE_RATE_HZ,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize,
                    AudioTrack.MODE_STREAM
            );
        }

        setVolume(volume);
        AudioTrack track = audioTrack;
        if (track == null) {
            playing = false;
            return;
        }

        track.play();

        thread = new Thread(() -> {
            short[] pcm = new short[2048];
            while (playing && audioTrack != null) {
                for (int i = 0; i < pcm.length; i++) {
                    pcm[i] = (short) (random.nextInt(Short.MAX_VALUE) - (Short.MAX_VALUE / 2));
                }
                AudioTrack t = audioTrack;
                if (t == null) break;
                t.write(pcm, 0, pcm.length);
            }
        }, "WhiteNoisePlayer");
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        playing = false;
        Thread t = thread;
        thread = null;
        if (t != null) {
            try { t.join(250); } catch (InterruptedException ignored) { }
        }
        AudioTrack track = audioTrack;
        audioTrack = null;
        if (track != null) {
            try { track.pause(); } catch (Exception ignored) { }
            try { track.flush(); } catch (Exception ignored) { }
            try { track.release(); } catch (Exception ignored) { }
        }
    }

    private static float clamp01(float v) {
        if (v < 0f) return 0f;
        if (v > 1f) return 1f;
        return v;
    }
}
