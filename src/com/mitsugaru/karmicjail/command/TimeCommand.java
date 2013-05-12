package com.mitsugaru.karmicjail.command;

import java.util.Vector;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.mitsugaru.karmicjail.KarmicJail;
import com.mitsugaru.karmicjail.config.RootConfig;
import com.mitsugaru.karmicjail.jail.JailLogic;
import com.mitsugaru.karmicjail.permissions.PermCheck;
import com.mitsugaru.karmicjail.permissions.PermissionNode;
import com.mitsugaru.karmicjail.services.JailCommand;

public class TimeCommand implements JailCommand {

   @Override
   public boolean execute(KarmicJail plugin, CommandSender sender, Command command, String label, String[] args) {
      RootConfig config = plugin.getModuleForClass(RootConfig.class);
      JailLogic logic = plugin.getModuleForClass(JailLogic.class);
      PermCheck perm = plugin.getModuleForClass(PermCheck.class);
      boolean hasPerm = true;
      if(!perm.has(sender, PermissionNode.JAIL)) {
         sender.sendMessage(ChatColor.RED + "Lack Permission: " + PermissionNode.JAIL.getNode());
         hasPerm = false;
      }
      if(config.timePerm) {
         if(!perm.has(sender, PermissionNode.TIMED)) {
            sender.sendMessage(ChatColor.RED + "Lack Permission: " + PermissionNode.TIMED.getNode());
            hasPerm = false;
         }
      }
      if(hasPerm) {
         boolean done = false;
         int time = 0;
         final Vector<String> players = new Vector<String>();
         for(int i = 0; i < args.length; i++) {
            if(!done) {
               try {
                  // Attempt to grab time
                  time = Integer.parseInt(args[i]);
                  done = true;
               } catch(NumberFormatException e) {
                  // Attempt to grab name and add to list
                  String name = plugin.expandName(args[i]);
                  if(name != null) {
                     players.add(name);
                  } else {
                     players.add(args[i]);
                  }
               }
            }
         }
         if(players.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Missing paramters");
            sender.sendMessage(ChatColor.RED + "/kj time <player> [player2] ... <time>");
         }
         for(String name : players) {
            logic.setJailTime(sender, name, time);
         }
      }
      return true;
   }

}
