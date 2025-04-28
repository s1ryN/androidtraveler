package com.example.AppProject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.content.pm.PackageManager;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    // UI elements for weather and compass
    private ViewFlipper weatherFlipper;
    private View dot1, dot2;
    private TextView tempMax, tempMin, weatherInfo, pinnedNote;
    private ImageView weatherIcon, compassNeedle;

    // Sensor and compass related variables
    private SensorManager sensorManager;
    private Sensor accelerometer, magnetometer;
    private float[] gravity, geomagnetic;
    private float currentAzimuth = 0f;
    private float smoothedAzimuth = 0f;

    // Location related variables
    private FusedLocationProviderClient fusedLocationClient;
    private GestureDetector gestureDetector;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final String API_KEY = "221056ea03f8c3d520dd31d31ba6fa7d";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Link UI components
        weatherInfo = findViewById(R.id.weatherInfo);
        weatherIcon = findViewById(R.id.weatherIcon);
        compassNeedle = findViewById(R.id.compassNeedle);
        pinnedNote = findViewById(R.id.pinnedNote);
        weatherFlipper = findViewById(R.id.weatherFlipper);
        dot1 = findViewById(R.id.dot1);
        dot2 = findViewById(R.id.dot2);
        tempMax = findViewById(R.id.tempMax);
        tempMin = findViewById(R.id.tempMin);

        // Tap on pinned note opens notes screen
        pinnedNote.setOnClickListener(v -> openActivity(NotesActivity.class));

        // Container opens notes screen
        LinearLayout notesContainer = findViewById(R.id.notesContainer);
        notesContainer.setOnClickListener(v -> openActivity(NotesActivity.class));

        // Container opens weather screen, allows swiping between views
        LinearLayout weatherContainer = findViewById(R.id.weatherContainer);
        weatherContainer.setOnClickListener(v -> openActivity(WeatherActivity.class));

        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 50;
            private static final int SWIPE_VELOCITY_THRESHOLD = 50;

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                float diffY = e2.getY() - e1.getY();
                if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY > 0) showPreviousWeatherSlide();
                    else showNextWeatherSlide();
                    return true;
                }
                return false;
            }
        });
        weatherContainer.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));

        // Sensor setup
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);

        // Location setup
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            requestLocationAndWeather();
        }

        // Clicking compass opens CompassActivity
        FrameLayout compassContainer = findViewById(R.id.compassContainer);
        compassContainer.setOnClickListener(v -> openActivity(CompassActivity.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPinnedNote();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    // Request weather from OpenWeather API using device location
    private void fetchWeatherData(double lat, double lon) {
        String url = "https://api.openweathermap.org/data/2.5/forecast?lat=" + lat + "&lon=" + lon +
                "&units=metric&appid=" + API_KEY;

        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray list = response.getJSONArray("list");
                        double minTemp = Double.MAX_VALUE;
                        double maxTemp = Double.MIN_VALUE;
                        double firstTemp = list.getJSONObject(0).getJSONObject("main").getDouble("temp");

                        String iconCode = list.getJSONObject(0).getJSONArray("weather").getJSONObject(0).getString("icon");
                        int iconResId = getIconForCode(iconCode);
                        if (iconResId != 0) weatherIcon.setImageResource(iconResId);

                        for (int i = 0; i < 8; i++) {
                            JSONObject entry = list.getJSONObject(i).getJSONObject("main");
                            double tempMinVal = entry.getDouble("temp_min");
                            double tempMaxVal = entry.getDouble("temp_max");
                            if (tempMinVal < minTemp) minTemp = tempMinVal;
                            if (tempMaxVal > maxTemp) maxTemp = tempMaxVal;
                        }

                        weatherInfo.setText(String.format("%.1f°C", firstTemp));
                        tempMax.setText("Max: " + String.format("%.1f°C", maxTemp));
                        tempMin.setText("Min: " + String.format("%.1f°C", minTemp));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, Throwable::printStackTrace);
        queue.add(request);
    }

    // Gets current location then fetches weather
    private void requestLocationAndWeather() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) fetchWeatherData(location.getLatitude(), location.getLongitude());
                })
                .addOnFailureListener(e -> Log.e("LOCATION", "Failed to get location", e));
    }

    // Maps OpenWeather icon code to drawable
    private int getIconForCode(String code) {
        switch (code) {
            case "01d":
            case "01n": return R.drawable.ic_sunny;
            case "02d":
            case "02n": return R.drawable.ic_partly_cloudy;
            case "03d":
            case "03n":
            case "04d":
            case "04n": return R.drawable.ic_cloudy;
            case "09d":
            case "09n": return R.drawable.ic_drizzle;
            case "10d":
            case "10n": return R.drawable.ic_rain;
            case "11d":
            case "11n": return R.drawable.ic_thunder;
            case "13d":
            case "13n": return R.drawable.ic_snow;
            case "50d":
            case "50n": return R.drawable.ic_fog;
            default: return 0;
        }
    }

    // Animates the compass needle to a new direction
    private void animateCompass(float from, float to) {
        RotateAnimation rotate = new RotateAnimation(-from, -to,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        rotate.setDuration(800);
        rotate.setFillAfter(true);
        rotate.setInterpolator(new LinearInterpolator());
        compassNeedle.startAnimation(rotate);
    }

    // Smooth transition between old and new azimuth values
    private float lowPassFilter(float newValue, float oldValue) {
        final float ALPHA = 0.1f;
        return oldValue + ALPHA * (newValue - oldValue);
    }

    // Load pinned note from SharedPreferences and display it
    private void loadPinnedNote() {
        SharedPreferences sharedPreferences = getSharedPreferences("NotesApp", Context.MODE_PRIVATE);
        String note = sharedPreferences.getString("pinned_note", "No Notes Pinned");
        pinnedNote.setText(note);
    }

    // Launch a new activity
    private void openActivity(Class<?> activityClass) {
        Intent intent = new Intent(this, activityClass);
        startActivity(intent);
    }

    // Show next weather slide
    private void showNextWeatherSlide() {
        if (weatherFlipper.getDisplayedChild() < 1) {
            weatherFlipper.setInAnimation(this, R.anim.slide_in_bottom);
            weatherFlipper.setOutAnimation(this, R.anim.slide_out_top);
            weatherFlipper.showNext();
            updateDots(1);
        }
    }

    // Show previous weather slide
    private void showPreviousWeatherSlide() {
        if (weatherFlipper.getDisplayedChild() > 0) {
            weatherFlipper.setInAnimation(this, R.anim.slide_in_top);
            weatherFlipper.setOutAnimation(this, R.anim.slide_out_bottom);
            weatherFlipper.showPrevious();
            updateDots(0);
        }
    }

    // Updates dot indicators for slide index
    private void updateDots(int index) {
        dot1.setBackgroundResource(index == 0 ? R.drawable.dot_selected : R.drawable.dot_unselected);
        dot2.setBackgroundResource(index == 1 ? R.drawable.dot_selected : R.drawable.dot_unselected);
    }

    // Handles changes from sensors
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) gravity = event.values;
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) geomagnetic = event.values;

        if (gravity != null && geomagnetic != null) {
            float[] R = new float[9], I = new float[9];
            if (SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {
                float[] orientation = new float[3];
                SensorManager.getOrientation(R, orientation);
                float azimuth = (float) Math.toDegrees(orientation[0]);
                azimuth = (azimuth + 360) % 360;
                smoothedAzimuth = lowPassFilter(azimuth, smoothedAzimuth);
                animateCompass(currentAzimuth, smoothedAzimuth);
                currentAzimuth = smoothedAzimuth;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}