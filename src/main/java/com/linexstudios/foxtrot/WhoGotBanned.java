package com.linexstudios.foxtrot;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class WhoGotBanned {
    private final Minecraft mc = Minecraft.getMinecraft();
    public static boolean enabled = true;

    // Tracks players currently in the Tab list
    private Set<String> previousPlayers = new HashSet<>();
    
    // Caches players who recently left and the exact millisecond they vanished
    private final Map<String, Long> recentLeaves = new HashMap<>();
    
    // Timer flag in case the chat message arrives BEFORE the player is removed
    private long lastBanMessageTime = 0;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!enabled || mc.theWorld == null || mc.getNetHandler() == null || event.phase != TickEvent.Phase.END) return;

        // 1. Get the current players in the Tab list
        Set<String> currentPlayers = new HashSet<>();
        for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
            if (info != null && info.getGameProfile() != null) {
                currentPlayers.add(info.getGameProfile().getName());
            }
        }

        // 2. Compare with the previous tick to see who just vanished
        if (!previousPlayers.isEmpty()) {
            for (String prev : previousPlayers) {
                if (!currentPlayers.contains(prev)) {
                    long now = System.currentTimeMillis();
                    
                    // Did a ban message happen in the last 2 seconds? (Message arrived FIRST)
                    if (now - lastBanMessageTime < 2000) {
                        announceBan(prev);
                        lastBanMessageTime = 0; // Reset to prevent double announcements
                    } else {
                        // Otherwise, just log their departure normally
                        recentLeaves.put(prev, now);
                    }
                }
            }
        }
        previousPlayers = currentPlayers;

        // 3. Memory management: Clean up players who left more than 3 seconds ago
        recentLeaves.entrySet().removeIf(entry -> System.currentTimeMillis() - entry.getValue() > 3000);
    }

    @SubscribeEvent
    public void onChatMessage(ClientChatReceivedEvent event) {
        if (!enabled || mc.theWorld == null) return;

        // Extract the raw text to ignore color formatting changes
        String message = event.message.getUnformattedText().trim();

        // Detect the official Hypixel removal messages
        if (message.equals("A player has been removed from your game.") || 
            message.equals("A player has been removed from your lobby for hacking or abuse.")) {
            
            long now = System.currentTimeMillis();
            boolean found = false;
            
            // Did someone leave the Tab list right before this message? (Removal happened FIRST)
            for (Map.Entry<String, Long> entry : recentLeaves.entrySet()) {
                if (now - entry.getValue() <= 2000) {
                    announceBan(entry.getKey());
                    found = true;
                }
            }
            
            if (found) {
                // Clear the cache so we don't accidentally double-announce
                recentLeaves.clear();
            } else {
                // Set the flag so the Tick event can catch them if they vanish in the next 2 seconds
                lastBanMessageTime = now;
            }
        }
    }

    private void announceBan(String playerName) {
        String prefix = EnumChatFormatting.GRAY + "[" + EnumChatFormatting.RED + "PIT" + EnumChatFormatting.GRAY + "] ";
        String alert = EnumChatFormatting.GREEN + playerName + " has been banned!";
        mc.thePlayer.addChatMessage(new ChatComponentText(prefix + alert));
    }
}