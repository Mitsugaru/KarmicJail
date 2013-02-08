package com.mitsugaru.karmicjail.jail;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.mitsugaru.karmicjail.KarmicJail;
import com.mitsugaru.karmicjail.database.DBHandler;
import com.mitsugaru.karmicjail.database.Field;
import com.mitsugaru.karmicjail.database.Table;
import com.mitsugaru.karmicjail.command.Commander;
import com.mitsugaru.karmicjail.config.RootConfig;
import com.mitsugaru.karmicjail.events.KarmicJailEvent;
import com.mitsugaru.karmicjail.inventory.JailInventoryHolder;
import com.mitsugaru.karmicjail.permissions.PermCheck;
import com.mitsugaru.karmicjail.permissions.PermissionNode;
import com.mitsugaru.karmicjail.services.JailModule;
import com.mitsugaru.karmicjail.tasks.JailTask;
import com.platymuus.bukkit.permissions.Group;
import com.platymuus.bukkit.permissions.PermissionsPlugin;

/**
 * Logic handler for KarmicJail.
 */
public class JailLogic extends JailModule {
   private final static DateFormat DATE_FORMAT = new SimpleDateFormat("MM-dd-yyyy 'at' HH:mm z");
   public final static Set<String> PLAYER_CACHE = new HashSet<String>();

   public JailLogic(KarmicJail plugin) {
      super(plugin);
   }

   @Override
   public void starting() {
   }

   @Override
   public void closing() {
   }

