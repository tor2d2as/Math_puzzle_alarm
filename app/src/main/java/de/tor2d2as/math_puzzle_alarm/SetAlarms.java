package de.tor2d2as.math_puzzle_alarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.widget.Toast;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;

import de.tor2d2as.math_puzzle_alarm.play_alarm.Play_alarm;

public class SetAlarms {

    private final Context context;
    private final int requestCode = 35434;
    private final HashMap<Long, String> time_and_folder = new HashMap<>();
    private final HashMap<Long, Integer> time_and_volume = new HashMap<>();
    private final HashMap<Long, Integer> time_and_id = new HashMap<>();
    private final HashMap<Long, Boolean> time_and_tomorrow = new HashMap<>();
    private final Save_clocks save_clocks;
    private final Global_methods global_methods = new Global_methods();

    public SetAlarms(Context mContext) {
        context = mContext;
        save_clocks = new Save_clocks(mContext);
    }

    /**
     * Sets the Alarm. It also overrides the file for the Next_alarm.
     * (NOTE: It should automatically override the last alarm, because an app can only have one Alarm with a specific Request Code.)
     * @return The next alarm time as a string, formatted as: dd-MM-yyyy HH:mm:ss
     */
    public String start_alarm(ArrayList<Clock_ui_entry> clock_ui_entries) {
        try {
            Long alarm_time_in_millisecond = find_next_alarm(clock_ui_entries);
            //The Values for the Hashmaps: time_and_folder, time_and_volume, time_and_id, time_and_tomorrow were set in the Method: find_next_alarm(clock_ui_entries)
            return start_alarm(alarm_time_in_millisecond, time_and_folder.get(alarm_time_in_millisecond), time_and_volume.get(alarm_time_in_millisecond), false, time_and_id.get(alarm_time_in_millisecond), time_and_tomorrow.get(alarm_time_in_millisecond));
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            Toast.makeText(context, context.getString(R.string.no_alarm_set) + "Error: \n" + sw, Toast.LENGTH_LONG).show();
            return sw.toString();
        }
    }

    /**
     * Sets the next Alarm. It also overrides the file for the Next_alarm.
     * (NOTE: It should automatically override the last alarm, because an app can only have one Alarm with a specific Request Code.)
     * @param alarm_time_in_millisecond The alarm time in milliseconds
     * @param alarm_folder The Alarm Folder
     * @param alarm_volume The Alarm Volume
     * @param snooze If this is a new alarm (false), or if the clock is snoozing (true);
     *               This variable is only used to check if the config for the next alarm has to be updated or not.
     * @param id The id of the clock, it is identical with the position in Clock_UI_Entry_config
     *           (id will be ignored, if snooze = true)
     * @param is_tomorrow It indicates if the alarm is a one-time thing or at least once a week.
     *             (is_tomorrow will be ignored, if snooze = true)
     * @return The next alarm time as a string, formatted as: dd-MM-yyyy HH:mm:ss
     */
    public String start_alarm(long alarm_time_in_millisecond, String alarm_folder, Integer alarm_volume, boolean snooze, Integer id, Boolean is_tomorrow) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(context.ALARM_SERVICE);

        Intent intent = new Intent(context, Play_alarm.class);
        //intent.putExtra("clock_folder", alarm_folder);
        //intent.putExtra("alarm_volume", alarm_volume);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(context.getApplicationContext(), requestCode, intent, PendingIntent.FLAG_IMMUTABLE);

