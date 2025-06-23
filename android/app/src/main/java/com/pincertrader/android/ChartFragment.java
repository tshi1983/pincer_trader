package com.pincertrader.android;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView;
import android.widget.ToggleButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.app.AlertDialog;
import android.util.Log;
import android.widget.ImageButton;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import android.content.res.Configuration;
import androidx.cardview.widget.CardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.CandleStickChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.CandleData;
import com.github.mikephil.charting.data.CandleDataSet;
import com.github.mikephil.charting.data.CandleEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineDataSet;
import com.pincertrader.android.api.ApiClient;
import com.pincertrader.android.api.StockApiService;
import com.pincertrader.android.api.StockDataResponse;
import com.pincertrader.android.api.StockInfoResponse;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.Legend.LegendForm;
import com.pincertrader.android.CandleMarkerView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChartFragment extends Fragment {
    private CombinedChart candleChart;
    private BarChart volumeChart;
    private EditText searchEditText;
    private Spinner intervalSpinner;
    private ProgressBar progressBar;
    private TextView companyNameTextView, priceTextView, sectorTextView, marketCapTextView, peRatioTextView, pbRatioTextView;
    private boolean drawMode = false;
    private List<float[]> drawnLines = new ArrayList<>(); // Each float[] is {x1, y1, x2, y2}
    private float[] tempLine = new float[4];
    private int tempLinePoint = 0;

    // Add color variables
    private int emaColor = Color.BLUE;
    private int macdColor = Color.RED;
    private int rsiColor = Color.GREEN;
    private int drawingColor = Color.BLUE;
    private List<Integer> drawingColors = new ArrayList<>();

    private String currentSymbol = null;
    private String currentPeriod = "1d";
    private String currentInterval = null;
    private List<StockDataResponse.StockBar> lastBars = null;
    private CardView infoHeaderLayout, infoDetailsLayout;
    private ImageView infoExpandIcon;
    private boolean infoExpanded = true;

    // FAB and popup controls
    private FloatingActionButton indicatorFab;
    private CardView indicatorPopup;
    private TextView indicatorEma, indicatorMacd, indicatorRsi, indicatorDrawing;
    private boolean popupVisible = false;

    // Indicator states
    private boolean emaEnabled = false, macdEnabled = false, rsiEnabled = false;
    private int emaPeriod = 10;
    private int macdFast = 12, macdSlow = 26;
    private int rsiPeriod = 14;

    private String lastInterval = "1d";

    private ImageButton searchButton;

    // Replace single EMA config with a list for multiple EMAs
    public static class EMAConfig {
        public int period;
        public int color;
        public EMAConfig(int period, int color) {
            this.period = period;
            this.color = color;
        }
    }
    private List<EMAConfig> emaConfigs = new ArrayList<>();

    // Dialog for managing multiple EMAs
    private AlertDialog emaDialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chart, container, false);

        candleChart = (CombinedChart) view.findViewById(R.id.candleChart);
        volumeChart = view.findViewById(R.id.volumeChart);
        searchEditText = view.findViewById(R.id.searchEditText);
        searchButton = view.findViewById(R.id.searchButton);
        intervalSpinner = view.findViewById(R.id.intervalSpinner);
        progressBar = view.findViewById(R.id.progressBar);
        companyNameTextView = view.findViewById(R.id.companyNameTextView);
        priceTextView = view.findViewById(R.id.priceTextView);
        sectorTextView = view.findViewById(R.id.sectorTextView);
        marketCapTextView = view.findViewById(R.id.marketCapTextView);
        peRatioTextView = view.findViewById(R.id.peRatioTextView);
        pbRatioTextView = view.findViewById(R.id.pbRatioTextView);

        infoHeaderLayout = view.findViewById(R.id.infoHeaderLayout);
        infoDetailsLayout = view.findViewById(R.id.infoDetailsLayout);
        infoExpandIcon = view.findViewById(R.id.infoExpandIcon);
        
        // Initialize FAB and popup
        indicatorFab = view.findViewById(R.id.indicatorFab);
        indicatorPopup = view.findViewById(R.id.indicatorPopup);
        indicatorEma = view.findViewById(R.id.indicatorEma);
        indicatorMacd = view.findViewById(R.id.indicatorMacd);
        indicatorRsi = view.findViewById(R.id.indicatorRsi);
        indicatorDrawing = view.findViewById(R.id.indicatorDrawing);

        infoHeaderLayout.setOnClickListener(v -> {
            infoExpanded = !infoExpanded;
            infoDetailsLayout.setVisibility(infoExpanded ? View.VISIBLE : View.GONE);
            infoExpandIcon.setImageResource(infoExpanded ? android.R.drawable.arrow_down_float : android.R.drawable.arrow_up_float);
        });
        infoDetailsLayout.setVisibility(View.VISIBLE);
        infoExpandIcon.setImageResource(android.R.drawable.arrow_down_float);

        setupCandleChart();
        setupVolumeChart();
        setupButtons();
        setupIntervalSpinner();
        setupChartSync();
        setupFabAndPopup();

        // Show prompt until user searches
        companyNameTextView.setText("");
        priceTextView.setText("");
        sectorTextView.setText("");
        marketCapTextView.setText("");
        peRatioTextView.setText("");
        pbRatioTextView.setText("");

        return view;
    }

    private void setupCandleChart() {
        candleChart.getDescription().setEnabled(false);
        candleChart.setTouchEnabled(true);
        candleChart.setDragEnabled(true);
        candleChart.setScaleEnabled(true);
        candleChart.setPinchZoom(true);
        candleChart.setDrawGridBackground(false);

        XAxis xAxis = candleChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(45f);

        YAxis leftAxis = candleChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.LTGRAY);
        leftAxis.setGridLineWidth(0.5f);
        leftAxis.setDrawLabels(true);

        YAxis rightAxis = candleChart.getAxisRight();
        rightAxis.setEnabled(false);

        candleChart.getLegend().setEnabled(true);
        candleChart.setDrawOrder(new CombinedChart.DrawOrder[]{
            CombinedChart.DrawOrder.CANDLE, CombinedChart.DrawOrder.LINE
        });

        // Set text color for dark/light theme
        boolean isDarkTheme = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        int textColor = isDarkTheme ? Color.WHITE : Color.BLACK;
        xAxis.setTextColor(textColor);
        leftAxis.setTextColor(textColor);
        candleChart.getLegend().setTextColor(textColor);
    }

    private void setupVolumeChart() {
        volumeChart.getDescription().setEnabled(false);
        volumeChart.setTouchEnabled(false);
        volumeChart.setDragEnabled(false);
        volumeChart.setScaleEnabled(false);
        volumeChart.setPinchZoom(false);
        volumeChart.setDrawGridBackground(false);

        XAxis xAxis = volumeChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(45f);

        YAxis leftAxis = volumeChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.LTGRAY);
        leftAxis.setGridLineWidth(0.5f);
        leftAxis.setDrawLabels(true);
        leftAxis.setAxisMinimum(0f);
        // Format volume numbers with M, K, or B
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value >= 1_000_000_000) {
                    return String.format("%.2fB", value / 1_000_000_000f);
                } else if (value >= 1_000_000) {
                    return String.format("%.2fM", value / 1_000_000f);
                } else if (value >= 1_000) {
                    return String.format("%.2fK", value / 1_000f);
                } else {
                    return String.valueOf((int)value);
                }
            }
        });
        leftAxis.setSpaceBottom(15f); // Add extra space at the bottom
        volumeChart.setExtraBottomOffset(15f); // Add extra bottom offset to the chart

        YAxis rightAxis = volumeChart.getAxisRight();
        rightAxis.setEnabled(false);

        volumeChart.getLegend().setEnabled(true);

        // Set text color for dark/light theme
        boolean isDarkTheme = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        int textColor = isDarkTheme ? Color.WHITE : Color.BLACK;
        xAxis.setTextColor(textColor);
        leftAxis.setTextColor(textColor);
        volumeChart.getLegend().setTextColor(textColor);
    }

    private void setupButtons() {
        searchButton.setOnClickListener(v -> {
            String symbol = searchEditText.getText().toString().trim();
            String interval = intervalSpinner.getSelectedItem() != null ? intervalSpinner.getSelectedItem().toString() : null;
            if (!TextUtils.isEmpty(symbol) && !TextUtils.isEmpty(interval) && !TextUtils.isEmpty(currentPeriod)) {
                currentSymbol = symbol.toUpperCase();
                currentInterval = interval;
                fetchStockInfo(currentSymbol);
                fetchStockData(currentSymbol, currentPeriod, currentInterval);
                // Hide the soft keyboard
                InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
            } else {
                Toast.makeText(getContext(), "Please enter a stock symbol, select an interval, and a period.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupIntervalSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.intervals_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        intervalSpinner.setAdapter(adapter);
        intervalSpinner.setSelection(0);

        intervalSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String interval = parent.getItemAtPosition(position).toString();
                if (currentSymbol != null) {
                    fetchStockData(currentSymbol, currentPeriod, interval);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupChartSync() {
        candleChart.setOnChartGestureListener(new com.github.mikephil.charting.listener.OnChartGestureListener() {
            @Override
            public void onChartGestureStart(android.view.MotionEvent me, com.github.mikephil.charting.listener.ChartTouchListener.ChartGesture lastPerformedGesture) {
            }

            @Override
            public void onChartGestureEnd(android.view.MotionEvent me, com.github.mikephil.charting.listener.ChartTouchListener.ChartGesture lastPerformedGesture) {
            }

            @Override
            public void onChartLongPressed(android.view.MotionEvent me) {
                // Show color picker dialog when long pressing on chart
                showColorPickerDialog();
            }

            @Override
            public void onChartDoubleTapped(android.view.MotionEvent me) {
            }

            @Override
            public void onChartSingleTapped(android.view.MotionEvent me) {
                if (drawMode) {
                    try {
                        // Get the transformer for the left Y axis to convert coordinates properly
                        com.github.mikephil.charting.utils.Transformer transformer = candleChart.getTransformer(YAxis.AxisDependency.LEFT);
                        
                        // Convert screen coordinates to chart coordinates using the transformer
                        float[] pts = new float[2];
                        pts[0] = me.getX();
                        pts[1] = me.getY();
                        transformer.pixelsToValue(pts);
                        
                        float x = pts[0];
                        float y = pts[1];
                        
                        // Clamp coordinates to chart bounds
                        x = Math.max(candleChart.getXChartMin(), Math.min(candleChart.getXChartMax(), x));
                        y = Math.max(candleChart.getYChartMin(), Math.min(candleChart.getYChartMax(), y));
                        
                        if (tempLinePoint == 0) {
                            // First tap - store first point
                            tempLine[0] = x;
                            tempLine[1] = y;
                            tempLinePoint = 1;
                            Toast.makeText(getContext(), "First point set. Tap again for second point.", Toast.LENGTH_SHORT).show();
                        } else {
                            // Second tap - complete the line
                            tempLine[2] = x;
                            tempLine[3] = y;
                            drawnLines.add(new float[]{tempLine[0], tempLine[1], tempLine[2], tempLine[3]});
                            drawingColors.add(drawingColor); // Store color for this drawing
                            tempLinePoint = 0;
                            tempLine[0] = tempLine[1] = tempLine[2] = tempLine[3] = 0;
                            updateCharts(lastBars);
                            Toast.makeText(getContext(), "Line drawn!", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e("ChartFragment", "Error handling chart tap: " + e.getMessage());
                        Toast.makeText(getContext(), "Error drawing line. Please try again.", Toast.LENGTH_SHORT).show();
                        tempLinePoint = 0;
                        tempLine[0] = tempLine[1] = tempLine[2] = tempLine[3] = 0;
                    }
                }
            }

            @Override
            public void onChartFling(android.view.MotionEvent me1, android.view.MotionEvent me2, float velocityX, float velocityY) {
            }

            @Override
            public void onChartScale(android.view.MotionEvent me, float scaleX, float scaleY) {
                syncCharts();
            }

            @Override
            public void onChartTranslate(android.view.MotionEvent me, float dX, float dY) {
                syncCharts();
            }
        });
        volumeChart.setOnChartGestureListener(new com.github.mikephil.charting.listener.OnChartGestureListener() {
            @Override
            public void onChartGestureStart(android.view.MotionEvent me, com.github.mikephil.charting.listener.ChartTouchListener.ChartGesture lastPerformedGesture) {
            }

            @Override
            public void onChartGestureEnd(android.view.MotionEvent me, com.github.mikephil.charting.listener.ChartTouchListener.ChartGesture lastPerformedGesture) {
            }

            @Override
            public void onChartLongPressed(android.view.MotionEvent me) {
            }

            @Override
            public void onChartDoubleTapped(android.view.MotionEvent me) {
            }

            @Override
            public void onChartSingleTapped(android.view.MotionEvent me) {
            }

            @Override
            public void onChartFling(android.view.MotionEvent me1, android.view.MotionEvent me2, float velocityX, float velocityY) {
            }

            @Override
            public void onChartScale(android.view.MotionEvent me, float scaleX, float scaleY) {
                syncCharts();
            }

            @Override
            public void onChartTranslate(android.view.MotionEvent me, float dX, float dY) {
                syncCharts();
            }
        });
    }

    private void syncCharts() {
        float lowestVisibleX = candleChart.getLowestVisibleX();
        float highestVisibleX = candleChart.getHighestVisibleX();
        volumeChart.setVisibleXRange(candleChart.getVisibleXRange(), candleChart.getVisibleXRange());
        volumeChart.moveViewToX(lowestVisibleX);
    }

    private void fetchStockData(String symbol, String period, String interval) {
        lastInterval = interval;
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        StockApiService api = ApiClient.getClient().create(StockApiService.class);
        boolean useFake = SettingsFragment.isFakeDataEnabled(getContext());
        if (useFake) {
            api.getFakeStockData(symbol, interval).enqueue(new Callback<StockDataResponse>() {
                @Override
                public void onResponse(Call<StockDataResponse> call, Response<StockDataResponse> response) {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (response.isSuccessful() && response.body() != null && response.body().data != null && !response.body().data.isEmpty()) {
                        updateCharts(response.body().data);
                    } else {
                        Toast.makeText(getContext(), "No data found for this symbol/period.", Toast.LENGTH_SHORT).show();
                        candleChart.clear();
                        volumeChart.clear();
                    }
                }

                @Override
                public void onFailure(Call<StockDataResponse> call, Throwable t) {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    candleChart.clear();
                    volumeChart.clear();
                }
            });
        } else {
            api.getStockData(symbol, period, interval).enqueue(new Callback<StockDataResponse>() {
                @Override
                public void onResponse(Call<StockDataResponse> call, Response<StockDataResponse> response) {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (response.isSuccessful() && response.body() != null && response.body().data != null && !response.body().data.isEmpty()) {
                        updateCharts(response.body().data);
                    } else {
                        String errorMsg = "No data found for this symbol/period.";
                        if (response.errorBody() != null) {
                            try {
                                String errorBody = response.errorBody().string();
                                if (errorBody.contains("Too Many Requests") || errorBody.contains("rate limit")) {
                                    errorMsg = "Rate limit is reached. Please wait a few minutes and try again.";
                                }
                            } catch (Exception ignored) {
                            }
                        }
                        Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
                        candleChart.clear();
                        volumeChart.clear();
                    }
                }

                @Override
                public void onFailure(Call<StockDataResponse> call, Throwable t) {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    candleChart.clear();
                    volumeChart.clear();
                }
            });
        }
    }

    private String formatMarketCap(long marketCap) {
        if (marketCap >= 1_000_000_000) {
            return String.format("%.2fB", marketCap / 1_000_000_000.0);
        } else if (marketCap >= 1_000_000) {
            return String.format("%.2fM", marketCap / 1_000_000.0);
        } else if (marketCap >= 1_000) {
            return String.format("%.2fK", marketCap / 1_000.0);
        } else {
            return String.valueOf(marketCap);
        }
    }

    private void fetchStockInfo(String symbol) {
        StockApiService api = ApiClient.getClient().create(StockApiService.class);
        boolean useFake = SettingsFragment.isFakeDataEnabled(getContext());
        Call<StockInfoResponse> call = useFake ? api.getFakeStockInfo(symbol) : api.getStockInfo(symbol);
        call.enqueue(new Callback<StockInfoResponse>() {
            @Override
            public void onResponse(Call<StockInfoResponse> call, Response<StockInfoResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    StockInfoResponse info = response.body();
                    companyNameTextView.setText(info.name + " (" + info.symbol + ")");
                    sectorTextView.setText(info.sector);
                    priceTextView.setText(formatPrice(info.currentPrice));
                    marketCapTextView.setText(formatMarketCap(info.marketCap));
                    peRatioTextView.setText(info.peRatio != 0 ? String.format("%.2f", info.peRatio) : "--");
                    pbRatioTextView.setText(info.pbRatio != 0 ? String.format("%.2f", info.pbRatio) : "--");
                } else {
                    companyNameTextView.setText("--");
                    sectorTextView.setText("--");
                    priceTextView.setText("--");
                    marketCapTextView.setText("--");
                    peRatioTextView.setText("--");
                    pbRatioTextView.setText("--");
                }
            }
            @Override
            public void onFailure(Call<StockInfoResponse> call, Throwable t) {
                companyNameTextView.setText("--");
                sectorTextView.setText("--");
                priceTextView.setText("--");
                marketCapTextView.setText("--");
                peRatioTextView.setText("--");
                pbRatioTextView.setText("--");
            }
        });
    }

    private String formatPrice(Object priceObj) {
        try {
            double price = Double.parseDouble(priceObj.toString());
            return String.format("$%.2f", price);
        } catch (Exception e) {
            return String.valueOf(priceObj);
        }
    }

    private void updateCharts(List<StockDataResponse.StockBar> bars) {
        lastBars = bars;
        if (bars == null || bars.isEmpty()) {
            Toast.makeText(getContext(), "No data found for this symbol/period.", Toast.LENGTH_SHORT).show();
            candleChart.clear();
            volumeChart.clear();
            return;
        }
        List<CandleEntry> candleEntries = new ArrayList<>();
        List<BarEntry> volumeEntries = new ArrayList<>();
        List<Entry> macdEntries = new ArrayList<>();
        List<Entry> rsiEntries = new ArrayList<>();
        float[] closes = new float[bars.size()];
        final List<String> xLabels = new ArrayList<>();
        for (int i = 0; i < bars.size(); i++) {
            StockDataResponse.StockBar bar = bars.get(i);
            try {
                candleEntries.add(new CandleEntry(i, bar.high, bar.low, bar.open, bar.close));
                volumeEntries.add(new BarEntry(i, bar.volume));
                closes[i] = bar.close;
                // For X axis label
                if (lastInterval.endsWith("d") || lastInterval.endsWith("w") || lastInterval.endsWith("mo")) {
                    xLabels.add(bar.time); // date string
                } else {
                    // Try to extract time part from bar.time (e.g., "2025-05-18 14:30:00")
                    String label = bar.time;
                    if (bar.time.contains(" ")) {
                        label = bar.time.split(" ")[1];
                        if (label.length() >= 5) label = label.substring(0, 5); // HH:mm
                    }
                    xLabels.add(label);
                }
            } catch (NumberFormatException e) {
                Log.e("ChartFragment", "NumberFormatException at index " + i + ": " + bar.toString(), e);
                continue;
            } catch (Exception e) {
                Log.e("ChartFragment", "Exception at index " + i + ": " + bar.toString(), e);
                continue;
            }
        }
        // Set custom X axis formatter
        candleChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int idx = (int) value;
                if (idx >= 0 && idx < xLabels.size()) {
                    return xLabels.get(idx);
                } else {
                    return "";
                }
            }
        });
        candleChart.getXAxis().setGranularity(1f);
        candleChart.getXAxis().setLabelCount(Math.min(6, xLabels.size()), true);
        // --- FIX: Clear previous datasets and legend ---
        candleChart.clear();
        candleChart.getLegend().resetCustom();
        // ---
        CandleDataSet candleDataSet = new CandleDataSet(candleEntries, "Price");
        candleDataSet.setColor(Color.rgb(80, 80, 80));
        candleDataSet.setShadowColor(Color.DKGRAY);
        candleDataSet.setShadowWidth(0.7f);
        candleDataSet.setDecreasingColor(Color.RED);
        candleDataSet.setDecreasingPaintStyle(android.graphics.Paint.Style.FILL);
        candleDataSet.setIncreasingColor(Color.GREEN);
        candleDataSet.setIncreasingPaintStyle(android.graphics.Paint.Style.FILL);
        candleDataSet.setNeutralColor(Color.BLUE);
        candleDataSet.setDrawValues(false);
        CandleData candleData = new CandleData(candleDataSet);
        LineData lineData = new LineData();

        // Draw multiple EMAs
        for (EMAConfig config : emaConfigs) {
            List<Entry> emaEntries = calculateEMAEntries(bars, config.period);
            if (!emaEntries.isEmpty()) {
                LineDataSet emaDataSet = new LineDataSet(emaEntries, "EMA " + config.period);
                emaDataSet.setColor(config.color);
                emaDataSet.setLineWidth(2f);
                emaDataSet.setDrawCircles(false);
                lineData.addDataSet(emaDataSet);
            }
        }

        // MACD (user-configurable fast/slow)
        int macdMin = Math.max(macdFast, macdSlow);
        if (macdEnabled && bars.size() >= macdMin) {
            float[] emaFast = new float[closes.length];
            float[] emaSlow = new float[closes.length];
            float mFast = closes[0], mSlow = closes[0];
            float multFast = 2f / (macdFast + 1);
            float multSlow = 2f / (macdSlow + 1);
            for (int i = 0; i < closes.length; i++) {
                mFast = (closes[i] - mFast) * multFast + mFast;
                mSlow = (closes[i] - mSlow) * multSlow + mSlow;
                emaFast[i] = mFast;
                emaSlow[i] = mSlow;
                if (i >= macdMin - 1) macdEntries.add(new Entry(i, emaFast[i] - emaSlow[i]));
            }
        }

        // RSI (user-configurable period)
        if (rsiEnabled && bars.size() >= rsiPeriod + 1) {
            for (int i = rsiPeriod; i < closes.length; i++) {
                float gain = 0, loss = 0;
                for (int j = i - rsiPeriod + 1; j <= i; j++) {
                    float diff = closes[j] - closes[j - 1];
                    if (diff > 0) gain += diff;
                    else loss -= diff;
                }
                float rs = (loss == 0) ? 100 : gain / loss;
                float rsi = 100 - (100 / (1 + rs));
                rsiEntries.add(new Entry(i, rsi));
            }
        }

        // Add MACD overlay if enabled
        if (macdEnabled && !macdEntries.isEmpty()) {
            LineDataSet macdDataSet = new LineDataSet(macdEntries, "MACD");
            macdDataSet.setColor(macdColor);
            macdDataSet.setLineWidth(2f);
            macdDataSet.setDrawCircles(false);
            lineData.addDataSet(macdDataSet);
        }

        // Add RSI overlay if enabled
        if (rsiEnabled && !rsiEntries.isEmpty()) {
            LineDataSet rsiDataSet = new LineDataSet(rsiEntries, "RSI");
            rsiDataSet.setColor(rsiColor);
            rsiDataSet.setLineWidth(2f);
            rsiDataSet.setDrawCircles(false);
            lineData.addDataSet(rsiDataSet);
        }

        // Draw trendlines as LineDataSet overlays
        for (int i = 0; i < drawnLines.size(); i++) {
            float[] line = drawnLines.get(i);
            List<Entry> trendLineEntries = new ArrayList<>();
            trendLineEntries.add(new Entry(line[0], line[1]));
            trendLineEntries.add(new Entry(line[2], line[3]));
            LineDataSet trendLineDataSet = new LineDataSet(trendLineEntries, "");
            trendLineDataSet.setColor(drawingColors.get(i));
            trendLineDataSet.setLineWidth(2f);
            trendLineDataSet.setDrawCircles(false);
            trendLineDataSet.setForm(LegendForm.NONE);
            lineData.addDataSet(trendLineDataSet);
        }

        CombinedData combinedData = new CombinedData();
        combinedData.setData(candleData);
        combinedData.setData(lineData);
        BarDataSet volumeDataSet = new BarDataSet(volumeEntries, "Volume");
        volumeDataSet.setColor(Color.rgb(60, 220, 78));
        volumeDataSet.setDrawValues(false);
        volumeChart.setData(new BarData(volumeDataSet));
        volumeChart.invalidate();
        candleChart.setData(combinedData);
        candleChart.getLegend().resetCustom(); // ensure legend is reset
        // Attach marker view for detailed info
        CandleMarkerView marker = new CandleMarkerView(getContext(), bars);
        candleChart.setMarker(marker);
        candleChart.invalidate();
    }

    // Helper to calculate EMA entries for a given period
    private List<Entry> calculateEMAEntries(List<StockDataResponse.StockBar> bars, int period) {
        List<Entry> emaEntries = new ArrayList<>();
        if (bars == null || bars.size() < period) return emaEntries;
        float[] closes = new float[bars.size()];
        for (int i = 0; i < bars.size(); i++) closes[i] = bars.get(i).close;
        float multiplier = 2f / (period + 1);
        float ema = closes[0];
        for (int i = 0; i < closes.length; i++) {
            ema = (closes[i] - ema) * multiplier + ema;
            if (i >= period - 1) emaEntries.add(new Entry(i, ema));
        }
        return emaEntries;
    }

    private void setupFabAndPopup() {
        // FAB click to toggle popup
        indicatorFab.setOnClickListener(v -> {
            popupVisible = !popupVisible;
            indicatorPopup.setVisibility(popupVisible ? View.VISIBLE : View.GONE);
        });

        // Indicator toggles
        indicatorEma.setOnClickListener(v -> {
            emaEnabled = !emaEnabled;
            indicatorEma.setAlpha(emaEnabled ? 1.0f : 0.4f);
            updateCharts(lastBars);
        });
        indicatorEma.setOnLongClickListener(v -> {
            showEmaSettingsDialog();
            return true;
        });

        indicatorMacd.setOnClickListener(v -> {
            macdEnabled = !macdEnabled;
            indicatorMacd.setAlpha(macdEnabled ? 1.0f : 0.4f);
            updateCharts(lastBars);
        });
        indicatorMacd.setOnLongClickListener(v -> {
            showMacdSettingsDialog();
            return true;
        });

        indicatorRsi.setOnClickListener(v -> {
            rsiEnabled = !rsiEnabled;
            indicatorRsi.setAlpha(rsiEnabled ? 1.0f : 0.4f);
            updateCharts(lastBars);
        });
        indicatorRsi.setOnLongClickListener(v -> {
            showRsiSettingsDialog();
            return true;
        });

        indicatorDrawing.setOnClickListener(v -> {
            drawMode = !drawMode;
            indicatorDrawing.setAlpha(drawMode ? 1.0f : 0.4f);
            tempLinePoint = 0;
            if (!drawMode) {
                tempLine[0] = tempLine[1] = tempLine[2] = tempLine[3] = 0;
            }
            // Hide popup when drawing mode is activated
            popupVisible = false;
            indicatorPopup.setVisibility(View.GONE);
            
            // Show feedback to user
            if (drawMode) {
                Toast.makeText(getContext(), "Drawing mode enabled. Tap two points on the chart to draw a line.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Drawing mode disabled.", Toast.LENGTH_SHORT).show();
            }
        });

        // Add long press on drawing to clear all lines
        indicatorDrawing.setOnLongClickListener(v -> {
            if (drawnLines.size() > 0) {
                new AlertDialog.Builder(getContext())
                    .setTitle("Clear Drawings")
                    .setMessage("Do you want to clear all drawn lines?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        drawnLines.clear();
                        drawingColors.clear();
                        updateCharts(lastBars);
                        Toast.makeText(getContext(), "All drawings cleared.", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("No", null)
                    .show();
            } else {
                Toast.makeText(getContext(), "No drawings to clear.", Toast.LENGTH_SHORT).show();
            }
            return true;
        });

        // Set initial alpha for indicators
        indicatorEma.setAlpha(0.4f);
        indicatorMacd.setAlpha(0.4f);
        indicatorRsi.setAlpha(0.4f);
        indicatorDrawing.setAlpha(0.4f);
    }

    // Dialog for managing multiple EMAs
    private void showEmaSettingsDialog() {
        if (emaDialog != null) {
            emaDialog.dismiss();
            emaDialog = null;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Manage EMAs");
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 8);

        // List all EMAs
        for (int i = 0; i < emaConfigs.size(); i++) {
            final int idx = i;
            EMAConfig config = emaConfigs.get(i);
            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 8, 0, 8);

            // Color swatch
            View colorSwatch = new View(getContext());
            colorSwatch.setBackgroundColor(config.color);
            LinearLayout.LayoutParams swatchParams = new LinearLayout.LayoutParams(60, 60);
            swatchParams.setMargins(0, 0, 16, 0);
            colorSwatch.setLayoutParams(swatchParams);
            row.addView(colorSwatch);

            // Period label
            TextView periodLabel = new TextView(getContext());
            periodLabel.setText("EMA " + config.period);
            periodLabel.setTextSize(18);
            periodLabel.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(periodLabel);

            // Edit button
            Button editBtn = new Button(getContext());
            editBtn.setText("Edit");
            editBtn.setOnClickListener(v -> showAddOrEditEmaDialog(config, idx));
            row.addView(editBtn);

            // Remove button
            Button removeBtn = new Button(getContext());
            removeBtn.setText("Remove");
            removeBtn.setOnClickListener(v -> {
                emaConfigs.remove(idx);
                updateCharts(lastBars);
                showEmaSettingsDialog(); // Refresh dialog
            });
            row.addView(removeBtn);

            layout.addView(row);
        }

        // Add EMA button
        Button addBtn = new Button(getContext());
        addBtn.setText("Add EMA");
        addBtn.setOnClickListener(v -> {
            showAddOrEditEmaDialog(null, -1);
        });
        layout.addView(addBtn);

        builder.setView(layout);
        builder.setNegativeButton("Close", (dialog, which) -> {
            emaDialog = null;
        });
        emaDialog = builder.create();
        emaDialog.show();
    }

    // Dialog to add or edit an EMA
    private void showAddOrEditEmaDialog(EMAConfig config, int index) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(config == null ? "Add EMA" : "Edit EMA");
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 8);

        // Period input
        final EditText periodInput = new EditText(getContext());
        periodInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        periodInput.setHint("Period (e.g. 10)");
        if (config != null) periodInput.setText(String.valueOf(config.period));
        layout.addView(periodInput);

        // Color picker
        final int[] colors = {
            Color.BLUE, Color.RED, Color.GREEN, Color.MAGENTA, Color.CYAN, Color.YELLOW,
            Color.DKGRAY, Color.GRAY, Color.LTGRAY, Color.BLACK, Color.WHITE,
            Color.rgb(255, 165, 0), // Orange
            Color.rgb(128, 0, 128), // Purple
            Color.rgb(0, 128, 0),   // Dark Green
            Color.rgb(128, 0, 0),   // Maroon
            Color.rgb(0, 0, 128)    // Navy
        };
        final String[] colorNames = {
            "Blue", "Red", "Green", "Magenta", "Cyan", "Yellow",
            "Dark Gray", "Gray", "Light Gray", "Black", "White",
            "Orange", "Purple", "Dark Green", "Maroon", "Navy"
        };
        final int[] selectedColor = {config != null ? config.color : colors[0]};
        int initialColorIdx = 0;
        if (config != null) {
            for (int i = 0; i < colors.length; i++) {
                if (colors[i] == config.color) { initialColorIdx = i; break; }
            }
        }
        TextView colorLabel = new TextView(getContext());
        colorLabel.setText("Color: " + colorNames[initialColorIdx]);
        layout.addView(colorLabel);
        LinearLayout colorGrid = new LinearLayout(getContext());
        colorGrid.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < colors.length; i++) {
            Button colorBtn = new Button(getContext());
            colorBtn.setBackgroundColor(colors[i]);
            colorBtn.setText("");
            colorBtn.setLayoutParams(new LinearLayout.LayoutParams(0, 100, 1f));
            final int idx = i;
            colorBtn.setOnClickListener(v -> {
                selectedColor[0] = colors[idx];
                colorLabel.setText("Color: " + colorNames[idx]);
            });
            colorGrid.addView(colorBtn);
        }
        layout.addView(colorGrid);

        builder.setView(layout);
        builder.setPositiveButton(config == null ? "Add" : "Save", (dialog, which) -> {
            String periodStr = periodInput.getText().toString().trim();
            if (periodStr.isEmpty()) {
                Toast.makeText(getContext(), "Please enter a period", Toast.LENGTH_SHORT).show();
                return;
            }
            int period = 10;
            try {
                period = Integer.parseInt(periodStr);
            } catch (Exception e) {
                Toast.makeText(getContext(), "Invalid period", Toast.LENGTH_SHORT).show();
                return;
            }
            if (config == null) {
                emaConfigs.add(new EMAConfig(period, selectedColor[0]));
            } else {
                config.period = period;
                config.color = selectedColor[0];
                emaConfigs.set(index, config);
            }
            updateCharts(lastBars);
            showEmaSettingsDialog(); // Refresh list
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> showEmaSettingsDialog());
        builder.show();
    }

    private void showColorPickerDialog() {
        String[] items = {"EMA Color", "MACD Color", "RSI Color", "Drawing Color"};
        new AlertDialog.Builder(getContext())
                .setTitle("Select Color")
                .setItems(items, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showColorPicker(emaColor, color -> {
                                emaColor = color;
                                updateCharts(lastBars);
                            });
                            break;
                        case 1:
                            showColorPicker(macdColor, color -> {
                                macdColor = color;
                                updateCharts(lastBars);
                            });
                            break;
                        case 2:
                            showColorPicker(rsiColor, color -> {
                                rsiColor = color;
                                updateCharts(lastBars);
                            });
                            break;
                        case 3:
                            showColorPicker(drawingColor, color -> {
                                drawingColor = color;
                            });
                            break;
                    }
                })
                .show();
    }

    private void showColorPicker(int initialColor, ColorPickerCallback callback) {
        int[] colors = {
            Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.CYAN, Color.MAGENTA,
            Color.DKGRAY, Color.GRAY, Color.LTGRAY, Color.WHITE, Color.BLACK,
            Color.rgb(255, 165, 0), // Orange
            Color.rgb(128, 0, 128), // Purple
            Color.rgb(0, 128, 0),   // Dark Green
            Color.rgb(128, 0, 0),   // Maroon
            Color.rgb(0, 0, 128)    // Navy
        };

        String[] colorNames = {
            "Red", "Green", "Blue", "Yellow", "Cyan", "Magenta",
            "Dark Gray", "Gray", "Light Gray", "White", "Black",
            "Orange", "Purple", "Dark Green", "Maroon", "Navy"
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Pick a Color");

        // Create a grid of color buttons
        LinearLayout colorGrid = new LinearLayout(getContext());
        colorGrid.setOrientation(LinearLayout.VERTICAL);
        LinearLayout currentRow = null;
        int colorsPerRow = 4;

        for (int i = 0; i < colors.length; i++) {
            if (i % colorsPerRow == 0) {
                currentRow = new LinearLayout(getContext());
                currentRow.setOrientation(LinearLayout.HORIZONTAL);
                colorGrid.addView(currentRow);
            }

            Button colorButton = new Button(getContext());
            colorButton.setBackgroundColor(colors[i]);
            colorButton.setText(colorNames[i]);
            colorButton.setTextColor(Color.WHITE);
            colorButton.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
            ));
            colorButton.setPadding(8, 8, 8, 8);

            final int color = colors[i];
            colorButton.setOnClickListener(v -> {
                callback.onColorSelected(color);
                ((AlertDialog) v.getTag()).dismiss();
            });

            currentRow.addView(colorButton);
        }

        builder.setView(colorGrid);
        builder.setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.create();

        // Set the dialog as a tag for each button
        for (int i = 0; i < colorGrid.getChildCount(); i++) {
            LinearLayout row = (LinearLayout) colorGrid.getChildAt(i);
            for (int j = 0; j < row.getChildCount(); j++) {
                row.getChildAt(j).setTag(dialog);
            }
        }

        dialog.show();
    }

    interface ColorPickerCallback {
        void onColorSelected(int color);
    }

    private void showMacdSettingsDialog() {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        final EditText fastInput = new EditText(getContext());
        fastInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        fastInput.setText(String.valueOf(macdFast));
        fastInput.setLayoutParams(params);
        final EditText slowInput = new EditText(getContext());
        slowInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        slowInput.setText(String.valueOf(macdSlow));
        slowInput.setLayoutParams(params);
        TextView fastLabel = new TextView(getContext());
        fastLabel.setText("Fast:");
        fastLabel.setLayoutParams(params);
        TextView slowLabel = new TextView(getContext());
        slowLabel.setText("  Slow:");
        slowLabel.setLayoutParams(params);
        layout.addView(fastLabel);
        layout.addView(fastInput);
        layout.addView(slowLabel);
        layout.addView(slowInput);
        new AlertDialog.Builder(getContext())
                .setTitle("Set MACD Periods")
                .setView(layout)
                .setPositiveButton("OK", (dialog, which) -> {
                    try {
                        macdFast = Integer.parseInt(fastInput.getText().toString());
                    } catch (Exception ignored) {
                        Log.e("ChartFragment", "Invalid MACD fast period input: " + fastInput.getText().toString());
                    }
                    try {
                        macdSlow = Integer.parseInt(slowInput.getText().toString());
                    } catch (Exception ignored) {
                        Log.e("ChartFragment", "Invalid MACD slow period input: " + slowInput.getText().toString());
                    }
                    updateCharts(lastBars);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showRsiSettingsDialog() {
        final EditText input = new EditText(getContext());
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(rsiPeriod));
        new AlertDialog.Builder(getContext())
                .setTitle("Set RSI Period")
                .setView(input)
                .setPositiveButton("OK", (dialog, which) -> {
                    try {
                        rsiPeriod = Integer.parseInt(input.getText().toString());
                    } catch (Exception ignored) {
                        Log.e("ChartFragment", "Invalid RSI period input: " + input.getText().toString());
                    }
                    updateCharts(lastBars);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
} 