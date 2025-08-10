package com.shellytask.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.shellytask.app.bluetooth.BluetoothActivity;
import com.shellytask.app.gallery.GalleryActivity;
import com.shellytask.app.legacy.LegacyNetworkActivity;
import com.shellytask.app.legacy.UserSessionManager;
import com.shellytask.app.web.WebContentActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        UserSessionManager sessionManager = new UserSessionManager(this);

        // This is just an example
        if (!sessionManager.isLoggedIn()) {
            sessionManager.saveLogin("Guest");
        }

        Button btnGallery       = findViewById(R.id.btnGallery);
        Button btnWebView       = findViewById(R.id.btnWebView);
        Button btnBluetooth     = findViewById(R.id.btnBluetooth);
        Button btnLegacyNetwork = findViewById(R.id.btnLegacyNetwork);

        btnGallery.setOnClickListener(v -> startActivity(new Intent(this, GalleryActivity.class)));
        btnWebView.setOnClickListener(v -> startActivity(new Intent(this, WebContentActivity.class)));
        btnBluetooth.setOnClickListener(v -> startActivity(new Intent(this, BluetoothActivity.class)));
        btnLegacyNetwork.setOnClickListener(v -> startActivity(new Intent(this, LegacyNetworkActivity.class)));
    }
}