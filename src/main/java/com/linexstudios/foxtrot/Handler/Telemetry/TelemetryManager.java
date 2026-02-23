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
        if (anonymousClientId == null || anonymousClientId.isEmpty()) {
            anonymousClientId = UUID.randomUUID().toString();
            // Note: Save this anonymousClientId in your ConfigHandler so it persists!
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
                // REPLACE THIS WITH YOUR RENDER.COM URL
                URL url = new URL("https://foxtrot-api.vercel.app/ping");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(4000);
                conn.setDoOutput(true);

                String jsonPayload = "{\"anonId\": \"" + anonymousClientId + "\"}";

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                conn.getResponseCode(); // Execute the request
            } catch (Exception e) {
                // Fail silently so the player never sees an error if the API is offline
            }
        }).start();
    }
}