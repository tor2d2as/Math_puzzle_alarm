package de.tor2d2as.math_puzzle_alarm;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class Save_clocks {

    private final Context context;
    private final String config_name = "alarm_clocks.json";
    private final String next_alarm_config = "next_alarm.json";

    public Save_clocks(Context mContext){
        context = mContext;
    }


    /**
     * Saves the string to the config for the next alarm.
     * @param what_to_save The content which should be saved.
     * @return true = File saved successfully
     * false = File wasn't saved
     */
    public boolean save_next_alarm(String what_to_save){
        return save_config(next_alarm_config, what_to_save);
    }

    /**
     * Reads the config of the next alarm.
     * @return The content of the file, or null if something went wrong.
     */
    public String read_next_alarm(){
        return read_file(next_alarm_config);
    }

    /**
     * Reads the config of the clocks.
     * @return The content of the file, or null if something went wrong.
     */
    public String read_config(){
        return read_file(config_name);
    }

    /**
     * Saves the string to the config for the clocks.
     * @param what_to_save The content which should be saved.
     * @return true = File saved successfully
     * false = File wasn't saved
     */
    public boolean save_config(String what_to_save){
        return save_config(config_name, what_to_save);
    }

    /**
     * Saves the string to the config for the clocks.
     * @param file_name The name of the file which should be saved
     * @param what_to_save The content which should be saved.
     * @return true = File saved successfully
     * false = File wasn't saved
     */
    private boolean save_config(String file_name, String what_to_save){
        File file = new File(context.getFilesDir(), file_name);
        try {
            FileOutputStream outputStream = new FileOutputStream(file);
            outputStream.write(what_to_save.getBytes());
            outputStream.close();
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Reads the config of the clocks
     * @param file_name The name of the file which should be read.
     * @return the content of the File or null if something went wrong.
     */
    private String read_file(String file_name){
        File file = new File(context.getFilesDir(), file_name);
        int length = (int) file.length();
        byte[] bytes = new byte[length];
        try {
            FileInputStream in = new FileInputStream(file);
            in.read(bytes);
            in.close();
            return new String(bytes);
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }
}
