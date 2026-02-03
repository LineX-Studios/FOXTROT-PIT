package com.linexstudios.foxtrot;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashSet;
import java.util.Set;

public class WhoGotBanned {
    private final Minecraft mc = Minecraft.getMinecraft();
    private Set<String> prevPlayers = new HashSet<>();
    private Set<String> currentPlayers = new HashSet<>();

    // optional local debug flag
    private static final boolean debugMode = false;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (mc.theWorld == null || event.phase != TickEvent.Phase.END) return;

        if (mc.thePlayer.ticksExisted % 20 == 0) {
            if (mc.getNetHandler() == null) return;
            prevPlayers = new HashSet<>(currentPlayers);
            currentPlayers.clear();
            for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
                if (info.getGameProfile() != null) {
                    currentPlayers.add(info.getGameProfile().getName());
                }
            }
        }
    }

    @SubscribeEvent
    public void onChatMessage(ClientChatReceivedEvent event) {
        String unformatted = event.message.getUnformattedText();

        if (unformatted.contains("removed") && unformatted.contains("lobby")) {
            boolean found = false;
            for (String name : prevPlayers) {
                if (!currentPlayers.contains(name)) {
                    mc.thePlayer.addChatMessage(new ChatComponentText(
                        EnumChatFormatting.DARK_GRAY + "[" + EnumChatFormatting.RED + "BANNED" + EnumChatFormatting.DARK_GRAY + "] " +
                        EnumChatFormatting.RED + name + EnumChatFormatting.YELLOW + " was removed from the lobby!"
                    ));
                    found = true;
                }
            }
            if (!found && debugMode) {
                mc.thePlayer.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.RED + "[Foxtrot] A player was banned, but they weren't in your tab list."
                ));
            }
        }
    }
}
