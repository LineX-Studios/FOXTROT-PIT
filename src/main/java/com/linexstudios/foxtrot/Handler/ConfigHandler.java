package com.linexstudios.foxtrot.Handler;

import com.linexstudios.foxtrot.Hud.EnemyHUD;
import com.linexstudios.foxtrot.Hud.FriendsHUD;
import com.linexstudios.foxtrot.Hud.NickedHUD;
import com.linexstudios.foxtrot.Hud.NameTags;
import com.linexstudios.foxtrot.Hud.EditHUDGui;
import com.linexstudios.foxtrot.Hud.SessionStatsHUD;
import com.linexstudios.foxtrot.Hud.EventHUD; 
import com.linexstudios.foxtrot.Hud.RegHUD;
import com.linexstudios.foxtrot.Hud.DarksHUD;
import com.linexstudios.foxtrot.Hud.PotionHUD;
import com.linexstudios.foxtrot.Hud.ArmorHUD;
import com.linexstudios.foxtrot.Hud.CoordsHUD;
import com.linexstudios.foxtrot.Hud.ToggleSprintModule;
import com.linexstudios.foxtrot.Hud.BossBarModule;
import com.linexstudios.foxtrot.Hud.CPSModule;
import com.linexstudios.foxtrot.Hud.FPSModule;
import com.linexstudios.foxtrot.Hud.DraggableHUD;
import com.linexstudios.foxtrot.Hud.TelebowHUD;
import com.linexstudios.foxtrot.Hud.PlayerCounterHUD; 
import com.linexstudios.foxtrot.Denick.AutoDenick;
import com.linexstudios.foxtrot.Combat.AutoClicker;
import com.linexstudios.foxtrot.Misc.AutoBulletTime;
import com.linexstudios.foxtrot.Misc.AutoPantSwap; 
import com.linexstudios.foxtrot.Misc.AutoGhead; 
import com.linexstudios.foxtrot.Misc.AutoQuickMath; 
import com.linexstudios.foxtrot.Util.Ranks;
import com.linexstudios.foxtrot.Render.PitESP; 
import com.linexstudios.foxtrot.Render.FriendsESP;
import com.linexstudios.foxtrot.Render.NickedRender;
import com.linexstudios.foxtrot.Render.LowLifeMystic;
import com.linexstudios.foxtrot.Enemy.EnemyManager; // NEW IMPORT

import net.minecraft.client.Minecraft;
import java.io.*;
import java.util.*;

public class ConfigHandler {
    public static boolean telemetryEnabled = true; 
    public static boolean autoUpdateEnabled = true; 

    private static final File configDir = new File("config/Foxtrot");
    private static final File enemyFile = new File(configDir, "enemies.txt");
    private static final File friendsFile = new File(configDir, "friends.txt");
    private static final File settingsFile = new File(configDir, "settings.txt");

    private static int getInt(Properties props, String key, int def) {
        try { return (int) Float.parseFloat(props.getProperty(key, String.valueOf(def))); } catch (Exception e) { return def; }
    }
    private static float getFloat(Properties props, String key, float def) {
        try { return Float.parseFloat(props.getProperty(key, String.valueOf(def))); } catch (Exception e) { return def; }
    }
    private static boolean getBool(Properties props, String key, boolean def) {
        try { return Boolean.parseBoolean(props.getProperty(key, String.valueOf(def))); } catch (Exception e) { return def; }
    }

    private static void initHUDs() {
        Object[] forceLoad = {
            PotionHUD.instance, ArmorHUD.instance, CoordsHUD.instance, EnemyHUD.instance,
            NickedHUD.instance, FriendsHUD.instance, SessionStatsHUD.instance, EventHUD.instance,
            RegHUD.instance, DarksHUD.instance, ToggleSprintModule.instance, CPSModule.instance, FPSModule.instance, 
            BossBarModule.instance, TelebowHUD.instance, PlayerCounterHUD.instance
        };
    }

    public static void disableForgeSplashScreen() {
        try {
            File forgeConfigDir = new File(Minecraft.getMinecraft().mcDataDir, "config");
            File splashFile = new File(forgeConfigDir, "splash.properties");
            if (splashFile.exists()) {
                Properties props = new Properties();
                FileInputStream in = new FileInputStream(splashFile);
                props.load(in); in.close();
                if ("true".equals(props.getProperty("enabled", "true"))) {
                    props.setProperty("enabled", "false"); 
                    FileOutputStream out = new FileOutputStream(splashFile);
                    props.store(out, "Automatically disabled by Foxtrot Client"); out.close();
                }
            }
        } catch (Exception e) {}
    }

