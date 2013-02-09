package com.mitsugaru.karmicjail.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.mitsugaru.karmicjail.KarmicJail;
import com.mitsugaru.karmicjail.jail.JailLogic;
import com.mitsugaru.karmicjail.permissions.PermCheck;
import com.mitsugaru.karmicjail.permissions.PermissionNode;
import com.mitsugaru.karmicjail.services.JailCommand;

public class ReasonCommand implements JailCommand {

   @Override
   public boolean execute(KarmicJail plugin, CommandSender sender, Command command, String label, String[] args) {
      JailLogic logic = plugin.getModuleForClass(JailLogic.class);
      PermCheck perm = plugin.getModuleForClass(PermCheck.class);
      if(!perm.has(sender, PermissionNode.JAIL)) {
         sender.sendMessage(ChatColor.RED + "Lack Permission: " + PermissionNode.JAIL.getNode());
      } else {
         if(args.length > 0) {
            String name = plugin.expandName(args[0]);
            final StringBuilder sb = new StringBuilder();
            for(int i = 1; i < args.length; i++) {
               sb.append(args[i] + " ");
            }
            String reason = "";
            if(sb.length() > 0) {
               // Remove all trailing whitespace
               reason = sb.toString().replaceAll("\\s+$", "");
            }
            if(logic.playerIsJailed(name) || logic.playerIsPendingJail(name)) {
               logic.setPlayerReason(name, reason);
               sender.sendMessage(ChatColor.GREEN + KarmicJail.TAG + " Set reason for " + ChatColor.AQUA + name + ChatColor.GREEN + " to: "
                     + ChatColor.GRAY + reason);
            } else {
               sender.sendMessage(ChatColor.RED + KarmicJail.TAG + " Player '" + ChatColor.AQUA + name + ChatColor.RED + "' not jailed.");
            }
         } else {
            sender.sendMessage(ChatColor.RED + "Missing name");
            sender.sendMessage(ChatColor.RED + "/kj reason <player> <reason>");
         }
      }
      return true;
   }

}
