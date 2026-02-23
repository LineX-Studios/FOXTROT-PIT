package com.linexstudios.foxtrot.Handler;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.Timer;
import java.util.TimerTask;

public class TelemetryManager {

    public static String anonymousClientId = "";
    private static Timer heartbeatTimer;

    public static void initialize() {
        // Generate a new ID if one wasn't loaded from the config
        if (anonymousClientId == null || anonymousClientId.isEmpty()) {
            anonymousClientId = UUID.randomUUID().toString();
            // Force the config to save this new ID to the text file immediately
            ConfigHandler.saveConfig();
        }

        // Send a ping immediately on launch
        sendPing();

        // Schedule a background heartbeat every 3 minutes (180,000 milliseconds)
        heartbeatTimer = new Timer(true); // 'true' makes it a daemon thread (closes when game closes)
        heartbeatTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                sendPing();
            }
        }, 180000, 180000);
    }

    private static void sendPing() {
        new Thread(() -> {
            try {
                URL url = new URL("https://foxtrot-api.vercel.app/ping");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                
                // --- Disguise the request as a normal browser to bypass anti-bot firewalls ---
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
                
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(4000);
                conn.setDoOutput(true);

                String jsonPayload = "{\"anonId\": \"" + anonymousClientId + "\"}";

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                // Execute the request and check Vercel's response
                int responseCode = conn.getResponseCode();
                System.out.println("[Foxtrot Telemetry] Sent Ping. Vercel responded with code: " + responseCode);
                ConfigHandler.logDebug("Telemetry Ping Sent! Code: " + responseCode);
                
            } catch (Exception e) {
                // Print the error to the console so we know exactly what is breaking
                System.out.println("[Foxtrot Telemetry] CRITICAL ERROR: Could not send ping! " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }
}