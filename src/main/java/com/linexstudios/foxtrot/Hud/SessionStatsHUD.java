package com.linexstudios.foxtrot.Hud;

import com.linexstudios.foxtrot.Handler.PitDataHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SessionStatsHUD {
    public static final SessionStatsHUD instance = new SessionStatsHUD();
    private final Minecraft mc = Minecraft.getMinecraft();

    public static boolean enabled = true;
    public static int hudX = 10;
    public static int hudY = 150;

    public int width = 0;
    public int height = 0;

    private final long startTime = System.currentTimeMillis();

    private String currentPrestige = "0";
    private double currentGold = 0.0;
    private int tickTimer = 0;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || mc.theWorld == null) return;

        // Scrapes the scoreboard once per second (20 ticks) to save FPS
        tickTimer++;
        if (tickTimer >= 20) {
            tickTimer = 0;
            updateFromScoreboard();
        }
    }

    private void updateFromScoreboard() {
        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        ScoreObjective sidebar = scoreboard.getObjectiveInDisplaySlot(1);
        if (sidebar == null) return;

        Collection<Score> scores = scoreboard.getSortedScores(sidebar);
        for (Score score : scores) {
            ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
            String line = ScorePlayerTeam.formatPlayerName(team, score.getPlayerName());
            String unformatted = EnumChatFormatting.getTextWithoutFormattingCodes(line);

            if (unformatted.contains("Gold:")) {
                String goldStr = unformatted.replaceAll("[^0-9]", "");
                if (!goldStr.isEmpty()) {
                    currentGold = Double.parseDouble(goldStr);
                }
            }

            if (unformatted.contains("Level:")) {
                if (unformatted.contains("-")) {
                    String[] split = unformatted.split("-");
                    currentPrestige = split[0].replaceAll("[^a-zA-Z]", "");
                } else {
                    currentPrestige = "0";
                }
            }
        }
    }

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) return;
        if (mc.currentScreen instanceof EditHUDGui) return;
        render(false);
    }

    public void render(boolean isEditing) {
        if (!enabled || mc.theWorld == null) return;

        FontRenderer fr = mc.fontRendererObj;
        int currentY = hudY;

        long diff = System.currentTimeMillis() - startTime;
        long h = (diff / (1000 * 60 * 60)) % 24;
        long m = (diff / (1000 * 60)) % 60;
        long s = (diff / 1000) % 60;
        String time = String.format("%02d:%02d:%02d", h, m, s);

        List<String> lines = new ArrayList<>();
        lines.add(EnumChatFormatting.GOLD + "" + EnumChatFormatting.BOLD + "Session Stats:");
        lines.add(EnumChatFormatting.YELLOW + "Playtime: " + EnumChatFormatting.WHITE + time);

        // Instantly grabs your exact prestige data from memory
        PitDataHandler.PrestigeData pData = PitDataHandler.getPrestige(currentPrestige);

        if (pData != null) {
            lines.add(EnumChatFormatting.YELLOW + "Prestige Req: " + EnumChatFormatting.LIGHT_PURPLE + pData.xpToPrestige + " XP");

            if (!pData.goldToNext.equals("N/A")) {
                double reqGold = Double.parseDouble(pData.goldToNext.replaceAll("[^0-9]", ""));
                double left = reqGold - currentGold;
                if (left < 0) left = 0;

                lines.add(EnumChatFormatting.YELLOW + "Target: " + EnumChatFormatting.GOLD + pData.goldToNext + "g");
                lines.add(EnumChatFormatting.YELLOW + "Gold Left: " + (left > 0 ? EnumChatFormatting.RED : EnumChatFormatting.GREEN) + String.format("%,.0f", left) + "g");
            } else {
                lines.add(EnumChatFormatting.YELLOW + "Target: " + EnumChatFormatting.GREEN + "MAX PRESTIGE");
            }
        } else {
            lines.add(EnumChatFormatting.GRAY + "Awaiting Scoreboard data...");
        }

        int maxWidth = 0;
        for (String line : lines) {
            fr.drawStringWithShadow(line, hudX, currentY, 0xFFFFFF);
            maxWidth = Math.max(maxWidth, fr.getStringWidth(line));
            currentY += fr.FONT_HEIGHT + 2;
        }

        this.width = maxWidth;
        this.height = currentY - hudY;

        if (isEditing) {
            Gui.drawRect(hudX - 2, hudY - 2, hudX + width + 2, hudY + height + 2, 0x44888888);
        }
    }

    public boolean isHovered(int mouseX, int mouseY) {
        return mouseX >= hudX - 2 && mouseX <= hudX + width + 2 && mouseY >= hudY - 2 && mouseY <= hudY + height + 2;
    }
}