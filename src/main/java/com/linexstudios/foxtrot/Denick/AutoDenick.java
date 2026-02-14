package com.linexstudios.foxtrot.Denick;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
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
    public static final AutoDenick instance = new AutoDenick();
    public static boolean enabled = false;
    private final Minecraft mc = Minecraft.getMinecraft();
    
    private static final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final String prefix = EnumChatFormatting.GRAY + "[" + EnumChatFormatting.RED + "PIT" + EnumChatFormatting.GRAY + "] ";
    
    private final Set<String> resolved = new HashSet<>();
    private long lastScan = 0;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!enabled || mc.theWorld == null || mc.thePlayer == null || event.phase != TickEvent.Phase.END) return;

        // 3-second cooldown to avoid API spam
        if (System.currentTimeMillis() - lastScan < 3000L) return;
        lastScan = System.currentTimeMillis();

        // ONLY look at players the NickScanner has already flagged
        for (Object obj : mc.theWorld.playerEntities) {
            if (obj instanceof EntityOtherPlayerMP) {
                EntityOtherPlayerMP p = (EntityOtherPlayerMP) obj;
                String name = p.getName();

                // If NickScanner detected them AND we haven't resolved them yet
                if (NickScanner.detectedNicks.contains(name) && !resolved.contains(name)) {
                    
                    // Because they are physically rendered, we can steal their nonce
                    int nonce = getNonce(p); 
                    if (nonce > 0) {
                        resolved.add(name); // Mark as resolved so we don't spam PitPal
                        runAsyncDenick(nonce, name); // Send to background thread
                    }
                }
            }
        }
    }

    private void runAsyncDenick(int nonce, String inGameName) {
        executor.execute(() -> {
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}

            String resolvedIGN = resolveOwnerFromNonce(nonce);
            
            if (resolvedIGN != null && !resolvedIGN.equalsIgnoreCase(inGameName)) {
                if (isValidMinecraftName(resolvedIGN)) {
                    // Update the HUD from "Awaiting Nonce..." to their real name
                    NickedManager.addNicked(inGameName, resolvedIGN);
                    sendMessage(EnumChatFormatting.AQUA + "AutoDenick: " + EnumChatFormatting.YELLOW + inGameName + EnumChatFormatting.GRAY + " is actually " + EnumChatFormatting.GREEN + resolvedIGN);
                } else {
                    NickedManager.addNicked(inGameName, EnumChatFormatting.RED + "Fake API Data");
                }
            } else {
                NickedManager.addNicked(inGameName, EnumChatFormatting.RED + "Resolution Failed");
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