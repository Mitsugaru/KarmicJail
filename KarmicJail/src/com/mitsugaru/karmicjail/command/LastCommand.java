package com.mitsugaru.karmicjail.command;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mitsugaru.karmicjail.KarmicJail;
import com.mitsugaru.karmicjail.jail.JailLogic;
import com.mitsugaru.karmicjail.permissions.PermCheck;
import com.mitsugaru.karmicjail.permissions.PermissionNode;
import com.mitsugaru.karmicjail.services.JailCommand;

public class LastCommand implements JailCommand {

   @Override
   public boolean execute(KarmicJail plugin, CommandSender sender, Command command, String label, String[] args) {
      JailLogic logic = plugin.getModuleForClass(JailLogic.class);
      PermCheck perm = plugin.getModuleForClass(PermCheck.class);
      if(!perm.has(sender, PermissionNode.WARP_LAST)) {
         sender.sendMessage(ChatColor.RED + "Lack Permission: " + PermissionNode.WARP_LAST.getNode());
      } else {
         if(sender instanceof Player) {
            final Player player = (Player) sender;
            if(args.length > 0) {
               String name = plugin.expandName(args[0]);
               final Location last = logic.getPlayerLastLocation(name);
               if(last != null) {
                  player.teleport(last);
                  sender.sendMessage(ChatColor.GREEN + KarmicJail.TAG + " Warp to last location of " + ChatColor.AQUA + name);
               } else {
                  sender.sendMessage(ChatColor.RED + KarmicJail.TAG + " No last location for " + ChatColor.AQUA + name);
               }
            } else {
               sender.sendMessage(ChatColor.RED + "Missing name");
               sender.sendMessage(ChatColor.RED + "/kj last <player>");
            }
         } else {
            sender.sendMessage(ChatColor.RED + "Cannot use command as console.");
         }
      }
      return true;
   }

}
