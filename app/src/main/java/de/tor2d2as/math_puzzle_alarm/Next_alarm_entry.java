package de.tor2d2as.math_puzzle_alarm;

public class Next_alarm_entry {

    private final String clock_folder;
    private final int clock_volume_int;
    private final int id;
    private final boolean is_tomorrow;
    /**
     * volume = -1 and folder != null -> User only set an Alarm Sound
     * volume != -1 and folder == null -> User only set Alarm Volume
     * volume != -1 and folder != null -> User set both Alarm Volume and Alarm Sound
     */
    public Next_alarm_entry(String mClock_folder, int mClock_volume_int, int mId, boolean mIs_tomorrow){
        clock_folder = mClock_folder;
        clock_volume_int = mClock_volume_int;
        id = mId;
        is_tomorrow = mIs_tomorrow;
    }

    public String getClock_folder() {return clock_folder;}

    public int getClock_volume_int() {return clock_volume_int;}

    public int getClock_id(){return id;}
    public boolean getClock_is_tomorrow(){return is_tomorrow;}
}
