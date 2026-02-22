package com.linexstudios.foxtrot.Denick;

import com.linexstudios.foxtrot.Hud.EnemyHUD;
import com.linexstudios.foxtrot.Hud.NickedHUD;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AutoDenick {
    public static final AutoDenick instance = new AutoDenick();
    
    private static final String prefix = EnumChatFormatting.GRAY + "[" + EnumChatFormatting.RED + "Foxtrot" + EnumChatFormatting.GRAY + "] ";
    
    private static final Set<String> resolvingNicks = ConcurrentHashMap.newKeySet();
    private static final Set<String> notifiedScraping = new HashSet<>(); 
    private static final Map<String, Long> retryCooldowns = new ConcurrentHashMap<>(); 
    
    public static Minecraft mc = Minecraft.getMinecraft();
    public static Set<String> lastNickedSet = new HashSet<>();
    public static boolean enabled = true; 
    
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
                if (p == null) continue; // Waiting for them to render physically
                
                String currentStatus = NickedManager.getResolvedIGN(nick);
                boolean needsDenick = currentStatus == null || currentStatus.equals("Failed") || currentStatus.equals("No Nonce") || currentStatus.equals("Scraping");
                
                if (needsDenick) {
                    
                    // Fail-safe to clear deadlocked threads
                    if (resolvingNicks.contains(nick)) {
                        long lastAttempt = retryCooldowns.getOrDefault(nick, 0L);
                        if (System.currentTimeMillis() - lastAttempt > 15000) {
                             resolvingNicks.remove(nick); 
                        } else {
                             continue;
                        }
                    }

                    long lastAttempt = retryCooldowns.getOrDefault(nick, 0L);
                    
                    if (System.currentTimeMillis() - lastAttempt >= 20000) {
                        
                        try {
                            // --- SMART INVENTORY SCANNER ---
                            int foundNonce = -1;
                            List<ItemStack> itemsToCheck = new ArrayList<>();
                            
                            // 1. Prioritize checking the sword they are holding first
                            if (p.getHeldItem() != null) {
                                itemsToCheck.add(p.getHeldItem());
                            }
                            
                            // 2. Fallback to armor if the sword is fresh/invalid
                            if (p.inventory.armorInventory != null) {
                                for (ItemStack armorItem : p.inventory.armorInventory) {
                                    if (armorItem != null) {
                                        itemsToCheck.add(armorItem);
                                    }
                                }
                            }

                            for (ItemStack item : itemsToCheck) {
                                if (item != null && item.hasTagCompound()) {
                                    NBTTagCompound extra = item.getTagCompound().getCompoundTag("ExtraAttributes");
                                    
                                    // Item must have both a Nonce AND Custom Enchants (meaning it's not a fresh)
                                    if (extra != null && extra.hasKey("Nonce") && extra.hasKey("CustomEnchants")) {
                                        int nonce = extra.getInteger("Nonce");
                                        
                                        // Skip fresh nonces (0), dark pants (5, 6), and rage pants (9)
                                        if (nonce != 0 && nonce != 5 && nonce != 6 && nonce != 9) {
                                            foundNonce = nonce;
                                            break; // Stop looking, we found a valid item to scrape!
                                        }
                                    }
                                }
                            }
                            
                            // If they are only wearing fresh/rage/dark items and holding nothing good, skip
                            if (foundNonce == -1) {
                                NickedManager.updateNicked(nick, "No Nonce");
                                retryCooldowns.put(nick, System.currentTimeMillis());
                                continue; 
                            }
                            
                            // Step 2: Prepare for API Scrape
                            resolvingNicks.add(nick);
                            retryCooldowns.put(nick, System.currentTimeMillis()); 
                            
                            if (!NickedHUD.nickedPlayers.contains(nick.toLowerCase())) {
                                NickedHUD.nickedPlayers.add(nick.toLowerCase());
                            }
                            
                            if (!notifiedScraping.contains(nick)) {
                                sendChatMsg(prefix + EnumChatFormatting.YELLOW + "Scraping " + EnumChatFormatting.DARK_AQUA + "[" + EnumChatFormatting.AQUA + "N" + EnumChatFormatting.DARK_AQUA + "] " + EnumChatFormatting.AQUA + nick + EnumChatFormatting.YELLOW + "...");
                                notifiedScraping.add(nick);
                            }
                            
                            // Step 3: Run API Request 
                            final int finalNonce = foundNonce;
                            final long millisStarted = System.currentTimeMillis();
                            sendDebug("Found Valid Nonce (" + finalNonce + "). Resolving...");
                            
                            new Thread(() -> {
                                try {
                                    String realName = resolveOwnerFromNonce(finalNonce);
                                    long time = System.currentTimeMillis() - millisStarted;
                                    
                                    synchronized (CacheManager.class) {
                                        if (realName != null) {
                                            CacheManager.addToCache(nick, realName);
                                            NickedManager.updateNicked(nick, realName);
                                            
                                            sendChatMsg(prefix + EnumChatFormatting.GREEN + "Denicked " + EnumChatFormatting.GRAY + "\u27A1 " + EnumChatFormatting.DARK_AQUA + "[" + EnumChatFormatting.AQUA + "N" + EnumChatFormatting.DARK_AQUA + "] " + EnumChatFormatting.AQUA + nick + " " + EnumChatFormatting.GRAY + "\u2192 " + EnumChatFormatting.RESET + EnumChatFormatting.YELLOW + realName + " " + EnumChatFormatting.GRAY + "(" + EnumChatFormatting.WHITE + time + "ms" + EnumChatFormatting.GRAY + ")");
                                        } else {
                                            sendClickableManualLink(finalNonce);
                                            NickedManager.updateNicked(nick, "Failed");
                                        }
                                        
                                        mc.addScheduledTask(() -> {
                                            EntityPlayer targetEntity = mc.theWorld.getPlayerEntityByName(nick);
                                            if (targetEntity != null) {
                                                targetEntity.refreshDisplayName();
                                            }
                                        });
                                    }
                                } finally {
                                    resolvingNicks.remove(nick);
                                }
                            }).start();
                            
                        } catch (Exception e) {
                            sendDebug("Internal error while scanning inventory: " + e.getMessage());
                            retryCooldowns.put(nick, System.currentTimeMillis());
                        }
                    }
                }
            }
        }
        
        for (String name : currentNickedSet) {
            if (!lastNickedSet.contains(name)) {
                sendChatMsg(prefix + EnumChatFormatting.YELLOW + "\u26A0 " + EnumChatFormatting.GOLD + "Nicked Player Detected " + EnumChatFormatting.GRAY + "\u27A1 " + EnumChatFormatting.DARK_AQUA + "[" + EnumChatFormatting.AQUA + "N" + EnumChatFormatting.DARK_AQUA + "] " + EnumChatFormatting.AQUA + name);
            }
        }
        lastNickedSet.clear();
        lastNickedSet.addAll(currentNickedSet);
    }

    private static String resolveOwnerFromNonce(int nonce) {
        HttpURLConnection conn = null;
        try {
            String fullUrl = "https://pitpal.rocks/api/listings/items/nonce" + nonce;
            sendDebug("Requesting URL: " + fullUrl);

            URL url = new URL(fullUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);

            if (conn.getResponseCode() != 200) {
                sendDebug("PitPal API returned error code: " + conn.getResponseCode());
                return null;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();

            String response = sb.toString();
            if (response.isEmpty() || !response.startsWith("{")) return null;

            JSONObject root = new JSONObject(response);

            // Handle both "items" and "data" arrays based on PitPal's formatting
            JSONArray itemsArr = null;
            if (root.has("items") && !root.isNull("items")) {
                itemsArr = root.getJSONArray("items");
            } else if (root.has("data") && !root.isNull("data")) {
                itemsArr = root.getJSONArray("data");
            }

            if (itemsArr != null) {
                for (int i = 0; i < itemsArr.length(); i++) {
                    JSONObject itemObj = itemsArr.getJSONObject(i);

                    String ownerName = null;
                    String ownerId = null;

                    if (itemObj.has("ownerusername") && !itemObj.isNull("ownerusername")) {
                        String raw = itemObj.getString("ownerusername");
                        ownerName = raw.replaceAll("§.", "").replaceAll("\\$.", "").trim();
                    }

                    if (itemObj.has("ownerId") && !itemObj.isNull("ownerId")) {
                        ownerId = itemObj.getString("ownerId").trim();
                    } else if (itemObj.has("ownerid") && !itemObj.isNull("ownerid")) {
                        ownerId = itemObj.getString("ownerid").trim();
                    }

                    if (ownerName != null && !ownerName.isEmpty()) {
                        sendDebug("Found potential owner: " + ownerName + ". Verifying existence...");
                        
                        // VALIDATION PATCH: Checks if it's a fake/junk name
                        if (isValidMinecraftName(ownerName)) {
                            sendDebug("Resolved valid owner: " + ownerName);
                            return ownerName;
                        } else {
                            sendDebug("Invalid/Non-existent name found: " + ownerName);
                            ownerName = null;
                        }
                    }

                    if (ownerId != null && !ownerId.isEmpty()) {
                        String resolvedName = resolveUsernameFromUUID(ownerId);
                        if (resolvedName != null) {
                            sendDebug("Resolved UUID " + ownerId + " to IGN: " + resolvedName);
                            return resolvedName; 
                        } else {
                            sendDebug("Could not resolve UUID: " + ownerId);
                            return null;
                        }
                    }
                }
            }
        } catch (Exception e) {
            sendDebug("Network error: " + e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    private static boolean isValidMinecraftName(String name) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + name);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            return conn.getResponseCode() == 200;
        } catch (Exception e) {
            return false; 
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static String resolveUsernameFromUUID(String uuid) {
        HttpURLConnection conn = null;
        try {
            String fullUrl = "https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.replace("-", "");
            sendDebug("Resolving UUID via Mojang API: " + fullUrl);

            URL url = new URL(fullUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);

            if (conn.getResponseCode() != 200) {
                return null;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();

            String response = sb.toString();
            if (response.isEmpty() || !response.startsWith("{")) return null;

            JSONObject root = new JSONObject(response);
            if (root.has("name")) {
                return root.getString("name");
            }
        } catch (Exception e) {
            sendDebug("UUID resolution error: " + e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    private static void sendClickableManualLink(int nonce) {
        String url = "https://pitpal.rocks/api/listings/items/nonce" + nonce;
        ChatComponentText msg = new ChatComponentText(prefix + EnumChatFormatting.RED + "Auto-denick failed. " + EnumChatFormatting.YELLOW + EnumChatFormatting.BOLD + "[CLICK TO VIEW JSON]");

        ChatStyle style = new ChatStyle();
        style.setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
        style.setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(EnumChatFormatting.GRAY + "Open: " + url)));

        msg.setChatStyle(style);
        sendRawChatMessage(msg);
    }

    private static void sendChatMsg(String msg) {
        sendRawChatMessage(new ChatComponentText(msg));
    }

    private static void sendDebug(String msg) {
        if (EnemyHUD.debugMode) {
            sendRawChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "[DEBUG] " + msg));
        }
    }

    private static void sendRawChatMessage(IChatComponent component) {
        mc.addScheduledTask(() -> {
            if (mc.thePlayer != null) {
                mc.thePlayer.addChatMessage(component);
            }
        });
    }
}