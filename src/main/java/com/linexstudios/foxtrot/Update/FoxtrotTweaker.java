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
import com.google.gson.*;
import net.minecraft.launchwrapper.*;
import javax.swing.SwingUtilities;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.List;

public class FoxtrotTweaker implements ITweaker {
    public static final String CURRENT_VERSION = "${version}";
    public static boolean UPDATE_AVAILABLE = false;
    public static String LATEST_VERSION = "", DOWNLOAD_URL = "";
    public static boolean DEV_TEST_MODE = false, isChecking = false;
    public static volatile boolean isManualUpdateRunning = false;
    public static File foxtrotDir;

    @SuppressWarnings("unchecked") @Override public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
        List<String> tweakClasses = (List<String>) Launch.blackboard.get("TweakClasses");
        if (tweakClasses != null && !tweakClasses.contains("org.spongepowered.asm.launch.MixinTweaker")) tweakClasses.add("org.spongepowered.asm.launch.MixinTweaker");
        foxtrotDir = new File(gameDir, "Foxtrot");
        if (!foxtrotDir.exists()) foxtrotDir.mkdirs();
        try { File installerJar = new File(foxtrotDir, "updater.jar"); if (installerJar.exists()) installerJar.delete(); } catch (Exception e) {}
        try {
            File currentJar = new File(FoxtrotTweaker.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            File actualModsDir = currentJar.getParentFile(); 
            if (actualModsDir.exists() && actualModsDir.isDirectory()) {
                File[] oldFiles = actualModsDir.listFiles((dir, name) -> name.endsWith(".old"));
                if (oldFiles != null) for (File old : oldFiles) old.delete();
                File[] duplicateJars = actualModsDir.listFiles((dir, name) -> name.toLowerCase().startsWith("foxtrot-") && name.endsWith(".jar") && !name.equals(currentJar.getName()));
                if (duplicateJars != null) for (File dup : duplicateJars) try { if (!dup.delete()) dup.renameTo(new File(dup.getAbsolutePath() + ".old")); } catch (Exception e) {}
            }
        } catch (Exception e) {}
    }

