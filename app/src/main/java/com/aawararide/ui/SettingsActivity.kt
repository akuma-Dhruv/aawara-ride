package com.aawararide.ui

import android.app.Activity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Switch
import com.aawararide.R
import com.aawararide.settings.AnnouncementSettingsRepository

class SettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        val repository = AnnouncementSettingsRepository(this)
        val initialSwitch = findViewById<Switch>(R.id.initial_announcement_switch)
        val repeatSwitch = findViewById<Switch>(R.id.repeat_announcement_switch)
        val delaySpinner = findViewById<Spinner>(R.id.repeat_delay_spinner)
        val maximumSpinner = findViewById<Spinner>(R.id.maximum_repeat_spinner)
        delaySpinner.setAdapter(ArrayAdapter.createFromResource(
            this, R.array.repeat_delays, android.R.layout.simple_spinner_dropdown_item))
        maximumSpinner.setAdapter(ArrayAdapter.createFromResource(
            this, R.array.maximum_repeats, android.R.layout.simple_spinner_dropdown_item))
        initialSwitch.isChecked = repository.isInitialEnabled()
        repeatSwitch.isChecked = repository.isRepeatEnabled()
        selectValue(delaySpinner, repository.getRepeatDelaySeconds())
        selectValue(maximumSpinner, repository.getMaximumRepeatCount())
        findViewById<Button>(R.id.save_button).setOnClickListener {
            repository.save(
                initialSwitch.isChecked, repeatSwitch.isChecked,
                delaySpinner.selectedItem.toString().toInt(),
                maximumSpinner.selectedItem.toString().toInt()
            )
            finish()
        }
    }

    private fun selectValue(spinner: Spinner, value: Int) {
        for (index in 0 until spinner.count) {
            if (spinner.getItemAtPosition(index).toString().toInt() == value) {
                spinner.setSelection(index)
                return
            }
        }
    }
}