   /**
    * Jails a player
    * 
    * @param sender
    *           of command
    * @param name
    *           of player to be jailed
    * @param reason
    *           for being jailed
    * @param minutes
    *           for how long they're in jail
    * @param boolean to determine of player has a timed release
    */
   public void jailPlayer(CommandSender sender, String inName, String reason, int minutes, boolean timedCom) {
      RootConfig config = plugin.getModuleForClass(RootConfig.class);
      DBHandler database = plugin.getModuleForClass(DBHandler.class);
      PermCheck perm = plugin.getModuleForClass(PermCheck.class);
      // Check if player is already jailed:
      if(playerIsJailed(inName) || playerIsPendingJail(inName)) {
         sender.sendMessage(ChatColor.RED + "That player is already in jail!");
      } else {

         // Check if player is in database
         String name = getPlayerInDatabase(inName);
         if(name == null) {
            sender.sendMessage(ChatColor.YELLOW + " Player '" + ChatColor.GREEN + inName + ChatColor.YELLOW
                  + "' has never been on server! Adding to database...");
            // Player has never been on server, adding to list
            addPlayerToDatabase(inName);
            name = inName;
         }
         // Check to see if the name is exempt
         if(perm.has(name, PermissionNode.EXEMPT)) {
            // Player is exempt
            sender.sendMessage(ChatColor.RED + KarmicJail.TAG + " '" + ChatColor.GOLD + name + ChatColor.RED + "' is exempt from being jailed!");
            return;
         }
         if(config.removeGroups) {
            // Save groups
            savePlayerGroups(name);
            // Remove all groups
            removePlayerGroups(name);
         }
         // Add to jail group
         if(config.useJailGroup) {
            perm.playerAddGroup(config.jailLoc.getWorld().getName(), name, config.jailGroup);
         }
         // Grab duration
         long duration = 0;
         boolean timed = timedCom;
         if(config.timePerm) {
            if(!perm.has(sender, PermissionNode.TIMED)) {
               timed = false;
               sender.sendMessage(ChatColor.RED + "Cannot put time on jailed player. Lack Permission: KarmicJail.timed");
            }
         }
         if(timed) {
            duration = minutes * KarmicJail.minutesToTicks;
         }
         updatePlayerTime(name, duration);

         // Grab player from server if they are online
         final Player player = plugin.getServer().getPlayer(name);
         if(player != null) {
            if(player.isOnline()) {
               // Add to cache
               PLAYER_CACHE.add(name);
               // Set previous location
               setPlayerLastLocation(name, player.getLocation());
               // Move to jail
               if(config.jailTeleport) {
                  player.teleport(config.jailLoc);
               }
               // Set inventory
               setPlayerInventory(name, player.getInventory(), config.clearInventory);
               // Set status to jailed
               setPlayerStatus(JailStatus.JAILED, name);
               // Notify player
               if(reason.equals("")) {
                  player.sendMessage(ChatColor.RED + "Jailed by " + ChatColor.AQUA + sender.getName() + ChatColor.RED);
               } else {
                  player.sendMessage(ChatColor.RED + "Jailed by " + ChatColor.AQUA + sender.getName() + ChatColor.RED + " for: " + ChatColor.GRAY
                        + ChatColor.translateAlternateColorCodes('&', reason));
               }
               if(timed) {
                  player.sendMessage(ChatColor.AQUA + "Time in jail: " + ChatColor.GOLD + plugin.prettifyMinutes(minutes));
                  // Create thread to release player
                  KarmicJail.getJailThreads().put(name, new JailTask(plugin, name, duration));
               }
            } else {
               // Set player status to pending
               setPlayerStatus(JailStatus.PENDINGJAIL, name);
            }
         } else {
            // Set player status to pending
            setPlayerStatus(JailStatus.PENDINGJAIL, name);
         }
         try {
            final String date = DATE_FORMAT.format(new Date());
            final PreparedStatement statement = database.prepare("UPDATE " + Table.JAILED.getName() + " SET " + Field.JAILER.getColumnName() + "=?,"
                  + Field.DATE.getColumnName() + "=?," + Field.REASON.getColumnName() + "=?, " + Field.MUTE.getColumnName() + "=? WHERE "
                  + Field.PLAYERNAME.getColumnName() + "=?;");
            statement.setString(1, sender.getName());
            statement.setString(2, date);
            statement.setString(3, reason);
            statement.setInt(4, 0);
            statement.setString(5, name);
            statement.executeUpdate();
            statement.close();
            // Add to history with same information
            StringBuilder sb = new StringBuilder();
            sb.append(ChatColor.AQUA + name + ChatColor.RED + " was jailed on " + ChatColor.GREEN + date + ChatColor.RED + " by " + ChatColor.GOLD
                  + sender.getName());
            if(!reason.equals("")) {
               sb.append(ChatColor.RED + " for " + ChatColor.GRAY + ChatColor.translateAlternateColorCodes('&', reason));
            }
            database.addToHistory(name, sb.toString());
            // Notify
            sender.sendMessage(ChatColor.RED + name + ChatColor.AQUA + " sent to jail.");
            final PrisonerInfo pi = new PrisonerInfo(name, sender.getName(), date, reason, duration, false);
            plugin.getCommandHandlerForClass(Commander.class).getCache().put(name, pi);
            // Throw jail event
            plugin.getServer().getPluginManager().callEvent(new KarmicJailEvent(pi));
            // Broadcast if necessary
            if(config.broadcastJail) {
               // Setup broadcast string
               sb = new StringBuilder();
               sb.append(ChatColor.AQUA + pi.name + ChatColor.RED + " was jailed on " + ChatColor.GREEN + pi.date + ChatColor.RED + " by "
                     + ChatColor.GOLD + pi.jailer);
               if(!pi.reason.equals("")) {
                  sb.append(ChatColor.RED + " for " + ChatColor.GRAY + ChatColor.translateAlternateColorCodes('&', pi.reason));
               }
               if(pi.mute) {
                  sb.append(ChatColor.GRAY + " - " + ChatColor.DARK_RED + "MUTED");
               }
               // Broadcast
               if(config.broadcastPerms) {
                  plugin.getServer().broadcast(sb.toString(), "KarmicJail.broadcast");
               } else {
                  plugin.getServer().broadcastMessage(sb.toString());
               }
            }
         } catch(SQLException e) {
            plugin.getLogger().warning("SQL Exception on jail command");
            e.printStackTrace();
         }
      }
   }

   /**
    * Unjails a player
    * 
    * @param sender
    *           of command
    * @param name
    *           of jailed player
    * @param fromTempJail
    *           , if the jailed player's time ran out
    */
   public void unjailPlayer(CommandSender sender, String inName, boolean fromTempJail) {
      RootConfig config = plugin.getModuleForClass(RootConfig.class);
      PermCheck perm = plugin.getModuleForClass(PermCheck.class);
      Commander commander = plugin.getCommandHandlerForClass(Commander.class);
      String name = getPlayerInDatabase(inName);
      if(name == null) {
         name = inName;
      }
      // Check if player is in jail:
      final JailStatus currentStatus = getPlayerStatus(name);
      if(currentStatus == JailStatus.FREED || currentStatus == JailStatus.PENDINGFREE) {
         sender.sendMessage(ChatColor.RED + "That player is not in jail!");
         return;
      }

      // Grab player if on server
      Player player = plugin.getServer().getPlayer(name);
      // Remove jail group
      if(config.useJailGroup) {
         perm.playerRemoveGroup(config.jailLoc.getWorld(), name, config.jailGroup);
      }
      if(config.useUnjailGroup) {
         perm.playerAddGroup(config.jailLoc.getWorld().getName(), name, config.unjailGroup);
      }
      if(config.returnGroups) {
         // Return previous groups
         returnGroups(name);
      }

      // Remove viewers
      final Set<String> viewList = new HashSet<String>();
      for(Map.Entry<String, JailInventoryHolder> entry : commander.getInventoryHolders().entrySet()) {
         if(entry.getValue().getTarget().equals(name)) {
            final Player viewer = plugin.getServer().getPlayer(entry.getKey());
            if(viewer != null) {
               viewer.closeInventory();
            }
            viewList.add(entry.getKey());
         }
      }
      for(String viewer : viewList) {
         commander.getInventoryHolders().remove(viewer);
      }
      // Remove from cache
      PLAYER_CACHE.remove(name);
      plugin.getCommandHandlerForClass(Commander.class).getCache().remove(name);
      // Check if player is offline:
      if(player == null) {
         setPlayerStatus(JailStatus.PENDINGFREE, name);
         sender.sendMessage(ChatColor.GOLD + name + ChatColor.AQUA + " will be released from jail on login.");
         return;
      }

      freePlayer(sender, inName, fromTempJail);
   }

