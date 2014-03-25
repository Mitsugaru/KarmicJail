package com.mitsugaru.karmicjail.command;

import java.util.Vector;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.mitsugaru.karmicjail.KarmicJail;
import com.mitsugaru.karmicjail.jail.JailLogic;
import com.mitsugaru.karmicjail.modules.PermCheck;
import com.mitsugaru.karmicjail.services.JailCommand;
import com.mitsugaru.karmicjail.services.PermissionNode;

public class UnjailCommand implements JailCommand {

   @Override
   public boolean execute(KarmicJail plugin, CommandSender sender, Command command, String label, String[] args) {
      JailLogic logic = plugin.getModuleForClass(JailLogic.class);
      PermCheck perm = plugin.getModuleForClass(PermCheck.class);
      if(!perm.has(sender, PermissionNode.UNJAIL)) {
         sender.sendMessage(ChatColor.RED + "Lack Permission: " + PermissionNode.UNJAIL.getNode());
      } else {
         final Vector<String> players = new Vector<String>();
         for(int i = 0; i < args.length; i++) {
            // Attempt to grab name and add to list
            String name = plugin.expandName(args[i]);
            if(name != null) {
               players.add(name);
            } else {
               players.add(args[i]);
            }
         }
         if(players.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Missing paramters");
            sender.sendMessage(ChatColor.RED + "/kj unjail <player> [player2]");
         }
         for(String name : players) {
            logic.unjailPlayer(sender, name);
         }
      }
      return true;
   }

}
