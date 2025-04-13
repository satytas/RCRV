package com.example.plsworkthistime;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NetworkScanner {
    private static final String TAG = "NetworkScanner";
    private static final int SCAN_TIMEOUT = 10000;
    private final ScannerCallBack callback;
    public static String scanState = "-";
    public static String foundIP = "-";

    public NetworkScanner(ScannerCallBack callback) {
        this.callback = callback;
    }

    public void scan() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            scanState = "Scanning for server...";
            callback.updateInfoLbl();
            String serverIp = findServerHost();
            callback.onScanComplete(serverIp);
            executor.shutdownNow();
        });
    }

    private String findServerHost() {
        DatagramSocket socket = null;

        try {
            // Setup socket
            socket = new DatagramSocket(5001);
            socket.setBroadcast(true);
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            // Wait for server broadcast
            socket.setSoTimeout(SCAN_TIMEOUT);
            socket.receive(packet);

            // Parse received message
            String message = new String(packet.getData(), 0, packet.getLength());
            if (message.startsWith("FLASK_SERVER:")) {
                String serverIp = message.substring("FLASK_SERVER:".length());
                Log.i(TAG, "Found server at: " + serverIp);

                foundIP = serverIp;
                scanState = "Connected: " + serverIp;

                callback.updateInfoLbl();
                return serverIp;
            }

            scanState = "No server found";
            Log.w(TAG, "Invalid or no server response");
        } catch (SocketTimeoutException e) {
            scanState = "Timeout after 10s";
            Log.w(TAG, "Scan timed out");
        } catch (IOException e) {
            scanState = "Scan error";
            Log.e(TAG, "Error during scan", e);
        } finally {
            if (socket != null) {
                socket.close();
            }
        }

        callback.updateInfoLbl();
        return "";
    }

    public interface ScannerCallBack {
        void onScanComplete(String host);
        void updateInfoLbl();
    }
}