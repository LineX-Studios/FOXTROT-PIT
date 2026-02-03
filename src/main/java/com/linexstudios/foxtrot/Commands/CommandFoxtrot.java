package com.linexstudios.foxtrot.Commands;

import com.linexstudios.foxtrot.Denick.DenickRunnable;
import com.linexstudios.foxtrot.Denick.AutoDenick;
import com.linexstudios.foxtrot.Enemy.EnemyESP;
import com.linexstudios.foxtrot.Hud.EnemyHUD;
import com.linexstudios.foxtrot.Hud.NickedHUD;
import com.linexstudios.foxtrot.Hud.HUDController;
import com.linexstudios.foxtrot.Handler.ConfigHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

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
        return "/foxtrot <add|remove|list|alerts|toggle|clear|denick|debug|esp|autodenick|hud|nickhud|enemyhud>";
    }

    @Override
    public int getRequiredPermissionLevel() { return 0; }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + "[" + EnumChatFormatting.RED + "FOXTROT" + EnumChatFormatting.GRAY + "] " + EnumChatFormatting.RED + EnumChatFormatting.BOLD + "HELP MENU"));
            sender.addChatMessage(new ChatComponentText(""));

            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/foxtrot add [name] " + EnumChatFormatting.GRAY + "- Add enemy"));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/foxtrot remove [name] " + EnumChatFormatting.GRAY + "- Remove enemy"));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/foxtrot list " + EnumChatFormatting.GRAY + "- View enemy list"));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/foxtrot alerts " + EnumChatFormatting.GRAY + "- Toggle join alerts"));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/foxtrot toggle " + EnumChatFormatting.GRAY + "- Toggle all HUDs"));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/foxtrot esp " + EnumChatFormatting.GRAY + "- Toggle ESP"));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/foxtrot denick [name] " + EnumChatFormatting.GRAY + "- Scrape Player"));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/foxtrot autodenick " + EnumChatFormatting.GRAY + "- Toggle automatic denicking"));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/foxtrot hud " + EnumChatFormatting.GRAY + "- Toggle HUD drag mode"));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/foxtrot nickhud " + EnumChatFormatting.GRAY + "- Toggle NickedHUD"));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/foxtrot enemyhud " + EnumChatFormatting.GRAY + "- Toggle EnemyHUD"));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/foxtrot clear " + EnumChatFormatting.GRAY + "- Clear enemy list"));
            return;
        }

        String action = args[0].toLowerCase();
        ConfigHandler.logDebug("User executed: /fx " + String.join(" ", args));

        switch (action) {
            case "debug":
                EnemyHUD.debugMode = !EnemyHUD.debugMode;
                ConfigHandler.saveConfig();
                sendMessage(sender, EnumChatFormatting.YELLOW + "Debug: " + (EnemyHUD.debugMode ? "ON" : "OFF"));
                break;

            case "denick":
                if (args.length > 1) {
                    new Thread(new DenickRunnable(args[1])).start();
                } else {
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Usage: /fx denick [name]"));
                }
                break;

            case "esp":
                EnemyESP.enabled = !EnemyESP.enabled;
                ConfigHandler.saveConfig();
                sendMessage(sender, EnumChatFormatting.YELLOW + "Enemy ESP: " + (EnemyESP.enabled ? EnumChatFormatting.GREEN + "ON" : EnumChatFormatting.RED + "OFF"));
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
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + "- " + EnemyHUD.getFormattedName(name)));
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
                HUDController.toggleDragMode();
                sendMessage(sender, EnumChatFormatting.YELLOW + "HUD Drag Mode: " + (HUDController.dragMode ? EnumChatFormatting.GREEN + "ON" : EnumChatFormatting.RED + "OFF"));
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
                    "add", "remove", "list", "alerts", "toggle", "clear", "denick", "debug", "esp", "autodenick", "hud", "nickhud", "enemyhud");
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("add") || args