   public void unjailPlayer(CommandSender sender, String name) {
      unjailPlayer(sender, name, false);
   }

   public void freePlayer(CommandSender sender, String name) {
      freePlayer(sender, name, false);
   }

   public void freePlayer(CommandSender sender, String inName, boolean fromTempJail) {
      RootConfig config = plugin.getModuleForClass(RootConfig.class);
      DBHandler database = plugin.getModuleForClass(DBHandler.class);
      String name = getPlayerInDatabase(inName);
      if(name == null) {
         name = inName;
      }
      // Grab player if on server
      Player player = plugin.getServer().getPlayer(name);
      if(player != null) {
         // Return items if any
         if(config.returnInventory) {
            Map<Integer, ItemStack> items = database.getPlayerItems(name);
            for(Map.Entry<Integer, ItemStack> item : items.entrySet()) {
               try {
                  player.getInventory().setItem(item.getKey().intValue(), item.getValue());
               } catch(ArrayIndexOutOfBoundsException e) {
                  // Ignore
               }
            }
         }
         // Clear other columns
         database.resetPlayer(name);

         // Move player out of jail
         if(config.unjailTeleport) {
            player.teleport(config.unjailLoc);
         }
         // Change status
         setPlayerStatus(JailStatus.FREED, name);

         // Remove task
         if(KarmicJail.getJailThreads().containsKey(name)) {
            int id = KarmicJail.getJailThreads().get(name).getId();
            if(id != -1) {
               plugin.getServer().getScheduler().cancelTask(id);
            }
            KarmicJail.removeTask(name);
         }
         player.sendMessage(ChatColor.AQUA + "You have been released from jail!");
         if(fromTempJail) {
            // Also notify jailer if they're online
            Player jailer = plugin.getServer().getPlayer(getJailer(name));
            if(jailer != null) {
               jailer.sendMessage(ChatColor.GOLD + player.getName() + ChatColor.AQUA + " auto-unjailed.");
            }
            // Notify sender
            sender.sendMessage(ChatColor.GOLD + player.getName() + ChatColor.AQUA + " auto-unjailed.");
         } else {
            sender.sendMessage(ChatColor.GOLD + name + ChatColor.AQUA + " removed from jail.");
         }
         // Broadcast if necessary
         if(config.broadcastUnjail) {
            // Setup broadcast string
            final StringBuilder sb = new StringBuilder();
            sb.append(ChatColor.AQUA + name);
            if(fromTempJail) {
               sb.append(ChatColor.RED + " was auto-unjailed by ");
            } else {
               sb.append(ChatColor.RED + " was unjailed by ");
            }
            sb.append(ChatColor.GOLD + sender.getName());
            // Broadcast
            if(config.broadcastPerms) {
               plugin.getServer().broadcast(sb.toString(), "KarmicJail.broadcast");
            } else {
               plugin.getServer().broadcastMessage(sb.toString());
            }
         }
      }
   }

   /**
    * Checks if the player was jailed while offline
    * 
    * @param Name
    *           of player
    * @return True if pending jailed, else false
    */
   public boolean playerIsPendingJail(String player) {
      boolean jailed = false;
      String name = getPlayerInDatabase(player);
      if(name == null) {
         name = player;
      }
      switch(getPlayerStatus(name)) {
      case PENDINGJAIL: {
         jailed = true;
         break;
      }
      default:
         break;
      }
      return jailed;
   }

