# PincerTrader

PincerTrader is an Android app for stock charting and analysis, featuring:

- Multiple EMA indicators (add, edit, remove, custom color/period)
- Detailed marker popup on price chart (shows date, open, high, low, close, volume)
- Dark mode support (chart text and legend colors auto-adjust)
- Volume chart with M/K/B formatting
- User drawings and indicator color customization
- Connects to a FastAPI backend for real or fake stock data

## Running the Backend

1. Install dependencies:
   ```
   cd backend
   pip install -r requirements.txt
   ```
2. Start the backend (accessible on your local network):
   ```
   uvicorn main:app --host 0.0.0.0 --port 8000
   ```
3. (Optional) Update CORS in `main.py` for your device/network.

## Running the App on Your Phone

1. Enable Developer Options and USB Debugging on your phone.
2. Connect your phone to your computer via USB.
3. In Android Studio, select your device and run the app.
4. Update the API base URL in the app to point to your computer's IP (e.g., `http://192.168.1.100:8000`).
5. Allow cleartext traffic by adding a network security config (see code for details).

## Features

- **Multiple EMAs:** Long-press the EMA icon to manage EMAs (add, edit, remove, color/period).
- **Detailed Marker:** Tap on a candle to see all OHLCV data for that entry.
- **Dark Mode:** Chart text and legend colors adapt to system theme.
- **Volume Formatting:** Volume axis uses K/M/B for readability.
- **User Drawings:** Draw trendlines and customize their color.

## Contributing
Pull requests are welcome! 