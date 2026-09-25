package de.tor2d2as.math_puzzle_alarm.play_alarm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.SurfaceHolder;

import androidx.core.app.NotificationCompat;

import java.io.IOException;

import de.tor2d2as.math_puzzle_alarm.R;

public class MediaService extends Service {
    private MediaPlayer mediaPlayer;
    private final IBinder binder = new MediaBinder();
    Alarm_State alarm_state;
    VideoPlayerNotify videoPlayerNotify;

    @Override
    public void onCreate() {
        super.onCreate();
        alarm_state = new Alarm_State();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            mediaPlayer = new MediaPlayer(createAttributionContext("audioPlayback"));
        }else{
            mediaPlayer = new MediaPlayer();
        }
    }

    /**
     * It saves the current link to the UI, so the interface: VideoPlayerNotify
     * can be used.
     */
    public void update_videoPlayerNotify(VideoPlayerNotify videoPlayerNotify){
        this.videoPlayerNotify = videoPlayerNotify;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = create_notification();
        //It starts the foreground service.
        startForeground(1, notification);
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class MediaBinder extends Binder {
        MediaService getService() {
            return MediaService.this;
        }
    }

    /**
     * It starts playing a new video, if a video already plays then nothing happens.
     */
    public void playVideo(){
        if(!alarm_state.is_alarm_playing()) {
            Uri video_uri = alarm_state.choose_random_video(this);
            if (video_uri == null) {
                video_uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            }

            //Set the device volume to the alarm volume.
            if (!alarm_state.isVolume_already_done()) {
                set_Media_Volume();
                alarm_state.setVolume_already_done(true);
            }

            try {
                mediaPlayer.reset();
                mediaPlayer.setDataSource(this, video_uri);
                mediaPlayer.prepare();
            } catch (IOException | IllegalStateException e) {
                Log.e("Math Alarm", "An error occurred when setting the data source for the media player.");
                e.printStackTrace();
            }

            mediaPlayer.setOnPreparedListener(mp ->{
                mp.start();
                request_current_video_information();
                alarm_state.set_is_alarm_playing(true);
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                alarm_state.set_is_alarm_playing(false);
                playVideo();
            });
        }
    }

    /**
     * It collects the video Information from the current video and
     * sends it to the UI over the interface: videoPlayerNotify.new_video(...)
     */
    public void request_current_video_information(){
        String video_title = alarm_state.getCurrent_song_title(this);
        if ((video_title != null) && (mediaPlayer != null)) {
            if (videoPlayerNotify != null) {
                try {
                    videoPlayerNotify.new_video(video_title, mediaPlayer.getDuration());
                    //The exception will be called if videoPlayerNotify == null after it was checked
                    //(update comes from a different Thread)
                }catch (Exception ignored){}
            }
        }
    }

    /**
     * Call it if the video should be shown on the Surface.
     * @param surfaceHolder The holder of the SurfaceView in which the video should be played.
     *                      (Tip: Check out the XML file of the corresponding UI)
     */
    public void bind_Video_to_Surface(SurfaceHolder surfaceHolder){
        if(mediaPlayer != null) {
            if(surfaceHolder == null){
                mediaPlayer.setDisplay(null);
            }else {
                if(surfaceHolder.getSurface().isValid()) {
                    mediaPlayer.setDisplay(surfaceHolder);
                }
            }
        }
    }

    /**
     * @return the current time of the video OR -1 if no video is playing.
     */
    public int getCurrentVideoTime(){
        if(mediaPlayer != null) {
            return mediaPlayer.getCurrentPosition();
        }else{
            return -1;
        }
    }


    /**
     * @return The current height of the video
     */
    public int getVideoHeight(){
        return mediaPlayer.getVideoHeight();
    }

    /**
     * @return The current width of the video
     */
    public int getVideoWidth(){
        return mediaPlayer.getVideoWidth();
    }

    public interface VideoPlayerNotify {
        /**
         * Gets called if the videotitel or the duration of the video changes
         * @param text The new videotitel
         * @param duration the new duration
         */
        void new_video(final String text, final int duration);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        stopSelf();
    }

    /**
     * Create the notification, so the user can easily go back to the playing alarm.
     * The notification won't be displayed in this method.
     * @return The notification which is ready to display.
     */
    private Notification create_notification(){
        //Creates the Notification Channel
        String name = getString(R.string.channel_name_player);
        String description = getString(R.string.channel_description_player);
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        String CHANNEL_ID = "MediaServiceChannel";
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setDescription(description);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

        //Creates the Notification for the playing clock
        Intent notificationIntent = new Intent(this, Play_alarm.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.playing_audio_title_notification))
                .setContentText(getString(R.string.playing_audio_desc_notification))
                .setSmallIcon(R.drawable.ic_math_puzzle_alarm_app_icon_transparenter_hintergrund)
                .setContentIntent(pendingIntent)
                .build();
    }

    //----------------------------------------------------------------------------------------------

    /**
     * The folder which the user selected to play the audio files from it.
     */
    public void setClock_folder(String folder){
        alarm_state.setClock_folder(folder);
    }

    /**
     * The volume which was set for the alarm.
     */
    public void setAlarm_volume(int volume){
        alarm_state.setAlarm_volume(volume);
    }

    /**
     * @return The folder which the user selected to play the audio files from it.
     */
    public String getClock_folder(){
        return alarm_state.getClock_folder();
    }

    /**
     * @return The volume which was set for the alarm.
     */
    public int getAlarm_volume(){
        return alarm_state.getAlarm_volume();
    }



    /**
     * Sets the System Volume to the Alarm Volume
     */
    public void set_Media_Volume(){
        alarm_state.setOld_volume(set_System_Volume(alarm_state.getAlarm_volume()));
    }

    /**
     * Sets the System Volume to the Old Volume
     */
    public void set_Old_Volume(){
        set_System_Volume(alarm_state.getOld_volume());
    }

    /**
     * Changes the Android setting for the volume for media files.
     * @param new_volume The volume, which should be set.
     * @return The volume, which was set before the method changed it.
     */
    private int set_System_Volume(int new_volume) {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int old_volume_back = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        int dif_volume = new_volume - old_volume_back;
        int volume_direction = AudioManager.ADJUST_SAME;

        if(dif_volume < 0){
            dif_volume = dif_volume * -1;
            volume_direction = AudioManager.ADJUST_LOWER;
        }else if(dif_volume > 0) {
            volume_direction = AudioManager.ADJUST_RAISE;
        }

        for (int i = 0; i < dif_volume; i++) {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, volume_direction, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        }

        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI);

        return old_volume_back;
    }
}
