package com.mitsugaru.karmicjail.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mitsugaru.karmicjail.KarmicJail;
import com.mitsugaru.karmicjail.jail.JailLogic;
import com.mitsugaru.karmicjail.modules.PermCheck;
import com.mitsugaru.karmicjail.services.JailCommand;
import com.mitsugaru.karmicjail.services.PermissionNode;

public class WarpCommand implements JailCommand {

   @Override
   public boolean execute(KarmicJail plugin, CommandSender sender, Command command, String label, String[] args) {
      JailLogic logic = plugin.getModuleForClass(JailLogic.class);
      PermCheck perm = plugin.getModuleForClass(PermCheck.class);
      if(!perm.has(sender, PermissionNode.WARP_JAIL)) {
         sender.sendMessage(ChatColor.RED + "Lack Permission: " + PermissionNode.WARP_JAIL);
      } else {
         if(args.length > 0) {
            final Player target = plugin.getServer().getPlayer(args[0]);
            boolean warped = false;
            if(target != null) {
               if(target.isOnline()) {
                  target.teleport(logic.getJailLocation());
                  warped = true;
                  sender.sendMessage(ChatColor.GREEN + "Warped " + ChatColor.AQUA + target.getName() + ChatColor.GREEN + " to jail location.");
               }
            }
            if(!warped) {
               sender.sendMessage(ChatColor.RED + "Could not warp " + ChatColor.AQUA + args[0]);
            }
         } else if(sender instanceof Player) {
            final Player player = (Player) sender;
            player.teleport(logic.getJailLocation());
         } else {
            sender.sendMessage(ChatColor.RED + "Cannot use command as console without giving name.");
         }
      }
      return true;
   }

}
