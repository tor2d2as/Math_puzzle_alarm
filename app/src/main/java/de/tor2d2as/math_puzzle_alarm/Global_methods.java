package de.tor2d2as.math_puzzle_alarm;

import android.content.Context;

import androidx.core.content.res.ResourcesCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Objects;

public class Global_methods {

    //The following constants are used in JSON.
    // This should minimize writing mistakes by creating and reading the JSON file from storage.
    private final String json_object_name = "clocks";
    private final String enabled = "enabled";
    private final String time = "time";
    private final String monday = "mo";
    private final String tuesday = "tu";
    private final String wednesday = "we";
    private final String thursday = "th";
    private final String friday = "fr";
    private final String saturday = "sa";
    private final String sunday = "su";
    private final String volume = "volume";
    private final String folder = "folder";
    private final String id = "id";
    private final String is_tomorrow = "is_tom";
    //--------------------------------------------------

    /**
     * It returns the information for the next alarm from JSON_CONFIG, or Null if something went wrong.
     * @return volume = -1 and folder != null -> User only set an Alarm Sound;
     *         volume != -1 and folder == null -> User only set Alarm Volume;
     *         volume != -1 and folder != null -> User set both Alarm Volume and Alarm Sound;
     *         default option for id = -1 and is_tomorrow = false;
     *
     *         null -> Something went wrong
     */
    public Next_alarm_entry decode_next_alarm(String json_config){
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(json_config);
            String tmp_folder = null;
            int tmp_volume = -1;
            int tmp_id = -1;
            boolean tmp_is_tomorrow = false;
            if(jsonObject.has(folder)){
                tmp_folder = jsonObject.getString(folder);
            }
            if(jsonObject.has(volume)){
                tmp_volume = jsonObject.getInt(volume);
            }
            if(jsonObject.has(id)){
                tmp_id = jsonObject.getInt(id);
            }
            if(jsonObject.has(is_tomorrow)){
                tmp_is_tomorrow = jsonObject.getBoolean(is_tomorrow);
            }
            return new Next_alarm_entry(tmp_folder, tmp_volume, tmp_id, tmp_is_tomorrow);
        } catch (JSONException e) {e.printStackTrace(); return null;}
    }

    /**
     * It generates a JSON-formatted string for the next alarm config.
     * @param alarm_folder The folder from which the alarm should play music.
     * @param alarm_volume The volume of the alarm.
     * @param id The id of the clock. It is identical with the position in the Clock_UI_Entry_config
     * @param is_tomorrow It indicates if the alarm is a one-time thing or at least once a week.
     * @return The given array as JSON formatted String or null if something went wrong.
     */
    public String generate_next_alarm(String alarm_folder, Integer alarm_volume, int id, boolean is_tomorrow){
        JSONObject entry = new JSONObject();
        try {
            entry.put(folder, alarm_folder);
            entry.put(volume, alarm_volume);
            entry.put(this.id, id);
            entry.put(this.is_tomorrow, is_tomorrow);
            return entry.toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * It converts the JSON-formatted string to an array, which can be used in the application.
     * @param context The application context
     * @param json_config A JSON-formatted string which holds the information for the clocks.
     * @return The array, which can be used to show the entries to the user.
     */
    public ArrayList<Clock_ui_entry> decode_config(Context context, String json_config){
        try {
            JSONObject jsonObject = new JSONObject(json_config);
            JSONArray jsonArray = jsonObject.getJSONArray(json_object_name);
            ArrayList<Clock_ui_entry> clocks = new ArrayList<>();
            clocks.clear();
            String folder_path;
            boolean[] days;
            for (int i = 0; i < jsonArray.length(); i++){
                //The boolean[] has to be initialized in the loop because it is immutable.
                days = new boolean[7];
                JSONObject single_clock = jsonArray.getJSONObject(i);
                days[0] = single_clock.getBoolean(monday);
                days[1] = single_clock.getBoolean(tuesday);
                days[2] = single_clock.getBoolean(wednesday);
                days[3] = single_clock.getBoolean(thursday);
                days[4] = single_clock.getBoolean(friday);
                days[5] = single_clock.getBoolean(saturday);
                days[6] = single_clock.getBoolean(sunday);
                if (single_clock.has(folder)) folder_path = single_clock.getString(folder); else folder_path = null;
                clocks.add(new Clock_ui_entry(
                        single_clock.getBoolean(enabled),
                        single_clock.getString(time),
                        days_boolean_to_string(context, days),
                        days,
                        folder_path,
                        single_clock.getInt(volume),
                        Objects.requireNonNull(ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_math_puzzle_alarm_app_icon_transparenter_hintergrund, null)))
                );
            }
            return clocks;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    /**
     * It generates a JSON-formatted string for the alarm clock config.
     * @param clock_ui_entries The information for the alarm clocks.
     * @return The given array as JSON formatted string, or null if something went wrong.
     */
    public String generate_config(ArrayList<Clock_ui_entry> clock_ui_entries){
        Clock_ui_entry clock_ui_entry;
        JSONArray jsonArray = new JSONArray();

        try {
            for (int i = 0; i < clock_ui_entries.size(); i++) {
                clock_ui_entry = clock_ui_entries.get(i);
                JSONObject clock = new JSONObject();
                clock.put(enabled, clock_ui_entry.isClock_enabled());
                clock.put(time, clock_ui_entry.getClock_time());
                //The days of the clock --------------------
                clock.put(monday, clock_ui_entry.getClock_day_boolean()[0]);
                clock.put(tuesday, clock_ui_entry.getClock_day_boolean()[1]);
                clock.put(wednesday, clock_ui_entry.getClock_day_boolean()[2]);
                clock.put(thursday, clock_ui_entry.getClock_day_boolean()[3]);
                clock.put(friday, clock_ui_entry.getClock_day_boolean()[4]);
                clock.put(saturday, clock_ui_entry.getClock_day_boolean()[5]);
                clock.put(sunday, clock_ui_entry.getClock_day_boolean()[6]);
                //Don't save: clock_ui_entry.getClock_day(), it depends on the user language and
                //should be generated with the method days_boolean_to_string(...) during runtime.
                //------------------------------------------
                clock.put(volume, clock_ui_entry.getClock_volume_int());
                clock.put(folder, clock_ui_entry.getClock_folder());
                //Clock id is not necessary; it is the order of the entries.
                jsonArray.put(clock);
            }
            JSONObject clockObject = new JSONObject();
            clockObject.put(json_object_name, jsonArray);
            return clockObject.toString();
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    /**
     * It generates a localized text for which days the clock is activated.
     * @param context The application context: For getting the translated strings from the Translation Editor.
     * @param clock_days An array with 7 values (0 = Monday, 1 = Tuesday, ..., 6 = Sunday)
     *                   True = The clock is activated for that day,
     *                   False the clock is disabled for that day
     * @return The generated String
     */
    public String days_boolean_to_string(Context context, boolean[] clock_days){
        StringBuilder builder = new StringBuilder();
        if(clock_days[0] && clock_days[1] && clock_days[2] && clock_days[3] && clock_days[4] && clock_days[5] && clock_days[6]){
            builder.append(context.getString(R.string.every_day));
        }else if(clock_days[0] && clock_days[1] && clock_days[2] && clock_days[3] && clock_days[4] && !clock_days[5] && !clock_days[6]){
            builder.append(context.getString(R.string.work_days));
        }else if(!clock_days[0] && !clock_days[1] && !clock_days[2] && !clock_days[3] && !clock_days[4] && clock_days[5] && clock_days[6]){
            builder.append(context.getString(R.string.weekend));
        }else {
            if (clock_days[0]) {
                builder.append(context.getString(R.string.Monday_short));
            }
            if (clock_days[1]) {
                if (builder.length() > 0) builder.append(", ");
                builder.append(context.getString(R.string.Tuesday_short));
            }
            if (clock_days[2]) {
                if (builder.length() > 0) builder.append(", ");
                builder.append(context.getString(R.string.Wednesday_short));
            }
            if (clock_days[3]) {
                if (builder.length() > 0) builder.append(", ");
                builder.append(context.getString(R.string.Thursday_short));
            }
            if (clock_days[4]) {
                if (builder.length() > 0) builder.append(", ");
                builder.append(context.getString(R.string.Friday_short));
            }
            if (clock_days[5]) {
                if (builder.length() > 0) builder.append(", ");
                builder.append(context.getString(R.string.Saturday_short));
            }
            if (clock_days[6]) {
                if (builder.length() > 0) builder.append(", ");
                builder.append(context.getString(R.string.Sunday_short));
            }
        }
        if(builder.length() == 0) builder.append(context.getString(R.string.tomorrow));
        return builder.toString();
    }
}
