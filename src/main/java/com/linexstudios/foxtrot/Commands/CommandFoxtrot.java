package com.linexstudios.foxtrot.Commands;

import com.linexstudios.foxtrot.Denick.DenickRunnable;
import com.linexstudios.foxtrot.Denick.AutoDenick;
import com.linexstudios.foxtrot.Enemy.EnemyESP;
import com.linexstudios.foxtrot.Hud.EditHUDGui;
import com.linexstudios.foxtrot.Hud.EnemyHUD;
import com.linexstudios.foxtrot.Hud.FriendsHUD;
import com.linexstudios.foxtrot.Hud.NickedHUD;
import com.linexstudios.foxtrot.Hud.HUDController;
import com.linexstudios.foxtrot.Handler.ConfigHandler;
import com.linexstudios.foxtrot.Denick.CacheManager;
import com.linexstudios.foxtrot.Util.Ranks; 
import com.linexstudios.foxtrot.Render.FocusManager;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CommandFoxtrot extends CommandBase {
    @Override
    public String getCommandName() { return "foxtrot"; }

    @Override
    public List<String> getCommandAliases() {
        List<String> aliases = new java.util.ArrayList<>();
        aliases.add("fx");
        return aliases;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/foxtrot <friend|add|remove|list|alerts|toggle|clear|denick|debug|esp|autodenick|hud|nickhud|enemyhud|denickentry|rank|focus>";
    }

    @Override
    public int getRequiredPermissionLevel() { return 0; }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + "[" + EnumChatFormatting.RED + "FOXTROT" + EnumChatFormatting.GRAY + "] " + EnumChatFormatting.RED + EnumChatFormatting.BOLD + "HELP MENU"));
            sender.addChatMessage(new ChatComponentText(""));

            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/foxtrot friend <add/remove/list> " + EnumChatFormatting.GRAY + "- Manage Friends"));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/foxtrot add [name] " + EnumChatFormatting.GRAY + "- Add enemy"));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/foxtrot remove [name] " + EnumChatFormatting.GRAY + "- Remove enemy"));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/foxtrot list " + EnumChatFormatting.GRAY + "- View enemy list"));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/foxtrot focus <name/remove/clear> " + EnumChatFormatting.GRAY + "- Hide all other players"));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/foxtrot alerts " + EnumChatFormatting.GRAY + "- Toggle join alerts"));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/foxtrot toggle " + EnumChatFormatting.GRAY + "- Toggle all HUDs"));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/foxtrot esp " + EnumChatFormatting.GRAY + "- Toggle ESP"));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/foxtrot denick [name] " + EnumChatFormatting.GRAY + "- Scrape Player"));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/foxtrot denickentry clear [name] " + EnumChatFormatting.GRAY + "- Clear cached nick"));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/foxtrot autodenick " + EnumChatFormatting.GRAY + "- Toggle automatic denicking"));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/foxtrot hud " + EnumChatFormatting.GRAY + "- Open HUD Editor"));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/foxtrot nickhud " + EnumChatFormatting.GRAY + "- Toggle NickedHUD"));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/foxtrot enemyhud " + EnumChatFormatting.GRAY + "- Toggle EnemyHUD"));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/foxtrot clear " + EnumChatFormatting.GRAY + "- Clear enemy list"));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/foxtrot rank <prestige> <level> <rank> " + EnumChatFormatting.GRAY + "- Change your Prestige/Rank")); 
            return;
        }

        String action = args[0].toLowerCase();
        ConfigHandler.logDebug("User executed: /fx " + String.join(" ", args));

        switch (action) {
            // ==========================================
            //               NEW: FOCUS COMMAND
            // ==========================================
            case "focus":
                if (args.length == 1) {
                    if (FocusManager.focusList.isEmpty()) {
                        sendMessage(sender, EnumChatFormatting.RED + "Usage: /fx focus [name] or /fx focus remove [name]");
                    } else {
                        sendMessage(sender, EnumChatFormatting.AQUA + "Currently Focusing: " + String.join(", ", FocusManager.focusList));
                    }
                } else if (args.length >= 2) {
                    if (args[1].equalsIgnoreCase("clear")) {
                        FocusManager.focusList.clear();
                        sendMessage(sender, EnumChatFormatting.GREEN + "Cleared focus list. All players are now visible.");
                    } else if (args[1].equalsIgnoreCase("remove") && args.length >= 3) {
                        String target = args[2];
                        if (FocusManager.focusList.removeIf(n -> n.equalsIgnoreCase(target))) {
                            sendMessage(sender, EnumChatFormatting.RED + "Removed " + target + " from focus list.");
                        } else {
                            sendMessage(sender, EnumChatFormatting.YELLOW + target + " is not in your focus list.");
                        }
                    } else if (args[1].equalsIgnoreCase("remove")) {
                        sendMessage(sender, EnumChatFormatting.RED + "Usage: /fx focus remove [name]");
                    } else {
                        String target = args[1];
                        if (!FocusManager.focusList.contains(target.toLowerCase())) {
                            FocusManager.focusList.add(target.toLowerCase());
                            sendMessage(sender, EnumChatFormatting.GREEN + "Now focusing on: " + target + ". All other players are hidden.");
                        } else {
                            sendMessage(sender, EnumChatFormatting.RED + "You are already focusing on " + target + "!");
                        }
                    }
                }
                break;

            case "rank":
                if (args.length == 1) {
                    Ranks.isEnabled = !Ranks.isEnabled;
                    ConfigHandler.saveConfig();
                    sendMessage(sender, EnumChatFormatting.YELLOW + "Rank Changer: " + (Ranks.isEnabled ? EnumChatFormatting.GREEN + "ON" : EnumChatFormatting.RED + "OFF"));
                    return;
                }
                
                if (args.length >= 4) {
                    try {
                        int prestige = Integer.parseInt(args[1]);
                        int level = Integer.parseInt(args[2]);
                        String rank = args[3].toLowerCase();

                        List<String> validRanks = Arrays.asList("none", "vip", "vip+", "mvp", "mvp+", "mvp++", "youtube", "staff", "admin");
                        if (!validRanks.contains(rank)) {
                            sendMessage(sender, EnumChatFormatting.RED + "Invalid rank! Options: none, vip, vip+, mvp, mvp+, mvp++, youtube, staff, admin");
                            return;
                        }

                        Ranks.targetPrestige = prestige;
                        Ranks.targetLevel = level;
                        Ranks.targetRank = rank;
                        Ranks.isEnabled = true; 
                        ConfigHandler.saveConfig();

                        sendMessage(sender, EnumChatFormatting.GREEN + "Successfully updated your rank!");
                        sendMessage(sender, EnumChatFormatting.GRAY + "Your new stats: Prestige " + prestige + ", Level " + level + ", Rank: " + rank.toUpperCase());
                    } catch (NumberFormatException e) {
                        sendMessage(sender, EnumChatFormatting.RED + "Prestige and Level must be numbers!");
                    }
                } else {
                    sendMessage(sender, EnumChatFormatting.RED + "Usage: /fx rank <prestige> <level> <rank>");
                    sendMessage(sender, EnumChatFormatting.GRAY + "Example: /fx rank 35 120 staff");
                }
                break;

            case "denickentry":
                if (args.length >= 3 && args[1].equalsIgnoreCase("clear")) {
                    String target = args[2];
                    NickedHUD.nickedPlayers.removeIf(n -> n.equalsIgnoreCase(target));
                    CacheManager.removeFromCache(target);
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "[Foxtrot] " + EnumChatFormatting.GREEN + "Cleared " + EnumChatFormatting.WHITE + target + EnumChatFormatting.GREEN + " from denick memory!"));
                } else {
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Usage: /fx denickentry clear [name]"));
                }
                break;

            case "debug":
                EnemyHUD.debugMode = !EnemyHUD.debugMode;
                com.linexstudios.foxtrot.Combat.AutoClicker.debugMode = EnemyHUD.debugMode;
                ConfigHandler.saveConfig();
                sendMessage(sender, EnumChatFormatting.YELLOW + "Foxtrot Global Debug: " + (EnemyHUD.debugMode ? EnumChatFormatting.GREEN + "ON" : EnumChatFormatting.RED + "OFF"));
                break;

            case "denick":
                if (args.length > 1) {
                    String target = args[1];
                    new Thread(new DenickRunnable(target)).start();
                    if (!NickedHUD.nickedPlayers.contains(target.toLowerCase())) {
                        NickedHUD.nickedPlayers.add(target.toLowerCase());
                    }
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + "[" + EnumChatFormatting.GOLD + "Foxtrot" + EnumChatFormatting.GRAY + "] " + EnumChatFormatting.GREEN + "Scraping " + target + " and added to NickedHUD."));
                } else {
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Usage: /fx denick [name]"));
                }
                break;

            case "esp":
                EnemyESP.enabled = !EnemyESP.enabled;
                ConfigHandler.saveConfig();
                sendMessage(sender, EnumChatFormatting.YELLOW + "Enemy ESP: " + (EnemyESP.enabled ? EnumChatFormatting.GREEN + "ON" : EnumChatFormatting.RED + "OFF"));
                break;

            case "friend":
            case "f":
                if (args.length < 2) {
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Usage: /fx friend <add|remove|list> [name]"));
                    break;
                }
                String subAction = args[1].toLowerCase();
                if (subAction.equals("add")) {
                    if (args.length > 2) {
                        String friendName = args[2];
                        if (!FriendsHUD.isFriend(friendName)) {
                            FriendsHUD.friendsList.add(friendName);
                            ConfigHandler.saveConfig();
                            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "Added " + friendName + " to friends."));
                        } else {
                            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + friendName + " is already on your friends list!"));
                        }
                    } else {
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Usage: /fx friend add [name]"));
                    }
                } else if (subAction.equals("remove")) {
                    if (args.length > 2) {
                        String friendName = args[2];
                        boolean removed = FriendsHUD.friendsList.removeIf(name -> name.equalsIgnoreCase(friendName));
                        if (removed) {
                            ConfigHandler.saveConfig();
                            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Removed " + friendName + " from friends."));
                        } else {
                            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + friendName + " was not found on your friends list."));
                        }
                    } else {
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Usage: /fx friend remove [name]"));
                    }
                } else if (subAction.equals("list")) {
                    if (FriendsHUD.friendsList.isEmpty()) {
                        sendMessage(sender, EnumChatFormatting.RED + "Your friends list is empty.");
                    } else {
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "Friends List (" + FriendsHUD.friendsList.size() + ")"));
                        sender.addChatMessage(new ChatComponentText(""));
                        for (String name : FriendsHUD.friendsList) {
                            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + "- " + EnumChatFormatting.GREEN + name));
                        }
                    }
                } else {
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Unknown action. Usage: /fx friend <add|remove|list> [name]"));
                }
                break;

            case "add":
                if (args.length > 1) {
                    String nameToAdd = args[1];
                    if (!EnemyHUD.targetList.stream().anyMatch(name -> name.equalsIgnoreCase(nameToAdd))) {
                        EnemyHUD.targetList.add(nameToAdd);
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "Added " + nameToAdd + " to enemies."));
                    } else {
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + nameToAdd + " is already on your enemy list!"));
                    }
                } else {
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Usage: /fx add [name]"));
                }
                break;

            case "remove":
                if (args.length > 1) {
                    String nameToRemove = args[1];
                    boolean removed = EnemyHUD.targetList.removeIf(name -> name.equalsIgnoreCase(nameToRemove));
                    if (removed) {
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Removed " + nameToRemove + " from enemies."));
                    } else {
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + nameToRemove + " was not found on your list."));
                    }
                } else {
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Usage: /fx remove [name]"));
                }
                break;

            case "list":
                if (EnemyHUD.targetList.isEmpty()) {
                    sendMessage(sender, EnumChatFormatting.RED + "Your list is empty.");
                } else {
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.AQUA + "Enemy List (" + EnemyHUD.targetList.size() + ")"));
                    sender.addChatMessage(new ChatComponentText(""));
                    for (String name : EnemyHUD.targetList) {
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + "- " + EnumChatFormatting.RED + name));
                    }
                }
                break;

            case "alerts":
                EnemyHUD.notificationsEnabled = !EnemyHUD.notificationsEnabled;
                ConfigHandler.saveConfig();
                sendMessage(sender, EnumChatFormatting.YELLOW + "Alerts: " + (EnemyHUD.notificationsEnabled ? EnumChatFormatting.GREEN + "ON" : EnumChatFormatting.RED + "OFF"));
                break;

            case "toggle":
                HUDController.setEnabled(!HUDController.enabled);
                ConfigHandler.saveConfig();
                sendMessage(sender, EnumChatFormatting.YELLOW + "HUDs: " + (HUDController.enabled ? EnumChatFormatting.GREEN + "ON" : EnumChatFormatting.RED + "OFF"));
                break;

            case "clear":
                EnemyHUD.targetList.clear();
                ConfigHandler.saveConfig();
                sendMessage(sender, EnumChatFormatting.RED + "List cleared.");
                break;

            case "autodenick":
                AutoDenick.enabled = !AutoDenick.enabled;
                ConfigHandler.saveConfig();
                sendMessage(sender, EnumChatFormatting.YELLOW + "AutoDenick: " + (AutoDenick.enabled ? EnumChatFormatting.GREEN + "ON" : EnumChatFormatting.RED + "OFF"));
                break;

            case "hud":
                new Thread(() -> {
                    try {
                        Thread.sleep(100);
                        Minecraft.getMinecraft().addScheduledTask(() -> {
                            Minecraft.getMinecraft().displayGuiScreen(new EditHUDGui());
                        });
                    } catch (Exception ignored) {}
                }).start();
                break;

            case "nickhud":
                NickedHUD.enabled = !NickedHUD.enabled;
                ConfigHandler.saveConfig();
                sendMessage(sender, EnumChatFormatting.YELLOW + "NickedHUD: " + (NickedHUD.enabled ? EnumChatFormatting.GREEN + "ON" : EnumChatFormatting.RED + "OFF"));
                break;

            case "enemyhud":
                EnemyHUD.enabled = !EnemyHUD.enabled;
                ConfigHandler.saveConfig();
                sendMessage(sender, EnumChatFormatting.YELLOW + "EnemyHUD: " + (EnemyHUD.enabled ? EnumChatFormatting.GREEN + "ON" : EnumChatFormatting.RED + "OFF"));
                break;

            default:
                sendMessage(sender, EnumChatFormatting.RED + "Unknown command.");
                break;
        }
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args,
                    "friend", "f", "add", "remove", "list", "alerts", "toggle", "clear", "denick", "debug", "esp", "autodenick", "hud", "nickhud", "enemyhud", "denickentry", "rank", "focus");
        }

        // --- NEW: FOCUS TAB COMPLETION ---
        if (args.length == 2 && args[0].equalsIgnoreCase("focus")) {
            List<String> options = Minecraft.getMinecraft().getNetHandler().getPlayerInfoMap().stream()
                    .map(info -> info.getGameProfile().getName())
                    .collect(Collectors.toList());
            options.add("remove");
            options.add("clear");
            return getListOfStringsMatchingLastWord(args, options);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("focus") && args[1].equalsIgnoreCase("remove")) {
            return getListOfStringsMatchingLastWord(args, FocusManager.focusList);
        }

        // --- RANK COMMAND AUTO-COMPLETE ---
        if (args.length == 4 && args[0].equalsIgnoreCase("rank")) {
            return getListOfStringsMatchingLastWord(args, "none", "vip", "vip+", "mvp", "mvp+", "mvp++", "youtube", "staff", "admin");
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("denickentry")) {
            return getListOfStringsMatchingLastWord(args, "clear");
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("denickentry") && args[1].equalsIgnoreCase("clear")) {
            return getListOfStringsMatchingLastWord(args, CacheManager.getCache().keySet());
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("friend") || args[0].equalsIgnoreCase("f"))) {
            return getListOfStringsMatchingLastWord(args, "add", "remove", "list");
        }

        if ((args.length == 2 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("denick"))) ||
                (args.length == 3 && (args[0].equalsIgnoreCase("friend") || args[0].equalsIgnoreCase("f")) && args[1].equalsIgnoreCase("add"))) {
            return getListOfStringsMatchingLastWord(args,
                    Minecraft.getMinecraft().getNetHandler().getPlayerInfoMap().stream()
                            .map(info -> info.getGameProfile().getName())
                            .collect(Collectors.toList()));
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            return getListOfStringsMatchingLastWord(args, EnemyHUD.targetList);
        }

        if (args.length == 3 && (args[0].equalsIgnoreCase("friend") || args[0].equalsIgnoreCase("f")) && args[1].equalsIgnoreCase("remove")) {
            return getListOfStringsMatchingLastWord(args, FriendsHUD.friendsList);
        }

        return null;
    }

    private void sendMessage(ICommandSender sender, String msg) {
        sender.addChatMessage(new ChatComponentText(msg));
    }
}