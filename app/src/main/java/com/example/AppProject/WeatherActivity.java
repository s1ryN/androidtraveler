package com.example.AppProject;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.*;

public class WeatherActivity extends AppCompatActivity {
    private static final int LOCATION_REQUEST_CODE = 1001;
    private static final String API_KEY = "221056ea03f8c3d520dd31d31ba6fa7d";

    private FusedLocationProviderClient fusedLocationClient;
    private TextView currentTemp, feelsLikeText, humidityText, pressureText, windText, visibilityText, locationName;
    private ImageView currentIcon, backButton, exportButton, searchButton;
    private RecyclerView hourlyRecycler, dailyRecycler;
    private final List<ForecastItem> hourlyList = new ArrayList<>();
    private final List<ForecastItem> dailyList = new ArrayList<>();
    private WeatherAdapter hourlyAdapter, dailyAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        // Bind all views from layout
        currentTemp = findViewById(R.id.currentTemp);
        feelsLikeText = findViewById(R.id.feelsLikeText);
        humidityText = findViewById(R.id.humidityText);
        pressureText = findViewById(R.id.pressureText);
        windText = findViewById(R.id.windText);
        visibilityText = findViewById(R.id.visibilityText);
        currentIcon = findViewById(R.id.currentIcon);
        backButton = findViewById(R.id.backButton);
        exportButton = findViewById(R.id.exportButton);
        searchButton = findViewById(R.id.searchButton);
        locationName = findViewById(R.id.locationName);
        hourlyRecycler = findViewById(R.id.hourlyRecycler);
        dailyRecycler = findViewById(R.id.dailyRecycler);

        // Setup RecyclerViews with horizontal layout
        hourlyRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        hourlyAdapter = new WeatherAdapter(hourlyList, true);
        hourlyRecycler.setAdapter(hourlyAdapter);

        dailyRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        dailyAdapter = new WeatherAdapter(dailyList, false);
        dailyRecycler.setAdapter(dailyAdapter);

        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Button actions
        backButton.setOnClickListener(v -> finish());
        exportButton.setOnClickListener(v -> showExportDialog());
        searchButton.setOnClickListener(v -> showSearchDialog());

