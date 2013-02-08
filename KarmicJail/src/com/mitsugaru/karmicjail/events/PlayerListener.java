/**
 * Player listener. Built upon the SimpleJail project, created by imjake9.
 * https://github.com/imjake9/SimpleJail
 * 
 * @author imjake9
 * @author Mitsugaru
 */
package com.mitsugaru.karmicjail.events;

import com.mitsugaru.karmicjail.KarmicJail;
import com.mitsugaru.karmicjail.jail.JailLogic;
import com.mitsugaru.karmicjail.jail.JailStatus;
import com.mitsugaru.karmicjail.command.Commander;
import com.mitsugaru.karmicjail.command.history.HistoryCommander;
import com.mitsugaru.karmicjail.config.RootConfig;
import com.mitsugaru.karmicjail.permissions.PermCheck;
import com.mitsugaru.karmicjail.permissions.PermissionNode;
import com.mitsugaru.karmicjail.tasks.LoginJailTask;
import com.mitsugaru.karmicjail.tasks.LoginWarpTask;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PlayerListener implements Listener {
   // Class variables
   private final KarmicJail plugin;

   public PlayerListener(KarmicJail plugin) {
      this.plugin = plugin;
   }

   @EventHandler(priority = EventPriority.LOWEST)
   public void onPlayerChat(final AsyncPlayerChatEvent event) {
      RootConfig config = plugin.getModuleForClass(RootConfig.class);
      JailLogic logic = plugin.getModuleForClass(JailLogic.class);
      final String name = event.getPlayer().getName();
      if(JailLogic.PLAYER_CACHE.contains(name)) {
         if(logic.playerIsMuted(name)) {
            if(config.debugLog && config.debugEvents) {
               plugin.getLogger().info("Muted '" + name + "' with message: " + event.getMessage());
            }
            event.setCancelled(true);
         }
      }
   }

   @EventHandler(priority = EventPriority.HIGHEST)
   public void onPlayerRespawn(final PlayerRespawnEvent event) {
      RootConfig config = plugin.getModuleForClass(RootConfig.class);
      JailLogic logic = plugin.getModuleForClass(JailLogic.class);
      // Grab name
      final String name = event.getPlayer().getName();
      // Check if they're in the cache
      if(config.jailTeleportRespawn && JailLogic.PLAYER_CACHE.contains(name)) {
         event.setRespawnLocation(logic.getJailLocation());
         if(config.debugLog && config.debugEvents) {
            plugin.getLogger().info("Respawned '" + name + "' to jail.");
         }
      }
   }

   @EventHandler(priority = EventPriority.HIGHEST)
   public void onPlayerJoin(final PlayerJoinEvent event) {
      RootConfig config = plugin.getModuleForClass(RootConfig.class);
      JailLogic logic = plugin.getModuleForClass(JailLogic.class);
      PermCheck perm = plugin.getModuleForClass(PermCheck.class);
      // Attempt to add player to database
      logic.addPlayerToDatabase(event.getPlayer().getName());

      final Player player = event.getPlayer();
      final JailStatus status = logic.getPlayerStatus(player.getName());
      // Check status
      switch(status) {
      case PENDINGJAIL: {
         logic.setPlayerStatus(JailStatus.JAILED, player.getName());
         int id = plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new LoginJailTask(plugin, player), 30);
         if(id == -1) {
            plugin.getLogger().severe("Could not jail player '" + player.getName() + "' on login!");
         }
         break;
      }
      case JAILED: {
         int id = plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new LoginJailTask(plugin, player), 30);
         if(id == -1) {
            plugin.getLogger().severe("Could not jail player '" + player.getName() + "' on login!");
         }
         break;
      }
      case PENDINGFREE: {
         logic.freePlayer(plugin.getServer().getConsoleSender(), player.getName());
         // Warp them to unjail location
         int id = plugin.getServer().getScheduler()
               .scheduleSyncDelayedTask(plugin, new LoginWarpTask(player, logic.getUnjailLocation()), 30);
         if(id == -1) {
            plugin.getLogger().severe("Could not warp player '" + player.getName() + "' out jail on login!");
         } else {
            player.sendMessage(ChatColor.GREEN + KarmicJail.TAG + ChatColor.AQUA + "You have been removed from jail.");
         }
         if(config.debugLog && config.debugEvents) {
            plugin.getLogger().info("Unjailed '" + player.getName() + "' on login.");
         }
         break;
      }
      default: {
         if(config.warpAllOnJoin) {
            if(!perm.has(player, PermissionNode.WARP_JOINIGNORE)) {
               // Warp them to jail location
               int id = plugin.getServer().getScheduler()
                     .scheduleSyncDelayedTask(plugin, new LoginWarpTask(player, logic.getJailLocation()), 30);
               if(id == -1) {
                  plugin.getLogger().severe("Could not warp player '" + player.getName() + "' to jail on login!");
               }
            }
         }
         break;
      }
      }
   }

   @EventHandler(priority = EventPriority.MONITOR)
   public void onPlayerQuit(final PlayerQuitEvent event) {
      RootConfig config = plugin.getModuleForClass(RootConfig.class);
      JailLogic logic = plugin.getModuleForClass(JailLogic.class);
      Commander commander = plugin.getCommandHandlerForClass(Commander.class);
      if(config.debugLog && config.debugEvents) {
         plugin.getLogger().info("Quit Event for: " + event.getPlayer().getName());
      }
      // Record location
      if(event.getPlayer() != null) {
         final String name = event.getPlayer().getName();
         if(name != null && event.getPlayer().getLocation() != null) {
            if(!logic.playerIsJailed(name)) {
               logic.setPlayerLastLocation(name, event.getPlayer().getLocation());
            }
         }
         if(event.getPlayer().getInventory() != null) {
            logic.setPlayerInventory(name, event.getPlayer().getInventory(), false);
         }
         // Remove from cache
         try {
            JailLogic.PLAYER_CACHE.remove(name);
         } catch(Exception e) {
            // IGNORE
         }
      }
      // Remove viewer
      commander.getInventoryHolders().remove(event.getPlayer().getName());
      // Remove history viewer
      plugin.getCommandHandlerForClass(HistoryCommander.class).getCache().remove(event.getPlayer().getName());
      plugin.stopTask(event.getPlayer().getName());
   }

}
