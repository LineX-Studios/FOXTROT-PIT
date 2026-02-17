package com.linexstudios.foxtrot.Handler;

import com.linexstudios.foxtrot.Hud.EnemyHUD;
import com.linexstudios.foxtrot.Hud.FriendsHUD;
import com.linexstudios.foxtrot.Hud.NickedHUD;
import com.linexstudios.foxtrot.Hud.NameTags;
import com.linexstudios.foxtrot.Hud.EditHUDGui;
import com.linexstudios.foxtrot.Hud.SessionStatsHUD;
import com.linexstudios.foxtrot.Hud.EventHUD; 
import com.linexstudios.foxtrot.Denick.AutoDenick;
import com.linexstudios.foxtrot.Combat.AutoClicker;
import com.linexstudios.foxtrot.Render.PitESP; // NEW PIT ESP
import com.linexstudios.foxtrot.Render.FriendsESP;
import com.linexstudios.foxtrot.Render.NickedRender;
import com.linexstudios.foxtrot.Hud.RegHUD;

import java.io.*;
import java.util.*;

public class ConfigHandler {
    private static final File configDir = new File("config/Foxtrot");
    private static final File enemyFile = new File(configDir, "enemies.txt");
    private static final File friendsFile = new File(configDir, "friends.txt");
    private static final File settingsFile = new File(configDir, "settings.txt");

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
                NickedHUD.hudX = Integer.parseInt(props.getProperty("nickedHudX", "10"));
                NickedHUD.hudY = Integer.parseInt(props.getProperty("nickedHudY", "80"));
                EnemyHUD.hudX = Integer.parseInt(props.getProperty("enemyHudX", "200"));
                EnemyHUD.hudY = Integer.parseInt(props.getProperty("enemyHudY", "80"));
                FriendsHUD.hudX = Integer.parseInt(props.getProperty("friendsHudX", "350"));
                FriendsHUD.hudY = Integer.parseInt(props.getProperty("friendsHudY", "80"));
                SessionStatsHUD.hudX = Integer.parseInt(props.getProperty("sessionStatsX", "10"));
                SessionStatsHUD.hudY = Integer.parseInt(props.getProperty("sessionStatsY", "150"));
                RegHUD.hudX = Integer.parseInt(props.getProperty("regHudX", "10"));
                RegHUD.hudY = Integer.parseInt(props.getProperty("regHudY", "180"));
                EventHUD.hudX = Integer.parseInt(props.getProperty("eventHudX", "10"));
                EventHUD.hudY = Integer.parseInt(props.getProperty("eventHudY", "250"));

                // Load GUI States
                EditHUDGui.panelX = Integer.parseInt(props.getProperty("panelX", "-1"));
                EditHUDGui.panelY = Integer.parseInt(props.getProperty("panelY", "-1"));
                EditHUDGui.panelCollapsed = Boolean.parseBoolean(props.getProperty("panelCollapsed", "false"));
                EditHUDGui.combatExpanded = Boolean.parseBoolean(props.getProperty("combatExpanded", "false"));
                EditHUDGui.renderExpanded = Boolean.parseBoolean(props.getProperty("renderExpanded", "false"));
                EditHUDGui.denickExpanded = Boolean.parseBoolean(props.getProperty("denickExpanded", "false"));
                EditHUDGui.hudExpanded = Boolean.parseBoolean(props.getProperty("hudExpanded", "false"));
                EditHUDGui.autoClickerDropdownExpanded = Boolean.parseBoolean(props.getProperty("autoClickerDropdownExpanded", "false"));
                EditHUDGui.randomDropdownExpanded = Boolean.parseBoolean(props.getProperty("randomDropdownExpanded", "false"));
                EditHUDGui.nameTagsDropdownExpanded = Boolean.parseBoolean(props.getProperty("nameTagsDropdownExpanded", "false"));
                EditHUDGui.pitEspDropdownExpanded = Boolean.parseBoolean(props.getProperty("pitEspDropdownExpanded", "false")); // NEW PIT ESP DROPDOWN

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
            props.setProperty("nickedHudX", String.valueOf(NickedHUD.hudX));
            props.setProperty("nickedHudY", String.valueOf(NickedHUD.hudY));
            props.setProperty("enemyHudX", String.valueOf(EnemyHUD.hudX));
            props.setProperty("enemyHudY", String.valueOf(EnemyHUD.hudY));
            props.setProperty("friendsHudX", String.valueOf(FriendsHUD.hudX));
            props.setProperty("friendsHudY", String.valueOf(FriendsHUD.hudY));
            props.setProperty("sessionStatsX", String.valueOf(SessionStatsHUD.hudX));
            props.setProperty("sessionStatsY", String.valueOf(SessionStatsHUD.hudY));
            props.setProperty("eventHudX", String.valueOf(EventHUD.hudX));
            props.setProperty("eventHudY", String.valueOf(EventHUD.hudY));
            props.setProperty("regHudX", String.valueOf(RegHUD.hudX));
            props.setProperty("regHudY", String.valueOf(RegHUD.hudY));

            // Save GUI States
            props.setProperty("panelX", String.valueOf(EditHUDGui.panelX));
            props.setProperty("panelY", String.valueOf(EditHUDGui.panelY));
            props.setProperty("panelCollapsed", String.valueOf(EditHUDGui.panelCollapsed));
            props.setProperty("combatExpanded", String.valueOf(EditHUDGui.combatExpanded));
            props.setProperty("renderExpanded", String.valueOf(EditHUDGui.renderExpanded));
            props.setProperty("denickExpanded", String.valueOf(EditHUDGui.denickExpanded));
            props.setProperty("hudExpanded", String.valueOf(EditHUDGui.hudExpanded));
            props.setProperty("autoClickerDropdownExpanded", String.valueOf(EditHUDGui.autoClickerDropdownExpanded));
            props.setProperty("randomDropdownExpanded", String.valueOf(EditHUDGui.randomDropdownExpanded));
            props.setProperty("nameTagsDropdownExpanded", String.valueOf(EditHUDGui.nameTagsDropdownExpanded));
            props.setProperty("pitEspDropdownExpanded", String.valueOf(EditHUDGui.pitEspDropdownExpanded));

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