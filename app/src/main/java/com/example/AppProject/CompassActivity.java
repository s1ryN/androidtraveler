package com.example.AppProject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CompassActivity extends AppCompatActivity implements SensorEventListener {
    private ImageView compassNeedle;
    private TextView gpsText;
    private TextView locationName;
    private SensorManager sensorManager;
    private Sensor accelerometer, magnetometer;
    private float[] gravity, geomagnetic;
    private float currentAzimuth = 0f;
    private float smoothedAzimuth = 0f;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compass);

        // UI components
        compassNeedle = findViewById(R.id.compassNeedle);
        gpsText = findViewById(R.id.gpsText);
        ImageView backButton = findViewById(R.id.backButton);
        ImageView exportButton = findViewById(R.id.exportButton);
        locationName = findViewById(R.id.locationName);

        // Navigate back
        backButton.setOnClickListener(v -> finish());

        // Initialize sensors
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        // Initialize location provider
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        updateLocationText();

        // Handle exporting current location to pinned note
        exportButton.setOnClickListener(v -> {
            String gps = gpsText.getText().toString();
            String locationLabel = locationName != null ? locationName.getText().toString() : "Unknown location";

            SharedPreferences prefs = getSharedPreferences("NotesApp", Context.MODE_PRIVATE);
            String original = prefs.getString("pinned_note", "");

            if (original.trim().isEmpty() || original.equals("No Notes Pinned")) {
                Toast.makeText(this, "No pinned note to append to", Toast.LENGTH_SHORT).show();
                return;
            }

            String updated = original + "\n\n\uD83D\uDCCD " + locationLabel + " [" + gps + "]";

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("pinned_note", updated);

            Set<String> notes = prefs.getStringSet("notes", new HashSet<>());
            notes.remove(original);
            notes.add(updated);
            editor.putStringSet("notes", notes);

            editor.apply();
            Toast.makeText(this, "Location appended to pinned note", Toast.LENGTH_SHORT).show();
        });
    }

    // Gets current GPS coordinates and resolves to human-readable location name
    private void updateLocationText() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        double lat = location.getLatitude();
                        double lon = location.getLongitude();

                        String latText = String.format(Locale.getDefault(), "%.5f° %s", Math.abs(lat), lat >= 0 ? "N" : "S");
                        String lonText = String.format(Locale.getDefault(), "%.5f° %s", Math.abs(lon), lon >= 0 ? "E" : "W");

                        gpsText.setText(latText + ", " + lonText);

                        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                        try {
                            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
                            if (addresses != null && !addresses.isEmpty()) {
                                Address address = addresses.get(0);
                                String city = address.getLocality();
                                String country = address.getCountryName();

                                if (city == null) city = address.getSubAdminArea();
                                if (city == null) city = "Unknown";

                                locationName.setText(city + ", " + country);
                            } else {
                                locationName.setText("Unknown location");
                            }
                        } catch (Exception e) {
                            locationName.setText("Location error");
                        }
                    } else {
                        gpsText.setText("Location not available");
                        locationName.setText("Unknown location");
                    }
                });
    }

    // Start listening to sensors when resumed
    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
    }

    // Stop listening to sensors when paused
    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    // Called when a sensor detects a change
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
                azimuth = (azimuth + 360) % 360; // normalize to [0,360)
                smoothedAzimuth = lowPassFilter(azimuth, smoothedAzimuth);
                animateCompass(currentAzimuth, smoothedAzimuth);
                currentAzimuth = smoothedAzimuth;
            }
        }
    }

    // Animate the compass needle rotation with smoothing
    private void animateCompass(float from, float to) {
        RotateAnimation rotate = new RotateAnimation(
                -from, -to,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        rotate.setDuration(800);
        rotate.setFillAfter(true);
        rotate.setInterpolator(new LinearInterpolator());
        compassNeedle.startAnimation(rotate);
    }

    // Smooth transition between azimuth values
    private float lowPassFilter(float newValue, float oldValue) {
        final float ALPHA = 0.1f; // lower = smoother, higher = faster response
        return oldValue + ALPHA * (newValue - oldValue);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
