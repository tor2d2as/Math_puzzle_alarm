package de.tor2d2as.math_puzzle_alarm.play_alarm;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;

import java.util.ArrayList;

/**
 * This class is used to save all values from the alarm that is currently playing.
 */
public class Alarm_State {
    private int old_volume;
    private String current_song_playing = null;
    private String clock_folder = null;
    private int alarm_volume;
    private boolean volume_already_done = false;
    private boolean standard_sound_chosen = false;
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
     * @return The song that the media player is currently playing.
     */
    public String getCurrent_song_playing(){return current_song_playing;}

    /**
     * Always update this value if the current file from the media player changes to a new file.
     */
    public void setCurrent_song_playing(String current_song_playing){this.current_song_playing = current_song_playing;}

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
     * Set if the standard sound should be played.
     * True = Play standard sound
     * False = Play music from an user-defined folder, defined in getClock_folder().
     */
    public void setStandard_sound_chosen(boolean standard_sound_chosen) {
        this.standard_sound_chosen = standard_sound_chosen;
    }

    /**
     * Returns an Arraylist with all possible audio and video files for the actual clock.
     * The list can also include unwanted files, like PDFs. It is not possible to filter them
     * because it will need too much resources for larger folders (Android complained about it).
     * It returns null if the folder wasn't found.
     * It returns an empty Arraylist, if the folder has no files in it.
     */
    public ArrayList<Uri> get_all_files(Context context) {
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
        return all_files;
    }

    /**
     * It checks if the chosen file is playable by the Media Player.
     * @param uri The path to the file
     * @return True = The media player can play the file; false = otherwise.
     */
    public boolean isFilePlayable(Context context, Uri uri){
        ContentResolver contentResolver = context.getContentResolver();
        String mimeType = contentResolver.getType(uri);
        return mimeType != null && (mimeType.startsWith("audio/") || mimeType.startsWith("video/"));
    }
}
