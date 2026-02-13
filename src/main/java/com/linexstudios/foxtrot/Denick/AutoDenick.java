package com.linexstudios.foxtrot.Denick;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
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
    // ADDED: The instance variable so Forge and other classes can access it
    public static final AutoDenick instance = new AutoDenick();

    public static boolean enabled = false;
    private final Minecraft mc = Minecraft.getMinecraft();
    
    // Limits background threads so we don't lag the game or get rate-limited
    private static final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final String prefix = EnumChatFormatting.GRAY + "[" + EnumChatFormatting.RED + "PIT" + EnumChatFormatting.GRAY + "] ";
    
    private final Set<String> processed = new HashSet<>();
    private long lastScan = 0;
    private final int scanRange = 40; // Roni Mod scan range

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!enabled || mc.theWorld == null || mc.thePlayer == null || event.phase != TickEvent.Phase.END) return;

        // 15-second cooldown to avoid API spam
        if (System.currentTimeMillis() - lastScan < 15000L) return;
        lastScan = System.currentTimeMillis();

        List<EntityOtherPlayerMP> targets = new ArrayList<>();
        
        for (Object obj : mc.theWorld.playerEntities) {
            if (obj instanceof EntityOtherPlayerMP) {
                EntityOtherPlayerMP p = (EntityOtherPlayerMP) obj;
                
                // Roni Range Check
                if (p.getDistanceToEntity(mc.thePlayer) > scanRange) continue;

                //  Ignore bots, NPCs, and holograms
                if (!isValidTarget(p)) continue;
                
                String name = p.getName();
                
                // Skips if player is already in our local "known" list
                if (processed.contains(name) || NickedManager.isResolved(name)) continue;

                targets.add(p);
            }
        }

        if (targets.isEmpty()) return;

        // Extract Nonce on Main Thread and schedule API lookup
        for (EntityOtherPlayerMP p : targets) {
            String name = p.getName();
            int nonce = getNonce(p); 
            
            if (nonce > 0) {
                processed.add(name); // Mark as processed so we don't spam
                runAsyncDenick(nonce, name); // Send to background thread
            }
        }
    }

    /**
     * Guarantees the entity is a real, legitimate human player.
     */
    private boolean isValidTarget(EntityOtherPlayerMP player) {
        // 1. Skip v2 UUIDs (Standard NPC Check)
        if (player.getUniqueID().version() == 2) return false;

        // 2. Skip color-coded names (Holograms/System text)
        if (player.getName().startsWith("§")) return false;

        // 3. Skip invisible entities (Watchdog bots/Armorstands)
        if (player.isInvisible()) return false;

        // 4. Tab List Check (Real players are in the NetHandler)
        NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(player.getUniqueID());
        if (info == null) return false; 

        // 5. Ping Check (Hypixel NPCs are hardcoded to 1 ping or 0 ping)
        if (info.getResponseTime() <= 1) return false;

        return true;
    }

    private void runAsyncDenick(int nonce, String inGameName) {
        executor.execute(() -> {
            // Slight delay inside the thread to stagger API requests
            try {
                Thread.sleep(500); 
            } catch (InterruptedException ignored) {}

            String resolvedIGN = resolveOwnerFromNonce(nonce);
            
            // If the API finds a name, and it DOES NOT match their in-game name, they are Nicked
            if (resolvedIGN != null && !resolvedIGN.equalsIgnoreCase(inGameName)) {
                
                // Validate via Mojang to ensure it's not corrupted API junk
                if (isValidMinecraftName(resolvedIGN)) {
                    NickedManager.addNicked(inGameName, resolvedIGN);
                    sendMessage(EnumChatFormatting.AQUA + "AutoDenick: " + EnumChatFormatting.YELLOW + inGameName + EnumChatFormatting.GRAY + " is actually " + EnumChatFormatting.GREEN + resolvedIGN);
                }
            }
        });
    }

    private int getNonce(EntityOtherPlayerMP player) {
        List<ItemStack> items = new ArrayList<>();
        Collections.addAll(items, player.inventory.armorInventory);
        items.add(player.getHeldItem());
        
        for (ItemStack item : items) {
            if (item != null && item.hasTagCompound()) {
                NBTTagCompound tag = item.getTagCompound().getCompoundTag("ExtraAttributes");
                if (tag != null && tag.hasKey("Nonce")) {
                    return tag.getInteger("Nonce");
                }
            }
        }
        return -1;
    }

    private String resolveOwnerFromNonce(int nonce) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL("https://pitpal.rocks/api/listings/items/nonce" + nonce);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);

            if (conn.getResponseCode() == 200) {
                BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String l;
                while ((l = r.readLine()) != null) sb.append(l);
                r.close();
                
                String response = sb.toString();
                if (response.isEmpty() || !response.startsWith("{")) return null;
                
                JSONObject root = new JSONObject(response);
                
                JSONArray itemsArr = null;
                if (root.has("items") && !root.isNull("items")) {
                    itemsArr = root.getJSONArray("items");
                } else if (root.has("data") && !root.isNull("data")) {
                    itemsArr = root.getJSONArray("data");
                }

                if (itemsArr != null && itemsArr.length() > 0) {
                    JSONObject itemObj = itemsArr.getJSONObject(0);
                    
                    if (itemObj.has("ownerusername") && !itemObj.isNull("ownerusername")) {
                        return itemObj.getString("ownerusername").replaceAll("§.", "").replaceAll("\\$.", "").trim();
                    }
                    
                    String ownerId = null;
                    if (itemObj.has("ownerId") && !itemObj.isNull("ownerId")) {
                        ownerId = itemObj.getString("ownerId").trim();
                    } else if (itemObj.has("ownerid") && !itemObj.isNull("ownerid")) {
                        ownerId = itemObj.getString("ownerid").trim();
                    }
                    
                    if (ownerId != null && !ownerId.isEmpty()) {
                        return resolveUsernameFromUUID(ownerId);
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    private boolean isValidMinecraftName(String name) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + name);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);
            return conn.getResponseCode() == 200;
        } catch (Exception e) {
            return false;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private String resolveUsernameFromUUID(String uuid) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.replace("-", ""));
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() == 200) {
                BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String l;
                while ((l = r.readLine()) != null) sb.append(l);
                r.close();

                JSONObject root = new JSONObject(sb.toString());
                if (root.has("name")) return root.getString("name");
            }
        } catch (Exception ignored) {
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    private void sendMessage(String msg) {
        mc.addScheduledTask(() -> {
            if (mc.thePlayer != null) mc.thePlayer.addChatMessage(new ChatComponentText(prefix + msg));
        });
    }
}