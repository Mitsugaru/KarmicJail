package com.mitsugaru.karmicjail.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.mitsugaru.karmicjail.KarmicJail;
import com.mitsugaru.karmicjail.modules.PermCheck;
import com.mitsugaru.karmicjail.services.JailCommand;
import com.mitsugaru.karmicjail.services.PermissionNode;

public class NextCommand implements JailCommand {

   @Override
   public boolean execute(KarmicJail plugin, CommandSender sender, Command command, String label, String[] args) {
      Commander commander = plugin.getCommandHandlerForClass(Commander.class);
      PermCheck perm = plugin.getModuleForClass(PermCheck.class);
      if(!perm.has(sender, PermissionNode.LIST)) {
         sender.sendMessage(ChatColor.RED + "Lack Permission: " + PermissionNode.LIST.getNode());
      } else {
         // List with next page
         commander.listJailed(sender, 1);
      }
      return true;
   }

}
