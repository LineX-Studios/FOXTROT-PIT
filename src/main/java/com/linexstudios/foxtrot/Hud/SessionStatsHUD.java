package com.linexstudios.foxtrot.Hud;

import com.linexstudios.foxtrot.Handler.APIHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

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

    // Advanced variables for calculating XP/Hour dynamically
    private long lastXpLeft = -1;
    private long totalXpGained = 0;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || mc.theWorld == null || mc.thePlayer == null) return;

        checkIfInPit();

        if (isInPit) {
            if (sessionStartTime == 0) {
                sessionStartTime = System.currentTimeMillis();
            }

            APIHandler.updateStats(mc.thePlayer);

            if (APIHandler.isLoaded) {
                if (lastXpLeft == -1) {
                    // Initialize the tracker the first time the API loads
                    lastXpLeft = APIHandler.xpLeft;
                } else if (APIHandler.xpLeft < lastXpLeft) {
                    // We gained XP! Add the difference to our total.
                    totalXpGained += (lastXpLeft - APIHandler.xpLeft);
                    lastXpLeft = APIHandler.xpLeft;
                } else if (APIHandler.xpLeft > lastXpLeft) {
                    // The player prestiged (XP left jumped up)! Just reset the tracker.
                    lastXpLeft = APIHandler.xpLeft;
                }
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
            // Dummy Data for the HUD Editor using your exact requested color formatting
            String timeStr = EnumChatFormatting.WHITE + "Playtime: " + EnumChatFormatting.GRAY + "01h 15m";
            String xpStr = EnumChatFormatting.WHITE + "XP Needed: " + EnumChatFormatting.AQUA + "14,500 " + EnumChatFormatting.GRAY + "(85.0%)";
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

            // 1. Time Played Format
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

            // 2. XP Needed & Percentage Calculation
            String percentFormatted = calculateXpPercentage();
            String xpStr = EnumChatFormatting.WHITE + "XP Needed: " + EnumChatFormatting.AQUA + String.format("%,d", APIHandler.xpLeft) + EnumChatFormatting.DARK_AQUA + " (" + percentFormatted + ")";

            fr.drawStringWithShadow(xpStr, hudX, currentY, 0xFFFFFF);
            maxWidth = Math.max(maxWidth, fr.getStringWidth(xpStr));
            currentY += fr.FONT_HEIGHT;

            // 3. Gold Needed
            String goldDisplay = APIHandler.isGoldReqMet() ? EnumChatFormatting.GREEN + "Met!" : EnumChatFormatting.GOLD + APIHandler.getFormattedGoldLeft() + "g";
            String goldStr = EnumChatFormatting.WHITE + "Gold Needed: " + goldDisplay;
            fr.drawStringWithShadow(goldStr, hudX, currentY, 0xFFFFFF);
            maxWidth = Math.max(maxWidth, fr.getStringWidth(goldStr));
            currentY += fr.FONT_HEIGHT;

            // 4. XP / Hour Calculation
            long xpPerHour = 0;
            // The math will process immediately the moment the API registers an XP change
            if (elapsed > 1000 && totalXpGained > 0) {
                xpPerHour = (long) ((totalXpGained / (double) elapsed) * 3600000);
            }
            String xpPerHourStr = EnumChatFormatting.WHITE + "XP/Hour: " + EnumChatFormatting.AQUA + String.format("%,d", xpPerHour);
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

    /**
     * Calculates the exact completion percentage using Pitmart's proportion data.
     */
    private String calculateXpPercentage() {
        // Pitmart sends the proportion completed (e.g. 0.2559 = 25.59%)
        double percentCompleted = APIHandler.xpProportion * 100.0;
        percentCompleted = Math.max(0.0, Math.min(100.0, percentCompleted));
        return String.format("%.1f%%", percentCompleted);
    }

    public boolean isHovered(int mouseX, int mouseY) {
        return mouseX >= hudX - 2 && mouseX <= hudX + width + 2 && mouseY >= hudY - 2 && mouseY <= hudY + height + 2;
    }
}