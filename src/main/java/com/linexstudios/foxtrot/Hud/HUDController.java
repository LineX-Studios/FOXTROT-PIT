package com.linexstudios.foxtrot.Hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class HUDController {
    public static boolean dragMode = false;
    public static boolean enabled = true;

    private static final NickedHUD nickedHUD = new NickedHUD();
    private static final EnemyHUD enemyHUD = new EnemyHUD();

    public static final KeyBinding dragHudKey = new KeyBinding(
            "key.foxtrot.draghud", Keyboard.KEY_U, "key.categories.foxtrot");

    static {
        ClientRegistry.registerKeyBinding(dragHudKey);
    }

    public static void setEnabled(boolean state) {
        enabled = state;
    }

    public static void toggleDragMode() {
        dragMode = !dragMode;
        NickedHUD.dragMode = dragMode;
        EnemyHUD.dragMode = dragMode;
        
        if (dragMode) {
            Minecraft.getMinecraft().mouseHelper.ungrabMouseCursor();
        } else {
            Minecraft.getMinecraft().mouseHelper.grabMouseCursor();
        }
    }

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (!enabled || Minecraft.getMinecraft().theWorld == null) return;

        if (dragMode && event.type == RenderGameOverlayEvent.ElementType.ALL) {
            ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
            Gui.drawRect(0, 0, sr.getScaledWidth(), sr.getScaledHeight(), 0x88000000);
        }

        if (event.type == RenderGameOverlayEvent.ElementType.TEXT) {
            if (NickedHUD.enabled) nickedHUD.onRender(event);
            if (EnemyHUD.enabled) enemyHUD.onRender(event);
        }
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (dragHudKey.isPressed()) {
            toggleDragMode();
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (!dragMode || event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen != null) return; 

        ScaledResolution sr = new ScaledResolution(mc);
        int mouseX = Mouse.getX() * sr.getScaledWidth() / mc.displayWidth;
        int mouseY = sr.getScaledHeight() - Mouse.getY() * sr.getScaledHeight() / mc.displayHeight - 1;

        nickedHUD.handleDrag(mouseX, mouseY);
        enemyHUD.handleDrag(mouseX, mouseY);
    }
}