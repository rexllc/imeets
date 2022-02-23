package gq.fora.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;

public class PreferencesManager {

    private SharedPreferences prefs;

    public PreferencesManager(@NonNull Context context) {
        this.prefs = context.getSharedPreferences("preferences", Context.MODE_PRIVATE);
    }

    public boolean hasKey(@NonNull String key) {
        if (prefs.contains(key)) return true;
        return false;
    }

    public String getString(@NonNull String key) {
        return this.prefs.getString(key, null);
    }

    public boolean getBoolean(@NonNull String key) {
        return this.prefs.getBoolean(key, false);
    }

    public int getInt(@NonNull String key) {
        return this.prefs.getInt(key, 0);
    }

    public void addString(@NonNull String key, @NonNull String value) {
        this.prefs.edit().putString(key, value).apply();
    }

    public void addBoolean(@NonNull String key, @NonNull boolean value) {
        this.prefs.edit().putBoolean(key, value).apply();
    }

    public void addInt(@NonNull String key, @NonNull int value) {
        this.prefs.edit().putInt(key, value).apply();
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}
