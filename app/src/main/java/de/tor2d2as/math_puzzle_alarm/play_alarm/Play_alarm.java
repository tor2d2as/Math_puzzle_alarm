package de.tor2d2as.math_puzzle_alarm.play_alarm;

import android.annotation.SuppressLint;
import android.app.PictureInPictureParams;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Insets;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Size;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextClock;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.AppBarLayout;

import java.util.ArrayList;
import java.util.TimerTask;

import de.tor2d2as.math_puzzle_alarm.Clock_ui_entry;
import de.tor2d2as.math_puzzle_alarm.Global_methods;
import de.tor2d2as.math_puzzle_alarm.Next_alarm_entry;
import de.tor2d2as.math_puzzle_alarm.R;
import de.tor2d2as.math_puzzle_alarm.Save_clocks;
import de.tor2d2as.math_puzzle_alarm.SetAlarms;

/**
 * This class plays the alarm sound.
 * It requires the information from which folder the sound should be played.
 * If this information is null, the standard Android sound will be played.
 * The information can be provided with an Intent which contains the string: "clock_folder".
 * The volume of the Alarm sound can be provided with an Intent which contains the int: "alarm_volume".
 */

public class Play_alarm extends AppCompatActivity implements View.OnTouchListener, View.OnClickListener, SurfaceHolder.Callback, MediaService.VideoPlayerNotify, Math_Dialog.NotifyUi {

    private SurfaceView video_player_surface_view;
    private Button stop_button;
    private Button snooze_button;
    private Button moving_button;
    private TextView videoTime;
    private TextView textview_file_name;
    private SeekBar videoSeekBar;
    private TextView text_background_turn_alarm_off;
    private AppBarLayout app_bar_layout_alarm;
    private ImageView imageview_background_turn_alarm_off;
    //The clock_video_layout needs to be modified for the Picture in Picture Mode
    private RelativeLayout clock_area;
    private RelativeLayout snooze_area;
 //   private final RelativeLayout.LayoutParams clock_video_layout_params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    private TextClock view_actual_time;
    private VideoTimer videoTimer;
    private final SetAlarms setAlarms = new SetAlarms(this);
    private final Save_clocks save_clocks = new Save_clocks(this);
    private final Global_methods global_methods = new Global_methods();
    //Create Connection to the music player Service
    private MediaService mediaService;
    private boolean isServiceBound = false;
    private final Math_Dialog math_dialog = new Math_Dialog();
    private float moving_start_x;
    private float moving_start_x_offset;
    private float moving_start_y;
    private float moving_start_y_offset;
    private int tmp_alarm_volume = 7;
    private String tmp_alarm_folder = "";


    private String video_length = "";

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            MediaService.MediaBinder binder = (MediaService.MediaBinder) iBinder;
            mediaService = binder.getService();
            mediaService.update_videoPlayerNotify(Play_alarm.this);
            isServiceBound = true;

            mediaService.setAlarm_volume(tmp_alarm_volume);
            mediaService.setClock_folder(tmp_alarm_folder);

