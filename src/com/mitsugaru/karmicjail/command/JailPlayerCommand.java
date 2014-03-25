package com.mitsugaru.karmicjail.command;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.mitsugaru.karmicjail.KarmicJail;
import com.mitsugaru.karmicjail.jail.JailLogic;
import com.mitsugaru.karmicjail.modules.PermCheck;
import com.mitsugaru.karmicjail.services.JailCommand;
import com.mitsugaru.karmicjail.services.PermissionNode;

public class JailPlayerCommand implements JailCommand {

   @Override
   public boolean execute(KarmicJail plugin, CommandSender sender, Command command, String label, String[] args) {
      JailLogic logic = plugin.getModuleForClass(JailLogic.class);
      PermCheck perm = plugin.getModuleForClass(PermCheck.class);
      if(!perm.has(sender, PermissionNode.JAIL)) {
         sender.sendMessage(ChatColor.RED + "Lack Permission: " + PermissionNode.JAIL.getNode());
      } else {
         // All numeric player name must be the first name
         boolean timed = false;
         boolean done = false;
         int time = 0;
         StringBuilder sb = new StringBuilder();
         String reason = "";
         final Set<String> players = new HashSet<String>();
         try {
            String first = plugin.expandName(args[0]);
            if(first == null) {
               // expand failed
               first = args[0];
            }
            players.add(first);
            for(int i = 1; i < args.length; i++) {
               if(!done) {
                  try {
                     // Attempt to grab time
                     time = Integer.parseInt(args[i]);
                     // Attempt to grab player name if its all
                     // numbers
                     if(time > 0) {
                        timed = true;
                     }
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
               } else {
                  // attempt to grab reason if it exists
                  sb.append(args[i] + " ");
               }
            }
            if(sb.length() > 0) {
               // Remove all trailing whitespace
               reason = sb.toString().replaceAll("\\s+$", "");
            }
            for(String name : players) {
               logic.jailPlayer(sender, name, reason, time, timed);
            }
         } catch(ArrayIndexOutOfBoundsException e) {
            // no player name given, error
            sender.sendMessage(ChatColor.RED + "Missing paramters");
            sender.sendMessage(ChatColor.RED + "/kj jail <player> [player2] ... [time] [reason]");
         }
      }
      return true;
   }

}
