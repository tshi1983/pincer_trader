package com.pincertrader.android;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set up bottom navigation
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) {
                loadFragment(new ChartFragment());
                return true;
            } else if (itemId == R.id.navigation_portfolio) {
                // TODO: Implement portfolio fragment
                Toast.makeText(this, "Portfolio coming soon", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.navigation_settings) {
                loadFragment(new SettingsFragment());
                return true;
            }
            return false;
        });

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(new ChartFragment());
        }
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            loadFragment(new SettingsFragment());
            return true;
        } else if (id == R.id.action_help) {
            Toast.makeText(this, "Help clicked", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_about) {
            loadFragment(new AboutFragment());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 