        String date_as_text;
        if (alarm_time_in_millisecond == 0) {
            //The next_alarm_config doesn't need to be overwritten. This will be down if the next alarm is set.
            Toast.makeText(context, context.getString(R.string.no_alarm_set), Toast.LENGTH_LONG).show();
            alarmManager.cancel(pendingIntent);
            date_as_text = null;
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                // If not, request the SCHEDULE_EXACT_ALARM permission
                Intent intent_per = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                intent_per.setData(Uri.fromParts("package", context.getPackageName(), null));
                context.startActivity(intent_per);
            }
            //The alarm will be set in the next line -----------------------------------------------
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarm_time_in_millisecond, pendingIntent);
            //--------------------------------------------------------------------------------------
            if(!snooze) {
                //If the alarm snooze, the next_alarm_config will not be changed (because it is the same, as the
                //one that plays at the moment)
                save_clocks.save_next_alarm(global_methods.generate_next_alarm(alarm_folder, alarm_volume, id, is_tomorrow));
            }
            date_as_text = getAlarmTime(alarm_time_in_millisecond);
            Toast.makeText(context, String.format(context.getString(R.string.next_Alarm), date_as_text), Toast.LENGTH_LONG).show();
        }
        return date_as_text;
        //return save_clocks.read_next_alarm(); //For Debugging only
    }

    /**
     * Returns the Time as formatted as: dd-MM-yyyy HH:mm:ss
     *
     * @param alarm_time The actual Alarm Time in Milliseconds
     */
    private String getAlarmTime(Long alarm_time) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(alarm_time);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        return simpleDateFormat.format(calendar.getTime());
    }

    /**
     * It calculates which alarm should be enabled next.
     * And adds the information to the Hashmaps: time_and_folder, time_and_volume, time_and_id, time_and_tomorrow
     * @param clock_ui_entries The List with all Alarms
     * @return The time at which the alarm should go off OR 0 if there is no alarm
     */
    private long find_next_alarm(ArrayList<Clock_ui_entry> clock_ui_entries) throws ParseException {
        ArrayList<Long> possible_times = new ArrayList<>();
        long possibleTime;
        boolean isTomorrow;
        Clock_ui_entry clock_ui_entry;
        LocalDate actual_Date = LocalDate.now();
        long currentTime = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-ddHH:mm");
        time_and_folder.clear();
        time_and_volume.clear();
        time_and_id.clear();
        time_and_tomorrow.clear();

        for (int i = 0; i < clock_ui_entries.size(); i++) {
            clock_ui_entry = clock_ui_entries.get(i);
            //Goes through all clocks which are enabled.
            if (clock_ui_entry.isClock_enabled()) {
                isTomorrow = true;
                //Loop checks if the alarm clock is set for a specific Date.
                for (int weekday = 0; weekday < clock_ui_entry.getClock_day_boolean().length; weekday++) {
                    if (clock_ui_entry.getClock_day_boolean()[weekday]) {
                        isTomorrow = false;
                        possibleTime = sdf.parse(actual_Date.with(TemporalAdjusters.nextOrSame(DayOfWeek.of(weekday + 1))) + clock_ui_entry.getClock_time()).getTime();
                        if (possibleTime < currentTime) {
                            //This Line will only be executed if the possible Time is in the past, it chooses the Time next Week
                            possibleTime = sdf.parse(actual_Date.with(TemporalAdjusters.next(DayOfWeek.of(weekday + 1))) + clock_ui_entry.getClock_time()).getTime();
                        }
                        possible_times.add(possibleTime);
                        time_and_folder.put(possibleTime, clock_ui_entry.getClock_folder());
                        time_and_volume.put(possibleTime, clock_ui_entry.getClock_volume_int());
                        time_and_id.put(possibleTime, i);
                        time_and_tomorrow.put(possibleTime, isTomorrow);
                    }
                }

                if (isTomorrow) {
                    //Sets the date, if the user chooses that the clock should be executed at the next day.
                    possibleTime = sdf.parse(actual_Date + clock_ui_entry.getClock_time()).getTime();
                    if (possibleTime < currentTime) {
                        possibleTime = sdf.parse(actual_Date.plusDays(1) + clock_ui_entry.getClock_time()).getTime();
                    }
                    possible_times.add(possibleTime);
                    time_and_folder.put(possibleTime, clock_ui_entry.getClock_folder());
                    time_and_volume.put(possibleTime, clock_ui_entry.getClock_volume_int());
                    time_and_id.put(possibleTime, i);
                    time_and_tomorrow.put(possibleTime, isTomorrow);
                }
            }
        }
        Collections.sort(possible_times);
        if (!possible_times.isEmpty()) {
            return possible_times.get(0);
        } else {
            return 0;
        }
    }
}
