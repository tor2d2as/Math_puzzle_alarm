package de.tor2d2as.math_puzzle_alarm;

import android.graphics.drawable.Drawable;
import android.net.Uri;

import androidx.annotation.NonNull;

public class Clock_ui_entry {

    private boolean clock_enabled;
    private String clock_time;
    private String clock_day;
    private boolean[] clock_day_boolean;
    private String clock_folder;
    private int clock_volume_int;
    private Drawable clock_image;

    /**
     * Holds the information of one clock.
     */
    public Clock_ui_entry(boolean mClock_enabled, String mClock_time, String mClock_day, boolean[] mClock_day_boolean, String mClock_folder, int mClock_volume_int, @NonNull Drawable mClock_image){
        clock_enabled = mClock_enabled;
        clock_time = mClock_time;
        clock_day = mClock_day;
        clock_day_boolean = mClock_day_boolean;
        clock_folder = mClock_folder;
        clock_volume_int = mClock_volume_int;
        clock_image = mClock_image;
    }

    public void modify_entry(boolean mClock_enabled, String mClock_time, String mClock_day, boolean[] mClock_day_boolean, String mClock_folder, int mClock_volume_int, @NonNull Drawable mClock_image){
        clock_enabled = mClock_enabled;
        clock_time = mClock_time;
        clock_day = mClock_day;
        clock_day_boolean = mClock_day_boolean;
        clock_folder = mClock_folder;
        clock_volume_int = mClock_volume_int;
        clock_image = mClock_image;
    }

    public boolean isClock_enabled() {
        return clock_enabled;
    }

    public void setClock_enabled(boolean clock_enabled) {
        this.clock_enabled = clock_enabled;
    }

    public String getClock_time() {
        return clock_time;
    }

    public String getClock_day() {
        return clock_day;
    }

    public boolean[] getClock_day_boolean() {
        return clock_day_boolean;
    }

    public String getClock_folder() {
        return clock_folder;
    }

    /**
     * It returns the folder path in a way that is more readable for users.
     */
    public String getClock_folder_user_view(){
        if((clock_folder != null) && (!clock_folder.isEmpty())) {
            Uri uri = Uri.parse(clock_folder);
            return uri.getLastPathSegment();
        }else {
            return "";
        }
    }

    public int getClock_volume_int() {
        return clock_volume_int;
    }

    public Drawable getClock_image() {
        return clock_image;
    }
}
