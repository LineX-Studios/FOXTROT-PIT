package com.linexstudios.foxtrot.Denick;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AutoDenick {
    public static boolean enabled = false;
    private final Minecraft mc = Minecraft.getMinecraft();
    
    // Using a fixed pool to prevent infinite thread creation while keeping the UI lag-free
    private static final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final String prefix = EnumChatFormatting.GRAY.toString() + "[" + EnumChatFormatting.RED.toString() + "PIT" + EnumChatFormatting.GRAY.toString() + "] ";

    // Tracks players processed in the current session to avoid spamming APIs
    private final Set<String> processed = new HashSet<>();

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        // Ensure we only run once per tick at the end
        if (!enabled || mc.theWorld == null || mc.thePlayer == null || event.phase != TickEvent.Phase.END) return;

        NetHandlerPlayClient netHandler = mc.getNetHandler();
        if (netHandler == null) return;

        List<EntityOtherPlayerMP> nickedPlayers = new ArrayList<>();
        
        // Scan the Tab List (NetworkPlayerInfo) for potential nicks
        for (NetworkPlayerInfo info : netHandler.getPlayerInfoMap()) {
            EntityOtherPlayerMP other = new EntityOtherPlayerMP(mc.theWorld, info.getGameProfile());
            
            // UUID Version 2 check identifies many nicks/NPCs
            if (other.getUniqueID().version() == 2 && !processed.contains(other.getName())) {
                nickedPlayers.add(other);
            }
        }

        if (!nickedPlayers.isEmpty()) {
            sendMessage(EnumChatFormatting.RED.toString() + EnumChatFormatting.BOLD.toString() + "------ Auto Denicker Started ------");

            int index = 1;
            for (EntityOtherPlayerMP target : nickedPlayers) {
                int nonce = findNonce(target);
                final String nick = target.getName();
                
                // Mark as processed immediately so we don't scan them again next tick
                processed.add(nick);

                if (!isValidNonce(nonce)) {
                    sendMessage(EnumChatFormatting.WHITE.toString() + index + " - No items found on " +
                            EnumChatFormatting.GRAY.toString() + nick + EnumChatFormatting.WHITE.toString() + ", skipping.");
                } else {
                    final int finalIndex = index;
                    final int finalNonce = nonce;

                    // Move the heavy network work to the background thread
                    executor.submit(() -> {
                        String resolved = resolveOwnerFromNonce(finalNonce);
                        if (resolved != null) {
                            // Link found: update the manager and notify the user
                            NickedManager.addNicked(nick, resolved);
                            sendMessage(EnumChatFormatting.WHITE.toString() + finalIndex + " - " +
                                    EnumChatFormatting.YELLOW.toString() + nick + EnumChatFormatting.GRAY.toString() + " → " +
                                    EnumChatFormatting.GREEN.toString() + resolved);
                        } else {
                            sendMessage(EnumChatFormatting.WHITE.toString() + finalIndex + " - Failed to resolve " +
                                    EnumChatFormatting.YELLOW.toString() + nick);
                        }
                    });
                }
                index++;
            }
        }
    }

    private boolean isValidNonce(int nonce) {
        if (nonce <= 0) return false;
        return String.valueOf(nonce).matches("\\d{9}"); // Must be exactly 9 digits
    }

    private int findNonce(EntityOtherPlayerMP target) {
        List<ItemStack> items = new ArrayList<>();
        Collections.addAll(items, target.inventory.armorInventory);
        Collections.addAll(items, target.inventory.mainInventory);

        for (ItemStack item : items) {
            if (item != null && item.hasTagCompound()) {
                NBTTagCompound extra = item.getTagCompound().getCompoundTag("ExtraAttributes");
                if (extra != null && extra.hasKey("Nonce")) {
                    int nonce = extra.getInteger("Nonce");
                    if (nonce > 0) return nonce;
                }
            }
        }
        return -1;
    }

    private String resolveOwnerFromNonce(int nonce) {
        HttpURLConnection conn = null;
        try {
            // Updated URL with correct endpoint
            URL url = new URL("https://pitpal.rocks/api/listings/items/nonce" + nonce);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);

            if (conn.getResponseCode() != 200) return null;

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();

            JSONObject root = new JSONObject(sb.toString());
            // Support both "items" and "data" keys for API flexibility
            JSONArray itemsArr = root.has("items") ? root.getJSONArray("items") :
                                 root.has("data") ? root.getJSONArray("data") : null;

            if (itemsArr != null && itemsArr.length() > 0) {
                for (int i = 0; i < itemsArr.length(); i++) {
                    JSONObject itemObj = itemsArr.getJSONObject(i);

                    // Strategy 1: Direct Username
                    if (itemObj.has("ownerusername") && !itemObj.isNull("ownerusername")) {
                        String raw = itemObj.getString("ownerusername");
                        return raw.replaceAll("§.", "").replaceAll("\\$.", "").trim();
                    }
                    
                    // Strategy 2: UUID Lookup (Fallback)
                    if (itemObj.has("ownerId") && !itemObj.isNull("ownerId")) {
                        String ownerId = itemObj.getString("ownerId").trim();
                        String resolvedName = resolveUsernameFromUUID(ownerId);
                        if (resolvedName != null) return resolvedName;
                    }
                }
            }
        } catch (Exception e) {
            // Handled internally to prevent thread crash
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    private String resolveUsernameFromUUID(String uuid) {
        HttpURLConnection conn = null;
        try {
            String fullUrl = "https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.replace("-", "");
            URL url = new URL(fullUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(5000);

            if (conn.getResponseCode() != 200) return null;

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();

            JSONObject root = new JSONObject(sb.toString());
            if (root.has("name")) return root.getString("name");
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    private void sendMessage(String msg) {
        // Essential: Run chat messages on the Main Thread to avoid concurrent modification errors
        mc.addScheduledTask(() -> {
            if (mc.thePlayer != null) {
                mc.thePlayer.addChatMessage(new ChatComponentText(prefix + msg));
            }
        });
    }
}