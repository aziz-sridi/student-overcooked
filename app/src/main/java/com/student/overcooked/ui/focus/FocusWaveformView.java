package com.student.overcooked.ui.focus;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.student.overcooked.R;

import java.util.Random;

/**
 * Tiny, subtle waveform-style animation for the Focus Mode ambient player.
 * This intentionally stays minimal so it doesn’t distract.
 */
public class FocusWaveformView extends View {

    private static final int BAR_COUNT = 18;
    private static final long FRAME_MS = 120;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();

    private boolean playing = false;
    private boolean externalDriven = false;
    private final float[] bars = new float[BAR_COUNT];

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!playing) {
                invalidate();
                return;
            }
            for (int i = 0; i < BAR_COUNT; i++) {
                // Keep it subtle: between 20% and 90% height
                bars[i] = 0.2f + (random.nextFloat() * 0.7f);
            }
            invalidate();
            postDelayed(this, FRAME_MS);
        }
    };

    public FocusWaveformView(Context context) {
        super(context);
        init();
    }

    public FocusWaveformView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FocusWaveformView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(getResources().getColor(R.color.divider));
        for (int i = 0; i < BAR_COUNT; i++) {
            bars[i] = 0.25f;
        }
    }

    public void setPlaying(boolean playing) {
        if (this.playing == playing) return;
        this.playing = playing;
        removeCallbacks(tick);
        if (playing) {
            if (!externalDriven) {
                post(tick);
            }
        } else {
            externalDriven = false;
            for (int i = 0; i < BAR_COUNT; i++) {
                bars[i] = 0.25f;
            }
            invalidate();
        }
    }

    /**
     * Drive the waveform from real audio waveform bytes (e.g., Visualizer callback).
     * This disables the internal random animation while playing.
     */
    public void setWaveform(byte[] waveform) {
        if (waveform == null || waveform.length == 0) return;
        externalDriven = true;
        removeCallbacks(tick);

        // Downsample waveform into BAR_COUNT buckets.
        int bucketSize = Math.max(1, waveform.length / BAR_COUNT);
        for (int i = 0; i < BAR_COUNT; i++) {
            int start = i * bucketSize;
            int end = Math.min(waveform.length, start + bucketSize);
            int sum = 0;
            int count = 0;
            for (int j = start; j < end; j++) {
                sum += Math.abs((int) waveform[j]);
                count++;
            }
            float avg = count == 0 ? 0f : (sum / (float) count);
            // waveform bytes are roughly -128..127; normalize into subtle range.
            float norm = Math.min(1f, avg / 90f);
            bars[i] = 0.2f + (norm * 0.7f);
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        float gap = dp(3f);
        float barWidth = (w - (gap * (BAR_COUNT - 1))) / (float) BAR_COUNT;
        float radius = dp(2f);

        float x = 0f;
        for (int i = 0; i < BAR_COUNT; i++) {
            float barH = Math.max(dp(2f), bars[i] * h);
            float top = (h - barH) / 2f;
            float bottom = top + barH;
            canvas.drawRoundRect(x, top, x + barWidth, bottom, radius, radius, paint);
            x += barWidth + gap;
        }
    }

    private float dp(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
