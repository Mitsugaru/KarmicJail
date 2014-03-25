package com.mitsugaru.karmicjail.command.history;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.mitsugaru.karmicjail.KarmicJail;
import com.mitsugaru.karmicjail.database.DBHandler;
import com.mitsugaru.karmicjail.jail.JailLogic;
import com.mitsugaru.karmicjail.modules.PermCheck;
import com.mitsugaru.karmicjail.services.JailCommand;
import com.mitsugaru.karmicjail.services.PermissionNode;

public class AddHistoryCommand implements JailCommand {

   @Override
   public boolean execute(KarmicJail plugin, CommandSender sender, Command command, String label, String[] args) {
      PermCheck perm = plugin.getModuleForClass(PermCheck.class);
      JailLogic logic = plugin.getModuleForClass(JailLogic.class);
      DBHandler database = plugin.getModuleForClass(DBHandler.class);
      if(!perm.has(sender, PermissionNode.HISTORY_ADD)) {
         sender.sendMessage(ChatColor.RED + "Lack Permission: " + PermissionNode.HISTORY_ADD.getNode());
      } else {
         String temp = plugin.expandName(args[0]);
         String name = logic.getPlayerInDatabase(temp);
         if(name == null) {
            name = temp;
         }
         final StringBuilder sb = new StringBuilder();
         for(int i = 1; i < args.length; i++) {
            sb.append(args[i] + " ");
         }
         String reason = "";
         if(sb.length() > 0) {
            // Remove all trailing whitespace
            reason = sb.toString().replaceAll("\\s+$", "");
            reason = ChatColor.GOLD + sender.getName() + ChatColor.BLUE + " - " + ChatColor.GRAY + reason;
         }
         if(!reason.equals("")) {
            database.addToHistory(name, reason);
            sender.sendMessage(ChatColor.GREEN + "Added comment '" + reason + ChatColor.GREEN + "' to " + ChatColor.AQUA + name);
         } else {
            sender.sendMessage(ChatColor.RED + KarmicJail.TAG + " Comment cannot be empty.");
            sender.sendMessage(ChatColor.RED + KarmicJail.TAG + " /jhistory add <player> <comment...>");
         }
      }
      return true;
   }

}
