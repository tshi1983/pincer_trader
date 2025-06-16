package com.pincertrader.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class SettingsFragment extends Fragment {
    private static final String PREFS_NAME = "app_settings";
    private static final String KEY_FAKE_DATA = "use_fake_data";
    private Switch fakeDataSwitch;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        fakeDataSwitch = view.findViewById(R.id.fakeDataSwitch);
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        fakeDataSwitch.setChecked(prefs.getBoolean(KEY_FAKE_DATA, false));
        fakeDataSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_FAKE_DATA, isChecked).apply();
        });
        return view;
    }

    public static boolean isFakeDataEnabled(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_FAKE_DATA, false);
    }
} 