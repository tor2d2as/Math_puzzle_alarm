package de.tor2d2as.math_puzzle_alarm.play_alarm;

import android.annotation.SuppressLint;
import android.app.PictureInPictureParams;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.AppBarLayout;

import java.util.ArrayList;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import de.tor2d2as.math_puzzle_alarm.Clock_ui_entry;
import de.tor2d2as.math_puzzle_alarm.Global_methods;
import de.tor2d2as.math_puzzle_alarm.Next_alarm_entry;
import de.tor2d2as.math_puzzle_alarm.R;
import de.tor2d2as.math_puzzle_alarm.Save_clocks;
import de.tor2d2as.math_puzzle_alarm.SetAlarms;
import de.tor2d2as.math_puzzle_alarm.VideoTimer;

/**
 * This class plays the alarm sound.
 * It requires the information from which folder the sound should be played.
 * If this information is null, the standard Android sound will be played.
 * The information can be provided with an Intent which contains the string: "clock_folder".
 * The volume of the Alarm sound can be provided with an Intent which contains the int: "alarm_volume".
 */

public class Play_alarm extends AppCompatActivity implements View.OnTouchListener, View.OnClickListener, SurfaceHolder.Callback, MediaService.VideoPlayerNotify, Math_Dialog.NotifyUi {

    private SurfaceView surfaceView;
    private VideoTimer videoTimeTimer;
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
    private RelativeLayout clock_video_layout;
    private final RelativeLayout.LayoutParams clock_video_layout_params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    private final SetAlarms setAlarms = new SetAlarms(this);
    private final Save_clocks save_clocks = new Save_clocks(this);
    private final Global_methods global_methods = new Global_methods();
    //Create Connection to the music player Service
    private MediaService mediaService;
    private boolean isServiceBound = false;
    private final Math_Dialog math_dialog = new Math_Dialog();
    private float moving_start_x;
    private int tmp_alarm_volume = 7;
    private String tmp_alarm_folder = "";

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            MediaService.MediaBinder binder = (MediaService.MediaBinder) iBinder;
            mediaService = binder.getService();
            isServiceBound = true;

