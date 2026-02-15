package com.linexstudios.foxtrot.Denick;

import com.google.gson.*;
import com.linexstudios.foxtrot.Util.ItemScraper;
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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoDenick {
    public static final AutoDenick instance = new AutoDenick();
    private static final Set<String> resolvingNicks = ConcurrentHashMap.newKeySet();
    private static final Set<String> notifiedScraping = new HashSet<>(); 
    
    public static Minecraft mc = Minecraft.getMinecraft();
    public static Set<String> lastNickedSet = new HashSet<>();
    public static boolean enabled = true; 
    private int tickTimer = 0;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!enabled || mc.theWorld == null || mc.getNetHandler() == null || event.phase != TickEvent.Phase.END) return;
        
        tickTimer++;
        if (tickTimer >= 40) { 
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
                
                // FIXED BUG: Don't scrape unless they are physically spawned in and wearing armor!
                EntityPlayer p = mc.theWorld.getPlayerEntityByName(nick);
                if (p == null) continue; 
                ArrayList<Integer> nonces = ItemScraper.getNoncesFromPlayer(p);
                if (nonces.isEmpty()) continue; 
                
                if (!CacheManager.nickInCache(nick) && !resolvingNicks.contains(nick)) {
                    resolvingNicks.add(nick);
                    
                    if (!notifiedScraping.contains(nick)) {
                        sendMessage(EnumChatFormatting.GRAY + "[" + EnumChatFormatting.GOLD + "Foxtrot" + EnumChatFormatting.GRAY + "] " + EnumChatFormatting.YELLOW + "Scraping " + EnumChatFormatting.AQUA + nick + EnumChatFormatting.YELLOW + "...");
                        notifiedScraping.add(nick);
                    }
                    
                    new Thread(() -> {
                        String realName = null;
                        long millisStarted = System.currentTimeMillis();
                        try {
                            realName = tryToResolveNick(nick);
                        } catch (Exception e) {
                            // Silent fail, will retry
                        } finally {
                            resolvingNicks.remove(nick); 
                        }
                        
                        long time = System.currentTimeMillis() - millisStarted;
                        
                        if (realName != null) {
                            synchronized (CacheManager.class) {
                                if (!CacheManager.nickInCache(nick)) {
                                    CacheManager.addToCache(nick, realName);
                                    NickedManager.updateNicked(nick, realName);
                                    
                                    sendMessage(EnumChatFormatting.GRAY + "[" + EnumChatFormatting.GOLD + "Foxtrot" + EnumChatFormatting.GRAY + "] " + EnumChatFormatting.GREEN + "Denicked " + EnumChatFormatting.DARK_GRAY + "\u00bb " + EnumChatFormatting.AQUA + nick + " " + EnumChatFormatting.GRAY + "\u2192 " + EnumChatFormatting.RESET + " " + EnumChatFormatting.YELLOW + realName + " " + EnumChatFormatting.GRAY + "(" + EnumChatFormatting.WHITE + time + "ms" + EnumChatFormatting.GRAY + ")");
                                    
                                    // FIXED BUG: Forces the vanilla game to instantly re-draw their overhead Nametag!
                                    EntityPlayer targetEntity = mc.theWorld.getPlayerEntityByName(nick);
                                    if (targetEntity != null) {
                                        targetEntity.refreshDisplayName();
                                    }
                                }
                            }
                        }
                    }).start();
                }
            }
        }
        
        for (String name : currentNickedSet) {
            if (!lastNickedSet.contains(name)) {
                sendMessage(EnumChatFormatting.GRAY + "[" + EnumChatFormatting.GOLD + "Foxtrot" + EnumChatFormatting.GRAY + "] " + EnumChatFormatting.WHITE + "Nicked Player Detected " + EnumChatFormatting.DARK_GRAY + "\u00bb " + EnumChatFormatting.AQUA + name);
            }
        }
        lastNickedSet.clear();
        lastNickedSet.addAll(currentNickedSet);
    }

    public static String tryToResolveNick(String nickedName) throws IOException {
        EntityPlayer player = mc.theWorld.getPlayerEntityByName(nickedName);
        if (player != null) {
            ArrayList<Integer> nonceList = ItemScraper.getNoncesFromPlayer(player);
            Set<String> UUIDS = new HashSet<>();
            for (int nonce : nonceList) {
                String UUID = getUUIDFromNonce(nonce);
                if (UUID != null) {
                    UUIDS.add(UUID);
                }
            }
            
            // FIXED BUG: No longer strict `UUIDS.size() == 1`. 
            // It now flawlessly loops through every piece of armor even if they wear mixed sets!
            if (!UUIDS.isEmpty()) {
                for (String uuid : UUIDS) {
                    if (isPlayerAbleToNick(uuid)) {
                        String realName = getNameFromUUID(uuid);
                        if (realName != null) return realName;
                    }
                }
            }
        }
        return null; 
    }

    public static boolean isPlayerAbleToNick(String UUID) {
        try {
            String pitMartResponse = fetchJson("https://pitmart.net/api/player/" + UUID);
            if (pitMartResponse != null) {
                return "SUPERSTAR".equals(getJsonObject(pitMartResponse).getAsJsonObject("player").get("rank").getAsString());
            }
            
            String pandaResponse = fetchJson("https://pitpanda.rocks/api/username/" + UUID);
            if (pandaResponse != null) {
                Matcher m = Pattern.compile(EnumChatFormatting.GREEN + "(\\w+)\\s*$").matcher(getJsonObject(pandaResponse).get("name").getAsString());
                if (m.find()) return true;
            }
            
            String pitPalResponse = fetchJson("https://pitpal.me/api/player/" + UUID);
            if (pitPalResponse != null) {
                JsonObject json = getJsonObject(pitPalResponse);
                String nameString = json.has("name") ? json.get("name").getAsString() : (json.has("data") ? json.getAsJsonObject("data").get("name").getAsString() : "");
                Matcher m = Pattern.compile(EnumChatFormatting.GREEN + "(\\w+)\\s*$").matcher(nameString);
                if (m.find()) return true;
            }
        } catch (Exception e) {}
        return false;
    }

    public static String getNameFromUUID(String UUID) {
        try {
            String mojangResponse = fetchJson("https://api.mojang.com/user/profile/" + UUID);
            if (mojangResponse != null) return getJsonObject(mojangResponse).get("name").getAsString();

            String pandaResponse = fetchJson("https://pitpanda.rocks/api/username/" + UUID);
            if (pandaResponse != null) {
                Matcher m = Pattern.compile("\\s\u00a7.(\\w+)").matcher(getJsonObject(pandaResponse).get("name").getAsString());
                if (m.find()) return m.group(1);
            }
            
            String pitPalResponse = fetchJson("https://pitpal.me/api/player/" + UUID);
            if (pitPalResponse != null) {
                JsonObject json = getJsonObject(pitPalResponse);
                String nameString = json.has("name") ? json.get("name").getAsString() : (json.has("data") ? json.getAsJsonObject("data").get("name").getAsString() : "");
                Matcher m = Pattern.compile("\\s\u00a7.(\\w+)").matcher(nameString);
                if (m.find()) return m.group(1);
            }

            String pitMartResponse = fetchJson("https://pitmart.net/api/player/" + UUID);
            if (pitMartResponse != null) return getJsonObject(pitMartResponse).getAsJsonObject("player").get("username").getAsString();
        } catch (Exception e) {}
        return null;
    }

    public static String getUUIDFromNonce(int nonce) {
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

            if (con.getResponseCode() != 200) return null;

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