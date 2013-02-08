package com.mitsugaru.karmicjail.command.history;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.mitsugaru.karmicjail.KarmicJail;
import com.mitsugaru.karmicjail.permissions.PermCheck;
import com.mitsugaru.karmicjail.permissions.PermissionNode;
import com.mitsugaru.karmicjail.services.JailCommand;

public class PrevHistoryCommand implements JailCommand {

   @Override
   public boolean execute(KarmicJail plugin, CommandSender sender, Command command, String label, String[] args) {
      PermCheck perm = plugin.getModuleForClass(PermCheck.class);
      if(!perm.has(sender, PermissionNode.HISTORY_VIEW)) {
         sender.sendMessage(ChatColor.RED + "Lack Permission: " + PermissionNode.HISTORY_VIEW.getNode());
      } else {
         HistoryCommander commander = plugin.getCommandHandlerForClass(HistoryCommander.class);
         if(commander.getCache().containsKey(sender.getName())) {
            commander.listHistory(sender, -1);
         } else {
            sender.sendMessage(ChatColor.RED + "No previous record open");
            sender.sendMessage(ChatColor.RED + "/jhistory <player>");
         }
      }
      return true;
   }

}
