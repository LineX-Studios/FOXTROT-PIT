package com.linexstudios.foxtrot.Denick;

import java.util.HashMap;
import java.util.Map;

public class NickedManager {
    // Map of nickedName -> resolvedIGN
    private static final Map<String, String> nickedPlayers = new HashMap<>();

    public static void addNicked(String nickedName, String resolvedIGN) {
        nickedPlayers.put(nickedName.toLowerCase(), resolvedIGN);
    }

    public static void removeNicked(String nickedName) {
        nickedPlayers.remove(nickedName.toLowerCase());
    }

    public static boolean isNicked(String name) {
        return nickedPlayers.containsKey(name.toLowerCase());
    }

    public static String getResolvedIGN(String nickedName) {
        return nickedPlayers.getOrDefault(nickedName.toLowerCase(), nickedName);
    }

    public static Map<String, String> getAllNicked() {
        return nickedPlayers;
    }

    public static void clear() {
        nickedPlayers.clear();
    }
}
