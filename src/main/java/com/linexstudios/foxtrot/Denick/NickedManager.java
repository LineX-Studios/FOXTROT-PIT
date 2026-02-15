package com.linexstudios.foxtrot.Denick;

import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores resolved identities discovered during the session and handles global nametag formatting.
 */
public class NickedManager {
    public static final NickedManager instance = new NickedManager();

    // Stores Nick -> Real IGN mapping (Case preserved for HUD rendering)
    private static final Map<String, String> resolvedNicks = new ConcurrentHashMap<>();

    public static void addNicked(String nick, String realName) {
        if (nick == null || realName == null) return;
        resolvedNicks.put(nick, realName);
    }

    public static void updateNicked(String nick, String realName) {
        if (nick == null || realName == null) return;
        resolvedNicks.put(nick, realName);
    }

    public static String getResolvedIGN(String nick) {
        if (nick == null) return null;
        return resolvedNicks.get(nick);
    }

    public static boolean isResolved(String nick) {
        return resolvedNicks.containsKey(nick);
    }

    public static Map<String, String> getAllNicks() {
        return resolvedNicks;
    }

    public static void clear() {
        resolvedNicks.clear();
    }

    // --- FORCES THE REAL NAME ONTO ALL IN-GAME NAMETAGS GLOBALLY ---
    @SubscribeEvent
    public void onNameFormat(PlayerEvent.NameFormat event) {
        String nickedName = event.username;
        
        // 1. Check current session memory
        String realName = getResolvedIGN(nickedName); 
        
        // 2. Check persistent JSON cache if not found in current session
        if (realName == null || realName.equals("Scraping...")) {
            realName = CacheManager.getFromCache(nickedName);
        }
        
        // 3. Apply the yellow brackets
        if (realName != null && !realName.equals("Scraping...")) {
            // Appends the yellow brackets exactly like your screenshot: 
            // The_F4st_Milk §e(ItsEmmanGaming)
            event.displayname = event.displayname + " \u00a7e(" + realName + ")";
        }
    }
}