package de.tor2d2as.math_puzzle_alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class Auto_start extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        //It sets the alarm after restarting the device.
        String action = intent.getAction();
        if (action != null){
            if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
                SetAlarms setAlarms = new SetAlarms(context);
                Global_methods global_methods = new Global_methods();
                Save_clocks save_clocks = new Save_clocks(context);

                setAlarms.start_alarm(global_methods.decode_config(context, save_clocks.read_config()));
            }
        }
    }
}
