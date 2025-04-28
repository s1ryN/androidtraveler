package com.example.AppProject;

public class ForecastItem {
    // Date and time of the forecast
    public String datetime;

    // Current temperature
    public double temp;

    // Min temperature
    public double tempMin;

    // Max temperature
    public double tempMax;

    // Temperature
    public double feelsLike;

    // Humidity percentage
    public int humidity;

    // Atmospheric pressure
    public int pressure;

    // Visibility
    public int visibility;

    // Wind speed
    public double windSpeed;

    // Short description of the weather
    public String description;

    // Weather icon code
    public String icon;

    //Initialize all forecast values
    public ForecastItem(String datetime, double temp, double tempMin, double tempMax, double feelsLike,
                        int humidity, int pressure, double windSpeed, int visibility,
                        String description, String icon) {
        this.datetime = datetime;
        this.temp = temp;
        this.tempMin = tempMin;
        this.tempMax = tempMax;
        this.feelsLike = feelsLike;
        this.humidity = humidity;
        this.pressure = pressure;
        this.windSpeed = windSpeed;
        this.visibility = visibility;
        this.description = description;
        this.icon = icon;
    }
}
