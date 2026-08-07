package de.tor2d2as.math_puzzle_alarm;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;

import de.tor2d2as.math_puzzle_alarm.play_alarm.Play_alarm;

public class Clock_overview_adapter extends RecyclerView.Adapter<Clock_overview_adapter.ViewHolder> {

    private final ArrayList<Clock_ui_entry> clock_ui_entries = new ArrayList<>();

    private final Context context;

    private final Save_clocks save_clocks;
    private final Global_methods global_methods = new Global_methods();

    private final int recyclerviewId;
    private final LayoutInflater mInflater;

    private ItemClickListener itemClickListener;

    private final SetAlarms setAlarms;

    // data is passed into the constructor
    Clock_overview_adapter(Context mContext, int mRecyclerviewId) {
        context = mContext;
        save_clocks = new Save_clocks(mContext);
        this.mInflater = LayoutInflater.from(mContext);
        recyclerviewId = mRecyclerviewId;

        setAlarms = new SetAlarms(mContext);
    }

    // inflates the row layout from XML when needed
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.alarm_clock_overview, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.clock_state.setChecked(clock_ui_entries.get(position).isClock_enabled());
        holder.clock_time.setText(clock_ui_entries.get(position).getClock_time());
        holder.clock_days.setText(clock_ui_entries.get(position).getClock_day());
        holder.clock_folder.setText(clock_ui_entries.get(position).getClock_folder_user_view());
        holder.clock_volume.setText(String.format(context.getString(R.string.volume), Integer.toString(clock_ui_entries.get(position).getClock_volume_int())));
        holder.clock_picture.setImageDrawable(clock_ui_entries.get(position).getClock_image());
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return clock_ui_entries.size();
    }

    // allows clicks events to be caught
    void setClickListener(ItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    /**
     * Enables drag and drop, and allows the user to delete an entry from the list.
     * @param recyclerView The list which should use drag and drop:
     */
    void setDragAndDrop_and_remove(RecyclerView recyclerView){
        helper.attachToRecyclerView(recyclerView);
    }


    // stores and recycles views as they are scrolled off-screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
        @SuppressLint("UseSwitchCompatOrMaterialCode")
        Switch clock_state;
        ImageView clock_picture;
        TextView clock_time;
        TextView clock_days;
        TextView clock_folder;
        TextView clock_volume;

        ViewHolder(final View clock_view) {
            super(clock_view);
            clock_state = clock_view.findViewById(R.id.clock_state);
            clock_picture = clock_view.findViewById(R.id.clock_picture);
            clock_time = clock_view.findViewById(R.id.clock_time);
            clock_days = clock_view.findViewById(R.id.clock_days);
            clock_folder = clock_view.findViewById(R.id.clock_folder);
            clock_volume = clock_view.findViewById(R.id.clock_volume);
            clock_view.setOnClickListener(this);
            clock_state.setOnCheckedChangeListener(this);

            clock_picture.setClickable(true);
            clock_picture.setOnClickListener(view -> {
                Intent intent = new Intent(context, Play_alarm.class);
                intent.putExtra("alarm_volume", clock_ui_entries.get(getLayoutPosition()).getClock_volume_int());
                intent.putExtra("clock_folder", clock_ui_entries.get(getLayoutPosition()).getClock_folder());
                context.startActivity(intent);
            });
        }

        @Override
        public void onClick(View view) {
            if (itemClickListener != null) itemClickListener.onItemClick(view, getLayoutPosition(), recyclerviewId);
        }

        @Override
        public void onCheckedChanged(@NonNull CompoundButton compoundButton, boolean isChecked) {
            if(clock_ui_entries.get(getLayoutPosition()).isClock_enabled() != isChecked) {
                clock_ui_entries.get(getLayoutPosition()).setClock_enabled(isChecked);
                save_clocks_to_storage_and_enable_clock();
            }
        }
    }

    /**
     * Updates the alarm clock at the given position in the list.
     * It also saves the update to disk and (re)enables the next alarm.
     * @param position The position which should be updated.
     */
    public void modify_clock(int position, boolean clock_enabled, String clock_time, String clock_day, boolean[] clock_day_boolean, String clock_folder, int clock_volume_int, @NonNull Drawable clock_picture){
        clock_ui_entries.get(position).modify_entry(clock_enabled, clock_time, clock_day, clock_day_boolean, clock_folder, clock_volume_int, clock_picture);
        notifyItemChanged(position);
        save_clocks_to_storage_and_enable_clock();
    }

    /**
     * Adds an entry to the end of the list.
     * It also saves the update to disk and (re)enables the next alarm.
     */
    public void add_clock(boolean clock_enabled, String clock_time, String clock_day, boolean[] clock_day_boolean, String clock_folder, int clock_volume_int, @NonNull Drawable clock_picture) {
        clock_ui_entries.add(new Clock_ui_entry(clock_enabled, clock_time, clock_day, clock_day_boolean, clock_folder, clock_volume_int, clock_picture));
        notifyItemInserted(clock_ui_entries.size()-1);
        save_clocks_to_storage_and_enable_clock();
    }

    /**
     * Returns the clock information from the specified entry.
     */
    public Clock_ui_entry get_clock(int index){
        return clock_ui_entries.get(index);
    }

    /**
     * Removes the alarm clock at the given position in the list.
     * It also saves the update to disk and (re)enables the next alarm.
     * @param position The position which should be removed.
     */
    private void remove_item(int position) {
        clock_ui_entries.remove(position);
        notifyItemRemoved(position);
        save_clocks_to_storage_and_enable_clock();
    }

    /**
     * Replaces all data in the list and enables the next alarm.
     */
    public void new_Data_and_set_Alarm(ArrayList<Clock_ui_entry> mClock_ui_list) {
        clock_ui_entries.clear();
        clock_ui_entries.addAll(mClock_ui_list);
        notifyDataSetChanged();
        //The config for the next_alarm gets automatically updated by calling
        //the method: setAlarms.start_alarm(...). So, we don't have to handle it here.
        itemClickListener.next_alarm_time(setAlarms.start_alarm(clock_ui_entries));
    }

    /**
     * It saves all clocks as a file to the internal storage system of the app, and enables the next alarm.
     * The method sends a toast to the user if something went wrong.
     * (Note: No Android permissions needed)
     */
    private void save_clocks_to_storage_and_enable_clock(){
        //The config for the next_alarm gets automatically updated by calling
        //the method: setAlarms.start_alarm(...) so we don't have to handle it here.
        String to_save = global_methods.generate_config(clock_ui_entries);
        if(to_save != null){
            boolean saved_clock_successful = save_clocks.save_config(to_save);
            if(saved_clock_successful){
                String next_alarm = setAlarms.start_alarm(clock_ui_entries);
                itemClickListener.next_alarm_time(next_alarm);
                return;
            }
        }
        Toast.makeText(context, context.getString(R.string.saving_clock_failure), Toast.LENGTH_LONG).show();
        String config = save_clocks.read_config();
        if(config == null) new_Data_and_set_Alarm(new ArrayList<>()); else new_Data_and_set_Alarm(global_methods.decode_config(context, config));
    }

    private final ItemTouchHelper helper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT) {
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder dragged, @NonNull RecyclerView.ViewHolder target) {
            int position_dragged = dragged.getAdapterPosition();
            int position_target = target.getAdapterPosition();

            Collections.swap(clock_ui_entries, position_dragged, position_target);
            notifyItemMoved(position_dragged, position_target);
            //We have to enable the alarm again so the next_alarm config gets updated with the new id.
            save_clocks_to_storage_and_enable_clock();
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
            int position = viewHolder.getLayoutPosition();
            remove_item(position);
        }
    });

    // Parent Activity will implement this method to respond to click events.
    public interface ItemClickListener {
        void onItemClick(View view, int position, int recyclerviewId);

        void next_alarm_time(String alarm_time);

        //void drag_and_dropped(int position_dragged, int position_target, int eigene_id);

        //void removed_Eintrag(int position_removed, int eigene_id);
    }
}
