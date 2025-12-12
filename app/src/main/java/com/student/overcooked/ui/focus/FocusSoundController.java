package com.student.overcooked.ui.focus;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.RawResourceDataSource;
import androidx.media3.exoplayer.ExoPlayer;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

final class FocusSoundController {

    private final Context context;
    private final ActivityResultLauncher<String[]> importLauncher;

    private final View presetsContainer;
    private final TextView selectedSoundText;
    private final SeekBar volumeSeek;
    private final MaterialButton btnSoundPlayPause;

    private boolean soundPlaying = false;
    private String selectedSoundName = "Work";
    private @Nullable Uri importedSoundUri;

    private @Nullable ExoPlayer player;
    private float volume = 0.6f;

    FocusSoundController(@NonNull Context context,
                         @NonNull ActivityResultLauncher<String[]> importLauncher,
                         @NonNull View presetsContainer,
                         @NonNull TextView selectedSoundText,
                         @NonNull SeekBar volumeSeek,
                         @NonNull MaterialButton btnSoundPlayPause) {
        this.context = context;
        this.importLauncher = importLauncher;
        this.presetsContainer = presetsContainer;
        this.selectedSoundText = selectedSoundText;
        this.volumeSeek = volumeSeek;
        this.btnSoundPlayPause = btnSoundPlayPause;

        setupPlayer();
        setupSoundUi();
        renderSoundUi();
    }

    void onImportedUri(@NonNull Uri uri) {
        try {
            ContentResolver resolver = context.getContentResolver();
            resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {
        }

        importedSoundUri = uri;
        selectedSoundName = "Imported Sound";
        setPlayerSourceForSelection(/*autoplay*/ soundPlaying);
        renderSoundUi();
    }

    void destroy() {
        releasePlayer();
    }

    private void setupPlayer() {
        if (player != null) return;

        player = new ExoPlayer.Builder(context).build();
        player.setRepeatMode(Player.REPEAT_MODE_ONE);
        player.setVolume(volume);

        btnSoundPlayPause.setOnClickListener(v -> {
            if (player == null) return;
            if (soundPlaying) {
                try {
                    player.pause();
                } catch (Exception ignored) {
                }
            } else {
                startSound();
            }
        });

        player.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                soundPlaying = isPlaying;
                renderSoundUi();
            }
        });

        setPlayerSourceForSelection(false);
    }

    private void setupSoundUi() {
        if (presetsContainer instanceof ViewGroup) {
            ViewGroup container = (ViewGroup) presetsContainer;
            container.removeAllViews();

            List<String> presets = new ArrayList<>();
            presets.add("Work");
            presets.add("Master");
            presets.add("Slaves");

            for (String name : presets) {
                MaterialButton chip = new MaterialButton(context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
                chip.setText(name);
                chip.setAllCaps(false);
                chip.setOnClickListener(v -> {
                    selectedSoundName = name;
                    importedSoundUri = null;
                    stopSound();
                    renderSoundUi();
                });
                container.addView(chip);
            }

            MaterialButton importBtn = new MaterialButton(context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            importBtn.setText("Import");
            importBtn.setAllCaps(false);
            importBtn.setOnClickListener(v -> importLauncher.launch(new String[]{"audio/*"}));
            container.addView(importBtn);
        }

        volumeSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                volume = progress / 100f;
                applyVolume();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        volume = volumeSeek.getProgress() / 100f;
        applyVolume();
    }

    private void startSound() {
        if (player == null) return;
        setPlayerSourceForSelection(false);
        player.prepare();
        player.play();
    }

    private void stopSound() {
        soundPlaying = false;
        if (player != null) {
            try {
                player.pause();
            } catch (Exception ignored) {
            }
            try {
                player.stop();
            } catch (Exception ignored) {
            }
        }
    }

    private void releasePlayer() {
        soundPlaying = false;
        if (player != null) {
            try {
                player.pause();
            } catch (Exception ignored) {
            }
            try {
                player.stop();
            } catch (Exception ignored) {
            }
            try {
                player.release();
            } catch (Exception ignored) {
            }
            player = null;
        }
    }

    private void renderSoundUi() {
        if (importedSoundUri != null) {
            selectedSoundText.setText(FocusSoundUtils.getDisplayNameForUri(context, importedSoundUri));
        } else {
            selectedSoundText.setText(selectedSoundName);
        }

        btnSoundPlayPause.setText(soundPlaying ? "Pause" : "Play");
    }

    private void applyVolume() {
        if (player != null) {
            try {
                player.setVolume(volume);
            } catch (Exception ignored) {
            }
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    private void setPlayerSourceForSelection(boolean autoplay) {
        if (player == null) return;

        try {
            MediaItem item;
            if (importedSoundUri != null) {
                item = MediaItem.fromUri(importedSoundUri);
            } else {
                int resId = FocusSoundUtils.resolveRawSoundResId(context, selectedSoundName);
                if (resId == 0) return;
                Uri rawUri = RawResourceDataSource.buildRawResourceUri(resId);
                item = MediaItem.fromUri(rawUri);
            }
            player.setMediaItem(item);
            player.prepare();
            if (autoplay) player.play();
        } catch (Exception ignored) {
        }
    }
}