   /**
    * Checks if the player is in jail
    * 
    * @param Name
    *           of player
    * @return true if jailed, else false
    */
   public boolean playerIsJailed(String player) {
      boolean jailed = false;
      String name = getPlayerInDatabase(player);
      if(name == null) {
         name = player;
      }
      switch(getPlayerStatus(name)) {
      case JAILED: {
         jailed = true;
         break;
      }
      default:
         break;
      }
      return jailed;
   }

   /**
    * Grabs player's time left in jail
    * 
    * @param name
    *           of player
    * @return long of time left to serve
    */
   public long getPlayerTime(String player) {
      DBHandler database = plugin.getModuleForClass(DBHandler.class);
      String name = getPlayerInDatabase(player);
      if(name == null) {
         name = player;
      }
      return (long) database.getDoubleField(Field.TIME, name);
   }

   /**
    * Sets a player's time
    * 
    * @param name
    *           of player
    * @param duration
    *           of time
    */
   public void updatePlayerTime(String player, long duration) {
      DBHandler database = plugin.getModuleForClass(DBHandler.class);
      String name = getPlayerInDatabase(player);
      if(name == null) {
         name = player;
      }
      ResultSet rs = null;
      try {
         rs = database.query("UPDATE " + Table.JAILED.getName() + " SET time='" + duration + "' WHERE playername='" + name + "';");
      } catch(SQLException e) {
         plugin.getLogger().log(Level.SEVERE, KarmicJail.TAG + " SQL Exception", e);
      } finally {
         database.cleanup(rs, null);
      }
   }

   /**
    * Check if player exists in master database
    * 
    * @param name
    *           of player
    * @return Name of player in database, else null
    */
   public String getPlayerInDatabase(String name) {
      DBHandler database = plugin.getModuleForClass(DBHandler.class);
      String has = null;
      ResultSet rs = null;
      try {
         rs = database.query("SELECT * FROM " + Table.JAILED.getName() + ";");
         if(rs.next()) {
            do {
               if(name.equalsIgnoreCase(rs.getString(Field.PLAYERNAME.getColumnName()))) {
                  has = rs.getString("playername");
                  break;
               }
            } while(rs.next());
         }
      } catch(SQLException e) {
         plugin.getLogger().log(Level.SEVERE, KarmicJail.TAG + " SQL Exception", e);
      } finally {
         database.cleanup(rs, null);
      }
      return has;
   }

   /**
    * Adds a player to the database if they do not exist
    * 
    * @param name
    *           of player
    */
   public void addPlayerToDatabase(String name) {
      DBHandler database = plugin.getModuleForClass(DBHandler.class);
      ResultSet first = null;
      ResultSet second = null;
      try {
         boolean has = false;
         first = database.query("SELECT COUNT(*) FROM " + Table.JAILED.getName() + " WHERE playername='" + name + "';");
         if(first.next()) {
            final int count = first.getInt(1);
            if(!first.wasNull()) {
               if(count > 0) {
                  has = true;
               }
            }
         }
         database.cleanup(first, null);
         if(!has) {
            // Add to database
            second = database.query("INSERT INTO " + Table.JAILED.getName() + " (playername,status,time) VALUES ('" + name + "', '"
                  + JailStatus.FREED + "', '-1');");
         }
      } catch(SQLException e) {
         plugin.getLogger().log(Level.SEVERE, KarmicJail.TAG + " SQL Exception", e);
      } finally {
         database.cleanup(second, null);
      }
   }

   /**
    * Checks to see if the player has a time associated with their jail sentence
    * 
    * @param Name
    *           of player
    * @return true if player has a valid time, else false
    */
   public boolean playerIsTempJailed(String player) {
      DBHandler database = plugin.getModuleForClass(DBHandler.class);
      String name = getPlayerInDatabase(player);
      if(name == null) {
         name = player;
      }
      double time = database.getDoubleField(Field.TIME, name);
      if(time > 0) {
         return true;
      }
      return false;
   }

