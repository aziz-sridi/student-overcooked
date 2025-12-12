package com.student.overcooked.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import com.student.overcooked.R;
import com.student.overcooked.data.model.CookedLevel;

public class MascotPackStore {

    public static final String PREFS = "mascot_prefs";
    public static final String KEY_SELECTED_PACK = "selected_pack";

    public static final String PACK_DEFAULT = "default";
    public static final String PACK_GIGA_TOAST = "giga_toast";
    public static final String PACK_STUDENT = "student";
    public static final String PACK_POTATO = "potato";

    private final Context context;

    public MascotPackStore(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    @NonNull
    public String getSelectedPackId() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getString(KEY_SELECTED_PACK, PACK_DEFAULT);
    }

    public void setSelectedPackId(@NonNull String packId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_SELECTED_PACK, packId).apply();
    }

    @DrawableRes
    public static int getDrawableForLevel(@NonNull String packId, @NonNull CookedLevel level) {
        switch (packId) {
            case PACK_GIGA_TOAST:
                return getToastDrawableForLevel(level);
            case PACK_STUDENT:
                return getStudentDrawableForLevel(level);
            case PACK_POTATO:
                return getPotatoDrawableForLevel(level);
            case PACK_DEFAULT:
            default:
                return getDefaultDrawableForLevel(level);
        }
    }

    @DrawableRes
    private static int getDefaultDrawableForLevel(@NonNull CookedLevel level) {
        switch (level) {
            case COZY:
                return R.drawable.mascot_cozy;
            case CRISPY:
                return R.drawable.mascot_crispy;
            case COOKED:
                return R.drawable.mascot_cooked;
            case OVERCOOKED:
            default:
                return R.drawable.mascot_overcooked;
        }
    }

    @DrawableRes
    private static int getToastDrawableForLevel(@NonNull CookedLevel level) {
        switch (level) {
            case COZY:
                return R.drawable.giga_cozy_toast;
            case CRISPY:
                return R.drawable.giga_crispy_toast;
            case COOKED:
                return R.drawable.giga_cooked_toast;
            case OVERCOOKED:
            default:
                return R.drawable.giga_overcooked_toast;
        }
    }

    @DrawableRes
    private static int getStudentDrawableForLevel(@NonNull CookedLevel level) {
        switch (level) {
            case COZY:
                return R.drawable.student_cozy;
            case CRISPY:
                return R.drawable.student_crispy;
            case COOKED:
                return R.drawable.student_cooked;
            case OVERCOOKED:
            default:
                return R.drawable.student_overcooked;
        }
    }

    @DrawableRes
    private static int getPotatoDrawableForLevel(@NonNull CookedLevel level) {
        switch (level) {
            case COZY:
                return R.drawable.potato_cozy;
            case CRISPY:
                return R.drawable.potato_crispy;
            case COOKED:
                return R.drawable.potato_cooked;
            case OVERCOOKED:
            default:
                return R.drawable.potato_overcooked;
        }
    }
}
