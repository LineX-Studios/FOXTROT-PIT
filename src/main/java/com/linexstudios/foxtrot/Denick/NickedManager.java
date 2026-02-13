package com.linexstudios.foxtrot.Denick;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores resolved identities discovered during the session.
 */
public class NickedManager {
    // Stores Nick -> Real IGN mapping
    private static final Map<String, String> resolvedNicks = new ConcurrentHashMap<>();

    public static void addNicked(String nick, String realName) {
        if (nick == null || realName == null) return;
        resolvedNicks.put(nick.toLowerCase(), realName);
    }

    public static String getResolvedIGN(String nick) {
        if (nick == null) return null;
        return resolvedNicks.get(nick.toLowerCase());
    }

    public static boolean isResolved(String nick) {
        return resolvedNicks.containsKey(nick.toLowerCase());
    }

    public static void clear() {
        resolvedNicks.clear();
    }
}