package com.example.plsworkthistime;

import android.annotation.SuppressLint;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NetworkScanner {
    private static final String TAG = "NetworkManager";
    private final ScannerCallBack callback;
    public static String scanState = "-", foundIP = "-";
    public NetworkScanner(ScannerCallBack callback) {
        this.callback = callback;
    }

    public void scan() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            scanState = "Listening for server IP";
            callback.updateInfoLbl();
            String host = findServerHost();
            callback.onScanComplete(host);
        });
        executorService.shutdown();
    }

    private String findServerHost() {
        try {
            DatagramSocket socket = new DatagramSocket(5001);
            socket.setBroadcast(true);
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            // Listen for up to 30 seconds (adjust timeout as needed)
            socket.setSoTimeout(30000);
            socket.receive(packet);

            String message = new String(packet.getData(), 0, packet.getLength());
            socket.close();

            if (message.startsWith("FLASK_SERVER:")) {
                String ip = message.substring("FLASK_SERVER:".length());
                Log.i(TAG, "Received server IP: " + ip);
                foundIP = ip;
                scanState = "Found server at " + ip;
                callback.updateInfoLbl();
                return ip;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error receiving UDP broadcast", e);
            scanState = "Failed to find server";
            callback.updateInfoLbl();
        }
        return "";
    }

    public interface ScannerCallBack {
        void onScanComplete(String host);
        void updateInfoLbl();
    }
}