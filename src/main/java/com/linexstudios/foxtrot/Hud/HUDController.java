package com.linexstudios.foxtrot.Hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class HUDController {
    public static boolean dragMode = false;
    public static boolean enabled = true;

    private static final NickedHUD nickedHUD = new NickedHUD();
    private static final EnemyHUD enemyHUD = new EnemyHUD();

    // Keybind to toggle drag mode (Default: U)
    public static final KeyBinding dragHudKey = new KeyBinding(
            "key.foxtrot.draghud", Keyboard.KEY_U, "key.categories.foxtrot");

    static {
        ClientRegistry.registerKeyBinding(dragHudKey);
    }

    public static void toggleDragMode() {
        dragMode = !dragMode;
        NickedHUD.dragMode = dragMode;
        EnemyHUD.dragMode = dragMode;
        
        Minecraft mc = Minecraft.getMinecraft();
        // Step 1: Handle mouse visibility
        if (dragMode) {
            mc.mouseHelper.ungrabMouseCursor();
        } else {
            // Re-lock mouse to game when finished
            mc.mouseHelper.grabMouseCursor();
        }
    }

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (!enabled || Minecraft.getMinecraft().theWorld == null) return;

        // Step 2: Overlay a dark tint when in drag mode to show it's active
        if (dragMode && event.type == RenderGameOverlayEvent.ElementType.ALL) {
            ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
            Gui.drawRect(0, 0, sr.getScaledWidth(), sr.getScaledHeight(), 0x88000000);
        }

        // Step 3: Draw the actual HUD elements during the TEXT phase
        if (event.type == RenderGameOverlayEvent.ElementType.TEXT) {
            if (NickedHUD.enabled) nickedHUD.onRender(event);
            if (EnemyHUD.enabled) enemyHUD.onRender(event);
        }
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        // Toggle drag mode when U is pressed
        if (dragHudKey.isPressed()) {
            toggleDragMode();
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        // Step 4: Handle dragging logic only when dragMode is ON
        if (!dragMode || event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getMinecraft();
        
        // Safety: If the player opens an inventory/menu, force-disable drag mode
        if (mc.currentScreen != null) {
            dragMode = false;
            NickedHUD.dragMode = false;
            EnemyHUD.dragMode = false;
            return;
        }

        // Step 5: CALCULATE SCALED MOUSE POSITION
        // This math converts raw pixels (1920x1080) into GUI scale (e.g., 960x540)
        ScaledResolution sr = new ScaledResolution(mc);
        int mouseX = Mouse.getX() * sr.getScaledWidth() / mc.displayWidth;
        int mouseY = sr.getScaledHeight() - Mouse.getY() * sr.getScaledHeight() / mc.displayHeight - 1;

        // Forward the calculated mouse position to the HUD classes
        nickedHUD.handleDrag(mouseX, mouseY);
        enemyHUD.handleDrag(mouseX, mouseY);
    }
}