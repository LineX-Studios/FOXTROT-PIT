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
    public static boolean enabled = false;
    private final Minecraft mc = Minecraft.getMinecraft();
    private static final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final String prefix = EnumChatFormatting.GRAY + "[" + EnumChatFormatting.RED + "PIT" + EnumChatFormatting.GRAY + "] ";
    private final Set<String> processed = new HashSet<>();
    private long lastScan = 0;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!enabled || mc.theWorld == null || mc.thePlayer == null || event.phase != TickEvent.Phase.END) return;

        if (System.currentTimeMillis() - lastScan < 5000) return;
        lastScan = System.currentTimeMillis();

        for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
            String name = info.getGameProfile().getName();
            if (info.getGameProfile().getId().version() == 2 && !processed.contains(name)) {
                processed.add(name);
                runAsyncDenick(name);
            }
        }
    }

    private void runAsyncDenick(String nick) {
        executor.execute(() -> {
            int nonce = -1;
            for (Object obj : mc.theWorld.playerEntities) {
                if (obj instanceof EntityOtherPlayerMP) {
                    EntityOtherPlayerMP p = (EntityOtherPlayerMP) obj;
                    if (p.getName().equalsIgnoreCase(nick)) {
                        nonce = getNonce(p);
                        break;
                    }
                }
            }

            if (nonce > 0) {
                String resolved = resolveFromPitPal(nonce);
                if (resolved != null) {
                    NickedManager.addNicked(nick, resolved);
                    sendMessage(EnumChatFormatting.YELLOW + nick + EnumChatFormatting.GRAY + " -> " + EnumChatFormatting.GREEN + resolved);
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
                if (tag.hasKey("Nonce")) return tag.getInteger("Nonce");
            }
        }
        return -1;
    }

    private String resolveFromPitPal(int nonce) {
        try {
            URL url = new URL("https://pitpal.rocks/api/listings/items/nonce" + nonce);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            if (conn.getResponseCode() == 200) {
                BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String l;
                while ((l = r.readLine()) != null) sb.append(l);
                JSONObject json = new JSONObject(sb.toString());
                if (json.has("data") && json.getJSONArray("data").length() > 0) {
                    return json.getJSONArray("data").getJSONObject(0).getString("ownerusername").replaceAll("§.", "");
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void sendMessage(String msg) {
        mc.addScheduledTask(() -> {
            if (mc.thePlayer != null) mc.thePlayer.addChatMessage(new ChatComponentText(prefix + msg));
        });
    }
}