   public void setJailTime(CommandSender sender, String name, int minutes) {
      // Check if player is in jail:
      if(!playerIsJailed(name) && !playerIsPendingJail(name)) {
         sender.sendMessage(ChatColor.RED + "That player is not in jail!");
         return;
      }

      // Grab player if on server
      Player player = plugin.getServer().getPlayer(name);
      // Remove task
      if(KarmicJail.getJailThreads().containsKey(name)) {
         int id = KarmicJail.getJailThreads().get(name).getId();
         if(id != -1) {
            plugin.getServer().getScheduler().cancelTask(id);
         }
         KarmicJail.removeTask(name);
      }
      // Jail indefinitely if 0 or negative
      if(minutes <= 0) {
         updatePlayerTime(name, minutes);
         sender.sendMessage(ChatColor.RED + name + ChatColor.AQUA + " is jailed forever.");
         if(player != null) {
            player.sendMessage(ChatColor.AQUA + "Jailed forever.");
         }
      } else {
         // Calculate time
         long duration = 0;
         duration = minutes * KarmicJail.minutesToTicks;
         updatePlayerTime(name, duration);
         if(player != null) {
            // Create thread to release player
            KarmicJail.getJailThreads().put(name, new JailTask(plugin, name, duration));
         }
         sender.sendMessage(ChatColor.AQUA + "Time set to " + ChatColor.GOLD + minutes + ChatColor.AQUA + " for " + ChatColor.RED + name
               + ChatColor.AQUA + ".");
         if(player != null) {
            player.sendMessage(ChatColor.AQUA + "Time set to " + ChatColor.GOLD + minutes + ChatColor.AQUA + ".");
         }
      }

   }

   public void setPlayerReason(String inName, String reason) {
      RootConfig config = plugin.getModuleForClass(RootConfig.class);
      DBHandler database = plugin.getModuleForClass(DBHandler.class);
      String name = getPlayerInDatabase(inName);
      if(name == null) {
         name = inName;
      }
      database.setField(Field.REASON, name, reason, 0, 0);
      // Add to history
      if(!reason.equals("")) {
         database.addToHistory(name, ChatColor.GOLD + "Reason changed for " + ChatColor.AQUA + name + ChatColor.RED + " to " + ChatColor.GRAY
               + ChatColor.translateAlternateColorCodes('&', reason));
      }
      // broadcast
      if(config.broadcastReason) {
         final String out = ChatColor.AQUA + name + ChatColor.RED + " for " + ChatColor.GRAY + ChatColor.translateAlternateColorCodes('&', reason);
         if(config.broadcastPerms) {
            plugin.getServer().broadcast(out, "KarmicJail.broadcast");
         } else {
            plugin.getServer().broadcastMessage(out);
         }
      }
   }

   /**
    * Grabs the reason for being in jail
    * 
    * @param name
    *           of player
    * @return String of jailer's reason
    */
   public String getJailReason(String player) {
      DBHandler database = plugin.getModuleForClass(DBHandler.class);
      String name = getPlayerInDatabase(player);
      if(name == null) {
         name = player;
      }
      String reason = database.getStringField(Field.REASON, name);
      if(reason.equals("")) {
         reason = "UNKOWN";
      }
      return reason;
   }

   public boolean playerIsMuted(String player) {
      DBHandler database = plugin.getModuleForClass(DBHandler.class);
      boolean mute = false;
      String name = getPlayerInDatabase(player);
      if(name == null) {
         name = player;
      }
      int muteint = database.getIntField(Field.MUTE, name);
      if(muteint == 1) {
         mute = true;
      }
      return mute;
   }

   public void mutePlayer(CommandSender sender, String player) {
      DBHandler database = plugin.getModuleForClass(DBHandler.class);
      String name = getPlayerInDatabase(player);
      if(name == null) {
         name = player;
      }
      // Check if player is in jail:
      if(!playerIsJailed(name) && !playerIsPendingJail(name)) {
         sender.sendMessage(ChatColor.RED + "That player is not in jail!");
         return;
      }
      if(playerIsMuted(name)) {
         database.setField(Field.MUTE, name, null, 0, 0);
         sender.sendMessage(ChatColor.GOLD + name + ChatColor.GREEN + " unmuted");
      } else {
         database.setField(Field.MUTE, name, null, 1, 0);
         sender.sendMessage(ChatColor.GOLD + name + ChatColor.RED + " muted");
      }
   }

   /**
    * Returns the player's current status
    * 
    * @param name
    *           of player
    * @return String of the player's JailStatus
    */
   public JailStatus getPlayerStatus(String inName) {
      DBHandler database = plugin.getModuleForClass(DBHandler.class);
      String name = getPlayerInDatabase(inName);
      if(name == null) {
         name = inName;
      }
      String status = database.getStringField(Field.STATUS, name);
      if(status.equals(JailStatus.JAILED.name())) {
         return JailStatus.JAILED;
      } else if(status.equals(JailStatus.PENDINGFREE.name())) {
         return JailStatus.PENDINGFREE;
      } else if(status.equals(JailStatus.PENDINGJAIL.name())) {
         return JailStatus.PENDINGJAIL;
      } else {
         return JailStatus.FREED;
      }
   }

