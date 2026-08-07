package de.tor2d2as.math_puzzle_alarm;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity implements Clock_overview_adapter.ItemClickListener{

    private Clock_overview_adapter clock_overview_adapter;

    private TextView next_alarm_time;

    private final Global_methods global_methods = new Global_methods();
    private final Save_clocks save_clocks = new Save_clocks(this);

    ActivityResultLauncher<Intent> mStartForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent intent = result.getData();
                    // Handle the Intent
                    String clock_time;
                    try {
                        clock_time = intent.getStringExtra("clock_time");
                        boolean[] clock_days = intent.getBooleanArrayExtra("clock_days");
                        int clock_volume = intent.getIntExtra("clock_volume", -1);
                        String clock_folder = intent.getStringExtra("clock_folder");
                        int clock_id = intent.getIntExtra("clock_id", -1);

                        //Clocks which were create or modified are initially always true
                        if(clock_id == -1) {
                            clock_overview_adapter.add_clock(true, clock_time, global_methods.days_boolean_to_string(this, clock_days), clock_days, clock_folder, clock_volume, ResourcesCompat.getDrawable(getResources(), R.drawable.ic_math_puzzle_alarm_app_icon_transparenter_hintergrund, null));
                        }else{
                            clock_overview_adapter.modify_clock(clock_id, true, clock_time, global_methods.days_boolean_to_string(this, clock_days), clock_days, clock_folder, clock_volume, ResourcesCompat.getDrawable(getResources(), R.drawable.ic_math_puzzle_alarm_app_icon_transparenter_hintergrund, null));
                        }
                    }catch (Exception e){
                        Toast.makeText(this, getString(R.string.clock_not_set), Toast.LENGTH_LONG).show();
                        load_clock_list_from_config_and_set_alarm();
                        e.printStackTrace();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        next_alarm_time = findViewById(R.id.next_alarm_time);
        // set up the RecyclerView **********************************************************
        RecyclerView alarm_clock_overview_list = findViewById(R.id.alarm_clock_overview_list);
        alarm_clock_overview_list.setLayoutManager(new LinearLayoutManager(this));

        clock_overview_adapter = new Clock_overview_adapter(this, 42);
        alarm_clock_overview_list.setAdapter(clock_overview_adapter);
        clock_overview_adapter.setDragAndDrop_and_remove(alarm_clock_overview_list);
        clock_overview_adapter.setClickListener(MainActivity.this);

        //The Orientation attribute seems to be unnecessary; just set it to 1 and it will be fine.
        DividerItemDecoration dividerItemDecoration_normal = new DividerItemDecoration(alarm_clock_overview_list.getContext(), 1);
        alarm_clock_overview_list.addItemDecoration(dividerItemDecoration_normal);
        //***********************************************************************************

        //Request the permission to start a GUI from a service.
        if (!Settings.canDrawOverlays(this)) {
            Intent intent_overlay = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            startActivity(intent_overlay);
        }

        //Without the Battery_optimization permission, the system will prevent the alarm from ringing!
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
            Intent intent_battery = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent_battery.setData(Uri.parse("package:" + getPackageName()));
            intent_battery.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent_battery);
        }

        load_clock_list_from_config_and_set_alarm();

     /*   Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);*/

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            Intent myIntent = new Intent(MainActivity.this, Clock_Setting_Activity.class);
            mStartForResult.launch(myIntent);
        });
    }

    /**
     * It loads all clocks from the config and shows them on the UI.
     * This method can also be used to reload the whole list.
     * It also sets the next alarm.
     */
    private void load_clock_list_from_config_and_set_alarm(){
        String config = save_clocks.read_config();
        if(config == null){
            next_alarm_time.setText(getString(R.string.no_alarm_set));
        }else {
            clock_overview_adapter.new_Data_and_set_Alarm(global_methods.decode_config(this, config));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(View view, int position, int recyclerviewId) {
        Intent resultIntent = new Intent(MainActivity.this, Clock_Setting_Activity.class);
        Clock_ui_entry clock_ui_entry = clock_overview_adapter.get_clock(position);
        resultIntent.putExtra("clock_time", clock_ui_entry.getClock_time());
        resultIntent.putExtra("clock_days", clock_ui_entry.getClock_day_boolean());
        resultIntent.putExtra("clock_volume", clock_ui_entry.getClock_volume_int());
        resultIntent.putExtra("clock_folder", clock_ui_entry.getClock_folder());
        resultIntent.putExtra("clock_id", position);
        mStartForResult.launch(resultIntent);
    }

    @Override
    public void next_alarm_time(String alarm_time) {
        if(alarm_time == null){
            next_alarm_time.setText(getString(R.string.no_alarm_set));
        }else {
            next_alarm_time.setText(String.format(getString(R.string.next_Alarm), alarm_time));
        }
    }
}