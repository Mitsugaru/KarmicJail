package com.mitsugaru.karmicjail.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.mitsugaru.karmicjail.KarmicJail;
import com.mitsugaru.karmicjail.jail.JailLogic;
import com.mitsugaru.karmicjail.permissions.PermCheck;
import com.mitsugaru.karmicjail.permissions.PermissionNode;
import com.mitsugaru.karmicjail.services.JailCommand;

public class StatusCommand implements JailCommand {

   @Override
   public boolean execute(KarmicJail plugin, CommandSender sender, Command command, String label, String[] args) {
      JailLogic logic = plugin.getModuleForClass(JailLogic.class);
      PermCheck perm = plugin.getModuleForClass(PermCheck.class);
      //TODO may need to check that they gave arguments. If not, give their own status.
      if(!perm.has(sender, PermissionNode.JAILSTATUS)) {
         sender.sendMessage(ChatColor.RED + "Lack Permission: " + PermissionNode.JAILSTATUS.getNode());
      } else {
         logic.jailStatus(sender, args);
      }
      return false;
   }

}
