package com.pk.alarmzy;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.pk.alarmzy.alarm.db.AlarmEntity;
import com.pk.alarmzy.alarm.db.AlarmViewModel;
import com.pk.alarmzy.alarm.helper.AlarmHelper;
import com.pk.alarmzy.alarm.helper.NotificationHelper;
import com.pk.alarmzy.alarm.recycler.AlarmRecViewAdapter;
import com.pk.alarmzy.settings.SettingsActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "PK:MainActivity";

    RecyclerView mRecyclerView;
    RecyclerView.LayoutManager mLayoutManager;
    AlarmRecViewAdapter mAdapter;
    AlarmHelper alarmHelper;
    ImageView noAlarmsImage;
    TextView noAlarmsText;
    // Store selected hour, minute by TimePicker
    private int mHour, mMinute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        noAlarmsImage = findViewById(R.id.image_when_empty);
        noAlarmsText = findViewById(R.id.text_when_empty);
        mRecyclerView = findViewById(R.id.item_alarm_recyclerView);
        alarmHelper = new AlarmHelper();

        mLayoutManager = new LinearLayoutManager(this);
        mAdapter = new AlarmRecViewAdapter();

        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        final AlarmViewModel alarmViewModel = new ViewModelProvider(this).get(AlarmViewModel.class);
        alarmViewModel.getAllAlarms().observe(this, new Observer<List<AlarmEntity>>() {
            @Override
            public void onChanged(List<AlarmEntity> alarmEntities) {
                mAdapter.submitList(alarmEntities);

                if (alarmEntities.size() == 0) {
                    noAlarmsImage.setVisibility(View.VISIBLE);
                    noAlarmsText.setVisibility(View.VISIBLE);
                } else {
                    noAlarmsImage.setVisibility(View.GONE);
                    noAlarmsText.setVisibility(View.GONE);
                }
            }
        });

        FloatingActionButton fab = findViewById(R.id.fab_btn);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pickTime();
            }
        });

        // Create Notification Channel using NotificationHelper (Pass -1 for alarmId, only creating notichannel here)
        NotificationHelper notificationHelper = new NotificationHelper(MainActivity.this, -1);
        notificationHelper.createNotificationChannel();

        // Helper method for Drag to del room item
        ItemTouchHelper helper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // This also cancels the alarm
                int adapterPos = viewHolder.getAdapterPosition();
                AlarmEntity currentEntity = mAdapter.getAlarmRecView(adapterPos);
                alarmHelper.cancelAlarm(currentEntity, true, true, -1);
                Snackbar.make(viewHolder.itemView, "Alarm Removed", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        helper.attachToRecyclerView(mRecyclerView);

        // Set default values on first launch
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
    }

    // Show dialog and set Time: Store selected time in Calendar object
    private void pickTime() {
        TimePickerDialog.OnTimeSetListener timeSetListener = new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int hourOfDay, int minute) {
                Calendar c = Calendar.getInstance();
                c.set(Calendar.HOUR_OF_DAY, hourOfDay);
                c.set(Calendar.MINUTE, minute);
                c.set(Calendar.SECOND, 0);

                mHour = hourOfDay;
                mMinute = minute;

                SimpleDateFormat sdf = new SimpleDateFormat("hh:mm aa",
                        Locale.getDefault());
                final String formatted = sdf.format(c.getTime());

                alarmHelper.createAlarm(c);
                Snackbar.make(findViewById(android.R.id.content), "Alarm Set for " + formatted, Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        };
        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                timeSetListener, mHour, mMinute, false);
        timePickerDialog.show();
    }

    // Inflate menu layout
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.settings) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}