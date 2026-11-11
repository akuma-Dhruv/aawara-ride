package com.aawararide.ui;

import android.os.Bundle;
import android.app.Activity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Switch;

import com.aawararide.R;
import com.aawararide.settings.AnnouncementSettingsRepository;

public final class SettingsActivity extends Activity {
    private AnnouncementSettingsRepository repository;
    private Switch initialSwitch;
    private Switch repeatSwitch;
    private Spinner delaySpinner;
    private Spinner maximumSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        repository = new AnnouncementSettingsRepository(this);
        initialSwitch = findViewById(R.id.initial_announcement_switch);
        repeatSwitch = findViewById(R.id.repeat_announcement_switch);
        delaySpinner = findViewById(R.id.repeat_delay_spinner);
        maximumSpinner = findViewById(R.id.maximum_repeat_spinner);

        delaySpinner.setAdapter(ArrayAdapter.createFromResource(this, R.array.repeat_delays,
                android.R.layout.simple_spinner_dropdown_item));
        maximumSpinner.setAdapter(ArrayAdapter.createFromResource(this, R.array.maximum_repeats,
                android.R.layout.simple_spinner_dropdown_item));
        initialSwitch.setChecked(repository.isInitialEnabled());
        repeatSwitch.setChecked(repository.isRepeatEnabled());
        selectValue(delaySpinner, repository.getRepeatDelaySeconds());
        selectValue(maximumSpinner, repository.getMaximumRepeatCount());

        Button save = findViewById(R.id.save_button);
        save.setOnClickListener(v -> {
            repository.save(
                    initialSwitch.isChecked(),
                    repeatSwitch.isChecked(),
                    Integer.parseInt(delaySpinner.getSelectedItem().toString()),
                    Integer.parseInt(maximumSpinner.getSelectedItem().toString()));
            finish();
        });
    }

    private static void selectValue(Spinner spinner, int value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (Integer.parseInt(spinner.getItemAtPosition(i).toString()) == value) {
                spinner.setSelection(i);
                return;
            }
        }
    }
}
