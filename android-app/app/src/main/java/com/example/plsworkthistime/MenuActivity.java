package com.example.plsworkthistime;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.UnstableApi;

import java.lang.reflect.Method;

public class MenuActivity extends AppCompatActivity implements NetworkScanner.ScannerCallBack {
    private Button hotspotBtn, connectBtn, mainBtn;
    private TextView dataLbl;
    private String hostIP = "";
    private boolean foundIp = false;

    @OptIn(markerClass = UnstableApi.class)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_connect);

        hotspotBtn = findViewById(R.id.hotspotBtn);
        connectBtn = findViewById(R.id.connectBtn);
        mainBtn = findViewById(R.id.mainBtn);
        dataLbl = findViewById(R.id.dataLbl);

        hotspotBtn.setOnClickListener(v -> turnOnHotspot());
        connectBtn.setOnClickListener(v -> {
            startIpScan();
            connectBtn.setText("Searching");
            connectBtn.setEnabled(false);
        });
        mainBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, MainActivity.class);
            intent.putExtra("hostIP", hostIP);

            startActivity(intent);
        });

        new Thread(() -> {
            boolean wasHotspotOn = false;

            while (true) {
                boolean hotspotCurrentlyOn = hotspotOn();

                if (hotspotCurrentlyOn && !wasHotspotOn) {
                    runOnUiThread(() -> {
                        hotspotBtn.setEnabled(false);
                        if (!foundIp) connectBtn.setEnabled(true);
                        if(!foundIp) dataLbl.setText("Waiting to start scan");
                    });
                    wasHotspotOn = true;
                } else if (!hotspotCurrentlyOn && wasHotspotOn) {
                    runOnUiThread(() -> {
                        hotspotBtn.setEnabled(true);
                        connectBtn.setEnabled(false);
                        if(foundIp) updateInfoLbl();
                        else dataLbl.setText("Hotspot is off, please turn it on");
                    });
                    wasHotspotOn = false;
                }

                runOnUiThread(() -> mainBtn.setEnabled(hotspotCurrentlyOn && foundIp));

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    private void startIpScan() {
        new NetworkScanner(this).scan();
    }

    private void turnOnHotspot(){
        final Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        final ComponentName cn = new ComponentName("com.android.settings", "com.android.settings.TetherSettings");
        intent.setComponent(cn);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity( intent);
    }

    private boolean hotspotOn() {
        try {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            Method method = wifiManager.getClass().getDeclaredMethod("isWifiApEnabled");
            method.setAccessible(true);
            return (boolean) method.invoke(wifiManager);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void updateInfoLbl(){
        runOnUiThread(() -> dataLbl.setText(NetworkScanner.scanState +
                "\nFound IP: " + NetworkScanner.foundIP));

    }

    @Override
    public void onScanComplete(String host) {
        hostIP = host;

        runOnUiThread(() -> {
            if(hostIP != "") {
                foundIp = true;
                connectBtn.setText("Connected!");
            }
            else{
                connectBtn.setText("Try again?");
                connectBtn.setEnabled(true);
            }
        });
    }
}
