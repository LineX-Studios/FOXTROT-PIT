package com.linexstudios.foxtrot.Handler;

import com.linexstudios.foxtrot.Hud.EnemyHUD;
import com.linexstudios.foxtrot.Hud.FriendsHUD;
import com.linexstudios.foxtrot.Hud.NickedHUD;
import com.linexstudios.foxtrot.Hud.NameTags;
import com.linexstudios.foxtrot.Hud.EditHUDGui;
import com.linexstudios.foxtrot.Hud.SessionStatsHUD;
import com.linexstudios.foxtrot.Hud.EventHUD; 
import com.linexstudios.foxtrot.Hud.RegHUD;
import com.linexstudios.foxtrot.Hud.PotionHUD;
import com.linexstudios.foxtrot.Hud.ArmorHUD;
import com.linexstudios.foxtrot.Hud.CoordsHUD;
import com.linexstudios.foxtrot.Hud.ToggleSprintModule; // <--- NEW IMPORT
import com.linexstudios.foxtrot.Denick.AutoDenick;
import com.linexstudios.foxtrot.Combat.AutoClicker;
import com.linexstudios.foxtrot.Render.PitESP; 
import com.linexstudios.foxtrot.Render.FriendsESP;
import com.linexstudios.foxtrot.Render.NickedRender;

import net.minecraft.client.Minecraft;

import java.io.*;
import java.util.*;

public class ConfigHandler {
    private static final File configDir = new File("config/Foxtrot");
    private static final File enemyFile = new File(configDir, "enemies.txt");
    private static final File friendsFile = new File(configDir, "friends.txt");
    private static final File settingsFile = new File(configDir, "settings.txt");

    // This safely edits the Forge config to disable the ugly Anvil screen.
    // It will fall back to the clean, default Mojang red screen on startup.
    public static void disableForgeSplashScreen() {
        try {
            File forgeConfigDir = new File(Minecraft.getMinecraft().mcDataDir, "config");
            File splashFile = new File(forgeConfigDir, "splash.properties");
            
            if (splashFile.exists()) {
                Properties props = new Properties();
                FileInputStream in = new FileInputStream(splashFile);
                props.load(in);
                in.close();

                if ("true".equals(props.getProperty("enabled", "true"))) {
                    props.setProperty("enabled", "false"); 
                    FileOutputStream out = new FileOutputStream(splashFile);
                    props.store(out, "Automatically disabled by Foxtrot Client");
                    out.close();
                    System.out.println("[Foxtrot] Disabled Forge Splash Screen for future launches.");
                }
            }
        } catch (Exception e) {
            System.out.println("[Foxtrot] Failed to edit splash.properties.");
        }
    }

