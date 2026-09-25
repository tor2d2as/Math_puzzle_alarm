package de.tor2d2as.math_puzzle_alarm.play_alarm;

import java.util.Timer;

/**
 * This class is a normal Timer, which adds the function that you can check
 * if the Timer was canceled or not.
 */
public class VideoTimer extends Timer {

    private boolean canceled = false;

    /**
     * If the timer is not canceled yet, it cancels the Timer,
     * and additionally saves the information that the Timer was canceled.
     * If the Timer was canceled or not, can be requested with the method: isCanceled().
     */
    @Override
    public void cancel() {
        if(!canceled) {
            canceled = true;
            super.cancel();
        }
    }

    /**
     * Returns whether the timer was canceled or not.
     * @return true = the timer was canceled;
     * false = the timer was not canceled
     */
    public boolean isCanceled() {
        return canceled;
    }
}
