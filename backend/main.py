from fastapi import FastAPI, HTTPException, Depends
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.orm import Session
from typing import List, Optional
import requests,os
import time
import random
from datetime import datetime, timedelta

app = FastAPI(title="Pincer Trader API")

# Configure CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # React frontend
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Simple in-memory cache: {(symbol, period, interval): (timestamp, data)}
stock_data_cache = {}
CACHE_TTL = 120  # seconds

ALPHA_VANTAGE_API_KEY = os.getenv("ALPHA_VANTAGE_API_KEY", "demo")

@app.get("/")
async def root():
    return {"message": "Welcome to Pincer Trader API"}

@app.get("/api/stock/{symbol}")
async def get_stock_data(symbol: str, period: str = "1d", interval: str = "1m"):
    try:
        print(f"[DEBUG] get_stock_data called with symbol={symbol}, period={period}, interval={interval}")
        cache_key = (symbol, period, interval)
        now = time.time()
        # Check cache
        if cache_key in stock_data_cache:
            ts, cached_data = stock_data_cache[cache_key]
            if now - ts < CACHE_TTL:
                print(f"[DEBUG] Returning cached data for {cache_key}")
                return {"data": cached_data}
            else:
                del stock_data_cache[cache_key]

        # Map interval to Alpha Vantage API
        av_interval_map = {
            "1m": "1min",
            "5m": "5min",
            "15m": "15min",
            "30m": "30min",
            "60m": "60min",
        }
        data = []
        if interval == "1d":
            url = f"https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol={symbol}&apikey={ALPHA_VANTAGE_API_KEY}&outputsize=compact"
            resp = requests.get(url)
            if resp.status_code != 200:
                raise HTTPException(status_code=400, detail="Alpha Vantage API error")
            json_data = resp.json()
            series = json_data.get("Time Series (Daily)", {})
            for date_str, values in series.items():
                try:
                    open_ = float(values["1. open"])
                    high = float(values["2. high"])
                    low = float(values["3. low"])
                    close = float(values["4. close"])
                    volume = int(values["5. volume"])
                except Exception:
                    continue  # skip this entry if any value is not a valid number
                data.append({
                    "time": date_str,
                    "open": open_,
                    "high": high,
                    "low": low,
                    "close": close,
                    "volume": volume
                })
        elif interval in av_interval_map:
            av_interval = av_interval_map[interval]
            url = f"https://www.alphavantage.co/query?function=TIME_SERIES_INTRADAY&symbol={symbol}&interval={av_interval}&apikey={ALPHA_VANTAGE_API_KEY}&outputsize=compact"
            resp = requests.get(url)
            if resp.status_code != 200:
                raise HTTPException(status_code=400, detail="Alpha Vantage API error")
            json_data = resp.json()
            key = f"Time Series ({av_interval})"
            series = json_data.get(key, {})
            for date_str, values in series.items():
                try:
                    open_ = float(values["1. open"])
                    high = float(values["2. high"])
                    low = float(values["3. low"])
                    close = float(values["4. close"])
                    volume = int(values["5. volume"])
                except Exception:
                    continue  # skip this entry if any value is not a valid number
                data.append({
                    "time": date_str,
                    "open": open_,
                    "high": high,
                    "low": low,
                    "close": close,
                    "volume": volume
                })
        else:
            raise HTTPException(status_code=400, detail="Unsupported interval for Alpha Vantage")

        # Sort data by time ascending
        data.sort(key=lambda x: x["time"])
        stock_data_cache[cache_key] = (now, data)
        return {"data": data}
    except Exception as e:
        print(f"[EXCEPTION] {e}")
        if isinstance(e, HTTPException):
            raise e
        raise HTTPException(status_code=400, detail=str(e))

@app.get("/api/stock/{symbol}/info")
async def get_stock_info(symbol: str):
    try:
        url = f"https://www.alphavantage.co/query?function=OVERVIEW&symbol={symbol}&apikey={ALPHA_VANTAGE_API_KEY}"
        resp = requests.get(url)
        if resp.status_code != 200:
            raise HTTPException(status_code=400, detail="Alpha Vantage API error")
        info = resp.json()
        if "Note" in info or "Information" in info:
            raise HTTPException(status_code=429, detail="Alpha Vantage rate limit reached. Please wait and try again.")
        if not info or "Symbol" not in info:
            raise HTTPException(status_code=404, detail="No info found for symbol")
        return {
            "symbol": info.get("Symbol", symbol),
            "name": info.get("Name", "-"),
            "sector": info.get("Sector", "-"),
            "currentPrice": info.get("MarketCapitalization", "-"),  # Alpha Vantage does not provide real-time price in OVERVIEW
            "marketCap": info.get("MarketCapitalization", "-"),
            "peRatio": info.get("PERatio", "-"),
            "pbRatio": info.get("PriceToBookRatio", "-")
        }
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@app.get("/api/fake/stock/{symbol}")
async def get_fake_stock_data(symbol: str, interval: str = "1d"):
    # Generate 30 days of fake daily bars with Elliott Wave pattern (1-5, A-B-C)
    data = []
    base_price = 100
    base_volume = 5_000_000
    now = datetime.now()
    days = 30
    wave_points = [
        0,   # start
        5,   # wave 1 peak
        8,   # wave 2 trough
        15,  # wave 3 peak
        18,  # wave 4 trough
        22,  # wave 5 peak
        25,  # wave A trough
        27,  # wave B peak
        29   # wave C trough
    ]
    wave_prices = [
        base_price,
        base_price + 20,   # 1
        base_price + 10,   # 2
        base_price + 35,   # 3
        base_price + 22,   # 4
        base_price + 40,   # 5
        base_price + 15,   # A
        base_price + 25,   # B
        base_price + 5     # C
    ]
    for i in range(days):
        # Find which segment we're in
        for j in range(len(wave_points) - 1):
            if wave_points[j] <= i <= wave_points[j+1]:
                t = (i - wave_points[j]) / (wave_points[j+1] - wave_points[j]) if wave_points[j+1] != wave_points[j] else 0
                price = wave_prices[j] + t * (wave_prices[j+1] - wave_prices[j])
                break
        else:
            price = wave_prices[-1]
        open_ = price + random.uniform(-1, 1)
        close = price + random.uniform(-1, 1)
        high = max(open_, close) + random.uniform(0, 1)
        low = min(open_, close) - random.uniform(0, 1)
        volume = base_volume + random.randint(-500_000, 500_000)
        date = now - timedelta(days=days - 1 - i)
        data.append({
            "time": date.strftime("%Y-%m-%d"),
            "open": round(open_, 2),
            "high": round(high, 2),
            "low": round(low, 2),
            "close": round(close, 2),
            "volume": volume
        })
    return {"data": data}

@app.get("/api/fake/stock/{symbol}/info")
async def get_fake_stock_info(symbol: str):
    return {
        "symbol": symbol,
        "name": f"Fake {symbol} Inc.",
        "sector": "Technology",
        "currentPrice": 123.45,
        "marketCap": 1234567890,
        "peRatio": 15.67,
        "pbRatio": 2.34
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)