   /**
    * Sets a player's status
    * 
    * @param JailStatus
    *           to set to
    * @param name
    *           of player
    */
   public void setPlayerStatus(JailStatus status, String inName) {
      DBHandler database = plugin.getModuleForClass(DBHandler.class);
      String name = getPlayerInDatabase(inName);
      if(name == null) {
         name = inName;
      }
      database.setField(Field.STATUS, name, status.name(), 0, 0);
   }

   /**
    * Saves the player's groups into database
    * 
    * @param name
    *           of player
    */
   private void savePlayerGroups(String name) {
      RootConfig config = plugin.getModuleForClass(RootConfig.class);
      DBHandler database = plugin.getModuleForClass(DBHandler.class);
      StringBuilder sb = new StringBuilder();
      boolean append = false;
      for(String s : getGroups(name)) {
         sb.append(s + "&");
         append = true;
      }
      if(append) {
         sb.deleteCharAt(sb.length() - 1);
      }
      if(config.debugGroups) {
         plugin.getLogger().info("Group string for '" + name + "': " + sb.toString());
      }
      database.setField(Field.GROUPS, name, sb.toString(), 0, 0);
   }

   /**
    * Removes all groups of a player
    * 
    * @param name
    *           of player
    */
   private void removePlayerGroups(String name) {
      PermCheck perm = plugin.getModuleForClass(PermCheck.class);
      if(perm.getName().equals("PermissionsBukkit")) {
         final PermissionsPlugin permission = (PermissionsPlugin) plugin.getServer().getPluginManager().getPlugin("PermissionsBukkit");
         for(Group group : permission.getGroups(name)) {
            perm.playerRemoveGroup(plugin.getServer().getWorlds().get(0), name, group.getName());
         }
      } else {
         for(World w : plugin.getServer().getWorlds()) {
            String[] groups = perm.getPlayerGroups(w, name);
            for(String group : groups) {
               perm.playerRemoveGroup(w, name, group);
            }
         }
      }
   }

   /**
    * Returns a list of all the groups a player has
    * 
    * @param name
    *           of player
    * @return List of groups with associated world
    */
   public List<String> getGroups(String player) {
      PermCheck perm = plugin.getModuleForClass(PermCheck.class);
      List<String> list = new ArrayList<String>();
      if(perm.getName().equals("PermissionsBukkit")) {
         final PermissionsPlugin permission = (PermissionsPlugin) plugin.getServer().getPluginManager().getPlugin("PermissionsBukkit");
         for(Group group : permission.getGroups(player)) {
            final String s = group.getName() + "!" + plugin.getServer().getWorlds().get(0).getName();
            list.add(s);
         }
      } else {
         for(World w : plugin.getServer().getWorlds()) {
            String[] groups = perm.getPlayerGroups(w, player);
            for(String group : groups) {
               String s = group + "!" + w.getName();
               if(!list.contains(s)) {
                  list.add(s);
               }
            }
         }
      }

      return list;
   }

   /**
    * Restores the players groups from database storage
    * 
    * @param name
    *           of player
    */
   private void returnGroups(String name) {
      PermCheck perm = plugin.getModuleForClass(PermCheck.class);
      DBHandler database = plugin.getModuleForClass(DBHandler.class);
      String groups = database.getStringField(Field.GROUPS, name);
      if(!groups.equals("")) {
         try {
            if(groups.contains("&")) {
               String[] cut = groups.split("&");
               for(String group : cut) {
                  String[] split = group.split("!");
                  perm.playerAddGroup(split[1], name, split[0]);
               }
            } else {
               String[] split = groups.split("!");
               perm.playerAddGroup(split[1], name, split[0]);
            }
         } catch(ArrayIndexOutOfBoundsException a) {
            plugin.getLogger().warning("Could not return groups for " + name);
         }
      }
   }

