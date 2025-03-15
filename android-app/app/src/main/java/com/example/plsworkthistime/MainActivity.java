package com.example.plsworkthistime;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
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


public class MainActivity extends AppCompatActivity{
    private static final String TAG = "MainActivity";
    private TextView dirLbl, angleLbl, fpsLbl, threatLbl, infoLbl;
    private Button searchBtn;
    private WebView videoStream;
    private String hostIP = null;
    private double fps = 0;
    private Boolean threatDetected = false;
    private String dir = "None";
    private double angle = 0;
    private Boolean scanMode = false;
    private Boolean connectedBefore = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        hostIP = intent.getStringExtra("hostIP");

        videoStream = findViewById(R.id.videoStream);
        videoStream.setBackgroundColor(Color.TRANSPARENT);
        dirLbl = findViewById(R.id.dirLbl);
        angleLbl = findViewById(R.id.angleLbl);
        fpsLbl = findViewById(R.id.fpsLbl);
        threatLbl = findViewById(R.id.threatLbl);
        infoLbl = findViewById(R.id.infoLbl);
        Button searchBtn = findViewById(R.id.angleBtn);


        JoyStick joyStick = findViewById(R.id.joyStick);
        joyStick.setDirChangeListener(new JoyStick.OnDirChangeListener() {
            @Override
            public void onDirChange(String dir, double angle) {
                angleLbl.setText("Angle: " + (angle == -1 ? "None" : new DecimalFormat("#.#").format(angle)));
                dirLbl.setText("Dir: " + dir);

                updateJoyStickData(dir, angle);
            }
        });

        scanMode = false;
        searchBtn.setOnClickListener(v -> {
            if(scanMode)
                searchBtn.setText("Scan Area");
            else
                searchBtn.setText("Manual Mode");

            scanMode = !scanMode;
        });


        // Apply window insets for edge-to-edge layout ??????
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupVideoStream();
        startDataTransaction();
    }

    private void updateJoyStickData(String dir, double angle){
        this.angle = angle;
        this.dir = dir;
    }

    private void setupVideoStream() {
        if(!connectedBefore) infoLbl.setText("Connecting...");

        connectedBefore = true;

        WebSettings webSettings = videoStream.getSettings();
        webSettings.setJavaScriptEnabled(true);

        videoStream.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                infoLbl.setText("Loading...");
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                infoLbl.setText("No connection...");
                retryConnection();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                infoLbl.setText("");
            }
        });

        videoStream.loadUrl("http://" + hostIP + ":5000");
    }

    private void retryConnection() {
        infoLbl.setText("Make sure the hotspot is on and in range");

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                setupVideoStream();
            }
        }, 1000);
    }




    private void startDataTransaction() {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                fetchData();
                sendData();
                handler.postDelayed(this, 100);
            }
        }, 1000);
    }

    private void sendData(){
        new Thread(() ->{
            try{
                URL url = new URL("http://" + hostIP + ":5000/send_data");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; utf-8");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true);

                JSONObject jsonInput = new JSONObject();
                jsonInput.put("direction", dir);
                jsonInput.put("angle", angle);
                jsonInput.put("scanMode", scanMode);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonInput.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line.trim());
                }
                reader.close();
                Log.d("ScanMode: ", scanMode.toString());
                Log.d("ServerResponse", response.toString());
            } catch (Exception e){
                Log.e("SendData", "failed sending data back", e);
            }
        }).start();
    }

    private void fetchData() {
        new Thread(() -> {
            try {
                URL url = new URL("http://" + hostIP + ":5000/get_data");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder result = new StringBuilder();

                String line;
                while ((line = reader.readLine()) != null)
                    result.append(line);

                reader.close();

                JSONObject fetchedData = new JSONObject(result.toString());

                fps = fetchedData.getDouble("fps");
                threatDetected = fetchedData.getBoolean("person_detected");

                new Handler(Looper.getMainLooper()).post(() -> {
                    fpsLbl.setText("fps: " + new DecimalFormat("#.#").format(fps));

                    if(threatDetected){
                        threatLbl.setText("THREAT DETECTED");
                        threatLbl.setTextColor(Color.RED);
                        threatLbl.setBackgroundColor(Color.parseColor("#80D32F2F"));
                        threatLbl.setTextSize(30);
                    }
                    else{
                        threatLbl.setText("no threats");
                        threatLbl.setTextColor(Color.GREEN);
                        threatLbl.setBackgroundColor(Color.TRANSPARENT);
                        threatLbl.setTextSize(20);
                    }

                    Log.d("DataFetcher", "FPS: " + fps + " PD: " + threatDetected + " host: " + hostIP);
                });

            } catch (Exception e) {
                Log.e("Fetcher", "fech error: ", e);
            }
        }).start();
    }
}