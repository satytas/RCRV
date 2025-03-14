package com.example.plsworkthistime;

import android.annotation.SuppressLint;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
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
    public static String scanState, foundSubnet, foundIP, checkingIp;

    public NetworkScanner(ScannerCallBack callback) {
        this.callback = callback;

        scanState = foundSubnet = foundIP = checkingIp = "-";
    }



    public void scan() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            scanState = "Starting scan";
            callback.updateInfoLbl();
            String host = findServerHost();

            callback.onScanComplete(host);
        });

        executorService.shutdown();
    }

    public String getSubnet() {
        scanState = "searching subnet";
        callback.updateInfoLbl();

        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                for (Enumeration<InetAddress> enumIpAddr = en.nextElement().getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();

                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        String localIP = inetAddress.getHostAddress();
                        String subnet = localIP.substring(0, localIP.lastIndexOf('.'));


                        Log.d(TAG, "Device IP Found: " + localIP + ", Subnet: " + subnet);

                        foundSubnet = subnet;
                        callback.updateInfoLbl();
                        return subnet;
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e(TAG, "Error retrieving network subnet", ex);
        }

        return null;
    }

    public String findServerHost() {
        String IP = "";
        String subnet = getSubnet();

        scanState = "searching ip";
        callback.updateInfoLbl();

        if (subnet == null) {
            Log.w(TAG, "Cant scan without subnet");
            return IP;
        }

        Log.d(TAG, "Starting scan on subnet: " + subnet);

        for (int i = 240; i >= 0; i--) {

            IP = subnet + "." + i;

            if (isHost(IP)) {
                Log.i(TAG, "Found Connection with: " + IP + ". Scan complete!");

                foundIP = IP;
                checkingIp = "-";
                callback.updateInfoLbl();
                scanState = "Finished scan successfully!";
                return IP;

            } else {
                Log.d(TAG, "No response with: " + IP);

            }
        }

        foundIP = IP;
        checkingIp = "-";
        scanState = "Scan Failed";
        callback.updateInfoLbl();
        return IP;
    }

    public boolean isHost(String IP) {
        checkingIp = (IP + "\n" + checkingIp);
        callback.updateInfoLbl();

        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL("http://" + IP + ":5000");

            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(700);
            urlConnection.connect();

            if (urlConnection.getResponseCode() == 200) return true;

        } catch (IOException e) {
        } finally {
            if (urlConnection != null) urlConnection.disconnect();
        }

        return false;
    }

    public interface ScannerCallBack {
        void onScanComplete(String host);

        void updateInfoLbl();
    }
}