        // Check permission and start weather request
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        } else {
            requestWeather();
        }
    }

    private void requestWeather() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                fetchWeather(location.getLatitude(), location.getLongitude());
            } else {
                Toast.makeText(this, "Location unavailable", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to get location", Toast.LENGTH_SHORT).show();
            Log.e("LOCATION", "Error: ", e);
        });
    }

    private void fetchWeather(double lat, double lon) {
        String url = "https://api.openweathermap.org/data/2.5/forecast?lat=" + lat + "&lon=" + lon + "&units=metric&appid=" + API_KEY;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, response -> {
            try {
                JSONArray list = response.getJSONArray("list");
                String city = response.getJSONObject("city").getString("name");
                String country = response.getJSONObject("city").getString("country");
                locationName.setText(city + ", " + country);

                hourlyList.clear();
                dailyList.clear();
                Map<String, ForecastItem> dailyMap = new LinkedHashMap<>();

                for (int i = 0; i < list.length(); i++) {
                    JSONObject obj = list.getJSONObject(i);
                    String dt = obj.getString("dt_txt");
                    JSONObject main = obj.getJSONObject("main");
                    JSONArray weatherArray = obj.getJSONArray("weather");
                    JSONObject weather = weatherArray.getJSONObject(0);
                    JSONObject wind = obj.getJSONObject("wind");

                    double temp = main.getDouble("temp");
                    double feels = main.getDouble("feels_like");
                    double tempMin = main.getDouble("temp_min");
                    double tempMax = main.getDouble("temp_max");
                    int humidity = main.getInt("humidity");
                    int pressure = main.getInt("pressure");
                    int visibility = obj.optInt("visibility", 10000);
                    double windSpeed = wind.getDouble("speed");
                    String icon = weather.getString("icon");
                    String desc = weather.getString("description");

                    ForecastItem item = new ForecastItem(dt, temp, tempMin, tempMax, feels, humidity, pressure, windSpeed, visibility, desc, icon);

                    if (i == 0) updateMainDisplay(item);
                    if (i < 8) hourlyList.add(item);
                    if (dt.contains("12:00:00")) {
                        String day = dt.substring(0, 10);
                        dailyMap.put(day, item);
                    }
                }

                dailyList.addAll(dailyMap.values());
                setupAdapters();

            } catch (Exception e) {
                Log.e("WEATHER_PARSE", e.getMessage());
            }
        }, error -> Log.e("WEATHER_FETCH", error.toString()));

        Volley.newRequestQueue(this).add(request);
    }

    private void updateMainDisplay(ForecastItem item) {
        currentTemp.setText(String.format(Locale.getDefault(), "%.1f°C", item.temp));
        feelsLikeText.setText(String.format(Locale.getDefault(), "Feels like: %.1f°C", item.feelsLike));
        humidityText.setText("Humidity: " + item.humidity + "%");
        pressureText.setText("Pressure: " + item.pressure + " hPa");
        windText.setText(String.format(Locale.getDefault(), "Wind: %.1f km/h", item.windSpeed));
        visibilityText.setText("Visibility: " + (item.visibility / 1000) + " km");

        int iconId = getIconForCode(item.icon);
        if (iconId != 0) currentIcon.setImageResource(iconId);
    }

    private void setupAdapters() {
        hourlyAdapter.notifyDataSetChanged();
        dailyAdapter.notifyDataSetChanged();
    }

    private int getIconForCode(String code) {
        switch (code) {
            case "01d": case "01n": return R.drawable.ic_sunny;
            case "02d": case "02n": return R.drawable.ic_partly_cloudy;
            case "03d": case "03n": case "04d": case "04n": return R.drawable.ic_cloudy;
            case "09d": case "09n": return R.drawable.ic_drizzle;
            case "10d": case "10n": return R.drawable.ic_rain;
            case "11d": case "11n": return R.drawable.ic_thunder;
            case "13d": case "13n": return R.drawable.ic_snow;
            case "50d": case "50n": return R.drawable.ic_fog;
            default: return 0;
        }
    }

    private void showExportDialog() {
        String[] days = {"1", "2", "3", "4", "5"};
        new AlertDialog.Builder(this)
                .setTitle("Export forecast for how many days?")
                .setItems(days, (dialog, which) -> exportForecast(which + 1))
                .show();
    }

    private void exportForecast(int days) {
        SharedPreferences prefs = getSharedPreferences("NotesApp", Context.MODE_PRIVATE);
        String original = prefs.getString("pinned_note", "");
        if (original.trim().isEmpty() || original.equals("No Notes Pinned")) {
            Toast.makeText(this, "No pinned note to export to", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder export = new StringBuilder(original);
        export.append("\n\n🌦️ Forecast for ").append(locationName.getText().toString()).append("\n");

        SimpleDateFormat sdf = new SimpleDateFormat("EEE dd.MM", Locale.getDefault());
        for (int i = 0; i < Math.min(days, dailyList.size()); i++) {
            ForecastItem item = dailyList.get(i);
            Date date = parseDate(item.datetime);
            String line = sdf.format(date) + ": " +
                    String.format(Locale.getDefault(), "%.1f°C / %.1f°C", item.tempMin, item.tempMax) +
                    ", " + item.description;
            export.append(line).append("\n");
        }

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("pinned_note", export.toString().trim());
        Set<String> notes = prefs.getStringSet("notes", new HashSet<>());
        notes.remove(original);
        notes.add(export.toString().trim());
        editor.putStringSet("notes", notes);
        editor.apply();

        Toast.makeText(this, "Forecast exported", Toast.LENGTH_SHORT).show();
    }

    private Date parseDate(String dtString) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(dtString);
        } catch (Exception e) {
            return new Date();
        }
    }

    private void showSearchDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter location name or coordinates (lat,lon)");

        EditText editText = new EditText(this);
        editText.setHint("e.g. London or 50.08,14.43");
        builder.setView(editText);

        builder.setPositiveButton("Search", (dialog, which) -> {
            String input = editText.getText().toString().trim();
            if (input.contains(",")) {
                String[] parts = input.split(",");
                if (parts.length == 2) {
                    try {
                        double lat = Double.parseDouble(parts[0]);
                        double lon = Double.parseDouble(parts[1]);
                        fetchWeather(lat, lon);
                    } catch (Exception e) {
                        Toast.makeText(this, "Invalid coordinates", Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                fetchWeatherByCity(input);
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void fetchWeatherByCity(String city) {
        String url = "https://api.openweathermap.org/data/2.5/forecast?q=" + city +
                "&units=metric&appid=" + API_KEY;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONObject cityObj = response.getJSONObject("city");
                        double lat = cityObj.getJSONObject("coord").getDouble("lat");
                        double lon = cityObj.getJSONObject("coord").getDouble("lon");
                        fetchWeather(lat, lon);
                    } catch (Exception e) {
                        Toast.makeText(this, "Error processing location", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "City not found", Toast.LENGTH_SHORT).show());

        Volley.newRequestQueue(this).add(request);
    }
}