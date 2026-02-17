package com.linexstudios.foxtrot.Hud;

import com.linexstudios.foxtrot.Handler.APIHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SessionStatsHUD {
    public static final SessionStatsHUD instance = new SessionStatsHUD();
    private final Minecraft mc = Minecraft.getMinecraft();

    public static boolean enabled = true;
    public static int hudX = 10;
    public static int hudY = 150;

    public int width = 0;
    public int height = 0;

    private long sessionStartTime = 0;
    private boolean isInPit = false;

    // Live variables for instant tracking
    private long sessionXpGained = 0;
    private static final Pattern XP_PATTERN = Pattern.compile("\\+(\\d+)XP");

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || mc.theWorld == null || mc.thePlayer == null) return;

        checkIfInPit();

        if (isInPit) {
            if (sessionStartTime == 0) {
                sessionStartTime = System.currentTimeMillis();
            }
            // Trigger the dual API fetcher (handles 30s cooldown internally)
            APIHandler.updateStats(mc.thePlayer);
        }
    }

    // --- INSTANT HYBRID XP TRACKER ---
    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (!enabled || !isInPit || !APIHandler.isLoaded) return;

        // Strip colors to cleanly read the raw text
        String message = EnumChatFormatting.getTextWithoutFormattingCodes(event.message.getUnformattedText());
        
        // Look for exact XP drops in chat (e.g. "+15XP")
        Matcher matcher = XP_PATTERN.matcher(message.replace(",", ""));
        if (matcher.find()) {
            long xpEarned = Long.parseLong(matcher.group(1));
            
            // 1. Instantly track session XP for live XP/Hour math
            sessionXpGained += xpEarned;
            
            // 2. Instantly update the API values locally
            APIHandler.pitPandaXpCurrent += xpEarned;
            
            // 3. Instantly re-calculate the percentage and "k/k" description
            if (APIHandler.pitPandaXpGoal > 0) {
                APIHandler.pitPandaXpPercent = ((double) APIHandler.pitPandaXpCurrent / APIHandler.pitPandaXpGoal);
                APIHandler.pitPandaXpDescription = String.format("%.2fk/%.2fk", APIHandler.pitPandaXpCurrent / 1000.0, APIHandler.pitPandaXpGoal / 1000.0);
            }
        }
    }

    private void checkIfInPit() {
        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        if (scoreboard != null) {
            ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1); 
            if (objective != null) {
                String title = EnumChatFormatting.getTextWithoutFormattingCodes(objective.getDisplayName());
                if (title != null && title.toUpperCase().contains("PIT")) {
                    isInPit = true;
                    return;
                }
            }
        }
        isInPit = false;
    }

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) return;
        if (mc.currentScreen instanceof EditHUDGui) return;
        render(false);
    }

    public void render(boolean isEditing) {
        if (!enabled || mc.theWorld == null) return;
        if (!isInPit && !isEditing) return;

        FontRenderer fr = mc.fontRendererObj;
        int currentY = hudY;
        int maxWidth = fr.getStringWidth("Session Stats");

        fr.drawStringWithShadow(EnumChatFormatting.GOLD + "" + EnumChatFormatting.BOLD + "Session Stats", hudX, currentY, 0xFFFFFF);
        currentY += fr.FONT_HEIGHT + 2;

        if (isEditing && !APIHandler.isLoaded) {
            String timeStr = EnumChatFormatting.WHITE + "Playtime: " + EnumChatFormatting.GRAY + "01h 15m";
            String xpStr = EnumChatFormatting.WHITE + "XP Progress: " + EnumChatFormatting.AQUA + "25.93k/98.94k " + EnumChatFormatting.GRAY + "(26.2%)";
            String goldStr = EnumChatFormatting.WHITE + "Gold Needed: " + EnumChatFormatting.GOLD + "10,000g";
            String xpPerHourStr = EnumChatFormatting.WHITE + "XP/Hour: " + EnumChatFormatting.AQUA + "15,000";

            fr.drawStringWithShadow(timeStr, hudX, currentY, 0xFFFFFF);
            maxWidth = Math.max(maxWidth, fr.getStringWidth(timeStr));
            currentY += fr.FONT_HEIGHT;

            fr.drawStringWithShadow(xpStr, hudX, currentY, 0xFFFFFF);
            maxWidth = Math.max(maxWidth, fr.getStringWidth(xpStr));
            currentY += fr.FONT_HEIGHT;

            fr.drawStringWithShadow(goldStr, hudX, currentY, 0xFFFFFF);
            maxWidth = Math.max(maxWidth, fr.getStringWidth(goldStr));
            currentY += fr.FONT_HEIGHT;

            fr.drawStringWithShadow(xpPerHourStr, hudX, currentY, 0xFFFFFF);
            maxWidth = Math.max(maxWidth, fr.getStringWidth(xpPerHourStr));
            currentY += fr.FONT_HEIGHT;

        } else if (APIHandler.isLoaded) {

            // 1. Time Played 
            long elapsed = System.currentTimeMillis() - sessionStartTime;
            long hours = elapsed / 3600000;
            long minutes = (elapsed % 3600000) / 60000;
            long seconds = ((elapsed % 3600000) % 60000) / 1000;

            String timeFormatted;
            if (hours > 0) timeFormatted = String.format("%02dh %02dm", hours, minutes);
            else timeFormatted = String.format("%02dm %02ds", minutes, seconds);

            String timeStr = EnumChatFormatting.WHITE + "Playtime: " + EnumChatFormatting.GRAY + timeFormatted;
            fr.drawStringWithShadow(timeStr, hudX, currentY, 0xFFFFFF);
            maxWidth = Math.max(maxWidth, fr.getStringWidth(timeStr));
            currentY += fr.FONT_HEIGHT;

            // 2. Instant XP Progress
            double percentCompleted = APIHandler.pitPandaXpPercent * 100.0;
            // Cap it at 100% just in case of weird server behavior
            percentCompleted = Math.max(0.0, Math.min(100.0, percentCompleted));
            String percentFormatted = String.format("%.1f%%", percentCompleted);
            
            String xpStr = EnumChatFormatting.WHITE + "XP Progress: " + EnumChatFormatting.AQUA + APIHandler.pitPandaXpDescription + EnumChatFormatting.DARK_AQUA + " (" + percentFormatted + ")";
            fr.drawStringWithShadow(xpStr, hudX, currentY, 0xFFFFFF);
            maxWidth = Math.max(maxWidth, fr.getStringWidth(xpStr));
            currentY += fr.FONT_HEIGHT;

            // 3. Gold Needed
            String goldDisplay = APIHandler.isGoldReqMet() ? EnumChatFormatting.GREEN + "Done" : EnumChatFormatting.GOLD + APIHandler.getFormattedGoldLeft() + "g";
            String goldStr = EnumChatFormatting.WHITE + "Gold Needed: " + goldDisplay;
            fr.drawStringWithShadow(goldStr, hudX, currentY, 0xFFFFFF);
            maxWidth = Math.max(maxWidth, fr.getStringWidth(goldStr));
            currentY += fr.FONT_HEIGHT;

            // 4. Instant Live XP/Hour Calculation
            long instantXpPerHour = 0;
            // Allow 5 seconds to pass before calculating to prevent crazy math spikes when you first join
            if (elapsed > 5000 && sessionXpGained > 0) {
                instantXpPerHour = (long) ((sessionXpGained / (double) elapsed) * 3600000);
            }
            String xpPerHourStr = EnumChatFormatting.WHITE + "XP/Hour: " + EnumChatFormatting.AQUA + String.format("%,d", instantXpPerHour);
            fr.drawStringWithShadow(xpPerHourStr, hudX, currentY, 0xFFFFFF);
            maxWidth = Math.max(maxWidth, fr.getStringWidth(xpPerHourStr));
            currentY += fr.FONT_HEIGHT;

        } else {
            String loading = EnumChatFormatting.YELLOW + "Loading API...";
            fr.drawStringWithShadow(loading, hudX, currentY, 0xFFFFFF);
            maxWidth = Math.max(maxWidth, fr.getStringWidth(loading));
            currentY += fr.FONT_HEIGHT;
        }

        this.width = maxWidth;
        this.height = currentY - hudY;

        if (isEditing) Gui.drawRect(hudX - 2, hudY - 2, hudX + width + 2, hudY + height + 2, 0x44888888);
    }

    public boolean isHovered(int mouseX, int mouseY) {
        return mouseX >= hudX - 2 && mouseX <= hudX + width + 2 && mouseY >= hudY - 2 && mouseY <= hudY + height + 2;
    }
}