   /**
    * Gets the status of a player
    * 
    * @param sender
    *           of command
    * @param arguments
    *           of command
    */
   public void jailStatus(CommandSender sender, String[] args) {
      if(!(sender instanceof Player) && args.length == 0) {
         sender.sendMessage(ChatColor.RED + "Must specify a player.");
         return;
      }
      final Player player = (args.length == 0) ? (Player) sender : plugin.getServer().getPlayer(args[0]);
      String temp = "";
      if(player == null) {
         temp = args[0];
      } else {
         temp = player.getName();
      }
      String name = getPlayerInDatabase(temp);
      if(name == null) {
         name = temp;
      }

      if(!playerIsJailed(name) && !playerIsPendingJail(name)) {
         if(args.length == 0)
            sender.sendMessage(ChatColor.RED + "You are not jailed.");
         else
            sender.sendMessage(ChatColor.AQUA + name + ChatColor.RED + " is not jailed.");
         return;
      }

      final StringBuilder sb = new StringBuilder();
      final String date = getJailDate(name);
      final String jailer = getJailer(name);
      final String reason = getJailReason(name);
      final boolean muted = playerIsMuted(name);
      if(args.length == 0) {
         sb.append(ChatColor.RED + "Jailed on " + ChatColor.GREEN + date + ChatColor.RED + " by " + ChatColor.GOLD + jailer);
      } else {
         sb.append(ChatColor.AQUA + name + ChatColor.RED + " was jailed on " + ChatColor.GREEN + date + ChatColor.RED + " by " + ChatColor.GOLD
               + jailer);
      }
      if(!reason.equals("UNKOWN")) {
         sb.append(ChatColor.RED + " for " + ChatColor.GRAY + ChatColor.translateAlternateColorCodes('&', reason));
      }
      if(muted) {
         sb.append(ChatColor.GRAY + " - " + ChatColor.DARK_RED + "MUTED");
      }
      sender.sendMessage(sb.toString());
      if(playerIsTempJailed(name)) {
         int minutes = (int) ((getPlayerTime(name) / KarmicJail.minutesToTicks));
         if(player == null) {
            sender.sendMessage(ChatColor.AQUA + "Remaining jail time: " + ChatColor.GOLD + plugin.prettifyMinutes(minutes));
         } else {
            // Player is online, check the thread for their remaining time
            if(KarmicJail.getJailThreads().containsKey(name)) {
               minutes = (int) (KarmicJail.getJailThreads().get(name).remainingTime() / KarmicJail.minutesToTicks);
               sender.sendMessage(ChatColor.AQUA + "Remaining jail time: " + plugin.prettifyMinutes(minutes));
            }
         }
      }
   }

   /**
    * Gets name of the jailer
    * 
    * @param name
    *           of person in jail
    * @return name of jailer
    */
   private String getJailer(String name) {
      DBHandler database = plugin.getModuleForClass(DBHandler.class);
      String jailer = database.getStringField(Field.JAILER, name);
      if(jailer.equals("")) {
         jailer = "UNKOWN";
      }
      return jailer;
   }

   /**
    * Grabs date of when player was originally jailed
    * 
    * @param name
    *           of person jailed
    * @return String of the date when player was jailed
    */
   private String getJailDate(String name) {
      DBHandler database = plugin.getModuleForClass(DBHandler.class);
      String date = database.getStringField(Field.DATE, name);
      if(date.equals("")) {
         date = "UNKOWN";
      }
      return date;
   }

   /**
    * Sets the jail location
    * 
    * @param sender
    *           of command
    * @param arguments
    *           of command
    */
   public void setJail(CommandSender sender, String[] args) {
      RootConfig config = plugin.getModuleForClass(RootConfig.class);
      if(!(sender instanceof Player) && args.length != 4) {
         sender.sendMessage(ChatColor.RED + "Only players can use that.");
         return;
      }
      if(args.length == 0) {
         Player player = (Player) sender;
         config.jailLoc = player.getLocation();
      } else {
         if(!(new Scanner(args[0]).hasNextInt()) || !(new Scanner(args[1]).hasNextInt()) || !(new Scanner(args[2]).hasNextInt())) {
            sender.sendMessage(ChatColor.RED + "Invalid coordinate.");
            return;
         }
         config.jailLoc = new Location(plugin.getServer().getWorld(args[3]), Integer.parseInt(args[0]), Integer.parseInt(args[1]),
               Integer.parseInt(args[2]));
      }

      config.set("jail.x", (int) config.jailLoc.getX());
      config.set("jail.y", (int) config.jailLoc.getY());
      config.set("jail.z", (int) config.jailLoc.getZ());
      config.set("jail.world", config.jailLoc.getWorld().getName());

      plugin.saveConfig();

      sender.sendMessage(ChatColor.AQUA + "Jail point saved.");
   }

   /**
    * Sets the unjail location
    * 
    * @param sender
    *           of command
    * @param arguments
    *           of command
    */
   public void setUnjail(CommandSender sender, String[] args) {
      RootConfig config = plugin.getModuleForClass(RootConfig.class);
      if(!(sender instanceof Player) && args.length != 4) {
         sender.sendMessage(ChatColor.RED + "Only players can use that.");
         return;
      }
      if(args.length == 0) {
         Player player = (Player) sender;
         config.unjailLoc = player.getLocation();
      } else {
         if(!(new Scanner(args[0]).hasNextInt()) || !(new Scanner(args[1]).hasNextInt()) || !(new Scanner(args[2]).hasNextInt())) {
            sender.sendMessage(ChatColor.RED + "Invalid coordinate.");
            return;
         }
         config.unjailLoc = new Location(plugin.getServer().getWorld(args[3]), Integer.parseInt(args[0]), Integer.parseInt(args[1]),
               Integer.parseInt(args[2]));
      }

      config.set("unjail.x", (int) config.unjailLoc.getX());
      config.set("unjail.y", (int) config.unjailLoc.getY());
      config.set("unjail.z", (int) config.unjailLoc.getZ());
      config.set("unjail.world", config.unjailLoc.getWorld().getName());

      plugin.saveConfig();

      sender.sendMessage(ChatColor.AQUA + "Unjail point saved.");
   }

