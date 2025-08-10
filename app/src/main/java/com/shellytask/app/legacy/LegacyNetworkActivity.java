package com.shellytask.app.legacy;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.shellytask.app.R;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LegacyNetworkActivity extends AppCompatActivity {

    private final ExecutorService networkExecutor   = Executors.newSingleThreadExecutor();
    private final Handler mainHandler               = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_legacy_network);
        setTitle(R.string.legacy_network);

        Toolbar toolbar = findViewById(R.id.topAppBar);

        if (toolbar != null) {
            setSupportActionBar(toolbar);

            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }

        TextView result = findViewById(R.id.tvNetworkResult);
        Button fetch    = findViewById(R.id.btnFetch);

        result.setText(R.string.network_hint);

        fetch.setOnClickListener(v -> {
            result.setText(R.string.network_loading);
            final String demoUrl = "https://www.rfc-editor.org/rfc/rfc862.txt";
            networkExecutor.execute(() -> {
                try {
                    String text = LegacyNetworkClient.fetchText(demoUrl);
                    mainHandler.post(() -> result.setText(text));
                } catch (Exception e) {
                    mainHandler.post(() -> result.setText(getString(R.string.network_error, e.getMessage())));
                }
            });
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        networkExecutor.shutdownNow();
    }
}