package com.linexstudios.foxtrot.Hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

public class HUDController {
    public static boolean enabled = true;
    public static boolean dragMode = false;

    public static final KeyBinding dragHudKey = new KeyBinding("key.foxtrot.draghud", Keyboard.KEY_U, "key.categories.foxtrot");

    static {
        ClientRegistry.registerKeyBinding(dragHudKey);
    }

    public static void setEnabled(boolean state) {
        enabled = state;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();

        // If your command sets dragMode to true, this safely opens the GUI
        // after the chat box closes (prevents Minecraft from glitching the GUI)
        if (dragMode && !(mc.currentScreen instanceof EditHUDGui)) {
            if (!(mc.currentScreen instanceof GuiChat)) { 
                mc.displayGuiScreen(new EditHUDGui());
            }
        }
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (dragHudKey.isPressed()) {
            dragMode = !dragMode;
        }
    }
}