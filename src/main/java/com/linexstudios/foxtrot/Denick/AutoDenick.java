package com.linexstudios.foxtrot.Denick;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
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
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private final String prefix = EnumChatFormatting.GRAY.toString() + "[" + EnumChatFormatting.RED.toString() + "PIT" + EnumChatFormatting.GRAY.toString() + "] ";

    // Players we've already attempted to denick this session
    private final Set<String> processed = new HashSet<>();

    @SubscribeEvent
    public void onWorldJoin(RenderGameOverlayEvent.Post event) {
        if (!enabled || mc.theWorld == null || event.type != RenderGameOverlayEvent.ElementType.TEXT) return;

        NetHandlerPlayClient netHandler = mc.getNetHandler();
        if (netHandler == null) return;

        // Collect nicked players that haven't been processed yet
        List<EntityOtherPlayerMP> nickedPlayers = new ArrayList<>();
        for (NetworkPlayerInfo info : netHandler.getPlayerInfoMap()) {
            EntityOtherPlayerMP other = new EntityOtherPlayerMP(mc.theWorld, info.getGameProfile());
            if (other.getUniqueID().version() == 2 && !processed.contains(other.getName())) {
                nickedPlayers.add(other);
            }
        }

        if (!nickedPlayers.isEmpty()) {
            // Single header (red + bold)
            sendMessage(EnumChatFormatting.RED.toString() + EnumChatFormatting.BOLD.toString() + "------ Auto Denicker Started ------");

            int index = 1;
            for (EntityOtherPlayerMP target : nickedPlayers) {
                int nonce = findNonce(target);

                if (!isValidNonce(nonce)) {
                    // White index, gray name, white trailing text
                    sendMessage(EnumChatFormatting.WHITE.toString() + index + " - No denickable items found on " +
                            EnumChatFormatting.GRAY.toString() + target.getName() + EnumChatFormatting.WHITE.toString() + ", skipping.");
                } else {
                    final String nick = target.getName();
                    processed.add(nick);
                    final int finalIndex = index;
                    final int finalNonce = nonce;

                    // Resolve in background
                    executor.submit(() -> {
                        String resolved = resolveOwnerFromNonce(finalNonce);
                        if (resolved != null) {
                            // Add to NickedManager (assumes that class exists)
                            NickedManager.addNicked(nick, resolved);
                            sendMessage(EnumChatFormatting.WHITE.toString() + finalIndex + " - " +
                                    EnumChatFormatting.YELLOW.toString() + nick + EnumChatFormatting.GRAY.toString() + " → " +
                                    EnumChatFormatting.GREEN.toString() + resolved);
                        } else {
                            sendMessage(EnumChatFormatting.WHITE.toString() + finalIndex + " - Failed to auto-denick " +
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
        String s = String.valueOf(nonce);
        return s.matches("\\d{9}"); // exactly 9 digits
    }

    private int findNonce(EntityOtherPlayerMP target) {
        int foundNonce = -1;
        List<net.minecraft.item.ItemStack> items = new ArrayList<>();
        Collections.addAll(items, target.inventory.armorInventory);
        Collections.addAll(items, target.inventory.mainInventory);

        for (net.minecraft.item.ItemStack item : items) {
            if (item != null && item.hasTagCompound()) {
                net.minecraft.nbt.NBTTagCompound extra = item.getTagCompound().getCompoundTag("ExtraAttributes");
                if (extra != null && extra.hasKey("Nonce")) {
                    int nonce = extra.getInteger("Nonce");
                    if (nonce > 0) {
                        foundNonce = nonce;
                        break;
                    }
                }
            }
        }
        return foundNonce;
    }

    private String resolveOwnerFromNonce(int nonce) {
        HttpURLConnection conn = null;
        try {
            String fullUrl = "https://pitpal.rocks/api/listings/items/nonce" + nonce;
            URL url = new URL(fullUrl);
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
            JSONArray itemsArr = root.has("items") ? root.getJSONArray("items") :
                                 root.has("data") ? root.getJSONArray("data") : null;

            if (itemsArr != null) {
                for (int i = 0; i < itemsArr.length(); i++) {
                    JSONObject itemObj = itemsArr.getJSONObject(i);

                    if (itemObj.has("ownerusername") && !itemObj.isNull("ownerusername")) {
                        String raw = itemObj.getString("ownerusername");
                        return raw.replaceAll("§.", "").replaceAll("\\$.", "").trim();
                    }
                    if (itemObj.has("ownerId") && !itemObj.isNull("ownerId")) {
                        String ownerId = itemObj.getString("ownerId").trim();
                        String resolvedName = resolveUsernameFromUUID(ownerId);
                        if (resolvedName != null) return resolvedName;
                    }
                }
            }
        } catch (Exception e) {
            sendMessage(EnumChatFormatting.RED.toString() + "Error resolving nonce: " + e.getMessage());
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
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);

            if (conn.getResponseCode() != 200) return null;

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();

            JSONObject root = new JSONObject(sb.toString());
            if (root.has("name")) return root.getString("name");
        } catch (Exception e) {
            sendMessage(EnumChatFormatting.RED.toString() + "UUID resolution error: " + e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    private void sendMessage(String msg) {
        mc.addScheduledTask(() -> {
            if (mc.thePlayer != null) {
                mc.thePlayer.addChatMessage(new ChatComponentText(prefix + msg));
            }
        });
    }
}
