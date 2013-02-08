package com.mitsugaru.karmicjail.events;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;

import com.mitsugaru.karmicjail.KarmicJail;
import com.mitsugaru.karmicjail.config.RootConfig;
import com.mitsugaru.karmicjail.jail.JailLogic;

public class JailedPlayerListener implements Listener {
   private KarmicJail plugin;

   public JailedPlayerListener(KarmicJail plugin) {
      this.plugin = plugin;
   }

   @EventHandler(priority = EventPriority.LOWEST)
   public void chatValid(final AsyncPlayerChatEvent event) {
      RootConfig config = plugin.getModuleForClass(RootConfig.class);
      JailLogic logic = plugin.getModuleForClass(JailLogic.class);
      if(!event.isCancelled() && event.getPlayer() != null) {
         if(config.denyChat && logic.getPlayerCache().contains(event.getPlayer().getName())) {
            event.setCancelled(true);
            sendDenyJailMessage(event.getPlayer());
         }
      }
   }

   @EventHandler(priority = EventPriority.LOWEST)
   public void commandValid(final PlayerCommandPreprocessEvent event) {
      RootConfig config = plugin.getModuleForClass(RootConfig.class);
      JailLogic logic = plugin.getModuleForClass(JailLogic.class);
      if(!event.isCancelled() && event.getPlayer() != null) {
         if(config.denyCommands && logic.getPlayerCache().contains(event.getPlayer().getName())) {
            event.setCancelled(true);
            sendDenyJailMessage(event.getPlayer());
         }
      }
   }

   @EventHandler(priority = EventPriority.LOWEST)
   public void interactValid(final PlayerInteractEvent event) {
      RootConfig config = plugin.getModuleForClass(RootConfig.class);
      JailLogic logic = plugin.getModuleForClass(JailLogic.class);
      if(!event.isCancelled() && event.getPlayer() != null) {
         if(config.denyInteract && logic.getPlayerCache().contains(event.getPlayer().getName())) {
            event.setCancelled(true);
            sendDenyJailMessage(event.getPlayer());
         }
      }
   }

   @EventHandler(priority = EventPriority.LOWEST)
   public void interactEntityValid(final PlayerInteractEntityEvent event) {
      RootConfig config = plugin.getModuleForClass(RootConfig.class);
      JailLogic logic = plugin.getModuleForClass(JailLogic.class);
      if(!event.isCancelled() && event.getPlayer() != null) {
         if(config.denyInteract && logic.getPlayerCache().contains(event.getPlayer().getName())) {
            event.setCancelled(true);
            sendDenyJailMessage(event.getPlayer());
         }
      }
   }

   @EventHandler(priority = EventPriority.LOWEST)
   public void moveValid(final PlayerMoveEvent event) {
      RootConfig config = plugin.getModuleForClass(RootConfig.class);
      JailLogic logic = plugin.getModuleForClass(JailLogic.class);
      if(!event.isCancelled() && event.getPlayer() != null) {
         if(config.denyMove && logic.getPlayerCache().contains(event.getPlayer().getName())) {
            event.setCancelled(true);
            sendDenyJailMessage(event.getPlayer());
         }
      }
   }

   @EventHandler(priority = EventPriority.LOWEST)
   public void blockPlaceValid(final BlockPlaceEvent event) {
      RootConfig config = plugin.getModuleForClass(RootConfig.class);
      JailLogic logic = plugin.getModuleForClass(JailLogic.class);
      if(!event.isCancelled() && event.getPlayer() != null) {
         if(config.denyBlockPlace && logic.getPlayerCache().contains(event.getPlayer().getName())) {
            event.setCancelled(true);
            sendDenyJailMessage(event.getPlayer());
         }
      }
   }

