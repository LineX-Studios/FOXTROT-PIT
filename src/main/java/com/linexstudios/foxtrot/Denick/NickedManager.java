package com.linexstudios.foxtrot.Denick;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores resolved identities discovered during the session.
 */
public class NickedManager {
    // Stores Nick -> Real IGN mapping (Case preserved for HUD rendering)
    private static final Map<String, String> resolvedNicks = new ConcurrentHashMap<>();

    public static void addNicked(String nick, String realName) {
        if (nick == null || realName == null) return;
        resolvedNicks.put(nick, realName);
    }

    // Alias for updating the placeholder once scraped
    public static void updateNicked(String nick, String realName) {
        if (nick == null || realName == null) return;
        resolvedNicks.put(nick, realName); // ConcurrentHashMap natively overwrites the old value
    }

    public static String getResolvedIGN(String nick) {
        if (nick == null) return null;
        return resolvedNicks.get(nick);
    }

    public static boolean isResolved(String nick) {
        return resolvedNicks.containsKey(nick);
    }

    // NEW: Needed for the HUD to loop through and render all detected players
    public static Map<String, String> getAllNicks() {
        return resolvedNicks;
    }

    public static void clear() {
        resolvedNicks.clear();
    }
}