package com.linexstudios.foxtrot.Hud;

import com.linexstudios.foxtrot.Handler.APIHandler; // UPDATED IMPORT
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

    // Variables for calculating XP/Hour
    private long startingXpLeft = -1;
    private long xpGained = 0;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || mc.theWorld == null || mc.thePlayer == null) return;

        // 1. Check if we are physically in The Hypixel Pit
        checkIfInPit();

        // 2. If in the Pit, start the timer and fetch API data
        if (isInPit) {
            if (sessionStartTime == 0) {
                sessionStartTime = System.currentTimeMillis();
            }

            // This safely handles its own 30-second cooldown internally!
            APIHandler.updateStats(mc.thePlayer);

            // Track XP gained for XP/Hour calculations
            if (APIHandler.isLoaded) {
                if (startingXpLeft == -1) {
                    startingXpLeft = APIHandler.xpLeft;
                } else if (APIHandler.xpLeft < startingXpLeft) {
                    xpGained = startingXpLeft - APIHandler.xpLeft;
                }
            }
        }
    }

    /**
     * Reads the sidebar scoreboard to verify we are actually in a Pit lobby.
     */
    private void checkIfInPit() {
        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        if (scoreboard != null) {
            ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1); // 1 is the Sidebar
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

        // Hide the HUD completely if we aren't in the Pit (unless we are editing the GUI)
        if (!isInPit && !isEditing) return;

        FontRenderer fr = mc.fontRendererObj;
        int currentY = hudY;
        int maxWidth = fr.getStringWidth("Session Stats");

        fr.drawStringWithShadow(EnumChatFormatting.GOLD + "" + EnumChatFormatting.BOLD + "Session Stats", hudX, currentY, 0xFFFFFF);
        currentY += fr.FONT_HEIGHT + 2;

        if (isEditing && !APIHandler.isLoaded) {
            // Dummy Data so you can see the box while dragging it in your GUI
            String timeStr = "Playtime: " + EnumChatFormatting.WHITE + "01h 15m";
            String xpStr = "XP Needed: " + EnumChatFormatting.AQUA + "14,500 (85.0%)";
            String goldStr = "Gold Needed: " + EnumChatFormatting.GOLD + "10,000g";
            String xpPerHourStr = "XP/Hour: " + EnumChatFormatting.AQUA + "15,000";

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

            // 1. Time Played Format (00h 00m)
            long elapsed = System.currentTimeMillis() - sessionStartTime;
            long hours = elapsed / 3600000;
            long minutes = (elapsed % 3600000) / 60000;
            long seconds = ((elapsed % 3600000) % 60000) / 1000;

            String timeFormatted;
            if (hours > 0) timeFormatted = String.format("%02dh %02dm", hours, minutes);
            else timeFormatted = String.format("%02dm %02ds", minutes, seconds);

            String timeStr = "Playtime: " + EnumChatFormatting.WHITE + timeFormatted;
            fr.drawStringWithShadow(timeStr, hudX, currentY, 0xFFFFFF);
            maxWidth = Math.max(maxWidth, fr.getStringWidth(timeStr));
            currentY += fr.FONT_HEIGHT;

            // 2. XP Needed
            String xpStr = "XP Needed: " + EnumChatFormatting.AQUA + String.format("%,d", APIHandler.xpLeft) + " (" + APIHandler.getXpPercentage() + ")";
            fr.drawStringWithShadow(xpStr, hudX, currentY, 0xFFFFFF);
            maxWidth = Math.max(maxWidth, fr.getStringWidth(xpStr));
            currentY += fr.FONT_HEIGHT;

            // 3. Gold Needed
            String goldDisplay = APIHandler.isGoldReqMet() ? EnumChatFormatting.GREEN + "Met!" : EnumChatFormatting.GOLD + APIHandler.getFormattedGoldLeft() + "g";
            String goldStr = "Gold Needed: " + goldDisplay;
            fr.drawStringWithShadow(goldStr, hudX, currentY, 0xFFFFFF);
            maxWidth = Math.max(maxWidth, fr.getStringWidth(goldStr));
            currentY += fr.FONT_HEIGHT;

            // 4. XP / Hour Calculation
            long xpPerHour = 0;
            // Wait at least 1 minute before calculating to avoid wild inflated numbers in the first few seconds
            if (elapsed > 60000 && xpGained > 0) {
                xpPerHour = (long) ((xpGained / (double) elapsed) * 3600000);
            }
            String xpPerHourStr = "XP/Hour: " + EnumChatFormatting.AQUA + String.format("%,d", xpPerHour);
            fr.drawStringWithShadow(xpPerHourStr, hudX, currentY, 0xFFFFFF);
            maxWidth = Math.max(maxWidth, fr.getStringWidth(xpPerHourStr));
            currentY += fr.FONT_HEIGHT;

        } else {
            String loading = EnumChatFormatting.GRAY + "Loading API...";
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