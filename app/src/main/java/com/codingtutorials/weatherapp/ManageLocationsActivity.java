package com.codingtutorials.weatherapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ManageLocationsActivity extends AppCompatActivity {
    private RecyclerView locationsRecyclerView;
    private LocationAdapter adapter;
    private ArrayList<String> savedCities;
    private AutoCompleteTextView cityNameInput;
    private ImageView searchIcon;
    private ArrayAdapter<String> suggestionAdapter;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private static final String API_KEY = "7be4a25466b8361c2ae28097a6aa5617";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_locations);

        // 1. Initialize views FIRST to avoid NullPointerException
        cityNameInput = findViewById(R.id.cityNameInput);
        searchIcon = findViewById(R.id.searchIcon);
        locationsRecyclerView = findViewById(R.id.locationsRecyclerView);

        // 2. Setup AutoComplete threshold
        cityNameInput.setThreshold(1);

        // 3. Setup Keyboard "Search" button listener
        cityNameInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                String city = cityNameInput.getText().toString().trim();
                if (!city.isEmpty()) {
                    hideKeyboard();
                    validateAndSaveCity(city);
                }
                return true;
            }
            return false;
        });

        // 4. Setup RecyclerView and Cities list
        loadCities();
        adapter = new LocationAdapter(this, savedCities);
        locationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        locationsRecyclerView.setAdapter(adapter);

        // 5. Setup Adapter Item Clicks
        adapter.setOnItemClickListener(new LocationAdapter.OnItemClickListener() {
            @Override
            public void onCitySelected(String cityName) {
                returnToMain(cityName);
            }

            @Override
            public void onCityDeleted(int position) {
                if (position > 0) {
                    savedCities.remove(position);
                    adapter.notifyItemRemoved(position);
                    saveCitiesToPrefs();
                }
            }
        });

        // 6. Setup Suggestions
        suggestionAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        cityNameInput.setAdapter(suggestionAdapter);

        cityNameInput.addTextChangedListener(new TextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() >= 2) fetchPakistanCities(s.toString());
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });

        // 7. Setup Search Icon Click
        searchIcon.setOnClickListener(v -> {
            String city = cityNameInput.getText().toString().trim();
            if (!city.isEmpty()) {
                cityNameInput.dismissDropDown();
                hideKeyboard();
                validateAndSaveCity(city);
            } else {
                Toast.makeText(this, "Enter a city name", Toast.LENGTH_SHORT).show();
            }
        });

        cityNameInput.setOnItemClickListener((parent, view, position, id) -> {
            String selected = suggestionAdapter.getItem(position);
            validateAndSaveCity(selected);
        });
    }

    private void fetchPakistanCities(String query) {
        String url = "https://api.openweathermap.org/geo/1.0/direct?q=" + query + ",PK&limit=5&appid=" + API_KEY;
        executorService.execute(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    JSONArray jsonArray = new JSONArray(response.body().string());
                    ArrayList<String> citiesFound = new ArrayList<>();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        citiesFound.add(obj.getString("name"));
                    }

                    runOnUiThread(() -> {
                        suggestionAdapter.clear();
                        suggestionAdapter.addAll(citiesFound);
                        suggestionAdapter.notifyDataSetChanged();
                        if (!citiesFound.isEmpty() && cityNameInput.hasFocus()) {
                            cityNameInput.showDropDown();
                        }
                    });
                }
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    private void validateAndSaveCity(String city) {
        final String cleanCity = city.trim();
        String url = "https://api.openweathermap.org/data/2.5/weather?q=" + cleanCity + ",PK&appid=" + API_KEY;

        executorService.execute(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();

                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        boolean exists = false;
                        for(String s : savedCities) {
                            if(s.equalsIgnoreCase(cleanCity)) exists = true;
                        }

                        if (!exists) {
                            savedCities.add(cleanCity);
                            saveCitiesToPrefs();
                            adapter.notifyDataSetChanged();
                        }
                        returnToMain(cleanCity);
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "City Not Found in Pakistan!", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Check Internet Connection", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void returnToMain(String city) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("selected_city", city);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    private void loadCities() {
        SharedPreferences prefs = getSharedPreferences("WeatherAppPrefs", MODE_PRIVATE);
        Set<String> set = prefs.getStringSet("SavedCities", new HashSet<>());
        savedCities = new ArrayList<>(set);
        if (!savedCities.contains("Current Location")) savedCities.add(0, "Current Location");
    }

    private void saveCitiesToPrefs() {
        SharedPreferences prefs = getSharedPreferences("WeatherAppPrefs", MODE_PRIVATE);
        Set<String> set = new HashSet<>(savedCities);
        set.remove("Current Location");
        prefs.edit().putStringSet("SavedCities", set).apply();
    }
}