package com.codingtutorials.weatherapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Switch;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Switch unitSwitch = findViewById(R.id.unitSwitch);

        // Using a consistent name for the preference file
        SharedPreferences prefs = getSharedPreferences("WeatherAppPrefs", MODE_PRIVATE);

        // Load the saved state. Default is 'false' (Celsius).
        // If the switch is ON, it means Fahrenheit.
        unitSwitch.setChecked(prefs.getBoolean("isFahrenheit", false));

        unitSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save the new state immediately
            prefs.edit().putBoolean("isFahrenheit", isChecked).apply();
        });
    }
}