    public static void checkUpdatesAsync() {
        if (isChecking) return;
        isChecking = true; LATEST_VERSION = "Checking...";
        new Thread(() -> {
            if (DEV_TEST_MODE) { UPDATE_AVAILABLE = true; LATEST_VERSION = "v0.7.9-TEST"; isChecking = false; return; }
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL("https://api.github.com/repos/LineX-Studios/FOXTROT-PIT/releases/latest").openConnection();
                conn.setRequestMethod("GET"); conn.setRequestProperty("User-Agent", "Foxtrot-Updater"); 
                if (conn.getResponseCode() == 200) {
                    InputStreamReader reader = new InputStreamReader(conn.getInputStream());
                    JsonObject response = new JsonParser().parse(reader).getAsJsonObject(); reader.close();
                    LATEST_VERSION = response.get("tag_name").getAsString();
                    if (isNewerVersion(CURRENT_VERSION.replace("v", ""), LATEST_VERSION.replace("v", ""))) {
                        UPDATE_AVAILABLE = true; DOWNLOAD_URL = response.getAsJsonArray("assets").get(0).getAsJsonObject().get("browser_download_url").getAsString();
                        boolean auto = true;
                        try {
                            File file = new File(foxtrotDir, "settings.txt");
                            if (file.exists()) { BufferedReader br = new BufferedReader(new FileReader(file)); String line; while ((line = br.readLine()) != null) if (line.replace(" ", "").contains("autoUpdateEnabled=false")) auto = false; br.close(); }
                        } catch (Exception e) {}
                        if (auto) runRealDownload(DOWNLOAD_URL);
                    }
                } else LATEST_VERSION = "API Limit";
            } catch (Throwable t) { LATEST_VERSION = "Failed"; }
            isChecking = false;
        }).start();
    }

    private static boolean isNewerVersion(String current, String latest) {
        try {
            String[] cParts = current.replaceAll("[^0-9.]", "").split("\\."); String[] lParts = latest.replaceAll("[^0-9.]", "").split("\\.");
            for (int i = 0; i < Math.max(cParts.length, lParts.length); i++) {
                int c = i < cParts.length && !cParts[i].isEmpty() ? Integer.parseInt(cParts[i]) : 0;
                int l = i < lParts.length && !lParts[i].isEmpty() ? Integer.parseInt(lParts[i]) : 0;
                if (l > c) return true; if (l < c) return false;
            }
        } catch (Exception e) {} return false;
    }

    public static void runRealDownload(String downloadUrl) {
        if (downloadUrl == null || downloadUrl.trim().isEmpty()) {
            System.err.println("[Foxtrot-Updater] ERROR: Missing download URL for update.");
            return;
        }

        FoxtrotUpdateWindow window = new FoxtrotUpdateWindow(LATEST_VERSION);
        SwingUtilities.invokeLater(() -> window.setVisible(true));
        try {
            File tempJar = new File(foxtrotDir, "update_temp.jar"), updaterJar = new File(foxtrotDir, "updater.jar");
            HttpURLConnection conn = (HttpURLConnection) new URL(downloadUrl).openConnection(); conn.setRequestProperty("User-Agent", "Foxtrot-Updater");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            int totalBytes = conn.getContentLength();
            try (InputStream in = new BufferedInputStream(conn.getInputStream()); OutputStream out = new BufferedOutputStream(new FileOutputStream(tempJar))) {
                byte[] buffer = new byte[8192]; int bytesRead = 0, nRead;
                while ((nRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, nRead);
                    bytesRead += nRead;
                    if (totalBytes > 0) {
                        window.setProgress((int) (((double) bytesRead / totalBytes) * 100));
                    }
                }
            }
            window.setProgress(100);
            Thread.sleep(500); 
            File currentJar = new File(FoxtrotTweaker.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            File destinationInMods = new File(currentJar.getParentFile(), "Foxtrot-" + LATEST_VERSION.replace("v", "") + ".jar");
            Files.copy(currentJar.toPath(), updaterJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
            String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + (System.getProperty("os.name").toLowerCase().contains("win") ? "javaw.exe" : "java");
            ProcessBuilder pb = new ProcessBuilder(javaBin, "-cp", updaterJar.getAbsolutePath(), "com.linexstudios.foxtrot.Update.FoxtrotRelocator", currentJar.getAbsolutePath(), tempJar.getAbsolutePath(), destinationInMods.getAbsolutePath());
            pb.redirectError(new File(foxtrotDir, "updater_error.log")); pb.redirectOutput(new File(foxtrotDir, "updater_output.log")); pb.start();
            SwingUtilities.invokeLater(() -> { window.dispose(); new FoxtrotSuccessWindow(LATEST_VERSION, null).setVisible(true); });
            forceClientShutdownForUpdate();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                File log = foxtrotDir != null ? new File(foxtrotDir, "updater_error.log") : null;
                if (log != null) {
                    try (FileWriter fw = new FileWriter(log, true)) {
                        fw.write("[Foxtrot-Updater] Manual update failed: " + e + "\n");
                    }
                }
            } catch (Exception ignored) {}
            SwingUtilities.invokeLater(window::dispose);
        } finally {
            isManualUpdateRunning = false;
        }
    }

    private static void forceClientShutdownForUpdate() {
        new Thread(() -> {
            try {
                Thread.sleep(2500);
                try {
                    Class<?> mcCls = Class.forName("net.minecraft.client.Minecraft");
                    Object mc = mcCls.getMethod("getMinecraft").invoke(null);
                    mcCls.getMethod("shutdown").invoke(mc);
                } catch (Throwable ignored) {}

                try { Runtime.getRuntime().exit(0); } catch (Throwable ignored) {}
                try { System.exit(0); } catch (Throwable ignored) {}
                Runtime.getRuntime().halt(0);
            } catch (Exception e) {
                Runtime.getRuntime().halt(0);
            }
        }, "Foxtrot-Update-Shutdown").start();
    }

    public static boolean triggerManualUpdate() {
        if (isManualUpdateRunning) {
            return false;
        }
        if (DOWNLOAD_URL == null || DOWNLOAD_URL.trim().isEmpty()) {
            return false;
        }
        isManualUpdateRunning = true;
        new Thread(() -> runRealDownload(DOWNLOAD_URL), "Foxtrot-Manual-Updater").start();
        return true;
    }

    @Override public void injectIntoClassLoader(LaunchClassLoader classLoader) {}
    @Override public String getLaunchTarget() { return "net.minecraft.client.main.Main"; }
    @Override public String[] getLaunchArguments() { return new String[0]; }
}
