package com.mitsugaru.karmicjail.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mitsugaru.karmicjail.KarmicJail;
import com.mitsugaru.karmicjail.inventory.JailInventoryHolder;
import com.mitsugaru.karmicjail.jail.JailLogic;
import com.mitsugaru.karmicjail.permissions.PermCheck;
import com.mitsugaru.karmicjail.permissions.PermissionNode;
import com.mitsugaru.karmicjail.services.JailCommand;

public class InventoryCommand implements JailCommand {

   @Override
   public boolean execute(KarmicJail plugin, CommandSender sender, Command command, String label, String[] args) {
      Commander commander = plugin.getCommandHandlerForClass(Commander.class);
      JailLogic logic = plugin.getModuleForClass(JailLogic.class);
      PermCheck perm = plugin.getModuleForClass(PermCheck.class);
      if(!perm.has(sender, PermissionNode.INVENTORY_VIEW)) {
         sender.sendMessage(ChatColor.RED + "Lack Permission: " + PermissionNode.INVENTORY_VIEW);
      } else {
         if(sender instanceof Player) {
            final Player player = (Player) sender;
            if(args.length > 0) {
               String temp = plugin.expandName(args[0]);
               String name = logic.getPlayerInDatabase(temp);
               if(name == null) {
                  name = temp;
               }
               if(logic.playerIsJailed(name) || logic.playerIsPendingJail(name)) {
                  JailInventoryHolder holder = null;
                  for(JailInventoryHolder h : commander.getInventoryHolders().values()) {
                     if(h.getTarget().equals(name)) {
                        holder = h;
                        break;
                     }
                  }
                  if(holder == null) {
                     holder = new JailInventoryHolder(plugin, name);
                     holder.setInventory(plugin.getServer().createInventory(holder, 45, name));
                  }
                  player.openInventory(holder.getInventory());
                  commander.getInventoryHolders().put(player.getName(), holder);
                  sender.sendMessage(ChatColor.GREEN + KarmicJail.TAG + " Open inventory of " + ChatColor.AQUA + name);
               } else {
                  sender.sendMessage(ChatColor.RED + KarmicJail.TAG + " Player '" + ChatColor.AQUA + name + ChatColor.RED + "' not jailed.");
               }
            } else {
               sender.sendMessage(ChatColor.RED + "Missing name");
               sender.sendMessage(ChatColor.RED + "/jinv <player>");
            }
         } else {
            sender.sendMessage(ChatColor.RED + "Cannot use command as console.");
         }
      }
      return true;
   }

}
