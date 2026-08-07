package de.tor2d2as.math_puzzle_alarm;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class Clock_Setting_Activity extends AppCompatActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    private TimePicker chosen_clock_time;
    private Button chose_monday;
    private Button chose_tuesday;
    private Button chose_wednesday;
    private Button chose_thursday;
    private Button chose_friday;
    private Button chose_saturday;
    private Button chose_sunday;
    private SeekBar chose_clock_volume;
    private TextView clock_volume_text;
    private Button chose_folder;
    private Button save_clock_settings;

    private String clock_time = null;
    //0 = Monday, 1 = Tuesday, ..., 6 = Sunday
    private boolean[] clock_days = {false, false, false, false, false, false, false};

    private int max_volume;
    private int clock_volume;

    private int clock_id = -1;

    private String clock_folder = null;

    /**
     * Handles the result from the Android dialog if the user adds a new folder as a playlist.
     */
    ActivityResultLauncher<Intent> folder_access_management = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if(result != null) {
                        Intent intent = result.getData();
                        if (intent != null) {
                            Uri uri = intent.getData();
                            if (uri != null) {
                                //Make the permission permanent
                                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); //| Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                                clock_folder = uri.toString();
                                chose_folder.setText(String.format(getString(R.string.path_chosen), uri.getLastPathSegment()));
                            }
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clock__configure);

        chosen_clock_time = findViewById(R.id.chosen_clock_time);
        chose_monday = findViewById(R.id.chose_monday);
        chose_tuesday = findViewById(R.id.chose_tuesday);
        chose_wednesday = findViewById(R.id.chose_wednesday);
        chose_thursday = findViewById(R.id.chose_thursday);
        chose_friday = findViewById(R.id.chose_friday);
        chose_saturday = findViewById(R.id.chose_saturday);
        chose_sunday = findViewById(R.id.chose_sunday);
        chose_clock_volume = findViewById(R.id.chose_clock_volume);
        clock_volume_text = findViewById(R.id.clock_volume_text);
        chose_folder = findViewById(R.id.chose_folder);
        save_clock_settings = findViewById(R.id.save_clock_settings);

        chosen_clock_time.setIs24HourView(true);

        chose_monday.setOnClickListener(this);
        chose_tuesday.setOnClickListener(this);
        chose_wednesday.setOnClickListener(this);
        chose_thursday.setOnClickListener(this);
        chose_friday.setOnClickListener(this);
        chose_saturday.setOnClickListener(this);
        chose_sunday.setOnClickListener(this);
        chose_clock_volume.setOnSeekBarChangeListener(this);
        chose_folder.setOnClickListener(this);
        save_clock_settings.setOnClickListener(this);

        AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audio != null) {
            max_volume = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        }else{
            max_volume = 20;
            Toast.makeText(this, getString(R.string.no_max_volume_found), Toast.LENGTH_SHORT).show();
        }
        clock_volume = max_volume/2;

        try{
            if(getIntent().hasExtra("clock_time")) clock_time = getIntent().getStringExtra("clock_time");
            if(getIntent().hasExtra("clock_days")) clock_days = getIntent().getBooleanArrayExtra("clock_days");
            if(getIntent().hasExtra("clock_volume")) clock_volume = getIntent().getIntExtra("clock_volume", clock_volume);
            if(getIntent().hasExtra("clock_folder")) clock_folder = getIntent().getStringExtra("clock_folder");
            if(getIntent().hasExtra("clock_id")) clock_id = getIntent().getIntExtra("clock_id", -1);
        }catch (Exception e){
            e.printStackTrace();
        }

        if(clock_time != null){
            String[] split_time = clock_time.split(":");
            chosen_clock_time.setHour(Integer.parseInt(split_time[0]));
            chosen_clock_time.setMinute(Integer.parseInt(split_time[1]));
        }

        if (clock_days[0]) chose_monday.setBackgroundResource(R.drawable.roundedbutton_enabled); else chose_monday.setBackgroundResource(R.drawable.roundedbutton_disabled);
        if (clock_days[1]) chose_tuesday.setBackgroundResource(R.drawable.roundedbutton_enabled); else chose_tuesday.setBackgroundResource(R.drawable.roundedbutton_disabled);
        if (clock_days[2]) chose_wednesday.setBackgroundResource(R.drawable.roundedbutton_enabled); else chose_wednesday.setBackgroundResource(R.drawable.roundedbutton_disabled);
        if (clock_days[3]) chose_thursday.setBackgroundResource(R.drawable.roundedbutton_enabled); else chose_thursday.setBackgroundResource(R.drawable.roundedbutton_disabled);
        if (clock_days[4]) chose_friday.setBackgroundResource(R.drawable.roundedbutton_enabled); else chose_friday.setBackgroundResource(R.drawable.roundedbutton_disabled);
        if (clock_days[5]) chose_saturday.setBackgroundResource(R.drawable.roundedbutton_enabled); else chose_saturday.setBackgroundResource(R.drawable.roundedbutton_disabled);
        if (clock_days[6]) chose_sunday.setBackgroundResource(R.drawable.roundedbutton_enabled); else chose_sunday.setBackgroundResource(R.drawable.roundedbutton_disabled);

        if(clock_folder != null){
            Uri uri = Uri.parse(clock_folder);
            chose_folder.setText(String.format(getString(R.string.path_chosen), uri.getLastPathSegment()));
        }

        new Handler().postDelayed(() -> runOnUiThread(() -> {
            chose_clock_volume.setMax(max_volume);
            chose_clock_volume.setProgress(clock_volume);
            update_chose_clock_volume(chose_clock_volume, clock_volume);
        }), 100);
    }

    @SuppressLint("SetTextI18n")
    private void update_chose_clock_volume(SeekBar seekBar, int volume) {
        int val = (volume * (seekBar.getWidth() - 2 * seekBar.getThumbOffset())) / seekBar.getMax();
        clock_volume_text.setText(Integer.toString(volume));
        clock_volume_text.setX(seekBar.getX() + val + (float) seekBar.getThumbOffset() / 2);
    }

    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.chose_monday){
            if (clock_days[0]) chose_monday.setBackgroundResource(R.drawable.roundedbutton_disabled); else chose_monday.setBackgroundResource(R.drawable.roundedbutton_enabled);
            clock_days[0] = !clock_days[0];

        }else if(view.getId() == R.id.chose_tuesday){
            if (clock_days[1]) chose_tuesday.setBackgroundResource(R.drawable.roundedbutton_disabled); else chose_tuesday.setBackgroundResource(R.drawable.roundedbutton_enabled);
            clock_days[1] = !clock_days[1];

        }else if(view.getId() == R.id.chose_wednesday){
            if (clock_days[2]) chose_wednesday.setBackgroundResource(R.drawable.roundedbutton_disabled); else chose_wednesday.setBackgroundResource(R.drawable.roundedbutton_enabled);
            clock_days[2] = !clock_days[2];

        }else if(view.getId() == R.id.chose_thursday){
            if (clock_days[3]) chose_thursday.setBackgroundResource(R.drawable.roundedbutton_disabled); else chose_thursday.setBackgroundResource(R.drawable.roundedbutton_enabled);
            clock_days[3] = !clock_days[3];

        }else if(view.getId() == R.id.chose_friday){
            if (clock_days[4]) chose_friday.setBackgroundResource(R.drawable.roundedbutton_disabled); else chose_friday.setBackgroundResource(R.drawable.roundedbutton_enabled);
            clock_days[4] = !clock_days[4];

        }else if(view.getId() == R.id.chose_saturday){
            if (clock_days[5]) chose_saturday.setBackgroundResource(R.drawable.roundedbutton_disabled); else chose_saturday.setBackgroundResource(R.drawable.roundedbutton_enabled);
            clock_days[5] = !clock_days[5];

        }else if(view.getId() == R.id.chose_sunday){
            if (clock_days[6]) chose_sunday.setBackgroundResource(R.drawable.roundedbutton_disabled); else chose_sunday.setBackgroundResource(R.drawable.roundedbutton_enabled);
            clock_days[6] = !clock_days[6];


        } else if(view.getId() == R.id.chose_folder){
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            folder_access_management.launch(intent);

        } else if(view.getId() == R.id.save_clock_settings){
            Intent resultIntent = new Intent();
            int clock_hour_int = chosen_clock_time.getHour();
            int clock_minute_int = chosen_clock_time.getMinute();
            String clock_hour;
            String clock_minute;

            if(clock_hour_int < 10) clock_hour = "0" + clock_hour_int; else clock_hour = Integer.toString(clock_hour_int);
            if(clock_minute_int < 10) clock_minute = "0" + clock_minute_int; else clock_minute = Integer.toString(clock_minute_int);

            //It returns the result to the previous UI, saving the alarm to disk and enabling it
            //has to be done there.
            resultIntent.putExtra("clock_time", clock_hour + ":" + clock_minute);
            resultIntent.putExtra("clock_days", clock_days);
            resultIntent.putExtra("clock_volume", clock_volume);
            resultIntent.putExtra("clock_folder", clock_folder);
            resultIntent.putExtra("clock_id", clock_id);
            setResult(Activity.RESULT_OK, resultIntent);
            finish();
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean fromUser) {
        if(fromUser) {
            if (seekBar.getId() == R.id.chose_clock_volume) {
                clock_volume = i;
                update_chose_clock_volume(seekBar, clock_volume);
            }
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}