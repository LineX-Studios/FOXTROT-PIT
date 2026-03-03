package com.linexstudios.foxtrot.Hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class PlayerCounterHUD extends DraggableHUD {
    public static PlayerCounterHUD instance = new PlayerCounterHUD();
    public static boolean enabled = true;

    // Independent colors
    public static int prefixColor = 0xFFFFFF; // Color for "Current Players: "
    public static int countColor = 0xAAAAAA;  // Color for "21/81"

    private int maxPlayers = 81;
    private int players = 0;
    private int ticks = 0;
    private Minecraft mc = Minecraft.getMinecraft();

    public PlayerCounterHUD() {
        super("Player Counter", 10, 70); 
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && mc.theWorld != null) {
            ticks++;
            // Scan every 20 ticks (1 second) to prevent network lag
            if (ticks >= 20) {
                ticks = 0;
                if (mc.getNetHandler() != null && mc.getNetHandler().getPlayerInfoMap() != null) {
                    players = mc.getNetHandler().getPlayerInfoMap().size();
                }
            }
        }
    }

    @Override
    public void draw(boolean isEditing) {
        if (!enabled || mc.theWorld == null) return;

        FontRenderer fr = mc.fontRendererObj;
        String prefix = "Current Players: ";
        String countText;

        if (isEditing) {
            countText = "21/81"; // Dummy text for GUI
        } else {
            countText = players + "/" + maxPlayers; // Real text in-game
        }

        int prefixWidth = fr.getStringWidth(prefix);
        int countWidth = fr.getStringWidth(countText);

        // Update the bounding box for DraggableHUD so it can be clicked/dragged
        this.width = prefixWidth + countWidth;
        this.height = fr.FONT_HEIGHT;

        // Draw the two parts independently with their own colors
        fr.drawStringWithShadow(prefix, 0, 0, prefixColor);
        fr.drawStringWithShadow(countText, prefixWidth, 0, countColor);
    }
}