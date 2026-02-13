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

@Mod(modid = "foxtrot", name = "Foxtrot", version = "0.2.9", acceptedMinecraftVersions = "[1.8.9]")
public class Foxtrot {
    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        // Load saved configuration
        ConfigHandler.loadConfig();

        // Register HUDs and ESP overlays
        MinecraftForge.EVENT_BUS.register(new EnemyHUD());
        MinecraftForge.EVENT_BUS.register(new NickedHUD());
        MinecraftForge.EVENT_BUS.register(new EnemyESP());

        // Register ban detection
        MinecraftForge.EVENT_BUS.register(new WhoGotBanned());

        // Register AutoDenick listener
        MinecraftForge.EVENT_BUS.register(new AutoDenick());

        // Register commands
        ClientCommandHandler.instance.registerCommand(new CommandFoxtrot());

        System.out.println("[Foxtrot] Loaded.");
    }
}
