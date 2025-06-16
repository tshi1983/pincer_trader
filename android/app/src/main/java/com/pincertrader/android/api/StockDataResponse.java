package com.pincertrader.android.api;

import java.util.List;

public class StockDataResponse {
    public List<StockBar> data;

    public static class StockBar {
        public String time;
        public float open;
        public float high;
        public float low;
        public float close;
        public int volume;
    }
} 