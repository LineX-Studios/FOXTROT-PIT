package com.linexstudios.foxtrot.Hud;

import com.linexstudios.foxtrot.Events.EventManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.lwjgl.input.Mouse;

import java.util.List;

public class EventsHUD {
    private final Minecraft mc = Minecraft.getMinecraft();

    public static boolean enabled = true;
    public static int hudX = 200;
    public static int hudY = 80;
    public static boolean dragMode = false;

    private static boolean dragging = false;
    private static int dragOffsetX;
    private static int dragOffsetY;

    public void onRender(RenderGameOverlayEvent.Post event) {
        if (!enabled || event.type != RenderGameOverlayEvent.ElementType.TEXT) return;

        FontRenderer fr = mc.fontRendererObj;
        int xPos = hudX;
        int yPos = hudY;

        fr.drawStringWithShadow(EnumChatFormatting.AQUA + "" + EnumChatFormatting.BOLD + "Events:", xPos, yPos, 16777215);
        yPos += fr.FONT_HEIGHT + 1;

        List<String> events = EventManager.getUpcomingEvents();
        for (String line : events) {
            fr.drawStringWithShadow(EnumChatFormatting.WHITE + line, xPos, yPos, 16777215);
            yPos += fr.FONT_HEIGHT;
        }

        if (dragMode) {
            if (Mouse.isButtonDown(0)) {
                if (!dragging) {
                    dragging = true;
                    dragOffsetX = Mouse.getX() - hudX;
                    dragOffsetY = mc.displayHeight - Mouse.getY() - hudY;
                }
                hudX = Mouse.getX() - dragOffsetX;
                hudY = mc.displayHeight - Mouse.getY() - dragOffsetY;
            } else {
                dragging = false;
            }
        }
    }
}
