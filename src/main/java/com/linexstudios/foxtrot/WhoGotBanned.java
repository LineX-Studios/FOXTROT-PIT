package com.linexstudios.foxtrot;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class WhoGotBanned {
    private final Minecraft mc = Minecraft.getMinecraft();
    
    // Toggle for the feature
    public static boolean enabled = true;
    
    // State tracker to link the ban message with the leave message
    private boolean expectingLeaveMessage = false;

    @SubscribeEvent
    public void onChatMessage(ClientChatReceivedEvent event) {
        if (!enabled || mc.theWorld == null || mc.thePlayer == null) return;

        // Extract the raw text to ignore color formatting changes
        String message = event.message.getUnformattedText().trim();

        // 1. Detect the initial Hypixel removal message
        if (message.equals("A player has been removed from your game.") || 
            message.equals("A player has been removed from your lobby for hacking or abuse.")) {
            expectingLeaveMessage = true;
            return;
        }

        // 2. Ignore the follow-up report message so it doesn't cancel our check
        if (expectingLeaveMessage && message.equals("Use /report to continue helping out the server!")) {
            return;
        }

        // 3. Catch the leave message immediately following the ban
        if (expectingLeaveMessage && message.startsWith("[-] ") && message.endsWith(" left!")) {
            // Extract the banned player's name (Removes "[-] " from the start and " left!" from the end)
            String playerName = message.substring(4, message.length() - 6);
            
            // Format: [Foxtrot] (IGN) has been banned!
            String prefix = EnumChatFormatting.GRAY + "[" + EnumChatFormatting.GOLD + "Foxtrot" + EnumChatFormatting.GRAY + "] ";
            String alert = EnumChatFormatting.GREEN + playerName + " has been banned!";
            
            mc.thePlayer.addChatMessage(new ChatComponentText(prefix + alert));
            
            // Reset state
            expectingLeaveMessage = false;
            
        } else if (expectingLeaveMessage) {
            // If a completely unrelated message appears, reset the state
            // to avoid falsely tagging a normal leave later.
            expectingLeaveMessage = false;
        }
    }
}