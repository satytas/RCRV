package com.example.plsworkthistime;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.lang.reflect.Method;

public class MenuActivity extends AppCompatActivity implements NetworkScanner.ScannerCallBack {
    private Button hotspotBtn, connectBtn;
    private TextView dataLbl;
    private String hostIP = "";
    private boolean foundIp = false;
    private boolean isScanning = false;
    private Thread hotspotThread;
    private volatile boolean running = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_connect);

        // Request wifi permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.CHANGE_WIFI_STATE},
                    1);
        }

        // UI & buttons
        hotspotBtn = findViewById(R.id.hotspotBtn);
        connectBtn = findViewById(R.id.connectBtn);
        dataLbl = findViewById(R.id.dataLbl);

        hotspotBtn.setOnClickListener(v -> turnOnHotspot());
        connectBtn.setOnClickListener(v -> handleConnectBtn());

        startHotspotMonitoring();
    }

    private void handleConnectBtn() {
        if (isScanning) return;
        if (foundIp) {
            Intent intent = new Intent(MenuActivity.this, MainActivity.class);
            intent.putExtra("hostIP", hostIP);

            startActivity(intent);
        }
        else {
            hostIP = "";
            dataLbl.setText("Searching");

            startIpScan();
            connectBtn.setEnabled(false);
        }
    }

    private void startIpScan() {
        new NetworkScanner(this).scan();
    }

    private void turnOnHotspot() {
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        ComponentName cn = new ComponentName("com.android.settings", "com.android.settings.TetherSettings");
        intent.setComponent(cn);

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private boolean hotspotOn() {
        try {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            Method method = wifiManager.getClass().getDeclaredMethod("isWifiApEnabled");

            method.setAccessible(true);

            return (Boolean) method.invoke(wifiManager);
        } catch (Exception e) {
            runOnUiThread(() -> dataLbl.setText("Error checking hotspot"));
            return false;
        }
    }

    private void startHotspotMonitoring() {
        hotspotThread = new Thread(() -> {
            while (running) {
                if (hotspotOn()) {
                    runOnUiThread(() -> {
                        hotspotBtn.setEnabled(false);

                        connectBtn.setEnabled(true);
                        if (!foundIp)  dataLbl.setText("Waiting to start scan");
                    });
                }
                else{
                    runOnUiThread(() -> {
                        hotspotBtn.setEnabled(true);
                        connectBtn.setEnabled(false);

                        if (foundIp) updateInfoLbl();
                        else dataLbl.setText("Please on Hotspot");
                    });
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    running = false;
                }
            }
        });

        hotspotThread.start();
    }

    @Override
    public void updateInfoLbl() {
        runOnUiThread(() -> dataLbl.setText(NetworkScanner.scanState));
    }

    @Override
    public void onScanComplete(String host) {
        hostIP = host;

        runOnUiThread(() -> {
            isScanning = false;

            if (!hostIP.isEmpty()) {
                foundIp = true;

                connectBtn.setText("Start");
                connectBtn.setBackgroundColor(Color.parseColor("#34A853"));
                connectBtn.setEnabled(true);
            }
            else {
                connectBtn.setText("Try again?");
                connectBtn.setEnabled(true);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        running = false;
        if (hotspotThread != null) {
            hotspotThread.interrupt();
        }
    }
}