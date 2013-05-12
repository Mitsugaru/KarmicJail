package com.mitsugaru.karmicjail.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.mitsugaru.karmicjail.KarmicJail;
import com.mitsugaru.karmicjail.config.RootConfig;
import com.mitsugaru.karmicjail.permissions.PermCheck;
import com.mitsugaru.karmicjail.permissions.PermissionNode;
import com.mitsugaru.karmicjail.services.JailCommand;

public class ReloadCommand implements JailCommand {

   @Override
   public boolean execute(KarmicJail plugin, CommandSender sender, Command command, String label, String[] args) {
      RootConfig config = plugin.getModuleForClass(RootConfig.class);
      PermCheck perm = plugin.getModuleForClass(PermCheck.class);
      if(perm.has(sender, PermissionNode.JAIL) || perm.has(sender, PermissionNode.UNJAIL) || perm.has(sender, PermissionNode.SETJAIL)) {
         config.reload();
         sender.sendMessage(ChatColor.GREEN + KarmicJail.TAG + " Config reloaded.");
      } else {
         sender.sendMessage(ChatColor.RED + "Lack permission to reload");
      }
      return true;
   }

}