            //This will be used when starting the Play_alarm.class the first time.
            mediaService.playVideo();
            set_video_to_surface();
            start_video_Timer();
            mediaService.request_current_video_information();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            isServiceBound = false;
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_play_alarm);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }

        //----------------------------------------------------------
        //Connect the UI_Elements to the xml-file
        stop_button = findViewById(R.id.stop_button);
        videoTime = findViewById(R.id.videoTime);
        textview_file_name = findViewById(R.id.textview_file_name);
        videoSeekBar = findViewById(R.id.videoSeekBar);
        view_actual_time = findViewById(R.id.view_actual_time);
        text_background_turn_alarm_off = findViewById(R.id.text_background_turn_alarm_off);
        imageview_background_turn_alarm_off = findViewById(R.id.imageview_background_turn_alarm_off);
        app_bar_layout_alarm = findViewById(R.id.app_bar_layout_alarm);
        clock_area = findViewById(R.id.clock_area);
        snooze_area = findViewById(R.id.snooze_area);

        snooze_button = findViewById(R.id.snooze_button);
        snooze_button.setOnClickListener(this);

        moving_button = findViewById(R.id.moving_button);
        moving_button.bringToFront();
        moving_button.setOnTouchListener(this);

        video_player_surface_view = findViewById(R.id.video_player_surface_view);
        video_player_surface_view.getHolder().addCallback(this);

        //----------------------------------------------------------
        //Get extras from intent / next_alarm_config
        Bundle bundle_extras = getIntent().getExtras();
        if(bundle_extras == null) {
            //The extra data are sometimes removed by a bug in Android, so the next_alarm_config will be used instead.
            // (The intent will only be set if the user starts a preview of the alarm from, e.g., MainActivity).
            Next_alarm_entry next_alarm_entry = global_methods.decode_next_alarm(save_clocks.read_next_alarm());
            if(next_alarm_entry != null) {
                tmp_alarm_folder = next_alarm_entry.getClock_folder();
                if(next_alarm_entry.getClock_volume_int() != -1) {
                    tmp_alarm_volume = next_alarm_entry.getClock_volume_int();
                }
                if(next_alarm_entry.getClock_id() != -1){
                    //It disables the clock, if the clock is not set for specific weekdays, since the
                    //Next alarm config gets always updated if a new alarm is set, we can safely
                    //assume that the id from the next alarm clock matches always with the one in the main config.
                    ArrayList<Clock_ui_entry> tmp = global_methods.decode_config(this, save_clocks.read_config());
                    tmp.get(next_alarm_entry.getClock_id()).setClock_enabled(!next_alarm_entry.getClock_is_tomorrow());
                    save_clocks.save_config(global_methods.generate_config(tmp));
                }
            }else{
                finish();
                return;
            }
        }else {
            //Will be used if the user starts a preview of the alarm from e.g. MainActivity
            if (bundle_extras.containsKey("clock_folder")) {
                tmp_alarm_folder = bundle_extras.getString("clock_folder");
            }
            tmp_alarm_volume = bundle_extras.getInt("alarm_volume", tmp_alarm_volume);
        }

        //----------------------------------------------------------
        //Starts and binds the Service
        Intent serviceIntent = new Intent(this, MediaService.class);
        startService(serviceIntent);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        if(isInPictureInPictureMode()) {
            change_visibility(View.GONE);
        }
    }

    @Override
    protected void onPause() {
        if (!isInPictureInPictureMode()) {
            if (mediaService != null) {
                if (videoTimer != null) {
                    videoTimer.cancel();
                }
                mediaService.bind_Video_to_Surface(null);
                mediaService.update_videoPlayerNotify(null);
            }
        }
        super.onPause();
    }

    @Override
    public void new_video(final String text, final int duration) {
        runOnUiThread(()-> {
            textview_file_name.setText(text);
            videoSeekBar.setMax(duration);
            video_length = " / " + formatTime(duration, new StringBuffer());
        });
    }

    /**
     * It formats the given Time to mm:ss
     * @param milliseconds The time in milliseconds
     * @param stringBuffer The stringBuffer which should be modified
     * @return The formatted string
     */
    private StringBuffer formatTime(final int milliseconds, StringBuffer stringBuffer) {
        int totalSeconds = milliseconds / 1000;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        if(seconds < 10) {
            return stringBuffer.append(minutes).append(":0").append(seconds);
        }else{
            return stringBuffer.append(minutes).append(":").append(seconds);
        }
    }

    @Override
    protected void onUserLeaveHint() {
        PictureInPictureParams params = new PictureInPictureParams.Builder().build();
        enterPictureInPictureMode(params);
        super.onUserLeaveHint();
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
        //This will be needed if the UI resumes or if it is recreated.
        if(mediaService != null){
            set_video_to_surface();
            start_video_Timer();
        }
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
    }

    /**
     * It sets the video on a surface (if Attached to a window).
     * It also sets the ideal size of the video and adjusts the
     * video size accordingly.
     */
    public void set_video_to_surface(){
        if(video_player_surface_view.isAttachedToWindow()) {
            ViewGroup.LayoutParams params = video_player_surface_view.getLayoutParams();
            if(isInPictureInPictureMode()){
                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            }else {
                int videoHeight = mediaService.getVideoHeight();
                int videoWidth = mediaService.getVideoWidth();

                int parentWidth;
                int parentHeight;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    final WindowMetrics metrics = getWindowManager().getCurrentWindowMetrics();
                    // Gets all excluding insets
                    final WindowInsets windowInsets = metrics.getWindowInsets();
                    Insets insets = windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.navigationBars() | WindowInsets.Type.displayCutout());

                    int insetsWidth = insets.right + insets.left;
                    int insetsHeight = insets.top + insets.bottom;

                    // Legacy size that Display#getSize reports
                    final Rect bounds = metrics.getBounds();
                    final Size legacySize = new Size(bounds.width() - insetsWidth, bounds.height() - insetsHeight);
                    parentWidth = legacySize.getWidth();
                    parentHeight = legacySize.getHeight();
                } else {
                    DisplayMetrics displayMetrics = new DisplayMetrics();
                    this.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                    parentWidth = displayMetrics.widthPixels;
                    parentHeight = displayMetrics.heightPixels;
                }

                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    int width_view_actual_time = view_actual_time.getWidth();
                    int imageview_background_turn_alarm_off_height = imageview_background_turn_alarm_off.getWidth();
                    parentWidth = parentWidth - width_view_actual_time - imageview_background_turn_alarm_off_height;
                    parentHeight = parentHeight - textview_file_name.getHeight();
                } else {
                    //The complete parentWidth can be used, no need to modify it here
                    int height_app_bar = app_bar_layout_alarm.getHeight();
                    int height_view_actual_time = view_actual_time.getHeight();
                    //TODO: The height of the snooze button is a guess,
                    // the height of view_actual_time and snooze_button_height
                    // is sometimes not reliable.
                    int snooze_button_height = 400;
                    //int snooze_button_height = snooze_button.getHeight();
                    int imageview_background_turn_alarm_off_height = imageview_background_turn_alarm_off.getHeight();

                    parentHeight = parentHeight - height_app_bar - height_view_actual_time - snooze_button_height - imageview_background_turn_alarm_off_height;
                    //parentHeight = parentHeight - app_bar_layout_alarm.getHeight() - view_actual_time.getHeight() - snooze_button.getHeight() - imageview_background_turn_alarm_off.getHeight();
                }

                float height_multiplicator = (float) parentHeight / videoHeight;
                float width_multiplicator = (float) parentWidth / videoWidth;

                int width;
                int height;

                if (width_multiplicator <= height_multiplicator) {
                    height = Math.round(videoHeight * width_multiplicator);
                    width = Math.round(videoWidth * width_multiplicator);
                } else {
                    height = Math.round(videoHeight * height_multiplicator);
                    width = Math.round(videoWidth * height_multiplicator);
                }
                if (height == 0) {
                    height = 200;
                }

                //ViewGroup.LayoutParams params = video_player_surface_view.getLayoutParams();
                params.width = width;
                params.height = height;
            }
            video_player_surface_view.setLayoutParams(params);

            mediaService.bind_Video_to_Surface(video_player_surface_view.getHolder());
        }
    }


    /**
     * It starts the Video Timer that updates the seekbar and the current videoTime
     * on the UI.
     * If the videoTimer is already started, nothing happens.
     */
    public void start_video_Timer() {
        if ((videoTimer == null) || (videoTimer.isCanceled())) {
            videoTimer = new VideoTimer();
        }else{
            return;
        }
        videoTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    if(mediaService != null) {
                        int currentTime = mediaService.getCurrentVideoTime();
                        if(currentTime == -1) {
                            videoTimer.cancel();
                        }else{
                            videoSeekBar.setProgress(currentTime);
                            StringBuffer stringBuffer = new StringBuffer();
                            videoTime.setText(formatTime(currentTime, stringBuffer).append(video_length));
                        }
                    }else{
                        videoTimer.cancel();
                    }
                });
            }
        }, 0, 1000);
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode);
        if (isInPictureInPictureMode) {
            //Hide everything except the video
            change_visibility(View.GONE);
        } else {
            //restore all Elements to the UI
            change_visibility(View.VISIBLE);
        }
    }

    /**
     * It changes the visibility of all UI Elements that are not needed for Picture in Picture.
     * @param visibility To what the visibility should be changed.
     */
    private void change_visibility(int visibility){
        app_bar_layout_alarm.setVisibility(visibility);
        clock_area.setVisibility(visibility);
        snooze_area.setVisibility(visibility);
        textview_file_name.setVisibility(visibility);
     //   videoTime.setVisibility(visibility);
     //   videoSeekBar.setVisibility(visibility);
        imageview_background_turn_alarm_off.setVisibility(visibility);
        text_background_turn_alarm_off.setVisibility(visibility);
        moving_button.setVisibility(visibility);
        stop_button.setVisibility(visibility);
    }


    @Override
    public void onClick(View view) {
        if(view.getId() == snooze_button.getId()){
            math_dialog.snooze_alarm(this, this);
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        //This event is used so the user can move the stop button, to stop the alarm
        view.performClick();
        int orientation = getResources().getConfiguration().orientation;
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    moving_start_y = moving_button.getY();
                    moving_start_y_offset = motionEvent.getRawY();
                }else{
                    moving_start_x = moving_button.getX();
                    moving_start_x_offset = motionEvent.getRawX();
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    moving_button.setY(motionEvent.getRawY() - moving_start_y_offset);
                }else{
                    moving_button.setX(motionEvent.getRawX() - moving_start_x_offset);
                }
                return true;

            case MotionEvent.ACTION_UP:
                float pos;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    pos = moving_button.getY() - stop_button.getY();
                    moving_button.setY(moving_start_y);
                }else{
                    pos = moving_button.getX() - stop_button.getX();
                    moving_button.setX(moving_start_x);
                }

                if (pos < 0) pos = pos * -1;
                if (pos < 50) {
                    math_dialog.stop_alarm(this, this);
                }
                return true;
        }
        return false;
    }
    //--------------------------------------------------------------------------------------------------------------------

    @Override
    public void snooze_alarm() {
        setAlarms.start_alarm(System.currentTimeMillis() + 5 * 60 * 1000, mediaService.getClock_folder(), mediaService.getAlarm_volume(), true, -1, false);
        end_Clock();
    }

    @Override
    public void stop_alarm() {
        setAlarms.start_alarm(global_methods.decode_config(this, save_clocks.read_config()));
        end_Clock();
    }

    /**
     * Call it to stop the service and the UI
     */
    private void end_Clock(){
        //The videoTimer will be stopped in the onPause() method.
        if (isServiceBound) {
            unbindService(serviceConnection);
            mediaService.stopSelf();
            isServiceBound = false;
        }
        mediaService.set_Old_Volume();
        finish();
    }
}