    public static void loadConfig() {
        initHUDs();
        EnemyManager.loadCache(); // <--- LOADS THE UUID CACHE ON STARTUP

        try {
            if (!configDir.exists()) configDir.mkdirs();

            if (enemyFile.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(enemyFile));
                String line; EnemyHUD.targetList.clear();
                while ((line = reader.readLine()) != null) if (!line.trim().isEmpty()) EnemyHUD.targetList.add(line.trim());
                reader.close();
            }

            if (friendsFile.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(friendsFile));
                String line; FriendsHUD.friendsList.clear();
                while ((line = reader.readLine()) != null) if (!line.trim().isEmpty()) FriendsHUD.friendsList.add(line.trim());
                reader.close();
            }

            if (settingsFile.exists()) {
                Properties props = new Properties();
                FileInputStream in = new FileInputStream(settingsFile);
                props.load(in); in.close();

                for (DraggableHUD hud : DraggableHUD.getRegistry()) {
                    String cleanName = hud.name.replaceAll("\\s+", "");
                    hud.x = getInt(props, cleanName + "X", hud.x);
                    hud.y = getInt(props, cleanName + "Y", hud.y);
                    hud.scale = getFloat(props, cleanName + "Scale", hud.scale);
                }

                PotionHUD.instance.isHorizontal = getBool(props, "potionHorizontal", false);
                PotionHUD.nameColor = getInt(props, "potionNameColor", 16777215); 
                PotionHUD.durationColor = getInt(props, "potionDurationColor", 11184810); 
                ArmorHUD.durabilityColor = getInt(props, "armorDurabilityColor", 16777215);
                ArmorHUD.instance.isHorizontal = getBool(props, "armorHorizontal", false);
                CoordsHUD.instance.isHorizontal = getBool(props, "coordsHorizontal", false);
                CoordsHUD.axisColor = getInt(props, "coordsAxisColor", 16733525); 
                CoordsHUD.numberColor = getInt(props, "coordsNumberColor", 16777215);
                
                ToggleSprintModule.instance.enabled = getBool(props, "toggleSprintEnabled", true);
                ToggleSprintModule.instance.toggleSprint = getBool(props, "tsSprint", true);
                ToggleSprintModule.instance.toggleSneak = getBool(props, "tsSneak", false);
                ToggleSprintModule.instance.wTapFix = getBool(props, "tsWTapFix", true);
                ToggleSprintModule.instance.flyBoost = getBool(props, "tsFlyBoost", true);
                ToggleSprintModule.instance.flyBoostAmount = getFloat(props, "tsFlyBoostAmount", 4.0f);
                ToggleSprintModule.instance.textColor = getInt(props, "tsTextColor", 16777215);

                CPSModule.showBackground = getBool(props, "cpsShowBg", true);
                CPSModule.textColor = getInt(props, "cpsTextColor", 16777215);
                FPSModule.showBackground = getBool(props, "fpsShowBg", true);
                FPSModule.textColor = getInt(props, "fpsTextColor", 16777215);

                EditHUDGui.collapsedX = getInt(props, "panelX", -1);
                EditHUDGui.collapsedY = getInt(props, "panelY", -1);
                EditHUDGui.panelCollapsed = getBool(props, "panelCollapsed", false);

                AutoClicker.enabled = getBool(props, "clickerEnabled", false);
                AutoClicker.leftClick = getBool(props, "clickerLeft", true);
                AutoClicker.fastPlaceEnabled = getBool(props, "fastPlace", false);
                AutoClicker.holdToClick = getBool(props, "clickerHoldToClick", true);
                AutoClicker.inventoryFill = getBool(props, "clickerInvFill", true);
                AutoClicker.breakBlocks = getBool(props, "clickerBreakBlocks", true);
                AutoClicker.limitItems = getBool(props, "clickerLimitItems", true);
                AutoClicker.inventoryFillCps = getFloat(props, "clickerInvFillCps", 15.0f);
                AutoClicker.minCps = getFloat(props, "clickerMinCps", 9.0f);
                AutoClicker.maxCps = getFloat(props, "clickerMaxCps", 13.0f);
                AutoClicker.randomMode = getInt(props, "clickerRandomMode", 1);
                String whitelistStr = props.getProperty("clickerWhitelist", "sword,axe,pickaxe");
                AutoClicker.itemWhitelist = new ArrayList<>(Arrays.asList(whitelistStr.split(",")));

                AutoDenick.enabled = getBool(props, "autoDenick", false);
                NickedRender.enabled = getBool(props, "nickedNametags", true);
                PitESP.espChests = getBool(props, "pitEspChests", true);
                PitESP.espDragonEggs = getBool(props, "pitEspDragonEggs", true);
                PitESP.espRaffleTickets = getBool(props, "pitEspRaffleTickets", true);
                PitESP.espMystics = getBool(props, "pitEspMystics", true);
                LowLifeMystic.enabled = getBool(props, "lowLifeMysticEnabled", true);

                AutoPantSwap.pantSwapEnabled = getBool(props, "autoPantSwap", true);
                AutoPantSwap.venomSwapEnabled = getBool(props, "autoVenomSwap", true);
                AutoPantSwap.autoPodEnabled = getBool(props, "autoPod", true);
                AutoGhead.enabled = getBool(props, "autoGhead", true);
                AutoQuickMath.enabled = getBool(props, "autoQuickMath", true);
                AutoBulletTime.enabled = getBool(props, "autoBulletTime", false);

                EnemyHUD.enabled = getBool(props, "enemyHudEnabled", true);
                EnemyHUD.notificationsEnabled = getBool(props, "enemyHudAlerts", true);
                EnemyHUD.debugMode = getBool(props, "enemyHudDebug", false);
                NickedHUD.enabled = getBool(props, "nickedHudEnabled", true);
                SessionStatsHUD.enabled = getBool(props, "sessionStatsEnabled", true);
                EventHUD.enabled = getBool(props, "eventHudEnabled", true);
                RegHUD.enabled = getBool(props, "regHudEnabled", true);
                DarksHUD.enabled = getBool(props, "darksHudEnabled", true); 
                PotionHUD.enabled = getBool(props, "potionHudEnabled", true);
                ArmorHUD.enabled = getBool(props, "armorHudEnabled", true);
                CoordsHUD.enabled = getBool(props, "coordsHudEnabled", true);
                BossBarModule.enabled = getBool(props, "bossBarEnabled", true);
                CPSModule.enabled = getBool(props, "cpsEnabled", true);
                FPSModule.enabled = getBool(props, "fpsEnabled", true);
                TelebowHUD.enabled = getBool(props, "telebowHudEnabled", true); 
                
                PlayerCounterHUD.enabled = getBool(props, "playerCounterEnabled", true);
                PlayerCounterHUD.prefixColor = getInt(props, "playerCounterPrefixColor", 0xFFFFFF);
                PlayerCounterHUD.countColor = getInt(props, "playerCounterCountColor", 0xAAAAAA);

                NameTags.enabled = getBool(props, "nameTagsEnabled", false);
                NameTags.showHealth = getBool(props, "nameTagsShowHealth", true);
                NameTags.showItems = getBool(props, "nameTagsShowItems", true);
                FriendsHUD.enabled = getBool(props, "friendsHudEnabled", true);
                FriendsESP.enabled = getBool(props, "friendsEspEnabled", true);

                com.linexstudios.foxtrot.Util.Ranks.isEnabled = getBool(props, "ranksEnabled", true);
                com.linexstudios.foxtrot.Util.Ranks.changeLevel = getBool(props, "ranksChangeLevel", true);
                com.linexstudios.foxtrot.Util.Ranks.targetLevel = getInt(props, "ranksTargetLevel", 120);
                com.linexstudios.foxtrot.Util.Ranks.changePrestige = getBool(props, "ranksChangePrestige", true);
                com.linexstudios.foxtrot.Util.Ranks.targetPrestige = getInt(props, "ranksTargetPrestige", 35);
                com.linexstudios.foxtrot.Util.Ranks.changeRank = getBool(props, "ranksChangeRank", true);
                com.linexstudios.foxtrot.Util.Ranks.targetRank = props.getProperty("ranksTargetRank", "staff");
                com.linexstudios.foxtrot.Util.Ranks.changeName = getBool(props, "ranksChangeName", false);
                com.linexstudios.foxtrot.Util.Ranks.targetName = props.getProperty("ranksTargetName", "");

                telemetryEnabled = getBool(props, "telemetryEnabled", true); 
                TelemetryManager.anonymousClientId = props.getProperty("telemetryId", "");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void saveConfig() {
        initHUDs();
        EnemyManager.saveCache(); // <--- SAVES THE UUID CACHE ON EXIT

        try {
            if (!configDir.exists()) configDir.mkdirs();

            PrintWriter enemyWriter = new PrintWriter(new FileWriter(enemyFile));
            for (String name : EnemyHUD.targetList) enemyWriter.println(name);
            enemyWriter.close();

            PrintWriter friendsWriter = new PrintWriter(new FileWriter(friendsFile));
            for (String name : FriendsHUD.friendsList) friendsWriter.println(name);
            friendsWriter.close();

            Properties props = new Properties();

            for (DraggableHUD hud : DraggableHUD.getRegistry()) {
                String cleanName = hud.name.replaceAll("\\s+", "");
                props.setProperty(cleanName + "X", String.valueOf(hud.x));
                props.setProperty(cleanName + "Y", String.valueOf(hud.y));
                props.setProperty(cleanName + "Scale", String.valueOf(hud.scale));
            }

            props.setProperty("potionHorizontal", String.valueOf(PotionHUD.instance.isHorizontal));
            props.setProperty("potionNameColor", String.valueOf(PotionHUD.nameColor));
            props.setProperty("potionDurationColor", String.valueOf(PotionHUD.durationColor));
            props.setProperty("armorDurabilityColor", String.valueOf(ArmorHUD.durabilityColor));
            props.setProperty("armorHorizontal", String.valueOf(ArmorHUD.instance.isHorizontal));
            props.setProperty("coordsHorizontal", String.valueOf(CoordsHUD.instance.isHorizontal));
            props.setProperty("coordsAxisColor", String.valueOf(CoordsHUD.axisColor));
            props.setProperty("coordsNumberColor", String.valueOf(CoordsHUD.numberColor));
            
            props.setProperty("toggleSprintEnabled", String.valueOf(ToggleSprintModule.instance.enabled));
            props.setProperty("tsSprint", String.valueOf(ToggleSprintModule.instance.toggleSprint));
            props.setProperty("tsSneak", String.valueOf(ToggleSprintModule.instance.toggleSneak));
            props.setProperty("tsWTapFix", String.valueOf(ToggleSprintModule.instance.wTapFix));
            props.setProperty("tsFlyBoost", String.valueOf(ToggleSprintModule.instance.flyBoost));
            props.setProperty("tsFlyBoostAmount", String.valueOf(ToggleSprintModule.instance.flyBoostAmount));
            props.setProperty("tsTextColor", String.valueOf(ToggleSprintModule.instance.textColor));

            props.setProperty("cpsShowBg", String.valueOf(CPSModule.showBackground));
            props.setProperty("cpsTextColor", String.valueOf(CPSModule.textColor));
            props.setProperty("fpsShowBg", String.valueOf(FPSModule.showBackground));
            props.setProperty("fpsTextColor", String.valueOf(FPSModule.textColor));

            props.setProperty("panelX", String.valueOf(EditHUDGui.collapsedX));
            props.setProperty("panelY", String.valueOf(EditHUDGui.collapsedY));
            props.setProperty("panelCollapsed", String.valueOf(EditHUDGui.panelCollapsed));

            props.setProperty("clickerEnabled", String.valueOf(AutoClicker.enabled));
            props.setProperty("clickerLeft", String.valueOf(AutoClicker.leftClick));
            props.setProperty("fastPlace", String.valueOf(AutoClicker.fastPlaceEnabled));
            props.setProperty("clickerHoldToClick", String.valueOf(AutoClicker.holdToClick));
            props.setProperty("clickerInvFill", String.valueOf(AutoClicker.inventoryFill));
            props.setProperty("clickerBreakBlocks", String.valueOf(AutoClicker.breakBlocks));
            props.setProperty("clickerLimitItems", String.valueOf(AutoClicker.limitItems));
            props.setProperty("clickerInvFillCps", String.valueOf(AutoClicker.inventoryFillCps));
            props.setProperty("clickerMinCps", String.valueOf(AutoClicker.minCps));
            props.setProperty("clickerMaxCps", String.valueOf(AutoClicker.maxCps));
            props.setProperty("clickerRandomMode", String.valueOf(AutoClicker.randomMode));
            String whitelistStr = String.join(",", AutoClicker.itemWhitelist);
            props.setProperty("clickerWhitelist", whitelistStr);

            props.setProperty("autoDenick", String.valueOf(AutoDenick.enabled));
            props.setProperty("nickedNametags", String.valueOf(NickedRender.enabled));
            props.setProperty("pitEspChests", String.valueOf(PitESP.espChests));
            props.setProperty("pitEspDragonEggs", String.valueOf(PitESP.espDragonEggs));
            props.setProperty("pitEspRaffleTickets", String.valueOf(PitESP.espRaffleTickets));
            props.setProperty("pitEspMystics", String.valueOf(PitESP.espMystics));
            props.setProperty("lowLifeMysticEnabled", String.valueOf(LowLifeMystic.enabled));

            props.setProperty("autoPantSwap", String.valueOf(AutoPantSwap.pantSwapEnabled));
            props.setProperty("autoVenomSwap", String.valueOf(AutoPantSwap.venomSwapEnabled));
            props.setProperty("autoPod", String.valueOf(AutoPantSwap.autoPodEnabled));
            props.setProperty("autoGhead", String.valueOf(AutoGhead.enabled));
            props.setProperty("autoQuickMath", String.valueOf(AutoQuickMath.enabled));
            props.setProperty("autoBulletTime", String.valueOf(AutoBulletTime.enabled));

            props.setProperty("enemyHudEnabled", String.valueOf(EnemyHUD.enabled));
            props.setProperty("enemyHudAlerts", String.valueOf(EnemyHUD.notificationsEnabled));
            props.setProperty("enemyHudDebug", String.valueOf(EnemyHUD.debugMode));
            props.setProperty("nickedHudEnabled", String.valueOf(NickedHUD.enabled));
            props.setProperty("sessionStatsEnabled", String.valueOf(SessionStatsHUD.enabled));
            props.setProperty("eventHudEnabled", String.valueOf(EventHUD.enabled));
            props.setProperty("regHudEnabled", String.valueOf(RegHUD.enabled));
            props.setProperty("darksHudEnabled", String.valueOf(DarksHUD.enabled)); 
            props.setProperty("potionHudEnabled", String.valueOf(PotionHUD.enabled));
            props.setProperty("armorHudEnabled", String.valueOf(ArmorHUD.enabled));
            props.setProperty("coordsHudEnabled", String.valueOf(CoordsHUD.enabled));
            props.setProperty("bossBarEnabled", String.valueOf(BossBarModule.enabled));
            props.setProperty("cpsEnabled", String.valueOf(CPSModule.enabled));
            props.setProperty("fpsEnabled", String.valueOf(FPSModule.enabled));
            props.setProperty("telebowHudEnabled", String.valueOf(TelebowHUD.enabled)); 
            
            props.setProperty("playerCounterEnabled", String.valueOf(PlayerCounterHUD.enabled));
            props.setProperty("playerCounterPrefixColor", String.valueOf(PlayerCounterHUD.prefixColor));
            props.setProperty("playerCounterCountColor", String.valueOf(PlayerCounterHUD.countColor));

            props.setProperty("nameTagsEnabled", String.valueOf(NameTags.enabled));
            props.setProperty("nameTagsShowHealth", String.valueOf(NameTags.showHealth));
            props.setProperty("nameTagsShowItems", String.valueOf(NameTags.showItems));
            props.setProperty("friendsHudEnabled", String.valueOf(FriendsHUD.enabled));
            props.setProperty("friendsEspEnabled", String.valueOf(FriendsESP.enabled));

            props.setProperty("ranksEnabled", String.valueOf(com.linexstudios.foxtrot.Util.Ranks.isEnabled));
            props.setProperty("ranksChangeLevel", String.valueOf(com.linexstudios.foxtrot.Util.Ranks.changeLevel));
            props.setProperty("ranksTargetLevel", String.valueOf(com.linexstudios.foxtrot.Util.Ranks.targetLevel));
            props.setProperty("ranksChangePrestige", String.valueOf(com.linexstudios.foxtrot.Util.Ranks.changePrestige));
            props.setProperty("ranksTargetPrestige", String.valueOf(com.linexstudios.foxtrot.Util.Ranks.targetPrestige));
            props.setProperty("ranksChangeRank", String.valueOf(com.linexstudios.foxtrot.Util.Ranks.changeRank));
            props.setProperty("ranksTargetRank", com.linexstudios.foxtrot.Util.Ranks.targetRank);
            props.setProperty("ranksChangeName", String.valueOf(com.linexstudios.foxtrot.Util.Ranks.changeName));
            props.setProperty("ranksTargetName", com.linexstudios.foxtrot.Util.Ranks.targetName != null ? com.linexstudios.foxtrot.Util.Ranks.targetName : "");

            props.setProperty("telemetryEnabled", String.valueOf(telemetryEnabled)); 
            if (TelemetryManager.anonymousClientId != null && !TelemetryManager.anonymousClientId.isEmpty()) {
                props.setProperty("telemetryId", TelemetryManager.anonymousClientId);
            }

            FileOutputStream out = new FileOutputStream(settingsFile);
            props.store(out, "Foxtrot Settings");
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void logDebug(String message) {
        if (EnemyHUD.debugMode) System.out.println("[Foxtrot-Debug] " + message);
    }
}