package com.linexstudios.foxtrot.Hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
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

    // Keybind to toggle drag mode (default ESC, but rebindable in Controls menu)
    public static final KeyBinding dragHudKey = new KeyBinding(
            "key.foxtrot.draghud", Keyboard.KEY_ESCAPE, "key.categories.foxtrot");

    static {
        ClientRegistry.registerKeyBinding(dragHudKey);
    }

    public static void toggleDragMode() {
        dragMode = !dragMode;
        NickedHUD.dragMode = dragMode;
        EnemyHUD.dragMode = dragMode;
    }

    public static void setEnabled(boolean state) {
        enabled = state;
        NickedHUD.enabled = state;
        EnemyHUD.enabled = state;
    }

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (!enabled) return;

        // If drag mode is active, gray out the screen
        if (dragMode && event.type == RenderGameOverlayEvent.ElementType.ALL) {
            Gui.drawRect(0, 0, Minecraft.getMinecraft().displayWidth,
                    Minecraft.getMinecraft().displayHeight, 0x88000000);
        }

        if (NickedHUD.enabled) nickedHUD.onRender(event);
        if (EnemyHUD.enabled) enemyHUD.onRender(event);
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

        // Handle dragging logic
        if (Mouse.isButtonDown(0)) { // left mouse held
            int mouseX = Mouse.getX() * Minecraft.getMinecraft().displayWidth / Minecraft.getMinecraft().displayWidth;
            int mouseY = Minecraft.getMinecraft().displayHeight - Mouse.getY() * Minecraft.getMinecraft().displayHeight / Minecraft.getMinecraft().displayHeight - 1;

            // Example: forward drag events to HUDs
            nickedHUD.handleDrag(mouseX, mouseY);
            enemyHUD.handleDrag(mouseX, mouseY);
        }
    }
}
