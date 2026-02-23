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
        // Strip out any invisible newlines or spaces that might corrupt the JSON
        if (anonymousClientId != null) {
            anonymousClientId = anonymousClientId.replace("\n", "").replace("\r", "").trim();
        }

        if (anonymousClientId == null || anonymousClientId.isEmpty()) {
            anonymousClientId = UUID.randomUUID().toString();
            ConfigHandler.saveConfig();
        }

        sendPing();

        heartbeatTimer = new Timer(true);
        heartbeatTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                sendPing();
            }
        }, 180000, 180000); // 3 minutes
    }

    private static void sendPing() {
        new Thread(() -> {
            try {
                URL url = new URL("https://foxtrot-api.vercel.app/ping");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(4000);
                conn.setDoOutput(true);

                // Add your current mod version here!
                String modVersion = "1.0.0"; 
                String jsonPayload = "{\"anonId\": \"" + anonymousClientId + "\", \"version\": \"" + modVersion + "\"}";

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                System.out.println("[Foxtrot Telemetry] Sent Ping. Vercel responded with code: " + responseCode);
                ConfigHandler.logDebug("Telemetry Ping Sent! Code: " + responseCode);
                
            } catch (Exception e) {
                System.out.println("[Foxtrot Telemetry] CRITICAL ERROR: Could not send ping! " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }
}