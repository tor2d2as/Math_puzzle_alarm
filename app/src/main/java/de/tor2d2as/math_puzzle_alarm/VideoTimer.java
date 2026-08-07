package de.tor2d2as.math_puzzle_alarm;

import java.util.Timer;

/**
 * This class is a normal Timer, which adds the function that you can check
 * if the Timer was canceled or not.
 */
public class VideoTimer extends Timer {

    private boolean canceled = false;

    /**
     * It cancels the Timer and additionally saves the information that the Timer was canceled.
     * If the Timer was canceled, can be requested with the method: isCanceled().
     */
    @Override
    public void cancel() {
        canceled = true;
        super.cancel();
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
