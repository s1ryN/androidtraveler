package com.example.AppProject;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WeatherAdapter extends RecyclerView.Adapter<WeatherAdapter.ViewHolder> {
    private final List<ForecastItem> forecastList; // List of forecast items to display
    private final boolean isHourly; // Determines whether this adapter is for hourly or daily forecast

    // Constructor receives the forecast list and mode (hourly or daily)
    public WeatherAdapter(List<ForecastItem> forecastList, boolean isHourly) {
        this.forecastList = forecastList;
        this.isHourly = isHourly;
    }

    // ViewHolder class that holds the layout elements for each item
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView timeText, tempText;
        ImageView weatherIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            timeText = itemView.findViewById(R.id.timeText); // Text showing time or day
            tempText = itemView.findViewById(R.id.tempText); // Text showing temperature info
            weatherIcon = itemView.findViewById(R.id.weatherIconSmall); // Weather icon
        }
    }

    // Inflates the item layout for each forecast box
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.weather_item, parent, false);
        return new ViewHolder(view);
    }

    // Binds forecast data to UI elements in the ViewHolder
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ForecastItem item = forecastList.get(position);

        if (isHourly) {
            // Format and show time for hourly forecast
            String time = item.datetime.substring(11, 16); // Extract HH:mm
            holder.timeText.setText(time);
            holder.tempText.setText(String.format(Locale.getDefault(), "%.0f°C", item.temp));
        } else {
            // Format and show day name for daily forecast
            try {
                Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(item.datetime);
                String day = new SimpleDateFormat("EEE", Locale.getDefault()).format(date);
                holder.timeText.setText(day);
            } catch (Exception e) {
                holder.timeText.setText("N/A"); // In case of parsing error
            }

            // Display min/max temperature for the day
            String minMax = String.format(Locale.getDefault(), "%.0f / %.0f°C", item.tempMin, item.tempMax);
            holder.tempText.setText(minMax);
        }

        // Set weather icon based on code
        int iconId = getIconForCode(item.icon);
        if (iconId != 0) holder.weatherIcon.setImageResource(iconId);

        // Debug log for each item bind
        Log.d("BIND", "Time: " + holder.timeText.getText() + ", Temp: " + holder.tempText.getText());
    }

    // Returns total number of forecast items
    @Override
    public int getItemCount() {
        return forecastList.size();
    }

    // Maps OpenWeather icon code to local drawable resource
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
            default: return 0; // Return 0 if icon code is unrecognized
        }
    }
}