   /**
    * 
    * @return location of jail
    */
   public Location getJailLocation() {
      RootConfig config = plugin.getModuleForClass(RootConfig.class);
      return config.jailLoc;
   }

   /**
    * 
    * @return location of unjail
    */
   public Location getUnjailLocation() {
      RootConfig config = plugin.getModuleForClass(RootConfig.class);
      return config.unjailLoc;
   }

   /**
    * Teleports a player to unjail locaiton
    * 
    * @param name
    *           of player to be teleported
    */
   public void teleportOut(String name) {
      RootConfig config = plugin.getModuleForClass(RootConfig.class);
      final Player player = plugin.getServer().getPlayer(name);
      if(player != null) {
         player.teleport(config.unjailLoc);
      }
   }

   public void setPlayerLastLocation(String playername, Location location) {
      DBHandler database = plugin.getModuleForClass(DBHandler.class);
      String name = getPlayerInDatabase(playername);
      if(name == null) {
         name = playername;
      }
      final String entry = location.getWorld().getName() + " " + location.getX() + " " + location.getY() + " " + location.getZ() + " "
            + location.getYaw() + " " + location.getPitch();
      database.setField(Field.LAST_POSITION, name, entry, 0, 0);
   }

   public Location getPlayerLastLocation(String playername) {
      DBHandler database = plugin.getModuleForClass(DBHandler.class);
      Location location = null;
      String name = getPlayerInDatabase(playername);
      if(name == null) {
         name = playername;
      }
      final String entry = database.getStringField(Field.LAST_POSITION, name);
      if(!entry.equals("") && entry.contains(" ")) {
         try {
            final String[] split = entry.split(" ");
            final World world = plugin.getServer().getWorld(split[0]);
            if(world != null) {
               location = new Location(world, Double.parseDouble(split[1]), Double.parseDouble(split[2]), Double.parseDouble(split[3]),
                     Float.parseFloat(split[4]), Float.parseFloat(split[5]));
            }
         } catch(ArrayIndexOutOfBoundsException a) {
            plugin.getLogger().warning("Bad last location for: " + name);
         } catch(NumberFormatException n) {
            plugin.getLogger().warning("Bad last location for: " + name);
         }
      }
      return location;
   }

   public void setPlayerInventory(String playername, Inventory inventory, boolean clear) {
      DBHandler database = plugin.getModuleForClass(DBHandler.class);
      Map<Integer, ItemStack> items = new HashMap<Integer, ItemStack>();
      if(inventory instanceof PlayerInventory) {
         PlayerInventory inv = (PlayerInventory) inventory;
         // Get normal inventory
         for(int i = 0; i < inventory.getSize(); i++) {
            try {
               final ItemStack item = inventory.getItem(i);
               if(item != null) {
                  if(!item.getType().equals(Material.AIR)) {
                     /*
                      * plugin.getLogger().info( item.toString() + " at " + i);
                      */
                     items.put(Integer.valueOf(i), item);
                  }
               }
            } catch(ArrayIndexOutOfBoundsException a) {
               // Ignore
            } catch(NullPointerException n) {
               // Ignore
            }
         }
         // TODO implement
         /*
          * ItemStack[] armor = inv.getArmorContents(); for (int i = 0; i <
          * armor.length; i++) { try { final ItemStack item = armor[i]; if (item
          * != null) { if (!item.getType().equals(Material.AIR)) {
          * plugin.getLogger().info( item.toString() + " at " + i);
          * items.put(new Integer(i + inv.getSize()), item); } } } catch
          * (ArrayIndexOutOfBoundsException a) { // Ignore } catch
          * (NullPointerException n) { // Ignore } }
          */
         if(database.setPlayerItems(playername, items) && clear) {
            // clear inventory
            try {
               inv.clear();
               final ItemStack[] cleared = new ItemStack[] { null, null, null, null };
               inv.setArmorContents(cleared);
            } catch(ArrayIndexOutOfBoundsException e) {
               // ignore again
            }
         }
      }

   }

}
