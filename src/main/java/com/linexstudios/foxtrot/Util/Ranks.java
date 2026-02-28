package com.linexstudios.foxtrot.Util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Standalone Hypixel Pit Custom Rank Modifier
 * Replaces your Rank, Level, and Prestige in Chat, Tab, and Scoreboard locally.
 * STRICTLY activates only when inside a Hypixel Pit lobby, and cleans up after itself.
 */
public class Ranks {

    public static final Ranks instance = new Ranks();
    private final Minecraft mc = Minecraft.getMinecraft();

    // ==========================================
    //             MODULE SETTINGS
    // ==========================================
    public static boolean isEnabled = true;

    public static boolean changeLevel = true;
    public static int targetLevel = 120;

    public static boolean changePrestige = true;
    public static int targetPrestige = 50; 

    public static boolean changeRank = true;
    public static String targetRank = "admin"; // Options: none, vip, vip+, mvp, mvp+, mvp++, youtube, staff, admin

    public static boolean hideLobby = true;

    // ==========================================
    //            STATE CACHING ENGINE
    // ==========================================
    private boolean wasEnabled = false;
    private String originalScoreboardTitle = null;
    private String originalTabTeam = null;

    public boolean isInPit() {
        if (mc.theWorld == null || mc.thePlayer == null) return false;
        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        if (scoreboard == null) return false;
        
        ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
        if (objective == null) return false;
        
        String title = StringUtils.stripControlCodes(objective.getDisplayName());
        return title.contains("THE HYPIXEL PIT") || title.contains("PIT");
    }

    // ==========================================
    //                CHAT REPLACER
    // ==========================================
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onChatReceived(ClientChatReceivedEvent event) {
        if (!isEnabled || !isInPit() || mc.thePlayer == null) return;

        String originalMessage = event.message.getFormattedText();
        String unformattedMessage = StringUtils.stripControlCodes(originalMessage);
        String realName = mc.thePlayer.getName();

        if (unformattedMessage.contains(realName)) {
            
            // 1. CHECK IF WE ARE THE SENDER (Public, Party, Guild, Co-op, Shout)
            // Group 1: The channel prefix (e.g., "Party > ", "Guild > ", "[SHOUT] ") and preceding colors
            // Group 2: Our actual name 
            // Group 3: The trailing colors before the colon
            // Group 4: The message itself
            String chatRegex = "^((?:\\u00A7[0-9a-fk-or])*(?:(?:Party|Guild|Officer|Co\\-op) \\> |\\[SHOUT\\] )?)(?:\\u00A7[0-9a-fk-or]|\\[[^\\]]+\\]|\\s)*(" + realName + ")((?:\\u00A7[0-9a-fk-or])*):(.*)$";
            Matcher m = Pattern.compile(chatRegex).matcher(originalMessage);
            
            if (m.find()) {
                String channelPrefix = m.group(1);
                String trailingColors = m.group(3);
                String chatMessage = m.group(4);
                
                // Rebuild the message perfectly, preserving the Guild/Party prefix!
                String customPrefix = channelPrefix + getCustomChatPitBracket() + " " + getCustomRankPrefix() + getRankColor(targetRank) + realName + trailingColors + ":" + getChatColor(targetRank);
                event.message = new ChatComponentText(customPrefix + chatMessage);
                return;
            }
            
            // 2. SAFE FALLBACK (For Kill Feeds, Bounties, and Mentions)
            // The [^\\]]+ ensures the regex ONLY eats brackets physically touching your name. 
            // It will no longer eat other players' names if they mention you!
            String fallbackRegex = "(?:\\u00A7[0-9a-fk-or]|\\[[^\\]]+\\]|\\s)*" + realName + "(?!\\w)";
            String replacement = getCustomChatPitBracket() + " " + getCustomRankPrefix() + getRankColor(targetRank) + realName + EnumChatFormatting.RESET;
            
            String replacedMessage = originalMessage.replaceAll(fallbackRegex, replacement);
            event.message = new ChatComponentText(replacedMessage);
        }
    }

