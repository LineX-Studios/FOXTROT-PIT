package com.linexstudios.foxtrot.Handler;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.Timer;
import java.util.TimerTask;
import net.minecraftforge.fml.common.Loader;

public class TelemetryManager {

    public static String anonymousClientId = "";
    private static Timer heartbeatTimer;

    public static void initialize() {
        // --- OPT-OUT CHECK ---
        // If the player disabled telemetry in their config, stop right here!
        if (!ConfigHandler.telemetryEnabled) {
            System.out.println("[Foxtrot] Telemetry is disabled by user. No data will be sent.");
            return;
        }

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
        }, 180000, 180000); 
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

                // --- DYNAMIC VERSION FETCHING ---
                String modVersion = "Unknown";
                try {
                    // This asks Forge for the exact version in your @Mod annotation
                    modVersion = Loader.instance().getIndexedModList().get("foxtrot").getVersion();
                } catch (Exception e) {
                    modVersion = "0.7.4"; // Fallback just in case
                }

                String jsonPayload = "{\"anonId\": \"" + anonymousClientId + "\", \"version\": \"" + modVersion + "\"}";

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                conn.getResponseCode(); 
            } catch (Exception e) {
                // Fail silently so it doesn't spam the user's console
            }
        }).start();
    }
}