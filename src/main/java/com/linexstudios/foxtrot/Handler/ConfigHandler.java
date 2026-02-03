package com.linexstudios.foxtrot.Handler;

import com.linexstudios.foxtrot.Hud.EnemyHUD;
import com.linexstudios.foxtrot.Hud.NickedHUD;
import com.linexstudios.foxtrot.Denick.AutoDenick;

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

                // HUD position
                NickedHUD.hudX = Integer.parseInt(props.getProperty("hudX", "10"));
                NickedHUD.hudY = Integer.parseInt(props.getProperty("hudY", "80"));

                // AutoDenick toggle
                AutoDenick.enabled = Boolean.parseBoolean(props.getProperty("autoDenick", "false"));

                // HUD enabled/disabled
                EnemyHUD.enabled = Boolean.parseBoolean(props.getProperty("enemyHudEnabled", "true"));
                EnemyHUD.notificationsEnabled = Boolean.parseBoolean(props.getProperty("enemyHudAlerts", "true"));
                EnemyHUD.debugMode = Boolean.parseBoolean(props.getProperty("enemyHudDebug", "false"));

                NickedHUD.enabled = Boolean.parseBoolean(props.getProperty("nickedHudEnabled", "true"));
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
            props.setProperty("hudX", String.valueOf(NickedHUD.hudX));
            props.setProperty("hudY", String.valueOf(NickedHUD.hudY));
            props.setProperty("autoDenick", String.valueOf(AutoDenick.enabled));
            props.setProperty("enemyHudEnabled", String.valueOf(EnemyHUD.enabled));
            props.setProperty("enemyHudAlerts", String.valueOf(EnemyHUD.notificationsEnabled));
            props.setProperty("enemyHudDebug", String.valueOf(EnemyHUD.debugMode));
            props.setProperty("nickedHudEnabled", String.valueOf(NickedHUD.enabled));

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