    // ==========================================
    //         TAB & SCOREBOARD REPLACER
    // ==========================================
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || mc.thePlayer == null) return;

        boolean pit = isInPit();

        if (isEnabled && pit) {
            wasEnabled = true;
            String realName = mc.thePlayer.getName();

            // 1. Replace in Tab List
            for (NetworkPlayerInfo playerInfo : mc.getNetHandler().getPlayerInfoMap()) {
                if (playerInfo.getGameProfile().getName().equals(realName)) {
                    String tabName = getRankColor(targetRank) + realName;
                    if (changeLevel || changePrestige) {
                        tabName = getCustomTabPitBracket() + " " + tabName;
                    }
                    playerInfo.setDisplayName(new ChatComponentText(tabName));
                }
            }

            // 2. Scoreboard Manipulation
            if (mc.theWorld != null) {
                Scoreboard scoreboard = mc.theWorld.getScoreboard();
                ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
                
                if (objective != null) {
                    // A. Hide Lobby Name
                    if (hideLobby) {
                        String currentTitle = objective.getDisplayName();
                        String customTitle = EnumChatFormatting.YELLOW + "" + EnumChatFormatting.BOLD + "THE HYPIXEL PIT";
                        if (!currentTitle.equals(customTitle)) {
                            originalScoreboardTitle = currentTitle;
                            objective.setDisplayName(customTitle);
                        }
                    }

                    // B. Dynamic Tablist Hierarchy Sorter
                    // Note: The old Scoreboard text replacement block was removed here because 
                    // our new MixinScorePlayerTeam handles it with zero flickering!
                }

                // C. Dynamic Tablist Hierarchy Sorter
                if (changeLevel || changePrestige) {
                    ScorePlayerTeam currentTeam = scoreboard.getPlayersTeam(realName);
                    
                    // Creates a hidden sorting prefix. 
                    // Minecraft sorts Tab alphabetically based on the Team's prefix + player name.
                    // A level 120 gets sorted before a level 0 because "A" comes before "Z"
                    char prestigeChar = (char) ('A' + (50 - Math.max(0, Math.min(50, targetPrestige))));
                    int lIndex = 120 - Math.max(0, Math.min(120, targetLevel));
                    String sortKey = String.format("!%c%03d", prestigeChar, lIndex); // Exclamation mark pushes it to the absolute top
                    
                    String customTeamName = "Fx" + sortKey;

                    if (currentTeam != null && !currentTeam.getRegisteredName().startsWith("Fx")) {
                        originalTabTeam = currentTeam.getRegisteredName();
                    }
                    
                    ScorePlayerTeam customTeam = scoreboard.getTeam(customTeamName);
                    if (customTeam == null) {
                        customTeam = scoreboard.createTeam(customTeamName);
                    }
                    
                    // We must apply the sort key to the prefix, but make it invisible using color codes
                    // We use §r so it doesn't color the actual name, but Minecraft still uses it for sorting
                    customTeam.setNamePrefix(EnumChatFormatting.RESET + "");
                    customTeam.setNameSuffix("");
                    
                    scoreboard.addPlayerToTeam(realName, customTeamName);
                }
            }
        } 
        // ==========================================
        //         CLEANUP & REVERT TO NORMAL
        // ==========================================
        else if (wasEnabled) {
            wasEnabled = false;
            String realName = mc.thePlayer.getName();

            if (mc.getNetHandler() != null) {
                for (NetworkPlayerInfo playerInfo : mc.getNetHandler().getPlayerInfoMap()) {
                    if (playerInfo.getGameProfile().getName().equals(realName)) {
                        playerInfo.setDisplayName(null);
                    }
                }
            }

            if (mc.theWorld != null) {
                Scoreboard scoreboard = mc.theWorld.getScoreboard();
                ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
                
                if (objective != null && originalScoreboardTitle != null) {
                    objective.setDisplayName(originalScoreboardTitle);
                }

                if (originalTabTeam != null && !originalTabTeam.isEmpty()) {
                    scoreboard.addPlayerToTeam(realName, originalTabTeam);
                }
            }
        }
    }

    // ==========================================
    //             UTILITY & FORMATTING
    // ==========================================

    private String getCustomChatPitBracket() {
        EnumChatFormatting prestigeColor = getPrestigeColor(targetPrestige);
        EnumChatFormatting levelColor = getLevelColor(targetLevel);
        
        StringBuilder bracket = new StringBuilder();
        
        bracket.append(prestigeColor).append("[");
        
        if (targetPrestige > 0 && changePrestige) {
            bracket.append(EnumChatFormatting.YELLOW).append(toRoman(targetPrestige)).append(prestigeColor).append("-");
        }
        
        if (changeLevel) {
            bracket.append(levelColor).append(targetLevel);
        }
        
        bracket.append(prestigeColor).append("]");
        
        return bracket.toString();
    }

    public String getCustomTabPitBracket() {
        EnumChatFormatting prestigeColor = getPrestigeColor(targetPrestige);
        EnumChatFormatting levelColor = getLevelColor(targetLevel);
        
        StringBuilder bracket = new StringBuilder();
        
        bracket.append(prestigeColor).append("[");
        if (changeLevel) {
            bracket.append(levelColor).append(targetLevel);
        }
        bracket.append(prestigeColor).append("]");
        
        return bracket.toString();
    }

    private String getCustomRankPrefix() {
        switch (targetRank.toLowerCase()) {
            case "vip":     return EnumChatFormatting.GREEN + "[VIP] ";
            case "vip+":    return EnumChatFormatting.GREEN + "[VIP" + EnumChatFormatting.GOLD + "+" + EnumChatFormatting.GREEN + "] ";
            case "mvp":     return EnumChatFormatting.AQUA + "[MVP] ";
            case "mvp+":    return EnumChatFormatting.AQUA + "[MVP" + EnumChatFormatting.RED + "+" + EnumChatFormatting.AQUA + "] ";
            case "mvp++":   return EnumChatFormatting.GOLD + "[MVP" + EnumChatFormatting.RED + "++" + EnumChatFormatting.GOLD + "] ";
            case "youtube": return EnumChatFormatting.RED + "[" + EnumChatFormatting.WHITE + "YOUTUBE" + EnumChatFormatting.RED + "] ";
            case "staff":   return EnumChatFormatting.RED + "[" + EnumChatFormatting.GOLD + "\u12DE" + EnumChatFormatting.RED + "] ";
            case "admin":   return EnumChatFormatting.RED + "[ADMIN] ";
            case "none":
            default:        return ""; 
        }
    }

    private EnumChatFormatting getRankColor(String rank) {
        switch (rank.toLowerCase()) {
            case "vip": 
            case "vip+": return EnumChatFormatting.GREEN;
            case "mvp": 
            case "mvp+": return EnumChatFormatting.AQUA;
            case "mvp++": return EnumChatFormatting.GOLD;
            case "admin":
            case "youtube": 
            case "staff": return EnumChatFormatting.RED;
            case "none":
            default: return EnumChatFormatting.GRAY;
        }
    }

    private EnumChatFormatting getChatColor(String rank) {
        if (rank.equalsIgnoreCase("none")) {
            return EnumChatFormatting.GRAY;
        }
        return EnumChatFormatting.WHITE;
    }

    private String toRoman(int num) {
        int[] values = {100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] romanLetters = {"C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        StringBuilder roman = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            while (num >= values[i]) {
                num -= values[i];
                roman.append(romanLetters[i]);
            }
        }
        return roman.toString();
    }

    private EnumChatFormatting getPrestigeColor(int prestige) {
        if (prestige >= 50) return EnumChatFormatting.DARK_GRAY;  
        if (prestige >= 48) return EnumChatFormatting.DARK_RED;   
        if (prestige >= 45) return EnumChatFormatting.BLACK;      
        if (prestige >= 40) return EnumChatFormatting.DARK_BLUE;  
        if (prestige >= 35) return EnumChatFormatting.AQUA;       
        if (prestige >= 30) return EnumChatFormatting.WHITE;      
        if (prestige >= 25) return EnumChatFormatting.LIGHT_PURPLE; 
        if (prestige >= 20) return EnumChatFormatting.DARK_PURPLE; 
        if (prestige >= 15) return EnumChatFormatting.RED;        
        if (prestige >= 10) return EnumChatFormatting.GOLD;       
        if (prestige >= 5)  return EnumChatFormatting.YELLOW;     
        if (prestige >= 1)  return EnumChatFormatting.BLUE;       
        return EnumChatFormatting.GRAY;                           
    }

    private EnumChatFormatting getLevelColor(int level) {
        if (level >= 120) return EnumChatFormatting.AQUA;
        if (level >= 110) return EnumChatFormatting.WHITE;
        if (level >= 100) return EnumChatFormatting.LIGHT_PURPLE;
        if (level >= 90) return EnumChatFormatting.DARK_PURPLE; 
        if (level >= 80) return EnumChatFormatting.DARK_RED;
        if (level >= 70) return EnumChatFormatting.RED;
        if (level >= 60) return EnumChatFormatting.GOLD;
        if (level >= 50) return EnumChatFormatting.YELLOW;
        if (level >= 40) return EnumChatFormatting.GREEN;
        if (level >= 30) return EnumChatFormatting.DARK_GREEN;
        if (level >= 20) return EnumChatFormatting.DARK_AQUA;
        if (level >= 10) return EnumChatFormatting.DARK_BLUE;
        return EnumChatFormatting.GRAY;
    }
}