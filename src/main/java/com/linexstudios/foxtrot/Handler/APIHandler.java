package com.linexstudios.foxtrot.Handler;

import net.minecraft.entity.player.EntityPlayer;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class APIHandler {

    // Cached variables so we don't spam the API every single frame
    public static int prestige = 0;
    public static int level = 0;
    public static long xpLeft = 0;
    public static double xpProportion = 0.0;
    public static double goldLeft = 0.0;
    public static double goldProportion = 0.0;

    public static boolean isLoaded = false;
    private static long lastFetchTime = 0;

    /**
     * Refreshes the player's stats via the web API.
     * Automatically rate-limits itself to only check once every 30 seconds.
     */
    public static void updateStats(EntityPlayer player) {
        if (player == null) return;

        long currentTime = System.currentTimeMillis();
        // Only fetch once every 30 seconds (30,000 ms) to avoid getting API banned
        if (currentTime - lastFetchTime < 30000) return;

        lastFetchTime = currentTime;

        // Run on a separate thread so Minecraft doesn't freeze!
        new Thread(() -> {
            try {
                // Remove dashes from the UUID to match the API format
                String uuid = player.getUniqueID().toString().replace("-", "");

                URL url = new URL("https://pitmart.net/api/player/" + uuid);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");

                if (conn.getResponseCode() == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String inputLine;
                    StringBuilder content = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        content.append(inputLine);
                    }
                    in.close();

                    // Parse the JSON data
                    JSONObject json = new JSONObject(content.toString());

                    // Use optInt/optDouble so it safely defaults to 0 if the API glitches
                    prestige = json.optInt("prestige", 0);
                    level = json.optInt("level", 1);
                    xpLeft = json.optLong("prestigeXpLeft", 0);
                    xpProportion = json.optDouble("prestigeXpReqProportion", 0.0);
                    goldLeft = json.optDouble("prestigeGoldLeft", 0.0);
                    goldProportion = json.optDouble("prestigeGoldReqProportion", 0.0);

                    isLoaded = true;
                }
                conn.disconnect();
            } catch (Exception e) {
                System.out.println("[Foxtrot] Failed to fetch data from API.");
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Helper Method: Checks if the gold value is negative (meaning requirement is met)
     */
    public static boolean isGoldReqMet() {
        return goldLeft <= 0;
    }

    /**
     * Helper Method: Formats the gold left dynamically
     */
    public static String getFormattedGoldLeft() {
        if (isGoldReqMet()) {
            return "Met!";
        } else {
            return String.format("%,.0f", goldLeft); // Formats 15000 to 15,000
        }
    }

    /**
     * Helper Method: Returns XP proportion as a clean percentage (e.g. 85.5%)
     */
    public static String getXpPercentage() {
        return String.format("%.1f%%", xpProportion * 100);
    }
}