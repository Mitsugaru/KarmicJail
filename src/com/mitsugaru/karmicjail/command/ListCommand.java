package com.mitsugaru.karmicjail.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.mitsugaru.karmicjail.KarmicJail;
import com.mitsugaru.karmicjail.permissions.PermCheck;
import com.mitsugaru.karmicjail.permissions.PermissionNode;
import com.mitsugaru.karmicjail.services.JailCommand;

public class ListCommand implements JailCommand {

   @Override
   public boolean execute(KarmicJail plugin, CommandSender sender, Command command, String label, String[] args) {
      Commander commander = plugin.getCommandHandlerForClass(Commander.class);
      PermCheck perm = plugin.getModuleForClass(PermCheck.class);
      if(!perm.has(sender, PermissionNode.LIST)) {
         sender.sendMessage(ChatColor.RED + "Lack Permission: " + PermissionNode.LIST.getNode());
      } else {
         // list jailed people
         if(args.length > 0) {
            // If they provided a page number
            try {
               // Attempt to parse argument for page number
               int pageNum = Integer.parseInt(args[0]);
               // Set current page to given number
               commander.getPage().put(sender.getName(), pageNum - 1);
               // Show page if possible
               commander.listJailed(sender, 0);
            } catch(NumberFormatException e) {
               sender.sendMessage(ChatColor.YELLOW + KarmicJail.TAG + " Invalid integer for page number");
            }
         } else {
            // List with current page
            commander.listJailed(sender, 0);
         }
      }
      return true;
   }

}
