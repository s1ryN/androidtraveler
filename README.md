# Traveler

**Traveler** is a sleek and intuitive Android app designed for travelers who want real-time weather updates, navigation via compass, and a simple notes system to pin important information — all in one place.

---

## Features

### Compass
- Smooth compass needle animation using gyroscope and magnetic field sensors.
- Displays your current GPS coordinates.
- Shows city and country using reverse geocoding.
- One-tap export of current location to your pinned notes.

### Weather Forecast
- Real-time weather data from OpenWeatherMap.
- Displays current temperature, feels-like, humidity, pressure, wind speed, and visibility.
- Hourly forecast (next 24 hours).
- Daily forecast (up to 5 days).
- Allows searching by city name or GPS coordinates.
- Export forecast to your notes in a structured format.

### Notes System
- Create, edit, and delete notes.
- Long-press to delete with confirmation dialog.
- Pin one note at a time to be visible across activities.
- Export weather and location info into the pinned note.

---

## Setup

1. Clone the repo
2. Add your OpenWeatherMap API key in the `WeatherActivity` and `MainActivity`:
   ```java
   private static final String API_KEY = "your_api_key_here";
