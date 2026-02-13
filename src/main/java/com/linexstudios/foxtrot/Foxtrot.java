package com.linexstudios.foxtrot;

import com.linexstudios.foxtrot.Commands.CommandFoxtrot;
import com.linexstudios.foxtrot.Denick.AutoDenick;
import com.linexstudios.foxtrot.Enemy.EnemyESP;
import com.linexstudios.foxtrot.Handler.ConfigHandler;
import com.linexstudios.foxtrot.Hud.EnemyHUD;
import com.linexstudios.foxtrot.Hud.NickedHUD;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@Mod(modid = "foxtrot", name = "Foxtrot", version = "0.3.3", acceptedMinecraftVersions = "[1.8.9]")
public class Foxtrot {
    
    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        // Load saved configuration
        ConfigHandler.loadConfig();

        // Register HUDs and ESP overlays (MUST use .instance so the HUD Editor dragging works!)
        MinecraftForge.EVENT_BUS.register(EnemyHUD.instance);
        MinecraftForge.EVENT_BUS.register(NickedHUD.instance);
        MinecraftForge.EVENT_BUS.register(new EnemyESP()); 

        // Register ban detection
        MinecraftForge.EVENT_BUS.register(new WhoGotBanned());

        // Register AutoDenick listener (MUST use .instance to link properly)
        MinecraftForge.EVENT_BUS.register(AutoDenick.instance);

        // Register commands
        ClientCommandHandler.instance.registerCommand(new CommandFoxtrot());

        System.out.println("[Foxtrot] Loaded.");
    }
}