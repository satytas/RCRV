package com.example.plsworkthistime;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private TextView dirLbl, angleLbl, fpsLbl, threatLbl, infoLbl;
    private Button searchBtn, changeAIBtn;
    private WebView videoStream;
    private String hostIP;
    private double fps = 0;
    private boolean threatDetected = false;
    private String dir = "None";
    private double angle = 0;
    private int aiMode = 0;
    private int lastAiMode = 0;
    private boolean scanMode = false;
    private boolean connectedBefore = false;
    private Handler handler;
    private ExecutorService executor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Initialize dependencies
        handler = new Handler(Looper.getMainLooper());
        executor = Executors.newFixedThreadPool(2); // For HTTP requests

        // Get host IP from intent
        Intent intent = getIntent();
        hostIP = intent.getStringExtra("hostIP");

        // Setup UI
        initializeViews();
        setupJoystick();
        setupButtons();
        setupEdgeToEdge();

        // Start core functions
        setupVideoStream();
        startDataTransaction();
    }

    private void initializeViews() {
        videoStream = findViewById(R.id.videoStream);
        videoStream.setBackgroundColor(Color.TRANSPARENT);
        dirLbl = findViewById(R.id.dirLbl);
        angleLbl = findViewById(R.id.angleLbl);
        fpsLbl = findViewById(R.id.fpsLbl);
        threatLbl = findViewById(R.id.threatLbl);
        infoLbl = findViewById(R.id.infoLbl);
        changeAIBtn = findViewById(R.id.changeAI);
        searchBtn = findViewById(R.id.angleBtn);
    }

    private void setupJoystick() {
        JoyStick joyStick = findViewById(R.id.joyStick);

        joyStick.setDirChangeListener((dir, angle) -> {
            angleLbl.setText("Angle: " + (angle == -1 ? "None" : new DecimalFormat("#.#").format(angle)));
            dirLbl.setText("Dir: " + dir);
            updateJoyStickData(dir, angle);
        });
    }

    private void setupButtons() {
        searchBtn.setOnClickListener(v -> {
            if (scanMode) {
                aiMode = lastAiMode;
                searchBtn.setText("Scan Area");
                changeAIBtn.setEnabled(true);
            }
            else {
                lastAiMode = aiMode;
                aiMode = 1;
                searchBtn.setText("Manual Mode");
                changeAIBtn.setEnabled(false);
            }

            scanMode = !scanMode;
        });

        changeAIBtn.setOnClickListener(v -> {
            aiMode = Math.abs(aiMode - 1);
            changeAIBtn.setText(aiMode == 0 ? "Activate AI" : "Deactivate AI");
        });
    }

    private void setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void updateJoyStickData(String dir, double angle) {
        this.angle = angle;
        this.dir = dir;
    }

    private void setupVideoStream() {
        if (!connectedBefore) infoLbl.setText("Connecting...");
        connectedBefore = true;

        WebSettings webSettings = videoStream.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        videoStream.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                infoLbl.setText("Loading...");
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                infoLbl.setText("No connection...");

                retryConnection();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                infoLbl.setText("");
            }
        });

        videoStream.loadUrl("http://" + hostIP + ":5000");
    }

    //creates a request from the main thread after 1 sec
    private void retryConnection() {
        infoLbl.setText("Check hotspot connection");
        handler.postDelayed(this::setupVideoStream, 1000);
    }

    //will request in 1 sec to run the frangible that will loop yb sending a request to the main thread 10 times a second
    private void startDataTransaction() {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                fetchData();
                sendData();
                handler.postDelayed(this, 100);
            }
        };
        handler.postDelayed(task, 1000);
    }

    private void sendData() {
        executor.execute(() -> {
            try {
                URL url = new URL("http://" + hostIP + ":5000/send_data");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setConnectTimeout(5000);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);

                JSONObject json = new JSONObject();
                json.put("direction", dir);
                json.put("angle", angle);
                json.put("scanMode", scanMode);
                json.put("mode", aiMode);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = json.toString().getBytes("utf-8");

                    os.write(input, 0, input.length);
                }

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line.trim());
                    }

                    Log.d(TAG, "Server response: " + response);
                }
            } catch (Exception e) {
                Log.e(TAG, "Send data failed", e);
                runOnUiThread(() -> infoLbl.setText("Send error"));
            }
        });
    }

    private void fetchData() {
        executor.execute(() -> {
            try {
                URL url = new URL("http://" + hostIP + ":5000/get_data");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setConnectTimeout(5000);
                conn.setRequestMethod("GET");

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()))) {
                    StringBuilder result = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }

                    JSONObject data = new JSONObject(result.toString());

                    fps = data.getDouble("fps");
                    threatDetected = data.getBoolean("person_detected");

                    runOnUiThread(() -> {
                        fpsLbl.setText("fps: " + new DecimalFormat("#.#").format(fps));
                        if (threatDetected) {
                            threatLbl.setText("THREAT DETECTED");
                            threatLbl.setTextColor(Color.RED);
                            threatLbl.setBackgroundColor(Color.parseColor("#80D32F2F"));
                            threatLbl.setTextSize(30);
                        } else {
                            threatLbl.setText("no threats");
                            threatLbl.setTextColor(Color.GREEN);
                            threatLbl.setBackgroundColor(Color.TRANSPARENT);
                            threatLbl.setTextSize(20);
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Fetch data failed", e);
                runOnUiThread(() -> infoLbl.setText("Fetch error"));
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        if (executor != null) {
            executor.shutdownNow();
        }
    }
}