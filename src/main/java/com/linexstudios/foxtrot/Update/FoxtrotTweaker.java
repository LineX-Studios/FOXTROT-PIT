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
    
    public static boolean DEV_TEST_MODE = false; 
    public static File foxtrotDir;

    @SuppressWarnings("unchecked")
    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
        
        List<String> tweakClasses = (List<String>) Launch.blackboard.get("TweakClasses");
        if (tweakClasses != null && !tweakClasses.contains("org.spongepowered.asm.launch.MixinTweaker")) {
            tweakClasses.add("org.spongepowered.asm.launch.MixinTweaker");
        }

        foxtrotDir = new File(gameDir, "Foxtrot");
        if (!foxtrotDir.exists()) foxtrotDir.mkdirs();

        try {
            File installerJar = new File(foxtrotDir, "updater.jar");
            if (installerJar.exists()) installerJar.delete();
        } catch (Exception ignored) {}

        try {
            File currentJar = new File(FoxtrotTweaker.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            File actualModsDir = currentJar.getParentFile(); 
            
            if (actualModsDir.exists() && actualModsDir.isDirectory()) {
                File[] oldFiles = actualModsDir.listFiles((dir, name) -> name.endsWith(".old"));
                if (oldFiles != null) {
                    for (File old : oldFiles) old.delete();
                }
                
                File[] duplicateJars = actualModsDir.listFiles((dir, name) -> name.toLowerCase().startsWith("foxtrot-") && name.endsWith(".jar") && !name.equals(currentJar.getName()));
                if (duplicateJars != null) {
                    for (File dup : duplicateJars) {
                        try { 
                            if (!dup.delete()) {
                                dup.renameTo(new File(dup.getAbsolutePath() + ".old"));
                            } 
                        } catch (Exception ignored) {}
                    }
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
                
                if (isNewerVersion(CURRENT_VERSION, latestVersion)) {
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

    private boolean isNewerVersion(String current, String latest) {
        try {
            String[] cParts = current.replaceAll("[^0-9.]", "").split("\\.");
            String[] lParts = latest.replaceAll("[^0-9.]", "").split("\\.");
            int length = Math.max(cParts.length, lParts.length);
            for (int i = 0; i < length; i++) {
                int c = i < cParts.length && !cParts[i].isEmpty() ? Integer.parseInt(cParts[i]) : 0;
                int l = i < lParts.length && !lParts[i].isEmpty() ? Integer.parseInt(lParts[i]) : 0;
                if (l > c) return true;
                if (l < c) return false;
            }
        } catch (Exception ignored) {}
        return false;
    }

    public static void runRealDownload(String downloadUrl) {
        FoxtrotUpdateWindow window = new FoxtrotUpdateWindow(LATEST_VERSION);
        SwingUtilities.invokeLater(() -> window.setVisible(true));

        try {
            File tempJar = new File(foxtrotDir, "update_temp.jar");
            File updaterJar = new File(foxtrotDir, "updater.jar");

            HttpURLConnection conn = (HttpURLConnection) new URL(downloadUrl).openConnection();
            conn.setRequestProperty("User-Agent", "Foxtrot-Updater");
            int totalBytes = conn.getContentLength();

            try (InputStream in = new BufferedInputStream(conn.getInputStream());
                 OutputStream out = new BufferedOutputStream(new FileOutputStream(tempJar))) {

                byte[] buffer = new byte[8192];
                int bytesRead = 0, nRead;
                while ((nRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, nRead);
                    bytesRead += nRead;
                    window.setProgress((int) (((double) bytesRead / totalBytes) * 100));
                }
            }

            Thread.sleep(500); 

            File currentJar = new File(FoxtrotTweaker.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            File destinationInMods = new File(currentJar.getParentFile(), "Foxtrot-" + LATEST_VERSION + ".jar");

            Files.copy(currentJar.toPath(), updaterJar.toPath(), StandardCopyOption.REPLACE_EXISTING);

            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
            String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + (isWindows ? "javaw.exe" : "java");

            ProcessBuilder pb = new ProcessBuilder(
                    javaBin, 
                    "-cp", updaterJar.getAbsolutePath(), 
                    "com.linexstudios.foxtrot.Update.FoxtrotRelocator", 
                    currentJar.getAbsolutePath(), 
                    tempJar.getAbsolutePath(), 
                    destinationInMods.getAbsolutePath()
            );

            pb.redirectError(new File(foxtrotDir, "updater_error.log"));
            pb.redirectOutput(new File(foxtrotDir, "updater_output.log"));
            pb.start();

            SwingUtilities.invokeLater(() -> {
                window.dispose(); 
                FoxtrotSuccessWindow successWindow = new FoxtrotSuccessWindow(LATEST_VERSION, null);
                successWindow.setVisible(true);
            });

        } catch (Exception e) {
            e.printStackTrace();
            SwingUtilities.invokeLater(window::dispose);
        }
    }

    public static void triggerManualUpdate() {
        new Thread(() -> {
            runRealDownload(DOWNLOAD_URL);
        }).start();
    }

    @Override public void injectIntoClassLoader(LaunchClassLoader classLoader) {}
    @Override public String getLaunchTarget() { return "net.minecraft.client.main.Main"; }
    @Override public String[] getLaunchArguments() { return new String[0]; }
}