package com.linexstudios.foxtrot.Hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

public class HUDController {
    public static boolean enabled = true;
    public static boolean dragMode = false; // Kept to prevent CommandFoxtrot compile errors

    public static final NickedHUD nickedHUD = new NickedHUD();
    public static final EnemyHUD enemyHUD = new EnemyHUD();

    public static final KeyBinding dragHudKey = new KeyBinding("key.foxtrot.draghud", Keyboard.KEY_U, "key.categories.foxtrot");

    static {
        ClientRegistry.registerKeyBinding(dragHudKey);
    }

    public static void setEnabled(boolean state) {
        enabled = state;
    }

    public static void toggleDragMode() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen instanceof EditHUDGui) {
            mc.displayGuiScreen(null);
        } else {
            mc.displayGuiScreen(new EditHUDGui());
        }
    }

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (!enabled || Minecraft.getMinecraft().theWorld == null) return;

        // Render standard HUD elements
        if (event.type == RenderGameOverlayEvent.ElementType.TEXT) {
            // Do NOT render normally if the editor is open (the editor handles drawing them)
            if (!(Minecraft.getMinecraft().currentScreen instanceof EditHUDGui)) {
                if (NickedHUD.enabled) nickedHUD.render(false);
                if (EnemyHUD.enabled) enemyHUD.render(false);
            }
        }
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (dragHudKey.isPressed()) {
            toggleDragMode();
        }
    }
}