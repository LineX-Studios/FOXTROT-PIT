package com.linexstudios.foxtrot.Enemy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.linexstudios.foxtrot.Hud.EnemyHUD;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class EnemyManager {
    // Stores UUID -> Last Known Username
    public static Map<String, String> enemyCache = new HashMap<>();
    private static final File cacheFile = new File("config/Foxtrot/enemyuuid_cache.json");

    public static void loadCache() {
        try {
            if (cacheFile.exists()) {
                FileReader reader = new FileReader(cacheFile);
                enemyCache = new Gson().fromJson(reader, new TypeToken<HashMap<String, String>>(){}.getType());
                reader.close();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void saveCache() {
        try {
            cacheFile.getParentFile().mkdirs();
            FileWriter writer = new FileWriter(cacheFile);
            new GsonBuilder().setPrettyPrinting().create().toJson(enemyCache, writer);
            writer.close();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void addEnemy(String name) {
        // 1. If they are in the lobby right now, instantly grab their UUID (Works on Nicked players too!)
        if (Minecraft.getMinecraft().theWorld != null) {
            for (EntityPlayer p : Minecraft.getMinecraft().theWorld.playerEntities) {
                if (p.getName().equalsIgnoreCase(name)) {
                    enemyCache.put(p.getUniqueID().toString(), p.getName());
                    saveCache();
                    return;
                }
            }
        }

        // 2. If they are NOT in the lobby, silently ask Mojang's API for their real UUID in the background
        new Thread(() -> {
            try {
                URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + name);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                
                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                    reader.close();

                    // Parse basic JSON without heavy libraries
                    String json = response.toString();
                    if (json.contains("\"id\"") && json.contains("\"name\"")) {
                        String rawUuid = json.split("\"id\"\\s*:\\s*\"")[1].split("\"")[0];
                        String realName = json.split("\"name\"\\s*:\\s*\"")[1].split("\"")[0];
                        
                        // Mojang returns UUIDs without dashes. Java requires dashes. We inject them here.
                        String formattedUuid = rawUuid.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");
                        
                        enemyCache.put(formattedUuid, realName);
                        saveCache();
                    }
                }
            } catch (Exception e) {
                System.out.println("[Foxtrot] Failed to fetch UUID for " + name);
            }
        }).start();
    }

    public static void removeEnemy(String name) {
        enemyCache.entrySet().removeIf(entry -> entry.getValue().equalsIgnoreCase(name));
        saveCache();
    }

    // Fetches the expected UUID for a name to stop nicked spoofers
    public static String getUUIDFromName(String name) {
        for (Map.Entry<String, String> entry : enemyCache.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(name)) return entry.getKey();
        }
        return null;
    }
}