            mediaService.setAlarm_volume(tmp_alarm_volume);
            mediaService.setClock_folder(tmp_alarm_folder);
            mediaService.playVideo(surfaceView.getHolder(), Play_alarm.this);
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }

        setContentView(R.layout.activity_play_alarm);

        stop_button = findViewById(R.id.stop_button);
        moving_button = findViewById(R.id.moving_button);
        snooze_button = findViewById(R.id.snooze_button);
        videoTime = findViewById(R.id.videoTime);
        textview_file_name = findViewById(R.id.textview_file_name);
        videoSeekBar = findViewById(R.id.videoSeekBar);
        clock_video_layout = findViewById(R.id.clock_video_layout);
        text_background_turn_alarm_off = findViewById(R.id.text_background_turn_alarm_off);
        imageview_background_turn_alarm_off = findViewById(R.id.imageview_background_turn_alarm_off);
        app_bar_layout_alarm = findViewById(R.id.app_bar_layout_alarm);

        moving_button.bringToFront();
        moving_button.setOnTouchListener(this);

        snooze_button.setOnClickListener(this);

        surfaceView = findViewById(R.id.surfaceView);
        surfaceView.getHolder().addCallback(this);

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

        //Starts and binds the Service----------------------------------------------
        Intent serviceIntent = new Intent(this, MediaService.class);
        startService(serviceIntent);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        //--------------------------------------------------------------------------
    }

    @Override
    protected void onStop() {
        stopVideoTimer();
        if(mediaService != null) {
            mediaService.removeSurfaceMediaPlayer();
        }
        super.onStop();
    }
    @Override
    protected void onUserLeaveHint() {
        PictureInPictureParams params = new PictureInPictureParams.Builder().build();
        enterPictureInPictureMode(params);
        super.onUserLeaveHint();
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        if (isInPictureInPictureMode) {
            //Hide everything except the video
            stop_button.setVisibility(View.INVISIBLE);
            moving_button.setVisibility(View.INVISIBLE);
            snooze_button.setVisibility(View.INVISIBLE);
            textview_file_name.setMaxLines(1);
            text_background_turn_alarm_off.setVisibility(View.INVISIBLE);
            imageview_background_turn_alarm_off.setVisibility(View.INVISIBLE);
            app_bar_layout_alarm.setVisibility(View.INVISIBLE);
            //clock_video_layout_params.addRule(RelativeLayout.ALIGN_BOTTOM);
            clock_video_layout.setLayoutParams(clock_video_layout_params);
//            Objects.requireNonNull(getSupportActionBar()).hide();
        } else {
            //restore all Elements to the UI
            stop_button.setVisibility(View.VISIBLE);
            moving_button.setVisibility(View.VISIBLE);
            snooze_button.setVisibility(View.VISIBLE);
            textview_file_name.setMaxLines(10);
            text_background_turn_alarm_off.setVisibility(View.VISIBLE);
            imageview_background_turn_alarm_off.setVisibility(View.VISIBLE);
            app_bar_layout_alarm.setVisibility(View.VISIBLE);
            //clock_video_layout_params.addRule(RelativeLayout.ABOVE, R.id.moving_button);
            clock_video_layout.setLayoutParams(clock_video_layout_params);
          //  Objects.requireNonNull(getSupportActionBar()).show();
        }
        super.onPictureInPictureModeChanged(isInPictureInPictureMode);
    }

    @Override
    public void onVideoStarted(MediaPlayer mediaPlayer, int videoLength, int videoPosition, boolean standard_sound_chosen) {
        stopVideoTimer();
        if ((videoTimeTimer == null) || (videoTimeTimer.isCanceled())) {
            videoTimeTimer = new VideoTimer();
        }
        if(!standard_sound_chosen){
            final int videoLengthMinute = (videoLength % (1000 * 60 * 60)) / (1000 * 60);
            final int videoLengthSecond = ((videoLength % (1000 * 60 * 60)) % (1000 * 60) / 1000);

            AtomicInteger actualMinute = new AtomicInteger(((videoPosition % (1000 * 60 * 60)) / (1000 * 60)));
            AtomicInteger actualSecond = new AtomicInteger(((videoPosition % (1000 * 60 * 60)) % (1000 * 60) / 1000));
            StringBuffer stringBuffer = new StringBuffer();

            String video_length;
            if(videoLengthSecond < 10){
                video_length = " / " + videoLengthMinute + ":0" + videoLengthSecond;
            }else{
                video_length = " / " + videoLengthMinute + ":" + videoLengthSecond;
            }

            videoSeekBar.setMax(videoLength);

            videoTimeTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(() -> {
                        try {
                            stringBuffer.setLength(0);
                            if (actualSecond.get() < 9) {
                                //Only for numbers smaller than 9 (not 10), because actualSecond will be incremented automatically.
                                videoTime.setText(stringBuffer.append(actualMinute).append(":0").append(actualSecond.incrementAndGet()).append(video_length).toString());
                            }else{
                                videoTime.setText(stringBuffer.append(actualMinute).append(":").append(actualSecond.incrementAndGet()).append(video_length).toString());
                            }
                            videoSeekBar.setProgress(mediaPlayer.getCurrentPosition());

                            if (actualSecond.get() == 59) {
                                actualSecond.set(0);
                                actualMinute.incrementAndGet();
                            }
                        }catch (Exception ignored){}
                    });
                }
            }, 0, 1000);
        }
    }

    @Override
    public void onVideoStopped() {
        stopVideoTimer();
        mediaService.playVideo(surfaceView.getHolder(), this);
    }

    @Override
    public void new_video_tile(String text) {
        textview_file_name.setText(text);
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
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                moving_start_x = moving_button.getX();
            case MotionEvent.ACTION_MOVE:
                moving_button.setX(motionEvent.getRawX());
                return true;
            case MotionEvent.ACTION_UP:
                float x = moving_button.getX() - stop_button.getX();
                if (x < 0) x = x * -1;
                if (x < 50) {
                    math_dialog.stop_alarm(this, this);
                }
                moving_button.setX(moving_start_x);
                return true;
        }
        return false;
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
        if(mediaService != null) {
            mediaService.start_video_Timer(this);
            mediaService.bind_Video_to_Surface(surfaceHolder);
        }
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
        stopVideoTimer();
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
        stopVideoTimer();
        if (isServiceBound) {
            unbindService(serviceConnection);
            mediaService.onDestroy();
            isServiceBound = false;
        }
        mediaService.set_Old_Volume();
        finish();
    }

    /**
     * It stops the Video Timer which is shown on the UI (It shows how long the video/audio is
     * already running.)
     */
    private void stopVideoTimer(){
        if(videoTimeTimer != null){
            videoTimeTimer.cancel();
        }
    }
}