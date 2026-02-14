package com.linexstudios.foxtrot.Handler;

import com.linexstudios.foxtrot.Hud.EnemyHUD;
import com.linexstudios.foxtrot.Hud.NickedHUD;
import com.linexstudios.foxtrot.Hud.NameTags;
import com.linexstudios.foxtrot.Hud.EditHUDGui;
import com.linexstudios.foxtrot.Denick.AutoDenick;
import com.linexstudios.foxtrot.Combat.AutoClicker;

import java.io.*;
import java.util.*;

public class ConfigHandler {
    private static final File configDir = new File("config/Foxtrot");
    private static final File enemyFile = new File(configDir, "enemies.txt");
    private static final File settingsFile = new File(configDir, "settings.txt");

    public static void loadConfig() {
        try {
            if (!configDir.exists()) configDir.mkdirs();

            // Load Enemies
            if (enemyFile.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(enemyFile));
                String line;
                EnemyHUD.targetList.clear();
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) EnemyHUD.targetList.add(line.trim());
                }
                reader.close();
            }

            // Load Settings
            if (settingsFile.exists()) {
                Properties props = new Properties();
                FileInputStream in = new FileInputStream(settingsFile);
                props.load(in);
                in.close();

                // HUD positions
                NickedHUD.hudX = Integer.parseInt(props.getProperty("nickedHudX", "10"));
                NickedHUD.hudY = Integer.parseInt(props.getProperty("nickedHudY", "80"));
                EnemyHUD.hudX = Integer.parseInt(props.getProperty("enemyHudX", "200"));
                EnemyHUD.hudY = Integer.parseInt(props.getProperty("enemyHudY", "80"));

                // --- GUI Memory States ---
                EditHUDGui.panelX = Integer.parseInt(props.getProperty("panelX", "-1"));
                EditHUDGui.panelY = Integer.parseInt(props.getProperty("panelY", "-1"));
                EditHUDGui.panelCollapsed = Boolean.parseBoolean(props.getProperty("panelCollapsed", "false"));
                EditHUDGui.combatExpanded = Boolean.parseBoolean(props.getProperty("combatExpanded", "false"));
                EditHUDGui.renderExpanded = Boolean.parseBoolean(props.getProperty("renderExpanded", "false"));
                EditHUDGui.denickExpanded = Boolean.parseBoolean(props.getProperty("denickExpanded", "false"));
                EditHUDGui.hudExpanded = Boolean.parseBoolean(props.getProperty("hudExpanded", "false"));

                // --- AutoClicker Settings ---
                AutoClicker.enabled = Boolean.parseBoolean(props.getProperty("clickerEnabled", "false"));
                AutoClicker.holdToClick = Boolean.parseBoolean(props.getProperty("clickerHoldToClick", "true"));
                AutoClicker.inventoryFill = Boolean.parseBoolean(props.getProperty("clickerInvFill", "true"));
                AutoClicker.breakBlocks = Boolean.parseBoolean(props.getProperty("clickerBreakBlocks", "true"));
                AutoClicker.limitItems = Boolean.parseBoolean(props.getProperty("clickerLimitItems", "true"));
                
                AutoClicker.inventoryFillCps = Float.parseFloat(props.getProperty("clickerInvFillCps", "15.0"));
                AutoClicker.minCps = Float.parseFloat(props.getProperty("clickerMinCps", "9.0"));
                AutoClicker.maxCps = Float.parseFloat(props.getProperty("clickerMaxCps", "13.0"));

                // Load Whitelist (split by commas)
                String whitelistStr = props.getProperty("clickerWhitelist", "sword,axe");
                AutoClicker.itemWhitelist = new ArrayList<>(Arrays.asList(whitelistStr.split(",")));

                // Existing Module Toggles
                AutoDenick.enabled = Boolean.parseBoolean(props.getProperty("autoDenick", "false"));
                EnemyHUD.enabled = Boolean.parseBoolean(props.getProperty("enemyHudEnabled", "true"));
                EnemyHUD.notificationsEnabled = Boolean.parseBoolean(props.getProperty("enemyHudAlerts", "true"));
                EnemyHUD.debugMode = Boolean.parseBoolean(props.getProperty("enemyHudDebug", "false"));
                NickedHUD.enabled = Boolean.parseBoolean(props.getProperty("nickedHudEnabled", "true"));
                NameTags.enabled = Boolean.parseBoolean(props.getProperty("nameTagsEnabled", "false"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void saveConfig() {
        try {
            if (!configDir.exists()) configDir.mkdirs();

            // Save Enemies
            PrintWriter writer = new PrintWriter(new FileWriter(enemyFile));
            for (String name : EnemyHUD.targetList) {
                writer.println(name);
            }
            writer.close();

            // Save Settings
            Properties props = new Properties();
            
            // HUD positions
            props.setProperty("nickedHudX", String.valueOf(NickedHUD.hudX));
            props.setProperty("nickedHudY", String.valueOf(NickedHUD.hudY));
            props.setProperty("enemyHudX", String.valueOf(EnemyHUD.hudX));
            props.setProperty("enemyHudY", String.valueOf(EnemyHUD.hudY));

            // --- GUI Memory States ---
            props.setProperty("panelX", String.valueOf(EditHUDGui.panelX));
            props.setProperty("panelY", String.valueOf(EditHUDGui.panelY));
            props.setProperty("panelCollapsed", String.valueOf(EditHUDGui.panelCollapsed));
            props.setProperty("combatExpanded", String.valueOf(EditHUDGui.combatExpanded));
            props.setProperty("renderExpanded", String.valueOf(EditHUDGui.renderExpanded));
            props.setProperty("denickExpanded", String.valueOf(EditHUDGui.denickExpanded));
            props.setProperty("hudExpanded", String.valueOf(EditHUDGui.hudExpanded));

            // --- AutoClicker Settings ---
            props.setProperty("clickerEnabled", String.valueOf(AutoClicker.enabled));
            props.setProperty("clickerHoldToClick", String.valueOf(AutoClicker.holdToClick));
            props.setProperty("clickerInvFill", String.valueOf(AutoClicker.inventoryFill));
            props.setProperty("clickerBreakBlocks", String.valueOf(AutoClicker.breakBlocks));
            props.setProperty("clickerLimitItems", String.valueOf(AutoClicker.limitItems));
            
            props.setProperty("clickerInvFillCps", String.valueOf(AutoClicker.inventoryFillCps));
            props.setProperty("clickerMinCps", String.valueOf(AutoClicker.minCps));
            props.setProperty("clickerMaxCps", String.valueOf(AutoClicker.maxCps));

            // Save Whitelist (join array with commas)
            String whitelistStr = String.join(",", AutoClicker.itemWhitelist);
            props.setProperty("clickerWhitelist", whitelistStr);

            // Existing Module Toggles
            props.setProperty("autoDenick", String.valueOf(AutoDenick.enabled));
            props.setProperty("enemyHudEnabled", String.valueOf(EnemyHUD.enabled));
            props.setProperty("enemyHudAlerts", String.valueOf(EnemyHUD.notificationsEnabled));
            props.setProperty("enemyHudDebug", String.valueOf(EnemyHUD.debugMode));
            props.setProperty("nickedHudEnabled", String.valueOf(NickedHUD.enabled));
            props.setProperty("nameTagsEnabled", String.valueOf(NameTags.enabled));

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