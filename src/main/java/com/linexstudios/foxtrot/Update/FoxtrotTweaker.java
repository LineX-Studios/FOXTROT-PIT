/*
 * ==============================================================================
 * Foxtrot-PIT - Open Source Bootstrapper & Auto-Updater
 * © 2026 Linex Studios & Foxtrot-PIT. All Rights Reserved.
 * * OPEN SOURCE LICENSE & LIABILITY WAIVER:
 * This code is open-source and provided "AS IS", without warranty of any kind, 
 * express or implied. In no event shall the authors or copyright holders (Linex 
 * Studios) be liable for any claim, damages, or other liability arising from, 
 * out of, or in connection with the software or the use of this software.
 * * ACCEPTABLE USE POLICY (ANTI-MALWARE):
 * While this code is open-source, this specific dynamic-injection and downloading 
 * architecture is highly sensitive. By viewing, copying, modifying, or distributing 
 * this code, you explicitly agree that it will NOT be repurposed, reverse-engineered, 
 * or utilized to download, execute, or inject unauthorized payloads, malware, 
 * remote access trojans (RATs), token loggers, or any malicious software.
 * ==============================================================================
 */
package com.linexstudios.foxtrot.Update;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class FoxtrotTweaker implements ITweaker {

    public static final String CURRENT_VERSION = "${version}";
    public static boolean UPDATE_AVAILABLE = false;
    public static String LATEST_VERSION = "";
    public static String DOWNLOAD_URL = "";
    
    // --- DEVELOPER TEST MODE ---
    public static boolean DEV_TEST_MODE = true; // < change this value to true or false, true = test mode is on false = test mode is off


    // --------------------------------------
    private static boolean IS_BOOTING = true; // DO NOT CHANGE THIS VALUE TO FALSE KEEP IT AS IS!!!!!!!
    public static File foxtrotDir;

    @SuppressWarnings("unchecked")
    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
        
        // --- NEW: THE BLACKBOARD INJECTION ---
        // This flawlessly chains the MixinTweaker without Minecraft ignoring it!
        List<String> tweakClasses = (List<String>) Launch.blackboard.get("TweakClasses");
        if (tweakClasses != null && !tweakClasses.contains("org.spongepowered.asm.launch.MixinTweaker")) {
            tweakClasses.add("org.spongepowered.asm.launch.MixinTweaker");
        }

        foxtrotDir = new File(gameDir, "Foxtrot");
        if (!foxtrotDir.exists()) foxtrotDir.mkdirs();

        try {
            File modsDir = new File(gameDir, "mods");
            if (modsDir.exists() && modsDir.isDirectory()) {
                File[] oldFiles = modsDir.listFiles((dir, name) -> name.endsWith(".old"));
                if (oldFiles != null) {
                    for (File oldFile : oldFiles) oldFile.delete(); 
                }
            }
        } catch (Exception ignored) {}

        boolean autoUpdateEnabled = true;
        try {
            File configDir = new File(gameDir, "config");
            if (configDir.exists()) {
                File[] files = configDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.getName().toLowerCase().contains("foxtrot")) {
                            BufferedReader br = new BufferedReader(new FileReader(file));
                            String line;
                            while ((line = br.readLine()) != null) {
                                String cleanLine = line.replace(" ", "");
                                if (cleanLine.contains("autoUpdateEnabled=false") || cleanLine.contains("\"autoUpdateEnabled\":false") || cleanLine.contains("autoUpdateEnabled:false")) {
                                    autoUpdateEnabled = false;
                                }
                            }
                            br.close();
                        }
                    }
                }
            }
        } catch (Throwable t) {}

        if (DEV_TEST_MODE) {
            UPDATE_AVAILABLE = true;
            LATEST_VERSION = "0.7.9-TEST";
            if (autoUpdateEnabled) {
                runUpdateUIAndSimulate();
            }
            return;
        }

        try {
            URL url = new URL("https://api.github.com/repos/LineX-Studios/FOXTROT-PIT/releases/latest");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Foxtrot-Updater"); 
            
            if (conn.getResponseCode() == 200) {
                InputStreamReader reader = new InputStreamReader(conn.getInputStream());
                JsonObject response = new JsonParser().parse(reader).getAsJsonObject();
                reader.close();

                String latestVersion = response.get("tag_name").getAsString().replace("v", "");
                
                if (!CURRENT_VERSION.equals(latestVersion)) {
                    UPDATE_AVAILABLE = true;
                    LATEST_VERSION = latestVersion;
                    DOWNLOAD_URL = response.getAsJsonArray("assets").get(0).getAsJsonObject().get("browser_download_url").getAsString();
                    
                    if (autoUpdateEnabled) {
                        runRealDownload(DOWNLOAD_URL);
                    }
                }
            }
        } catch (Throwable t) {
            System.out.println("[Foxtrot] Failed to check GitHub for updates.");
        }
    }

    private void runUpdateUIAndSimulate() {
        FoxtrotUpdateWindow window = new FoxtrotUpdateWindow(LATEST_VERSION);
        SwingUtilities.invokeLater(() -> window.setVisible(true));

        try {
            for (int i = 0; i <= 100; i += 2) {
                window.setProgress(i);
                Thread.sleep(50); 
            }
            Thread.sleep(1000);
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(window::dispose);
    }

    public static void runRealDownload(String downloadUrl) {
        FoxtrotUpdateWindow window = new FoxtrotUpdateWindow(LATEST_VERSION);
        SwingUtilities.invokeLater(() -> window.setVisible(true));

        try {
            File newJar = new File(foxtrotDir, "Foxtrot-" + LATEST_VERSION + ".jar");

            HttpURLConnection conn = (HttpURLConnection) new URL(downloadUrl).openConnection();
            conn.setRequestProperty("User-Agent", "Foxtrot-Updater");
            int totalBytes = conn.getContentLength();

            try (InputStream in = new BufferedInputStream(conn.getInputStream());
                 OutputStream out = new BufferedOutputStream(new FileOutputStream(newJar))) {

                byte[] buffer = new byte[8192];
                int bytesRead = 0;
                int nRead;
                while ((nRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, nRead);
                    bytesRead += nRead;
                    window.setProgress((int) (((double) bytesRead / totalBytes) * 100));
                }
            }

            Thread.sleep(1000); 

            File currentJar = new File(FoxtrotTweaker.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            File oldJar = new File(currentJar.getAbsolutePath() + ".old");

            if (currentJar.exists()) {
                currentJar.renameTo(oldJar);
            }

            File destinationInMods = new File(currentJar.getParentFile(), newJar.getName());
            Files.copy(newJar.toPath(), destinationInMods.toPath(), StandardCopyOption.REPLACE_EXISTING);

            SwingUtilities.invokeLater(window::dispose);

            if (!IS_BOOTING) {
                System.exit(0);
            }

        } catch (Exception e) {
            e.printStackTrace();
            SwingUtilities.invokeLater(window::dispose);
        }
    }

    public static void triggerManualUpdate() {
        new Thread(() -> {
            if (DEV_TEST_MODE) {
                FoxtrotUpdateWindow window = new FoxtrotUpdateWindow(LATEST_VERSION);
                SwingUtilities.invokeLater(() -> window.setVisible(true));
                try {
                    for (int i = 0; i <= 100; i += 5) {
                        window.setProgress(i);
                        Thread.sleep(100);
                    }
                    Thread.sleep(1000);
                    SwingUtilities.invokeLater(window::dispose);
                    System.exit(0); 
                } catch (Exception ignored) {}
            } else {
                runRealDownload(DOWNLOAD_URL);
            }
        }).start();
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) { 
        IS_BOOTING = false;
    }
    
    @Override
    public String getLaunchTarget() { 
        return "net.minecraft.client.main.Main"; 
    }

    @Override
    public String[] getLaunchArguments() { 
        // THIS MUST BE EMPTY NOW! The blackboard handles the Mixins above.
        return new String[0]; 
    }
}