    public static void loadConfig() {
        try {
            if (!configDir.exists()) configDir.mkdirs();

            if (enemyFile.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(enemyFile));
                String line;
                EnemyHUD.targetList.clear();
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) EnemyHUD.targetList.add(line.trim());
                }
                reader.close();
            }

            if (friendsFile.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(friendsFile));
                String line;
                FriendsHUD.friendsList.clear();
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) FriendsHUD.friendsList.add(line.trim());
                }
                reader.close();
            }

            if (settingsFile.exists()) {
                Properties props = new Properties();
                FileInputStream in = new FileInputStream(settingsFile);
                props.load(in);
                in.close();

                // Load HUD Positions
                NickedHUD.instance.x = Integer.parseInt(props.getProperty("nickedHudX", "10"));
                NickedHUD.instance.y = Integer.parseInt(props.getProperty("nickedHudY", "80"));
                EnemyHUD.instance.x = Integer.parseInt(props.getProperty("enemyHudX", "200"));
                EnemyHUD.instance.y = Integer.parseInt(props.getProperty("enemyHudY", "80"));
                FriendsHUD.instance.x = Integer.parseInt(props.getProperty("friendsHudX", "350"));
                FriendsHUD.instance.y = Integer.parseInt(props.getProperty("friendsHudY", "80"));
                SessionStatsHUD.instance.x = Integer.parseInt(props.getProperty("sessionStatsX", "10"));
                SessionStatsHUD.instance.y = Integer.parseInt(props.getProperty("sessionStatsY", "150"));
                RegHUD.instance.x = Integer.parseInt(props.getProperty("regHudX", "10"));
                RegHUD.instance.y = Integer.parseInt(props.getProperty("regHudY", "180"));
                EventHUD.instance.x = Integer.parseInt(props.getProperty("eventHudX", "10"));
                EventHUD.instance.y = Integer.parseInt(props.getProperty("eventHudY", "250"));
                PotionHUD.instance.x = Integer.parseInt(props.getProperty("potionHudX", "10"));
                PotionHUD.instance.y = Integer.parseInt(props.getProperty("potionHudY", "50"));
                ArmorHUD.instance.x = Integer.parseInt(props.getProperty("armorHudX", "10"));
                ArmorHUD.instance.y = Integer.parseInt(props.getProperty("armorHudY", "100"));
                CoordsHUD.instance.x = Integer.parseInt(props.getProperty("coordsHudX", "10"));
                CoordsHUD.instance.y = Integer.parseInt(props.getProperty("coordsHudY", "10"));
                ToggleSprintModule.instance.x = Integer.parseInt(props.getProperty("toggleSprintX", "2"));
                ToggleSprintModule.instance.y = Integer.parseInt(props.getProperty("toggleSprintY", "12"));

                // Load HUD Scales
                NickedHUD.instance.scale = Float.parseFloat(props.getProperty("nickedHudScale", "1.0"));
                EnemyHUD.instance.scale = Float.parseFloat(props.getProperty("enemyHudScale", "1.0"));
                FriendsHUD.instance.scale = Float.parseFloat(props.getProperty("friendsHudScale", "1.0"));
                SessionStatsHUD.instance.scale = Float.parseFloat(props.getProperty("sessionStatsScale", "1.0"));
                RegHUD.instance.scale = Float.parseFloat(props.getProperty("regHudScale", "1.0"));
                EventHUD.instance.scale = Float.parseFloat(props.getProperty("eventHudScale", "1.0"));
                PotionHUD.instance.scale = Float.parseFloat(props.getProperty("potionHudScale", "1.0"));
                ArmorHUD.instance.scale = Float.parseFloat(props.getProperty("armorHudScale", "1.0"));
                CoordsHUD.instance.scale = Float.parseFloat(props.getProperty("coordsHudScale", "1.0"));
                ToggleSprintModule.instance.scale = Float.parseFloat(props.getProperty("toggleSprintScale", "1.0"));

                // Load HUD Layouts & Colors
                PotionHUD.instance.isHorizontal = Boolean.parseBoolean(props.getProperty("potionHorizontal", "false"));
                PotionHUD.nameColor = Integer.parseInt(props.getProperty("potionNameColor", "16777215")); // 0xFFFFFF
                PotionHUD.durationColor = Integer.parseInt(props.getProperty("potionDurationColor", "11184810")); // 0xAAAAAA
                ArmorHUD.durabilityColor = Integer.parseInt(props.getProperty("armorDurabilityColor", "16777215"));

                ArmorHUD.instance.isHorizontal = Boolean.parseBoolean(props.getProperty("armorHorizontal", "false"));

                CoordsHUD.instance.isHorizontal = Boolean.parseBoolean(props.getProperty("coordsHorizontal", "false"));
                CoordsHUD.axisColor = Integer.parseInt(props.getProperty("coordsAxisColor", "16733525")); // 0xFF5555
                CoordsHUD.numberColor = Integer.parseInt(props.getProperty("coordsNumberColor", "16777215"));
                
                // Load Toggle Sprint Specific Settings
                ToggleSprintModule.instance.enabled = Boolean.parseBoolean(props.getProperty("toggleSprintEnabled", "true"));
                ToggleSprintModule.instance.toggleSprint = Boolean.parseBoolean(props.getProperty("tsSprint", "true"));
                ToggleSprintModule.instance.toggleSneak = Boolean.parseBoolean(props.getProperty("tsSneak", "false"));
                ToggleSprintModule.instance.wTapFix = Boolean.parseBoolean(props.getProperty("tsWTapFix", "true"));
                ToggleSprintModule.instance.flyBoost = Boolean.parseBoolean(props.getProperty("tsFlyBoost", "true"));
                ToggleSprintModule.instance.flyBoostAmount = Float.parseFloat(props.getProperty("tsFlyBoostAmount", "4.0"));
                ToggleSprintModule.instance.textColor = Integer.parseInt(props.getProperty("tsTextColor", "16777215"));

                // Load GUI States
                EditHUDGui.collapsedX = Integer.parseInt(props.getProperty("panelX", "-1"));
                EditHUDGui.collapsedY = Integer.parseInt(props.getProperty("panelY", "-1"));
                EditHUDGui.panelCollapsed = Boolean.parseBoolean(props.getProperty("panelCollapsed", "false"));

                // Load AutoClicker Settings
                AutoClicker.enabled = Boolean.parseBoolean(props.getProperty("clickerEnabled", "false"));
                AutoClicker.leftClick = Boolean.parseBoolean(props.getProperty("clickerLeft", "true"));
                AutoClicker.fastPlaceEnabled = Boolean.parseBoolean(props.getProperty("fastPlace", "false"));
                AutoClicker.holdToClick = Boolean.parseBoolean(props.getProperty("clickerHoldToClick", "true"));
                AutoClicker.inventoryFill = Boolean.parseBoolean(props.getProperty("clickerInvFill", "true"));
                AutoClicker.breakBlocks = Boolean.parseBoolean(props.getProperty("clickerBreakBlocks", "true"));
                AutoClicker.limitItems = Boolean.parseBoolean(props.getProperty("clickerLimitItems", "true"));

                AutoClicker.inventoryFillCps = Float.parseFloat(props.getProperty("clickerInvFillCps", "15.0"));
                AutoClicker.minCps = Float.parseFloat(props.getProperty("clickerMinCps", "9.0"));
                AutoClicker.maxCps = Float.parseFloat(props.getProperty("clickerMaxCps", "13.0"));
                AutoClicker.randomMode = Integer.parseInt(props.getProperty("clickerRandomMode", "1"));

                String whitelistStr = props.getProperty("clickerWhitelist", "sword,axe,pickaxe");
                AutoClicker.itemWhitelist = new ArrayList<>(Arrays.asList(whitelistStr.split(",")));

                // Load Render Modules & PIT ESP
                AutoDenick.enabled = Boolean.parseBoolean(props.getProperty("autoDenick", "false"));
                NickedRender.enabled = Boolean.parseBoolean(props.getProperty("nickedNametags", "true"));
                
                PitESP.espChests = Boolean.parseBoolean(props.getProperty("pitEspChests", "true"));
                PitESP.espDragonEggs = Boolean.parseBoolean(props.getProperty("pitEspDragonEggs", "true"));
                PitESP.espRaffleTickets = Boolean.parseBoolean(props.getProperty("pitEspRaffleTickets", "true"));
                PitESP.espMystics = Boolean.parseBoolean(props.getProperty("pitEspMystics", "true"));

                // Load HUD States
                EnemyHUD.enabled = Boolean.parseBoolean(props.getProperty("enemyHudEnabled", "true"));
                EnemyHUD.notificationsEnabled = Boolean.parseBoolean(props.getProperty("enemyHudAlerts", "true"));
                EnemyHUD.debugMode = Boolean.parseBoolean(props.getProperty("enemyHudDebug", "false"));
                NickedHUD.enabled = Boolean.parseBoolean(props.getProperty("nickedHudEnabled", "true"));
                SessionStatsHUD.enabled = Boolean.parseBoolean(props.getProperty("sessionStatsEnabled", "true"));
                EventHUD.enabled = Boolean.parseBoolean(props.getProperty("eventHudEnabled", "true"));
                RegHUD.enabled = Boolean.parseBoolean(props.getProperty("regHudEnabled", "true"));
                PotionHUD.enabled = Boolean.parseBoolean(props.getProperty("potionHudEnabled", "true"));
                ArmorHUD.enabled = Boolean.parseBoolean(props.getProperty("armorHudEnabled", "true"));
                CoordsHUD.enabled = Boolean.parseBoolean(props.getProperty("coordsHudEnabled", "true"));

                NameTags.enabled = Boolean.parseBoolean(props.getProperty("nameTagsEnabled", "false"));
                NameTags.showHealth = Boolean.parseBoolean(props.getProperty("nameTagsShowHealth", "true"));
                NameTags.showItems = Boolean.parseBoolean(props.getProperty("nameTagsShowItems", "true"));

                FriendsHUD.enabled = Boolean.parseBoolean(props.getProperty("friendsHudEnabled", "true"));
                FriendsESP.enabled = Boolean.parseBoolean(props.getProperty("friendsEspEnabled", "true"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void saveConfig() {
        try {
            if (!configDir.exists()) configDir.mkdirs();

            PrintWriter enemyWriter = new PrintWriter(new FileWriter(enemyFile));
            for (String name : EnemyHUD.targetList) {
                enemyWriter.println(name);
            }
            enemyWriter.close();

            PrintWriter friendsWriter = new PrintWriter(new FileWriter(friendsFile));
            for (String name : FriendsHUD.friendsList) {
                friendsWriter.println(name);
            }
            friendsWriter.close();

            Properties props = new Properties();

            // Save HUD Positions
            props.setProperty("nickedHudX", String.valueOf(NickedHUD.instance.x));
            props.setProperty("nickedHudY", String.valueOf(NickedHUD.instance.y));
            props.setProperty("enemyHudX", String.valueOf(EnemyHUD.instance.x));
            props.setProperty("enemyHudY", String.valueOf(EnemyHUD.instance.y));
            props.setProperty("friendsHudX", String.valueOf(FriendsHUD.instance.x));
            props.setProperty("friendsHudY", String.valueOf(FriendsHUD.instance.y));
            props.setProperty("sessionStatsX", String.valueOf(SessionStatsHUD.instance.x));
            props.setProperty("sessionStatsY", String.valueOf(SessionStatsHUD.instance.y));
            props.setProperty("eventHudX", String.valueOf(EventHUD.instance.x));
            props.setProperty("eventHudY", String.valueOf(EventHUD.instance.y));
            props.setProperty("regHudX", String.valueOf(RegHUD.instance.x));
            props.setProperty("regHudY", String.valueOf(RegHUD.instance.y));
            props.setProperty("potionHudX", String.valueOf(PotionHUD.instance.x));
            props.setProperty("potionHudY", String.valueOf(PotionHUD.instance.y));
            props.setProperty("armorHudX", String.valueOf(ArmorHUD.instance.x));
            props.setProperty("armorHudY", String.valueOf(ArmorHUD.instance.y));
            props.setProperty("coordsHudX", String.valueOf(CoordsHUD.instance.x));
            props.setProperty("coordsHudY", String.valueOf(CoordsHUD.instance.y));
            props.setProperty("toggleSprintX", String.valueOf(ToggleSprintModule.instance.x));
            props.setProperty("toggleSprintY", String.valueOf(ToggleSprintModule.instance.y));

            // Save HUD Scales
            props.setProperty("nickedHudScale", String.valueOf(NickedHUD.instance.scale));
            props.setProperty("enemyHudScale", String.valueOf(EnemyHUD.instance.scale));
            props.setProperty("friendsHudScale", String.valueOf(FriendsHUD.instance.scale));
            props.setProperty("sessionStatsScale", String.valueOf(SessionStatsHUD.instance.scale));
            props.setProperty("regHudScale", String.valueOf(RegHUD.instance.scale));
            props.setProperty("eventHudScale", String.valueOf(EventHUD.instance.scale));
            props.setProperty("potionHudScale", String.valueOf(PotionHUD.instance.scale));
            props.setProperty("armorHudScale", String.valueOf(ArmorHUD.instance.scale));
            props.setProperty("coordsHudScale", String.valueOf(CoordsHUD.instance.scale));
            props.setProperty("toggleSprintScale", String.valueOf(ToggleSprintModule.instance.scale));

            // Save HUD Layouts & Colors
            props.setProperty("potionHorizontal", String.valueOf(PotionHUD.instance.isHorizontal));
            props.setProperty("potionNameColor", String.valueOf(PotionHUD.nameColor));
            props.setProperty("potionDurationColor", String.valueOf(PotionHUD.durationColor));
            props.setProperty("armorDurabilityColor", String.valueOf(ArmorHUD.durabilityColor));

            props.setProperty("armorHorizontal", String.valueOf(ArmorHUD.instance.isHorizontal));

            props.setProperty("coordsHorizontal", String.valueOf(CoordsHUD.instance.isHorizontal));
            props.setProperty("coordsAxisColor", String.valueOf(CoordsHUD.axisColor));
            props.setProperty("coordsNumberColor", String.valueOf(CoordsHUD.numberColor));
            
            // Save Toggle Sprint Specific Settings
            props.setProperty("toggleSprintEnabled", String.valueOf(ToggleSprintModule.instance.enabled));
            props.setProperty("tsSprint", String.valueOf(ToggleSprintModule.instance.toggleSprint));
            props.setProperty("tsSneak", String.valueOf(ToggleSprintModule.instance.toggleSneak));
            props.setProperty("tsWTapFix", String.valueOf(ToggleSprintModule.instance.wTapFix));
            props.setProperty("tsFlyBoost", String.valueOf(ToggleSprintModule.instance.flyBoost));
            props.setProperty("tsFlyBoostAmount", String.valueOf(ToggleSprintModule.instance.flyBoostAmount));
            props.setProperty("tsTextColor", String.valueOf(ToggleSprintModule.instance.textColor));

            // Save GUI States
            props.setProperty("panelX", String.valueOf(EditHUDGui.collapsedX));
            props.setProperty("panelY", String.valueOf(EditHUDGui.collapsedY));
            props.setProperty("panelCollapsed", String.valueOf(EditHUDGui.panelCollapsed));

            // Save AutoClicker Settings
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

            // Save Render Modules & PIT ESP
            props.setProperty("autoDenick", String.valueOf(AutoDenick.enabled));
            props.setProperty("nickedNametags", String.valueOf(NickedRender.enabled));
            
            props.setProperty("pitEspChests", String.valueOf(PitESP.espChests));
            props.setProperty("pitEspDragonEggs", String.valueOf(PitESP.espDragonEggs));
            props.setProperty("pitEspRaffleTickets", String.valueOf(PitESP.espRaffleTickets));
            props.setProperty("pitEspMystics", String.valueOf(PitESP.espMystics));

            // Save HUD States
            props.setProperty("enemyHudEnabled", String.valueOf(EnemyHUD.enabled));
            props.setProperty("enemyHudAlerts", String.valueOf(EnemyHUD.notificationsEnabled));
            props.setProperty("enemyHudDebug", String.valueOf(EnemyHUD.debugMode));
            props.setProperty("nickedHudEnabled", String.valueOf(NickedHUD.enabled));
            props.setProperty("sessionStatsEnabled", String.valueOf(SessionStatsHUD.enabled));
            props.setProperty("eventHudEnabled", String.valueOf(EventHUD.enabled));
            props.setProperty("regHudEnabled", String.valueOf(RegHUD.enabled));
            props.setProperty("potionHudEnabled", String.valueOf(PotionHUD.enabled));
            props.setProperty("armorHudEnabled", String.valueOf(ArmorHUD.enabled));
            props.setProperty("coordsHudEnabled", String.valueOf(CoordsHUD.enabled));

            props.setProperty("nameTagsEnabled", String.valueOf(NameTags.enabled));
            props.setProperty("nameTagsShowHealth", String.valueOf(NameTags.showHealth));
            props.setProperty("nameTagsShowItems", String.valueOf(NameTags.showItems));

            props.setProperty("friendsHudEnabled", String.valueOf(FriendsHUD.enabled));
            props.setProperty("friendsEspEnabled", String.valueOf(FriendsESP.enabled));

            FileOutputStream out = new FileOutputStream(settingsFile);
            props.store(out, "Foxtrot Settings");
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void logDebug(String message) {
        if (EnemyHUD.debugMode) {
            System.out.println("[Foxtrot-Debug] " + message);
        }
    }
}