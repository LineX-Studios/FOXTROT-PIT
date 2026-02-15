package com.linexstudios.foxtrot;

import com.linexstudios.foxtrot.Commands.CommandFoxtrot;
import com.linexstudios.foxtrot.Denick.AutoDenick;
import com.linexstudios.foxtrot.Denick.NickScanner;
import com.linexstudios.foxtrot.Enemy.EnemyESP;
import com.linexstudios.foxtrot.Handler.ConfigHandler;
import com.linexstudios.foxtrot.Handler.KeybindHandler;
import com.linexstudios.foxtrot.Hud.EnemyHUD;
import com.linexstudios.foxtrot.Hud.NickedHUD;
import com.linexstudios.foxtrot.Hud.NameTags;
import com.linexstudios.foxtrot.Hud.FriendsHUD;
import com.linexstudios.foxtrot.Render.FriendsESP;
import com.linexstudios.foxtrot.Combat.AutoClicker; 
import com.linexstudios.foxtrot.Render.ChestESP;
import net.minecraft.client.settings.KeyBinding; 
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry; 
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import org.lwjgl.input.Keyboard; 

@Mod(modid = "foxtrot", name = "Foxtrot", version = "0.5.6", acceptedMinecraftVersions = "[1.8.9]")
public class Foxtrot {
    
    // The shared KeyBinding for the AutoClicker
    public static KeyBinding toggleCombatKey;
    
    // NEW: The KeyBinding for toggling Inventory Fill
    public static KeyBinding toggleInvFillKey;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        // Load saved configuration
        ConfigHandler.loadConfig();

        // 1. Initialize vanilla Keybinds
        KeybindHandler.init();
        
        // 2. Setup the "Toggle Combat" key (Arrow Down) for the AutoClicker
        toggleCombatKey = new KeyBinding("Toggle Combat", Keyboard.KEY_DOWN, "Foxtrot");
        ClientRegistry.registerKeyBinding(toggleCombatKey);

        // 3. Setup the "Toggle Inv Fill" key (Arrow Right)
        toggleInvFillKey = new KeyBinding("Toggle Inv Fill", Keyboard.KEY_RIGHT, "Foxtrot");
        ClientRegistry.registerKeyBinding(toggleInvFillKey);

        // Register HUDs and ESP overlays
        MinecraftForge.EVENT_BUS.register(EnemyHUD.instance);
        MinecraftForge.EVENT_BUS.register(NickedHUD.instance);
        MinecraftForge.EVENT_BUS.register(new EnemyESP()); 
        MinecraftForge.EVENT_BUS.register(ChestESP.instance);
        MinecraftForge.EVENT_BUS.register(FriendsHUD.instance);
        MinecraftForge.EVENT_BUS.register(new FriendsESP());
        MinecraftForge.EVENT_BUS.register(new com.linexstudios.foxtrot.Render.NickedRender());
        
        // Register Combat modules
        MinecraftForge.EVENT_BUS.register(AutoClicker.instance); // REGISTERED AUTOCLICKER

        // Register the NameTags module
        MinecraftForge.EVENT_BUS.register(NameTags.instance);

        // Register ban detection
        MinecraftForge.EVENT_BUS.register(new WhoGotBanned());

        // Register AutoDenick and NickScanner listeners
        MinecraftForge.EVENT_BUS.register(AutoDenick.instance);
        MinecraftForge.EVENT_BUS.register(NickScanner.instance);
        
        // Register the KeybindHandler to listen for keyboard events
        MinecraftForge.EVENT_BUS.register(new KeybindHandler());

        // Register commands
        ClientCommandHandler.instance.registerCommand(new CommandFoxtrot());

        System.out.println("[Foxtrot] Loaded.");
    }
}