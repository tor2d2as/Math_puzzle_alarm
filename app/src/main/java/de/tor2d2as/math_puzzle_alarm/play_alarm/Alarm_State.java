package de.tor2d2as.math_puzzle_alarm.play_alarm;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;

import java.util.ArrayList;
import java.util.Random;

import de.tor2d2as.math_puzzle_alarm.R;

/**
 * This class is used to save all values from the alarm that is currently playing.
 */
public class Alarm_State {

    private Uri selected_file = null;
    private boolean standard_sound_chosen = false;
    private boolean is_alarm_playing = false;
    private int alarm_volume;
    private int old_volume;
    private String clock_folder = null;
    private boolean volume_already_done = false;
    //The Arraylist: "all_files" has to be null at the beginning; it is used to check if the list was already initialized.
    private ArrayList<Uri> all_files = null;

    /**
     * @return The volume which was set before the alarm started.
     */
    public int getOld_volume() {
        return old_volume;
    }

    /**
     * Save the volume which was set before the alarm started.
     */
    public void setOld_volume(int old_volume) {
        this.old_volume = old_volume;
    }

    /**
     * @return The song name which should be displayed to the user.
     * Null if no file was selected.
     */
    public String getCurrent_song_title(Context context){
        if(standard_sound_chosen){
            return context.getString(R.string.standard_alarm_song);
        }else {
            String path = selected_file.getLastPathSegment();
            if (path != null) {
                String[] parted = path.split("/");
                return parted[parted.length - 1];
            }
            return null;
        }
    }

    /**
     * @return The folder which the user selected to play the audio files from it.
     */
    public String getClock_folder() {
        return clock_folder;
    }

    /**
     * The folder which the user selected to play the audio files from it.
     */
    public void setClock_folder(String clock_folder) {
        this.clock_folder = clock_folder;
    }

    /**
     * @return True = if the alarm is playing, false otherwise.
     */
    public boolean is_alarm_playing(){
        return is_alarm_playing;
    }

    /**
     * Update if the alarm starts playing (alarm_playing = true) / stops (alarm_playing = false) playing.
     */
    public void set_is_alarm_playing(boolean alarm_playing){
        this.is_alarm_playing = alarm_playing;
    }

    /**
     * @return The volume which was set for the alarm.
     */
    public int getAlarm_volume() {
        return alarm_volume;
    }

    /**
     * The volume which was set for the alarm.
     */
    public void setAlarm_volume(int alarm_volume) {
        this.alarm_volume = alarm_volume;
    }

    /**
     * @return If the volume was already adjusted for the alarm.
     */
    public boolean isVolume_already_done() {
        return volume_already_done;
    }

    /**
     * Set to true If the volume was already adjusted for the alarm.
     */
    public void setVolume_already_done(boolean volume_already_done) {
        this.volume_already_done = volume_already_done;
    }

    /**
     * @return If the standard sound should be played.
     */
    public boolean isStandard_sound_chosen() {
        return standard_sound_chosen;
    }

    /**
     * This method chooses a sound to play and returns its path.
     * If it returns null, the standard sound was chosen.
     */
    public Uri choose_random_video(Context context) {
        if (!standard_sound_chosen) {
            // get_all_files saves the result in all_files
            get_all_files(context);
            if((all_files != null) && (!all_files.isEmpty())) {
                Random random = new Random();
                Uri tmp_path;
                int random_number;

                while (!all_files.isEmpty()) {
                    random_number = random.nextInt(all_files.size());
                    tmp_path = all_files.get(random_number);
                    if (isFilePlayable(context, tmp_path)){
                        selected_file = tmp_path;
                        return tmp_path;
                    } else {
                        all_files.remove(random_number);
                    }
                }
            }
            //Chooses the standard Sound and remembers its decision
            standard_sound_chosen = true;
        }
        return null;
    }

    /**
     * It checks if the chosen file is playable by the Media Player.
     * @param uri The path to the file
     * @return True = The media player can play the file; false = otherwise.
     */
    private boolean isFilePlayable(Context context, Uri uri){
        ContentResolver contentResolver = context.getContentResolver();
        String mimeType = contentResolver.getType(uri);
        return mimeType != null && (mimeType.startsWith("audio/") || mimeType.startsWith("video/"));
    }

    /**
     * Returns an Arraylist with all possible audio and video files for the actual clock.
     * The list can include unwanted files, like PDFs. It is not possible to filter them
     * because it will need too much resources for larger folders (Android complained about it).
     * It saves the result in: all_files.
     */
    public void get_all_files(Context context) {
        if(all_files == null) {
            all_files = new ArrayList<>();
            if (clock_folder != null) {
                Uri folderUri = Uri.parse(clock_folder);
                DocumentFile documentFiles = DocumentFile.fromTreeUri(context, folderUri);
                if (documentFiles != null){
                    DocumentFile[] files = documentFiles.listFiles();
                    for (final DocumentFile fileEntry : files){
                        if(!fileEntry.isDirectory()){
                            all_files.add(fileEntry.getUri());
                        }
                    }
                }
            }
        }
    }
}
