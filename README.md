# PincerTrader

PincerTrader is an Android app for stock charting and analysis, featuring:

- **Interactive Chart Drawing**: Draw trendlines and annotations anywhere on the price chart
- **Multiple Technical Indicators**: EMA, MACD, RSI with customizable parameters
- **Floating Action Button**: Easy access to indicators and drawing tools via dropdown menu
- **Detailed Stock Information**: Company details, financial ratios, and market data
- **Real-time Stock Data**: Connects to Alpha Vantage API or uses fake data for testing
- **Dark Mode Support**: Chart text and legend colors auto-adjust to system theme
- **Volume Chart**: Separate volume display with M/K/B formatting
- **Company Name Search**: Search stocks by company name or symbol
- **Chart Synchronization**: Price and volume charts stay synchronized during zoom/pan

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

### Chart Drawing
- **Two-tap Line Drawing**: Tap two points anywhere on the chart to draw trendlines
- **Zoom/Pan Support**: Drawing coordinates work correctly at any zoom level or chart position
- **Color Customization**: Choose from multiple colors for your drawings
- **Clear Drawings**: Long-press the Drawing option to clear all drawn lines

### Technical Indicators
- **EMA (Exponential Moving Average)**: 
  - Multiple EMAs with custom periods and colors
  - Long-press to manage EMAs (add, edit, remove)
- **MACD (Moving Average Convergence Divergence)**:
  - Customizable fast and slow periods
  - Long-press to adjust parameters
- **RSI (Relative Strength Index)**:
  - Customizable period
  - Long-press to adjust parameters

### User Interface
- **Floating Action Button**: Tap to access indicators and drawing tools
- **Collapsible Stock Info**: Expandable company information panel
- **Auto-complete Search**: Search by company name or stock symbol
- **Interval Selection**: Multiple time intervals (1m, 5m, 15m, 30m, 1h, 1d, 1w, 1mo)
- **Detailed Markers**: Tap candles to see OHLCV data

### Data Management
- **Settings Persistence**: Indicator settings and drawings saved per stock symbol
- **Fake Data Mode**: Test with simulated data when API is unavailable
- **Error Handling**: Graceful handling of network errors and rate limits

## Technical Details

- **Frontend**: Android (Java) with MPAndroidChart library
- **Backend**: FastAPI (Python) with Alpha Vantage integration
- **Data Format**: JSON with OHLCV candlestick data
- **Network**: Retrofit for API communication
- **Storage**: SharedPreferences for local settings

## Contributing
Pull requests are welcome! Please ensure your code follows the existing style and includes appropriate tests. 