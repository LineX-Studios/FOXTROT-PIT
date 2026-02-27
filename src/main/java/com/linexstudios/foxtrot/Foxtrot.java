package com.linexstudios.foxtrot;

import com.linexstudios.foxtrot.Commands.CommandFoxtrot;
import com.linexstudios.foxtrot.Denick.AutoDenick;
import com.linexstudios.foxtrot.Denick.NickScanner;
import com.linexstudios.foxtrot.Enemy.EnemyESP;
import com.linexstudios.foxtrot.Handler.ConfigHandler;
import com.linexstudios.foxtrot.Handler.KeybindHandler;
import com.linexstudios.foxtrot.Handler.TelemetryManager;
import com.linexstudios.foxtrot.Hud.EnemyHUD;
import com.linexstudios.foxtrot.Hud.NickedHUD;
import com.linexstudios.foxtrot.Hud.NameTags;
import com.linexstudios.foxtrot.Hud.FriendsHUD;
import com.linexstudios.foxtrot.Hud.DarksHUD;
import com.linexstudios.foxtrot.Render.FriendsESP;
import com.linexstudios.foxtrot.Render.PitESP;
import com.linexstudios.foxtrot.Render.LowLifeMystic;
import com.linexstudios.foxtrot.Render.FocusManager;
import com.linexstudios.foxtrot.Combat.AutoClicker;
import com.linexstudios.foxtrot.Hud.SessionStatsHUD;
import com.linexstudios.foxtrot.Hud.PotionHUD;
import com.linexstudios.foxtrot.Hud.ArmorHUD;
import com.linexstudios.foxtrot.Hud.CoordsHUD;
import com.linexstudios.foxtrot.Hud.ToggleSprintModule;
import com.linexstudios.foxtrot.Util.EnemyAlert;
import com.linexstudios.foxtrot.Util.Ranks;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.lwjgl.input.Keyboard;
import com.linexstudios.foxtrot.Misc.AutoPantSwap;
import com.linexstudios.foxtrot.Misc.AutoBulletTime;
import com.linexstudios.foxtrot.Misc.AutoGhead;
import com.linexstudios.foxtrot.Misc.AutoQuickMath;
import com.linexstudios.foxtrot.Misc.TelebowTimer;
import com.linexstudios.foxtrot.Hud.CPSModule;

@Mod(modid = "foxtrot", name = "Foxtrot", version = "${version}", acceptedMinecraftVersions = "[1.8.9]")
public class Foxtrot {

    public static KeyBinding toggleCombatKey;
    public static KeyBinding toggleInvFillKey;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // --- 1. DISABLE FORGE SPLASH SCREEN ---
        ConfigHandler.disableForgeSplashScreen();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        ConfigHandler.loadConfig();
        
        // --- START TELEMETRY HEARTBEAT ---
        TelemetryManager.initialize();

        KeybindHandler.init();

        toggleCombatKey = new KeyBinding("Toggle Combat", Keyboard.KEY_DOWN, "Foxtrot");
        ClientRegistry.registerKeyBinding(toggleCombatKey);

        toggleInvFillKey = new KeyBinding("Toggle Inv Fill", Keyboard.KEY_RIGHT, "Foxtrot");
        ClientRegistry.registerKeyBinding(toggleInvFillKey);

        MinecraftForge.EVENT_BUS.register(EnemyHUD.instance);
        MinecraftForge.EVENT_BUS.register(NickedHUD.instance);
        MinecraftForge.EVENT_BUS.register(new EnemyESP());
        MinecraftForge.EVENT_BUS.register(PitESP.instance);
        MinecraftForge.EVENT_BUS.register(LowLifeMystic.instance);
        MinecraftForge.EVENT_BUS.register(FriendsHUD.instance);
        MinecraftForge.EVENT_BUS.register(new FriendsESP());
        MinecraftForge.EVENT_BUS.register(CPSModule.instance);
        MinecraftForge.EVENT_BUS.register(com.linexstudios.foxtrot.Hud.FPSModule.instance);
        MinecraftForge.EVENT_BUS.register(new com.linexstudios.foxtrot.Render.NickedRender());
        MinecraftForge.EVENT_BUS.register(com.linexstudios.foxtrot.Hud.EventHUD.instance);
        MinecraftForge.EVENT_BUS.register(com.linexstudios.foxtrot.Hud.RegHUD.instance);
        MinecraftForge.EVENT_BUS.register(com.linexstudios.foxtrot.Hud.DarksHUD.instance);
        MinecraftForge.EVENT_BUS.register(PotionHUD.instance);
        MinecraftForge.EVENT_BUS.register(ArmorHUD.instance);
        MinecraftForge.EVENT_BUS.register(CoordsHUD.instance);
        MinecraftForge.EVENT_BUS.register(ToggleSprintModule.instance);
        MinecraftForge.EVENT_BUS.register(AutoClicker.instance); 
        MinecraftForge.EVENT_BUS.register(NameTags.instance);
        MinecraftForge.EVENT_BUS.register(new WhoGotBanned());
        MinecraftForge.EVENT_BUS.register(AutoDenick.instance);
        MinecraftForge.EVENT_BUS.register(NickScanner.instance);
        MinecraftForge.EVENT_BUS.register(SessionStatsHUD.instance);
        MinecraftForge.EVENT_BUS.register(new EnemyAlert());
        MinecraftForge.EVENT_BUS.register(new KeybindHandler());
        MinecraftForge.EVENT_BUS.register(AutoPantSwap.instance);
        MinecraftForge.EVENT_BUS.register(AutoGhead.instance);
        MinecraftForge.EVENT_BUS.register(AutoBulletTime.instance);
        MinecraftForge.EVENT_BUS.register(AutoQuickMath.instance);
        MinecraftForge.EVENT_BUS.register(TelebowTimer.instance);
        MinecraftForge.EVENT_BUS.register(Ranks.instance);
        MinecraftForge.EVENT_BUS.register(FocusManager.instance);

        ClientCommandHandler.instance.registerCommand(new CommandFoxtrot());

        System.out.println("[Foxtrot] Loaded.");
    }
}