package com.linexstudios.foxtrot.Denick;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

public class Denick extends CommandBase {
   private final String prefix = EnumChatFormatting.GRAY + "[" + EnumChatFormatting.RED + "PIT" + EnumChatFormatting.GRAY + "] ";

   public String getCommandName() {
      return "dn"; // main command
   }

   public List getCommandAliases() {
      List temp = new ArrayList();
      temp.add("denick"); // Alias command
      return temp;
   }

   public boolean canCommandSenderUseCommand(ICommandSender sender) {
      return true;
   }

   public int getRequiredPermissionLevel() {
      return 0;
   }

   public String getCommandUsage(ICommandSender sender) {
      return "Denicks a player";
   }

   public void processCommand(ICommandSender sender, String[] args) throws CommandException {
      if (args.length > 0) {
         // Starts the logic in a separate thread to prevent game lag
         DenickRunnable newDenick = new DenickRunnable(args[0]);
         Thread newThread = new Thread(newDenick);
         newThread.start();
      } else {
         Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(prefix + EnumChatFormatting.RED + "Usage: /dn [ign]"));
      }
   }
}