package com.linexstudios.foxtrot.Denick;

import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NickedManager {
    public static final NickedManager instance = new NickedManager();

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

    @SubscribeEvent
    public void onNameFormat(PlayerEvent.NameFormat event) {
        String username = event.username;
        String display = event.displayname;

        // 1. Friends Check
        if (com.linexstudios.foxtrot.Hud.FriendsHUD.isFriend(username)) {
            display = EnumChatFormatting.DARK_GREEN + "[" + EnumChatFormatting.GREEN + "F" + EnumChatFormatting.DARK_GREEN + "] " + EnumChatFormatting.RESET + display;
        } 
        // 2. Enemy Check
        else if (com.linexstudios.foxtrot.Hud.EnemyHUD.isTarget(username)) {
            display = EnumChatFormatting.DARK_RED + "[" + EnumChatFormatting.RED + "E" + EnumChatFormatting.DARK_RED + "] " + EnumChatFormatting.RESET + display;
        }

        // 3. Nicked Check
        String realName = CacheManager.getFromCache(username);
        if (realName == null) {
            realName = getResolvedIGN(username); 
        }
        
        if (realName != null && !realName.equals("Scraping") && !realName.equals("Failed") && !realName.equals("No Nonce")) {
            display = EnumChatFormatting.DARK_BLUE + "[" + EnumChatFormatting.BLUE + "N" + EnumChatFormatting.DARK_BLUE + "] " + EnumChatFormatting.RESET + display + " \u00a7e(" + realName + ")";
        }

        event.displayname = display;
    }
}