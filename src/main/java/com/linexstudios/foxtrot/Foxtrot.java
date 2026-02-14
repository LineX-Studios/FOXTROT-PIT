package com.linexstudios.foxtrot;

import com.linexstudios.foxtrot.Commands.CommandFoxtrot;
import com.linexstudios.foxtrot.Denick.AutoDenick;
import com.linexstudios.foxtrot.Denick.NickScanner; // <-- THIS IS THE FIX
import com.linexstudios.foxtrot.Enemy.EnemyESP;
import com.linexstudios.foxtrot.Handler.ConfigHandler;
import com.linexstudios.foxtrot.Handler.KeybindHandler;
import com.linexstudios.foxtrot.Hud.EnemyHUD;
import com.linexstudios.foxtrot.Hud.NickedHUD;
import com.linexstudios.foxtrot.Hud.NameTags;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@Mod(modid = "foxtrot", name = "Foxtrot", version = "0.3.6", acceptedMinecraftVersions = "[1.8.9]")
public class Foxtrot {
    
    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        // Load saved configuration
        ConfigHandler.loadConfig();

        // 1. Initialize the Keybinds so they appear in your ESC -> Controls menu
        KeybindHandler.init();

        // Register HUDs and ESP overlays
        MinecraftForge.EVENT_BUS.register(EnemyHUD.instance);
        MinecraftForge.EVENT_BUS.register(NickedHUD.instance);
        MinecraftForge.EVENT_BUS.register(new EnemyESP()); 
        
        // Register the new NameTags module
        MinecraftForge.EVENT_BUS.register(NameTags.instance);

        // Register ban detection
        MinecraftForge.EVENT_BUS.register(new WhoGotBanned());

        // Register AutoDenick and NickScanner listeners
        MinecraftForge.EVENT_BUS.register(AutoDenick.instance);
        MinecraftForge.EVENT_BUS.register(NickScanner.instance);
        
        // 2. Register the KeybindHandler to listen for when you actually press 'X'
        MinecraftForge.EVENT_BUS.register(new KeybindHandler());

        // Register commands
        ClientCommandHandler.instance.registerCommand(new CommandFoxtrot());

        System.out.println("[Foxtrot] Loaded.");
    }
}