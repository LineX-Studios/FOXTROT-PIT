package com.linexstudios.foxtrot.Denick;

import com.google.gson.*;
import com.linexstudios.foxtrot.Util.ItemScraper;
import com.linexstudios.foxtrot.Hud.NickedHUD;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AutoDenick {
    public static final AutoDenick instance = new AutoDenick();
    private static final Set<String> resolvingNicks = ConcurrentHashMap.newKeySet();
    private static final Set<String> notifiedScraping = new HashSet<>(); 
    
    // Tracks the exact time a player was last checked to enforce the 20-second retry loop
    private static final Map<String, Long> retryCooldowns = new ConcurrentHashMap<>(); 
    
    public static Minecraft mc = Minecraft.getMinecraft();
    public static Set<String> lastNickedSet = new HashSet<>();
    public static boolean enabled = true; 
    public static boolean debugMode = true; // KEEP TRUE FOR NOW
    
    private int tickTimer = 0;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!enabled || mc.theWorld == null || mc.getNetHandler() == null || event.phase != TickEvent.Phase.END) return;
        
        tickTimer++;
        if (tickTimer >= 40) { // Runs every 2 seconds
            tickTimer = 0;
            detectIfPlayerIsNicked();
        }
    }

    public static boolean isNicked(UUID playerUUID) {
        return playerUUID.version() == 1;
    }

    public static void detectIfPlayerIsNicked() {
        Set<String> currentNickedSet = new HashSet<>();
        
        for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
            if (info == null || info.getGameProfile() == null || info.getGameProfile().getId() == null) continue;
            
            UUID playerUUID = info.getGameProfile().getId();
            if (isNicked(playerUUID)) {
                String nick = info.getGameProfile().getName();
                currentNickedSet.add(nick);
                
                EntityPlayer p = mc.theWorld.getPlayerEntityByName(nick);
                if (p == null) {
                    if (debugMode && !notifiedScraping.contains(nick)) {
                        sendMessage(EnumChatFormatting.GRAY + "[Debug] " + nick + " is nicked but not rendered in world yet.");
                    }
                    continue; 
                }
                
                String currentStatus = NickedManager.getResolvedIGN(nick);
                boolean needsDenick = currentStatus == null || currentStatus.equals("Failed") || currentStatus.equals("No Nonce") || currentStatus.equals("Scraping...");
                
                if (needsDenick) {
                    if (resolvingNicks.contains(nick)) {
                        // It is deadlocked here. Let's force it out if it gets stuck for more than 10 seconds.
                        long lastAttempt = retryCooldowns.getOrDefault(nick, 0L);
                        if (System.currentTimeMillis() - lastAttempt > 10000) {
                             if(debugMode) sendMessage(EnumChatFormatting.GRAY + "[Debug] " + nick + " was deadlocked in resolvingNicks. Clearing deadlock.");
                             resolvingNicks.remove(nick);
                        } else {
                             continue; // Wait for the thread to finish
                        }
                    }

                    long lastAttempt = retryCooldowns.getOrDefault(nick, 0L);
                    long timeSinceLastAttempt = System.currentTimeMillis() - lastAttempt;

                    if (timeSinceLastAttempt >= 20000) {
                        
                        if(debugMode) sendMessage(EnumChatFormatting.GRAY + "[Debug] Attempting to scrape items for " + nick);
                        
                        ArrayList<Integer> nonces = ItemScraper.getNoncesFromPlayer(p);
                        
                        if(debugMode) sendMessage(EnumChatFormatting.GRAY + "[Debug] Found " + nonces.size() + " nonces on " + nick);
                        
                        if (nonces.isEmpty()) {
                            if(debugMode) sendMessage(EnumChatFormatting.GRAY + "[Debug] Scraper returned 0 nonces for " + nick + ". Setting status to No Nonce.");
                            NickedManager.updateNicked(nick, "No Nonce");
                            retryCooldowns.put(nick, System.currentTimeMillis());
                            continue; 
                        }
                        
                        resolvingNicks.add(nick);
                        retryCooldowns.put(nick, System.currentTimeMillis()); 
                        
                        if (!NickedHUD.nickedPlayers.contains(nick.toLowerCase())) {
                            NickedHUD.nickedPlayers.add(nick.toLowerCase());
                        }
                        
                        if (!notifiedScraping.contains(nick)) {
                            sendMessage(EnumChatFormatting.GRAY + "[" + EnumChatFormatting.RED + "Foxtrot" + EnumChatFormatting.GRAY + "] " + EnumChatFormatting.YELLOW + "Scraping " + EnumChatFormatting.DARK_AQUA + "[" + EnumChatFormatting.AQUA + "N" + EnumChatFormatting.DARK_AQUA + "] " + EnumChatFormatting.AQUA + nick + EnumChatFormatting.YELLOW + "...");
                            notifiedScraping.add(nick);
                        }
                        
                        new Thread(() -> {
                            String realName = null;
                            long millisStarted = System.currentTimeMillis();
                            try {
                                realName = tryToResolveNick(nick, nonces);
                            } catch (Exception e) {
                                if(debugMode) sendMessage(EnumChatFormatting.GRAY + "[Debug] Exception in thread for " + nick + ": " + e.getMessage());
                            } finally {
                                resolvingNicks.remove(nick); 
                            }
                            
                            long time = System.currentTimeMillis() - millisStarted;
                            final String finalRealName = realName;
                            
                            synchronized (CacheManager.class) {
                                if (finalRealName != null) {
                                    CacheManager.addToCache(nick, finalRealName);
                                    NickedManager.updateNicked(nick, finalRealName);
                                    sendMessage(EnumChatFormatting.GRAY + "[" + EnumChatFormatting.RED + "Foxtrot" + EnumChatFormatting.GRAY + "] " + EnumChatFormatting.GREEN + "Denicked " + EnumChatFormatting.DARK_GRAY + "\u00BB " + EnumChatFormatting.DARK_AQUA + "[" + EnumChatFormatting.AQUA + "N" + EnumChatFormatting.DARK_AQUA + "] " + EnumChatFormatting.AQUA + nick + " " + EnumChatFormatting.GRAY + "\u2192 " + EnumChatFormatting.RESET + EnumChatFormatting.YELLOW + finalRealName + " " + EnumChatFormatting.GRAY + "(" + EnumChatFormatting.WHITE + time + "ms" + EnumChatFormatting.GRAY + ")");
                                } else {
                                    if(debugMode) sendMessage(EnumChatFormatting.GRAY + "[Debug] Failed to resolve a real name for " + nick);
                                    NickedManager.updateNicked(nick, "Failed");
                                }
                                
                                mc.addScheduledTask(() -> {
                                    EntityPlayer targetEntity = mc.theWorld.getPlayerEntityByName(nick);
                                    if (targetEntity != null) {
                                        targetEntity.refreshDisplayName();
                                    }
                                });
                            }
                        }).start();
                    } else {
                        if (debugMode && !notifiedScraping.contains(nick + "_cooldown")) {
                             sendMessage(EnumChatFormatting.GRAY + "[Debug] " + nick + " is on a 20s cooldown. " + (20000 - timeSinceLastAttempt)/1000 + "s remaining.");
                             notifiedScraping.add(nick + "_cooldown");
                        }
                    }
                }
            }
        }
        
        for (String name : currentNickedSet) {
            if (!lastNickedSet.contains(name)) {
                sendMessage(EnumChatFormatting.GRAY + "[" + EnumChatFormatting.RED + "Foxtrot" + EnumChatFormatting.GRAY + "] " + EnumChatFormatting.YELLOW + "\u26A0 " + EnumChatFormatting.GOLD + "Nicked Player Detected " + EnumChatFormatting.DARK_GRAY + "\u00BB " + EnumChatFormatting.DARK_AQUA + "[" + EnumChatFormatting.AQUA + "N" + EnumChatFormatting.DARK_AQUA + "] " + EnumChatFormatting.AQUA + name);
            }
        }
        lastNickedSet.clear();
        lastNickedSet.addAll(currentNickedSet);
    }

    public static String tryToResolveNick(String nickedName, ArrayList<Integer> nonceList) {
        Set<String> UUIDS = new HashSet<>();
        
        for (int nonce : nonceList) {
            if(debugMode) sendMessage(EnumChatFormatting.GRAY + "[Debug] Looking up owner for Nonce: " + nonce);
            String UUID = getUUIDFromNonce(nonce);
            if (UUID != null) {
                if(debugMode) sendMessage(EnumChatFormatting.GRAY + "[Debug] Found UUID: " + UUID + " from Nonce: " + nonce);
                UUIDS.add(UUID);
            }
        }
        
        if (!UUIDS.isEmpty()) {
            for (String uuid : UUIDS) {
                if(debugMode) sendMessage(EnumChatFormatting.GRAY + "[Debug] Attempting to convert UUID to Name: " + uuid);
                String realName = getNameFromUUID(uuid);
                if (realName != null) return realName;
            }
        } else {
            if(debugMode) sendMessage(EnumChatFormatting.GRAY + "[Debug] No UUIDs could be found from the nonces.");
        }
        return null; 
    }

    public static String getNameFromUUID(String UUID) {
        String cleanUUID = UUID.replace("-", "");

        // 1. Try Mojang API First
        try {
            String mojangResponse = fetchJson("https://sessionserver.mojang.com/session/minecraft/profile/" + cleanUUID);
            if (mojangResponse != null && !mojangResponse.isEmpty()) {
                JsonObject json = getJsonObject(mojangResponse);
                if (json.has("name")) {
                    if(debugMode) sendMessage(EnumChatFormatting.GRAY + "[Debug] Mojang API success: " + json.get("name").getAsString());
                    return json.get("name").getAsString();
                }
            }
        } catch (Exception e) {}

        // 2. Try Pit Panda Fallback
        try {
            String pitPandaResponse = fetchJson("https://pitpanda.rocks/api/players/" + cleanUUID);
            if (pitPandaResponse != null && !pitPandaResponse.isEmpty()) {
                JsonObject json = getJsonObject(pitPandaResponse);
                if (json.has("data") && !json.get("data").isJsonNull()) {
                    JsonObject data = json.getAsJsonObject("data");
                    if (data.has("name") && !data.get("name").isJsonNull()) {
                        if(debugMode) sendMessage(EnumChatFormatting.GRAY + "[Debug] PitPanda API success: " + data.get("name").getAsString());
                        return data.get("name").getAsString();
                    }
                }
            }
        } catch (Exception e) {}

        // 3. Try Pitmart Fallback
        try {
            String pitMartResponse = fetchJson("https://pitmart.net/api/player/" + cleanUUID);
            if (pitMartResponse != null && !pitMartResponse.isEmpty()) {
                JsonObject json = getJsonObject(pitMartResponse);
                if (json.has("username") && !json.get("username").isJsonNull()) {
                    if(debugMode) sendMessage(EnumChatFormatting.GRAY + "[Debug] Pitmart API success: " + json.get("username").getAsString());
                    return json.get("username").getAsString(); 
                }
            }
        } catch (Exception e) {}
        
        return null;
    }

    public static String getUUIDFromNonce(int nonce) {
        // Try PitPal API
        try {
            String pitPalURL = "https://pitpal.rocks/api/listings/items/nonce" + nonce;
            String pitPalResponse = fetchJson(pitPalURL);
            if (pitPalResponse != null) {
                JsonElement element = new JsonParser().parse(pitPalResponse);
                if (element.isJsonArray() && element.getAsJsonArray().size() > 0) {
                    JsonObject first = element.getAsJsonArray().get(0).getAsJsonObject();
                    if (first.has("uuid")) return first.get("uuid").getAsString();
                    if (first.has("owner_uuid")) return first.get("owner_uuid").getAsString();
                } else if (element.isJsonObject()) {
                    JsonObject first = element.getAsJsonObject();
                    if (first.has("uuid")) return first.get("uuid").getAsString();
                    if (first.has("owner_uuid")) return first.get("owner_uuid").getAsString();
                }
            }
        } catch (Exception e) {}

        // Try Pitmart API
        try {
            String apiURL = "https://pitmart.net/api/searchitems?nonce=" + nonce;
            String response = fetchJson(apiURL);
            if (response != null) {
                JsonArray docs = getJsonObject(response).getAsJsonArray("docs");
                if (docs.size() > 0) {
                    JsonElement ownerUUID = docs.get(0).getAsJsonObject().get("ownerUuid");
                    if (ownerUUID != null) return ownerUUID.getAsString();
                }
            }
        } catch (Exception e) {}
        
        return null;
    }

    private static JsonObject getJsonObject(String response) { return new JsonParser().parse(response).getAsJsonObject(); }

    private static String fetchJson(String urlString) throws IOException {
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) new URL(urlString).openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("User-Agent", "Mozilla/5.0"); 
            con.setConnectTimeout(3000); 
            con.setReadTimeout(3000);

            if (con.getResponseCode() != 200) {
                if(debugMode) sendMessage(EnumChatFormatting.GRAY + "[Debug] HTTP " + con.getResponseCode() + " for URL: " + urlString);
                return null;
            }

            try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                StringBuilder content = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) content.append(inputLine);
                return content.toString();
            }
        } finally {
            if (con != null) con.disconnect();
        }
    }

    private static void sendMessage(String text) {
        if (mc.thePlayer != null) mc.thePlayer.addChatMessage(new ChatComponentText(text));
    }
}