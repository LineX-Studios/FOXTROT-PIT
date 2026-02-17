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
    
    private int tickTimer = 0;
    private int heartbeatTimer = 0; // NEW: Tracks the 10-second heartbeat

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!enabled || mc.theWorld == null || mc.getNetHandler() == null || event.phase != TickEvent.Phase.END) return;
        
        // --- HEARTBEAT LOGIC ---
        heartbeatTimer++;
        if (heartbeatTimer >= 200) { // 200 ticks = 10 seconds
            sendMessage(EnumChatFormatting.DARK_GRAY + "[Debug] Heartbeat: AutoDenick is actively scanning...");
            heartbeatTimer = 0;
        }

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
                    if (!notifiedScraping.contains(nick + "_null")) {
                        sendMessage(EnumChatFormatting.DARK_GRAY + "[Debug] " + nick + " is in Tab but NOT in the World. Waiting for entity to render...");
                        notifiedScraping.add(nick + "_null");
                    }
                    continue; 
                }
                
                String currentStatus = NickedManager.getResolvedIGN(nick);
                
                boolean needsDenick = currentStatus == null || currentStatus.equals("Failed") || currentStatus.equals("No Nonce") || currentStatus.equals("Scraping...");
                
                if (needsDenick) {
                    // Fail-safe: Prevents the background thread from deadlocking indefinitely
                    if (resolvingNicks.contains(nick)) {
                        long lastAttempt = retryCooldowns.getOrDefault(nick, 0L);
                        if (System.currentTimeMillis() - lastAttempt > 15000) {
                             sendMessage(EnumChatFormatting.DARK_GRAY + "[Debug] Thread deadlocked for " + nick + ". Force clearing.");
                             resolvingNicks.remove(nick);
                        } else {
                             continue;
                        }
                    }

                    long lastAttempt = retryCooldowns.getOrDefault(nick, 0L);
                    
                    if (System.currentTimeMillis() - lastAttempt >= 20000) {
                        
                        ArrayList<Integer> nonces = ItemScraper.getNoncesFromPlayer(p);
                        
                        sendMessage(EnumChatFormatting.DARK_GRAY + "[Debug] Scraped " + nonces.size() + " nonces from " + nick);
                        
                        if (nonces.isEmpty()) {
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
                            // PRESERVED FORMATTING
                            sendMessage(EnumChatFormatting.GRAY + "[" + EnumChatFormatting.RED + "Foxtrot" + EnumChatFormatting.GRAY + "] " + EnumChatFormatting.YELLOW + "Scraping " + EnumChatFormatting.DARK_AQUA + "[" + EnumChatFormatting.AQUA + "N" + EnumChatFormatting.DARK_AQUA + "] " + EnumChatFormatting.AQUA + nick + EnumChatFormatting.YELLOW + "...");
                            notifiedScraping.add(nick);
                        }
                        
                        new Thread(() -> {
                            String realName = null;
                            long millisStarted = System.currentTimeMillis();
                            try {
                                realName = tryToResolveNick(nick, nonces);
                            } catch (Exception e) {
                                sendMessage(EnumChatFormatting.DARK_GRAY + "[Debug] Thread Crash: " + e.getMessage());
                            } finally {
                                resolvingNicks.remove(nick); 
                            }
                            
                            long time = System.currentTimeMillis() - millisStarted;
                            final String finalRealName = realName;
                            
                            synchronized (CacheManager.class) {
                                if (finalRealName != null) {
                                    CacheManager.addToCache(nick, finalRealName);
                                    NickedManager.updateNicked(nick, finalRealName);
                                    
                                    // PRESERVED FORMATTING + NEW ARROW
                                    sendMessage(EnumChatFormatting.GRAY + "[" + EnumChatFormatting.RED + "Foxtrot" + EnumChatFormatting.GRAY + "] " + EnumChatFormatting.GREEN + "Denicked " + EnumChatFormatting.GRAY + "\u27A1 " + EnumChatFormatting.DARK_AQUA + "[" + EnumChatFormatting.AQUA + "N" + EnumChatFormatting.DARK_AQUA + "] " + EnumChatFormatting.AQUA + nick + " " + EnumChatFormatting.GRAY + "\u2192 " + EnumChatFormatting.RESET + EnumChatFormatting.YELLOW + finalRealName + " " + EnumChatFormatting.GRAY + "(" + EnumChatFormatting.WHITE + time + "ms" + EnumChatFormatting.GRAY + ")");
                                } else {
                                    sendMessage(EnumChatFormatting.DARK_GRAY + "[Debug] Failed to resolve a real name for " + nick);
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
                    }
                }
            }
        }
        
        for (String name : currentNickedSet) {
            if (!lastNickedSet.contains(name)) {
                // PRESERVED FORMATTING + NEW ARROW
                sendMessage(EnumChatFormatting.GRAY + "[" + EnumChatFormatting.RED + "Foxtrot" + EnumChatFormatting.GRAY + "] " + EnumChatFormatting.YELLOW + "\u26A0 " + EnumChatFormatting.GOLD + "Nicked Player Detected " + EnumChatFormatting.GRAY + "\u27A1 " + EnumChatFormatting.DARK_AQUA + "[" + EnumChatFormatting.AQUA + "N" + EnumChatFormatting.DARK_AQUA + "] " + EnumChatFormatting.AQUA + name);
            }
        }
        lastNickedSet.clear();
        lastNickedSet.addAll(currentNickedSet);
    }

    public static String tryToResolveNick(String nickedName, ArrayList<Integer> nonceList) {
        Set<String> UUIDS = new HashSet<>();
        
        for (int nonce : nonceList) {
            sendMessage(EnumChatFormatting.DARK_GRAY + "[Debug] Requesting API for Nonce: " + nonce);
            String UUID = getUUIDFromNonce(nonce);
            if (UUID != null) {
                sendMessage(EnumChatFormatting.DARK_GRAY + "[Debug] Target Hit -> UUID: " + UUID);
                UUIDS.add(UUID);
            }
        }
        
        if (!UUIDS.isEmpty()) {
            for (String uuid : UUIDS) {
                sendMessage(EnumChatFormatting.DARK_GRAY + "[Debug] Converting UUID " + uuid + " to IGN...");
                String realName = getNameFromUUID(uuid);
                if (realName != null) return realName;
            }
        } else {
            sendMessage(EnumChatFormatting.DARK_GRAY + "[Debug] APIs returned null for all nonces.");
        }
        return null; 
    }

    public static String getNameFromUUID(String UUID) {
        String cleanUUID = UUID.replace("-", "");

        try {
            String mojangResponse = fetchJson("https://sessionserver.mojang.com/session/minecraft/profile/" + cleanUUID);
            if (mojangResponse != null && !mojangResponse.isEmpty()) {
                JsonObject json = getJsonObject(mojangResponse);
                if (json.has("name")) {
                    sendMessage(EnumChatFormatting.DARK_GRAY + "[Debug] Mojang API Match -> " + json.get("name").getAsString());
                    return json.get("name").getAsString();
                }
            }
        } catch (Exception e) {}

        try {
            String pitPandaResponse = fetchJson("https://pitpanda.rocks/api/players/" + cleanUUID);
            if (pitPandaResponse != null && !pitPandaResponse.isEmpty()) {
                JsonObject json = getJsonObject(pitPandaResponse);
                if (json.has("data") && !json.get("data").isJsonNull()) {
                    JsonObject data = json.getAsJsonObject("data");
                    if (data.has("name") && !data.get("name").isJsonNull()) {
                        sendMessage(EnumChatFormatting.DARK_GRAY + "[Debug] PitPanda API Match -> " + data.get("name").getAsString());
                        return data.get("name").getAsString();
                    }
                }
            }
        } catch (Exception e) {}

        try {
            String pitMartResponse = fetchJson("https://pitmart.net/api/player/" + cleanUUID);
            if (pitMartResponse != null && !pitMartResponse.isEmpty()) {
                JsonObject json = getJsonObject(pitMartResponse);
                if (json.has("username") && !json.get("username").isJsonNull()) {
                    sendMessage(EnumChatFormatting.DARK_GRAY + "[Debug] Pitmart API Match -> " + json.get("username").getAsString());
                    return json.get("username").getAsString(); 
                }
            }
        } catch (Exception e) {}
        
        return null;
    }

    public static String getUUIDFromNonce(int nonce) {
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

    private static String fetchJson(String urlString) {
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) new URL(urlString).openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("User-Agent", "Mozilla/5.0"); 
            con.setConnectTimeout(3000); 
            con.setReadTimeout(3000);

            if (con.getResponseCode() != 200) {
                sendMessage(EnumChatFormatting.DARK_GRAY + "[Debug] Failed HTTP " + con.getResponseCode() + " on " + new URL(urlString).getHost());
                return null;
            }

            try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                StringBuilder content = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) content.append(inputLine);
                return content.toString();
            }
        } catch (Exception e) {
            sendMessage(EnumChatFormatting.DARK_GRAY + "[Debug] Disconnected from " + urlString);
            return null;
        } finally {
            if (con != null) con.disconnect();
        }
    }

    private static void sendMessage(String text) {
        if (mc.thePlayer != null) mc.thePlayer.addChatMessage(new ChatComponentText(text));
    }
}