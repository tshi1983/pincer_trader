package com.pincertrader.android.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

import java.util.List;
import java.util.Map;

public interface StockApiService {
    @GET("/api/stock/{symbol}")
    Call<StockDataResponse> getStockData(
        @Path("symbol") String symbol,
        @Query("period") String period,
        @Query("interval") String interval
    );

    @GET("/api/stock/{symbol}/info")
    Call<StockInfoResponse> getStockInfo(
        @Path("symbol") String symbol
    );

    @GET("/api/fake/stock/{symbol}")
    Call<StockDataResponse> getFakeStockData(
        @Path("symbol") String symbol,
        @Query("interval") String interval
    );

    @GET("/api/fake/stock/{symbol}/info")
    Call<StockInfoResponse> getFakeStockInfo(
        @Path("symbol") String symbol
    );
} 