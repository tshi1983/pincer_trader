package com.pincertrader.android;

import android.content.Context;
import android.widget.TextView;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.CandleEntry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;
import android.view.LayoutInflater;
import android.view.View;
import com.pincertrader.android.api.StockDataResponse;
import java.util.List;

public class CandleMarkerView extends MarkerView {
    private TextView dateText, openText, highText, lowText, closeText, volumeText;
    private List<StockDataResponse.StockBar> bars;

    public CandleMarkerView(Context context, List<StockDataResponse.StockBar> bars) {
        super(context, R.layout.marker_view);
        this.bars = bars;
        dateText = findViewById(R.id.marker_date);
        openText = findViewById(R.id.marker_open);
        highText = findViewById(R.id.marker_high);
        lowText = findViewById(R.id.marker_low);
        closeText = findViewById(R.id.marker_close);
        volumeText = findViewById(R.id.marker_volume);
    }

    @Override
    public void refreshContent(com.github.mikephil.charting.data.Entry e, Highlight highlight) {
        int xIndex = (int) e.getX();
        if (bars != null && xIndex >= 0 && xIndex < bars.size()) {
            StockDataResponse.StockBar bar = bars.get(xIndex);
            dateText.setText("Date: " + bar.time);
            openText.setText("Open: " + bar.open);
            highText.setText("High: " + bar.high);
            lowText.setText("Low: " + bar.low);
            closeText.setText("Close: " + bar.close);
            volumeText.setText("Volume: " + formatVolume(bar.volume));
        } else {
            dateText.setText("");
            openText.setText("");
            highText.setText("");
            lowText.setText("");
            closeText.setText("");
            volumeText.setText("");
        }
        super.refreshContent(e, highlight);
    }

    private String formatVolume(int volume) {
        if (volume >= 1_000_000_000) {
            return String.format("%.2fB", volume / 1_000_000_000f);
        } else if (volume >= 1_000_000) {
            return String.format("%.2fM", volume / 1_000_000f);
        } else if (volume >= 1_000) {
            return String.format("%.2fK", volume / 1_000f);
        } else {
            return String.valueOf(volume);
        }
    }

    @Override
    public MPPointF getOffset() {
        // Center the marker above the selected value
        return new MPPointF(-(getWidth() / 2f), -getHeight());
    }
} 