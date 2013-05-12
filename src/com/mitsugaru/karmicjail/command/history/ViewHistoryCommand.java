package com.mitsugaru.karmicjail.command.history;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.mitsugaru.karmicjail.KarmicJail;
import com.mitsugaru.karmicjail.jail.JailLogic;
import com.mitsugaru.karmicjail.permissions.PermCheck;
import com.mitsugaru.karmicjail.permissions.PermissionNode;
import com.mitsugaru.karmicjail.services.JailCommand;

public class ViewHistoryCommand implements JailCommand {

   @Override
   public boolean execute(KarmicJail plugin, CommandSender sender, Command command, String label, String[] args) {
      PermCheck perm = plugin.getModuleForClass(PermCheck.class);
      if(!perm.has(sender, PermissionNode.HISTORY_VIEW)) {
         sender.sendMessage(ChatColor.RED + "Lack Permission: " + PermissionNode.HISTORY_VIEW.getNode());
      } else {
         HistoryCommander commander = plugin.getCommandHandlerForClass(HistoryCommander.class);
         try {
            String temp = plugin.expandName(args[0]);
            String name = plugin.getModuleForClass(JailLogic.class).getPlayerInDatabase(temp);
            if(name == null) {
               name = temp;
            }
            commander.getCache().put(sender.getName(), name);
            commander.listHistory(sender, 0);
         } catch(IndexOutOfBoundsException e) {
            if(commander.getCache().containsKey(sender.getName())) {
               commander.listHistory(sender, 0);
            } else {
               sender.sendMessage(ChatColor.RED + "No previous record open, try /jhistory help");
            }
         }
      }
      return true;
   }

}
