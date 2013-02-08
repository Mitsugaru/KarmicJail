package com.mitsugaru.karmicjail.tasks;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.mitsugaru.karmicjail.KarmicJail;
import com.mitsugaru.karmicjail.config.RootConfig;
import com.mitsugaru.karmicjail.jail.JailLogic;
import com.mitsugaru.karmicjail.permissions.PermissionNode;

public class LoginJailTask implements Runnable {
   private KarmicJail plugin;
   private Player player;
   private static final long minutesToTicks = 1200;

   public LoginJailTask(KarmicJail plugin, Player player) {
      this.plugin = plugin;
      this.player = player;
   }

   @Override
   public void run() {
      RootConfig config = plugin.getModuleForClass(RootConfig.class);
      JailLogic logic = plugin.getModuleForClass(JailLogic.class);
      // Get name
      final String playerName = player.getName();
      // Add to cache
      logic.getPlayerCache().add(playerName);
      if(logic.playerIsTempJailed(playerName)) {
         final long time = logic.getPlayerTime(playerName);
         if(time > 0) {
            final int minutes = (int) ((time / minutesToTicks));
            player.sendMessage(ChatColor.RED + KarmicJail.TAG + ChatColor.AQUA + " You are jailed for " + plugin.prettifyMinutes(minutes) + ".");
            plugin.addThread(playerName, time);
            if(config.debugLog && config.debugEvents) {
               plugin.getLogger().info("Jailed '" + player.getName() + "' on login with time: " + plugin.prettifyMinutes(minutes));
            }
         }
      } else {
         player.sendMessage(ChatColor.RED + KarmicJail.TAG + ChatColor.AQUA + " You are jailed.");
         if(config.debugLog && config.debugEvents) {
            plugin.getLogger().info("Jailed '" + playerName + "' on login.");
         }
      }
      if(config.clearInventory) {
         player.getInventory().clear();
      }
      if(config.jailTeleport) {
         player.teleport(logic.getJailLocation());
      }
      if(config.broadcastJoin) {
         final StringBuilder sb = new StringBuilder();
         final String reason = logic.getJailReason(player.getName());
         sb.append(ChatColor.RED + KarmicJail.TAG + ChatColor.AQUA + " " + playerName + ChatColor.RED + " jailed");
         if(!reason.equals("")) {
            sb.append(" for " + ChatColor.GRAY + ChatColor.translateAlternateColorCodes('&', reason));
         }
         if(config.broadcastPerms) {
            plugin.getServer().broadcast(sb.toString(), PermissionNode.BROADCAST.getNode());
         } else {
            plugin.getServer().broadcastMessage(sb.toString());
         }
      }
   }
}
