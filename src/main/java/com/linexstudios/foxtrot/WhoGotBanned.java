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
    public static final WhoGotBanned instance = new WhoGotBanned();
    private final Minecraft mc = Minecraft.getMinecraft();

    // Keeps track of the lobby from the previous tick
    private final Set<String> previousPlayers = new HashSet<>();

    // Memory bank of people who left in the last 5 seconds (Name -> Timestamp)
    private final Map<String, Long> recentlyLeft = new HashMap<>();

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || mc.theWorld == null || mc.getNetHandler() == null) return;

        Set<String> currentPlayers = new HashSet<>();

        // Grab everyone currently in the Tab List
        for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
            if (info != null && info.getGameProfile() != null && info.getGameProfile().getName() != null) {
                String name = info.getGameProfile().getName();
                // Filter out NPCs/Holograms (they usually start with color codes in the tab logic)
                if (!name.startsWith("§")) {
                    currentPlayers.add(name);
                }
            }
        }

        // If previousPlayers is empty, it means we just joined the lobby. Just sync and return to avoid false positives.
        if (previousPlayers.isEmpty()) {
            previousPlayers.addAll(currentPlayers);
            return;
        }

        // Compare: Who was here a tick ago, but is gone now?
        for (String prev : previousPlayers) {
            if (!currentPlayers.contains(prev)) {
                // They vanished! Log their name and the exact millisecond they disappeared.
                recentlyLeft.put(prev, System.currentTimeMillis());
            }
        }

        // Update the previous list for the next tick calculation
        previousPlayers.clear();
        previousPlayers.addAll(currentPlayers);

        // Cleanup: Delete anyone from the memory bank who left more than 5 seconds ago
        long now = System.currentTimeMillis();
        recentlyLeft.entrySet().removeIf(entry -> now - entry.getValue() > 5000);
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (event.type == 2) return; // Ignore action bar messages (like health or mana)

        // Strip the formatting so we can read the raw text easily
        String unformatted = EnumChatFormatting.getTextWithoutFormattingCodes(event.message.getUnformattedText());

        // Check for Hypixel's exact Watchdog ban messages
        if (unformatted.contains("A player has been removed from your game") ||
                unformatted.contains("A player has been removed from your lobby")) {

            // The ban message just hit! Let's find the person who vanished closest to this exact millisecond.
            String bannedPlayer = "Unknown (Too Fast)";
            long closestTime = Long.MAX_VALUE;

            for (Map.Entry<String, Long> entry : recentlyLeft.entrySet()) {
                long timeDiff = System.currentTimeMillis() - entry.getValue();

                if (timeDiff < closestTime) {
                    closestTime = timeDiff;
                    bannedPlayer = entry.getKey();
                }
            }

            // --- TRIGGER THE CHAT ALERT ---
            if (mc.thePlayer != null) {
                mc.thePlayer.addChatMessage(new ChatComponentText(
                        EnumChatFormatting.GRAY + "[" + 
                        EnumChatFormatting.RED + "Foxtrot" + 
                        EnumChatFormatting.GRAY + "] " + 
                        EnumChatFormatting.YELLOW + "\u26A0 " + 
                        EnumChatFormatting.RED + "" + EnumChatFormatting.BOLD + bannedPlayer + " " + 
                        EnumChatFormatting.GOLD + "Has been banned!"
                ));
            }

            // Clear the map so we don't accidentally flag the same person twice if the server lags
            recentlyLeft.clear();
        }
    }
}