   @EventHandler(priority = EventPriority.LOWEST)
   public void blockDestroyValid(final BlockBreakEvent event) {
      RootConfig config = plugin.getModuleForClass(RootConfig.class);
      JailLogic logic = plugin.getModuleForClass(JailLogic.class);
      if(!event.isCancelled() && event.getPlayer() != null) {
         if(config.denyBlockBreak && logic.getPlayerCache().contains(event.getPlayer().getName())) {
            event.setCancelled(true);
            sendDenyJailMessage(event.getPlayer());
         }
      }
   }

   @EventHandler(priority = EventPriority.LOWEST)
   public void craftItemValid(final CraftItemEvent event) {
      RootConfig config = plugin.getModuleForClass(RootConfig.class);
      JailLogic logic = plugin.getModuleForClass(JailLogic.class);
      if(!event.isCancelled() && event.getWhoClicked() != null) {
         if(config.denyItemCraft && event.getWhoClicked() instanceof Player && logic.getPlayerCache().contains(event.getWhoClicked().getName())) {
            event.setCancelled(true);
            sendDenyJailMessage((Player) event.getWhoClicked());
         }
      }
   }

   @EventHandler(priority = EventPriority.LOWEST)
   public void enchantItemValid(final EnchantItemEvent event) {
      RootConfig config = plugin.getModuleForClass(RootConfig.class);
      JailLogic logic = plugin.getModuleForClass(JailLogic.class);
      if(!event.isCancelled() && event.getEnchanter() != null) {
         if(config.denyItemEnchant && event.getEnchanter() instanceof Player && logic.getPlayerCache().contains(event.getEnchanter().getName())) {
            event.setCancelled(true);
            sendDenyJailMessage((Player) event.getEnchanter());
         }
      }
   }

   @EventHandler(priority = EventPriority.LOWEST)
   public void itemPickupValid(final PlayerPickupItemEvent event) {
      RootConfig config = plugin.getModuleForClass(RootConfig.class);
      JailLogic logic = plugin.getModuleForClass(JailLogic.class);
      if(!event.isCancelled() && event.getPlayer() != null) {
         if(config.denyItemPickup && logic.getPlayerCache().contains(event.getPlayer().getName())) {
            event.setCancelled(true);
            sendDenyJailMessage(event.getPlayer());
         }
      }
   }

   @EventHandler(priority = EventPriority.LOWEST)
   public void itemDropValid(final PlayerDropItemEvent event) {
      RootConfig config = plugin.getModuleForClass(RootConfig.class);
      JailLogic logic = plugin.getModuleForClass(JailLogic.class);
      if(!event.isCancelled() && event.getPlayer() != null) {
         if(config.denyItemDrop && logic.getPlayerCache().contains(event.getPlayer().getName())) {
            event.setCancelled(true);
            sendDenyJailMessage(event.getPlayer());
         }
      }
   }

   @EventHandler(priority = EventPriority.MONITOR)
   public void itemDrop(final PlayerDropItemEvent event) {
      JailLogic logic = plugin.getModuleForClass(JailLogic.class);
      if(!event.isCancelled() && event.getPlayer() != null) {
         if(logic.getPlayerCache().contains(event.getPlayer().getName())) {
            // They are jailed and dropped an item
            // TODO update thier inventory in the database
         }
      }
   }

   @EventHandler(priority = EventPriority.LOWEST)
   public void inventoryClickValid(final InventoryClickEvent event) {
      RootConfig config = plugin.getModuleForClass(RootConfig.class);
      JailLogic logic = plugin.getModuleForClass(JailLogic.class);
      if(!event.isCancelled() && event.getWhoClicked() != null) {
         if(config.denyInventory && event.getWhoClicked() instanceof Player && logic.getPlayerCache().contains(event.getWhoClicked().getName())) {
            event.setCancelled(true);
            sendDenyJailMessage((Player) event.getWhoClicked());
         }
      }
   }

   private void sendDenyJailMessage(Player player) {
      player.sendMessage(ChatColor.RED + KarmicJail.TAG + " Cannot do that while jailed.");
   }
}
