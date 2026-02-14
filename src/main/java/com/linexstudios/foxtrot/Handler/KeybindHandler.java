package com.linexstudios.foxtrot.Handler;

import com.linexstudios.foxtrot.Hud.NameTags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

public class KeybindHandler {
    // Creates the keybinding object
    public static KeyBinding toggleNameTags;

    public static void init() {
        // "Toggle NameTags ESP" = Name in the controls menu
        // Keyboard.KEY_X = Default key is X
        // "Foxtrot Mod" = The category it will show up under in the controls menu
        toggleNameTags = new KeyBinding("Toggle Names", Keyboard.KEY_X, "Foxtrot");
        
        // Registers it into Minecraft's native keybind system
        ClientRegistry.registerKeyBinding(toggleNameTags);
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        // Checks if the key was pressed this exact tick
        if (toggleNameTags.isPressed()) {
            // Flip the toggle
            NameTags.enabled = !NameTags.enabled;
            
            // Save to settings.txt so it remembers your choice
            ConfigHandler.saveConfig();

            // Send a quick chat message so you know it worked
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer != null) {
                String status = NameTags.enabled ? EnumChatFormatting.GREEN + "ON" : EnumChatFormatting.RED + "OFF";
                mc.thePlayer.addChatMessage(new ChatComponentText(
                        EnumChatFormatting.GRAY + "[" + EnumChatFormatting.RED + "PIT" + EnumChatFormatting.GRAY + "] " +
                        EnumChatFormatting.YELLOW + "NameTags ESP: " + status
                ));
            }
        }
    }
}