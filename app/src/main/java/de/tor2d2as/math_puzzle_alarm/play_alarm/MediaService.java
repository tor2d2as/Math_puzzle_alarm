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
import java.util.ArrayList;
import java.util.Random;

import de.tor2d2as.math_puzzle_alarm.R;

public class MediaService extends Service {
    private MediaPlayer mediaPlayer;
    private final IBinder binder = new MediaBinder();

    Alarm_State alarm_state = new Alarm_State();

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            mediaPlayer = new MediaPlayer(createAttributionContext("audioPlayback"));
        }else{
            mediaPlayer = new MediaPlayer();
        }
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
     * If no video or audio is playing, it starts to play the given file and adjusts the audio volume.
     */
    public void playVideo(SurfaceHolder surfaceHolder, VideoPlayerNotify videoPlayerNotify) {

        //Play the video
        if(mediaPlayer.isPlaying()) {
            if(alarm_state.getCurrent_song_playing() != null) {
                videoPlayerNotify.new_video_tile(alarm_state.getCurrent_song_playing());
            }
        }else{
            //Choose video
            Uri video_url = choose_random_video();
            if (video_url == null) {
                video_url = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                videoPlayerNotify.new_video_tile(getString(R.string.standard_alarm_song));
            } else {
                String path = video_url.getLastPathSegment();
                if(path != null) {
                    String[] parted = path.split("/");
                    alarm_state.setCurrent_song_playing(parted[parted.length-1]);
                    videoPlayerNotify.new_video_tile(parted[parted.length-1]);
                }
            }

            //Set the device volume to the alarm volume.
            if (!alarm_state.isVolume_already_done()) {
                set_Media_Volume();
                alarm_state.setVolume_already_done(true);
            }

            //Start playing the video.
            try {
                mediaPlayer.reset();
                mediaPlayer.setDataSource(this, video_url);
            } catch (IOException e) {
                Log.e("Math Alarm", "An error occurred when setting the data source for the media player.");
                e.printStackTrace();
            }
            new Thread(() -> {
                bind_Video_to_Surface(surfaceHolder);
                if(!surfaceHolder.getSurface().isValid()){
                    removeSurfaceMediaPlayer();
                }
            }).start();
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(mp -> {
                start_video_Timer(videoPlayerNotify);
                mediaPlayer.start();
            });
            // Sets the listener to check if the current file has finished playing.
            mediaPlayer.setOnCompletionListener(mediaPlayer -> {
                if (surfaceHolder.getSurface().isValid()) {
                    videoPlayerNotify.onVideoStopped();
                }else {
                    playVideo(surfaceHolder, videoPlayerNotify);
                }
            });
        }
    }

    /**
     * It removes the connection between the VideoSurface and the playing video.
     * In fact, after calling this method, the mediaPlayer will only play the audio from
     * the video. With the method bind_Video_to_Surface(...), the video can be shown again on the UI.
     */
    public void removeSurfaceMediaPlayer(){
        if(mediaPlayer != null) {
            mediaPlayer.setSurface(null);
        }
    }

    /**
     * Starts the Video Timer (It shows how long the video/audio is already running.),
     * which is shown on the UI.
     * @param videoPlayerNotify The interface which should be called.
     */
    public void start_video_Timer(VideoPlayerNotify videoPlayerNotify){
        videoPlayerNotify.onVideoStarted(mediaPlayer, mediaPlayer.getDuration(), mediaPlayer.getCurrentPosition(), alarm_state.isStandard_sound_chosen());
    }

    /**
     * Call it if the video should be shown on the Surface.
     * @param surfaceHolder The holder of the SurfaceView in which the video should be played.
     *                      (Tip: Check out the XML file of the corresponding UI)
     */
    public void bind_Video_to_Surface(SurfaceHolder surfaceHolder){
        if(surfaceHolder.getSurface().isValid()) {
            mediaPlayer.setDisplay(surfaceHolder);
        }
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

    public interface VideoPlayerNotify {
        /**
         * This method will be called if a new video starts.
         * It will update the progress bar every second while the video plays.
         */
        void onVideoStarted(MediaPlayer mediaPlayer, int videoLength, int videoPosition, boolean standard_sound_chosen);

        /**
         * This method will be called if the media player stopped. It will also start a new audio/video.
         */
        void onVideoStopped();

        /**
         * This method is called when the video tile changes.
         */
        void new_video_tile(String text);
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
     * This method chooses a sound to play and returns its path.
     * If it returns null, the standard sound was chosen.
     */
    public Uri choose_random_video() {
        if (!alarm_state.isStandard_sound_chosen()) {
            ArrayList<Uri> all_files = alarm_state.get_all_files(this);
            if((all_files != null) && (!all_files.isEmpty())) {
                //Some initialization of variables, so that it don't have to be done inside the loop.
                Random random = new Random();
                Uri tmp_path;
                int random_number;

                while (!all_files.isEmpty()) {
                    random_number = random.nextInt(all_files.size());
                    tmp_path = all_files.get(random_number);
                    if (alarm_state.isFilePlayable(this, tmp_path)){
                        return tmp_path;
                    } else {
                        //This line should automatically modify the list in alarm state, since all_files is just a reference to the variable in Alarm_State and not a copy
                        all_files.remove(random_number);
                    }
                }
            }
            //Chooses the standard Sound and remembers its decision
            alarm_state.setStandard_sound_chosen(true);
        }
        return null;
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
