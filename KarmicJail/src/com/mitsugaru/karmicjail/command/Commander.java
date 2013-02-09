package com.mitsugaru.karmicjail.command;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mitsugaru.karmicjail.KarmicJail;
import com.mitsugaru.karmicjail.database.DBHandler;
import com.mitsugaru.karmicjail.database.Table;
import com.mitsugaru.karmicjail.jail.JailStatus;
import com.mitsugaru.karmicjail.jail.PrisonerInfo;
import com.mitsugaru.karmicjail.config.RootConfig;
import com.mitsugaru.karmicjail.inventory.JailInventoryHolder;
import com.mitsugaru.karmicjail.permissions.PermCheck;
import com.mitsugaru.karmicjail.permissions.PermissionNode;
import com.mitsugaru.karmicjail.services.CommandHandler;
import com.mitsugaru.karmicjail.services.JailCommand;

public class Commander extends CommandHandler {
   private final Map<String, Integer> page = new HashMap<String, Integer>();
   private final Map<String, PrisonerInfo> cache = new HashMap<String, PrisonerInfo>();
   private final Map<String, JailInventoryHolder> inventoryHolders = new HashMap<String, JailInventoryHolder>();

   public Commander(KarmicJail plugin) {
      super(plugin, "kj");
      // Register commands
      JailPlayerCommand jail = new JailPlayerCommand();
      registerCommand("jail", jail);
      registerCommand("j", jail);
      registerCommand("unjail", new UnjailCommand());
      registerCommand("setjail", new SetJailCommand());
      registerCommand("setunjail", new SetUnjailCommand());
      HelpCommand help = new HelpCommand();
      registerCommand("help", help);
      registerCommand("?", help);
      VersionCommand version = new VersionCommand();
      registerCommand("version", version);
      registerCommand("ver", version);
      StatusCommand status = new StatusCommand();
      registerCommand("status", status);
      registerCommand("check", status);
      registerCommand("reload", new ReloadCommand());
      registerCommand("list", new ListCommand());
      registerCommand("prev", new PrevCommand());
      registerCommand("next", new NextCommand());
      registerCommand("mute", new MuteCommand());
      registerCommand("time", new TimeCommand());
      registerCommand("reason", new ReasonCommand());
      registerCommand("last", new LastCommand());
      registerCommand("inv", new InventoryCommand());
      registerCommand("warp", new WarpCommand());
   }

   @Override
   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      RootConfig config = plugin.getModuleForClass(RootConfig.class);
      long dTime = 0;
      if(config.debugTime) {
         dTime = System.nanoTime();
      }
      boolean value = super.onCommand(sender, command, label, args);
      if(config.debugTime) {
         dTime = System.nanoTime() - dTime;
         sender.sendMessage("[Debug]" + KarmicJail.TAG + "Process time: " + dTime);
      }
      return value;
   }

   public void showHelp(CommandSender sender) {
      PermCheck perm = plugin.getModuleForClass(PermCheck.class);
      sender.sendMessage(ChatColor.BLUE + "=====" + ChatColor.GREEN + "KarmicJail" + ChatColor.BLUE + "=====");
      if(perm.has(sender, PermissionNode.JAIL)) {
         sender.sendMessage(ChatColor.GREEN + "/kj jail " + ChatColor.AQUA + "<player> " + ChatColor.LIGHT_PURPLE + "[player2]... [time] [reason]"
               + ChatColor.YELLOW + " : Jails player(s)");
         sender.sendMessage(ChatColor.YELLOW + "Note - Names auto-complete if player is online.");
         sender.sendMessage(ChatColor.GREEN + "/kj time" + ChatColor.AQUA + " <player> <time>" + ChatColor.YELLOW
               + " : View and set time for jailed player.");
         sender.sendMessage(ChatColor.GREEN + "/kj reason" + ChatColor.AQUA + " <player> " + ChatColor.LIGHT_PURPLE + "[reason]" + ChatColor.YELLOW
               + " : Sets jail reason for player.");
      }
      if(perm.has(sender, PermissionNode.UNJAIL)) {
         sender.sendMessage(ChatColor.GREEN + "/kj unjail" + ChatColor.AQUA + " <player>" + ChatColor.YELLOW + " : Unjail player");
      }
      if(perm.has(sender, PermissionNode.MUTE)) {
         sender.sendMessage(ChatColor.GREEN + "/kj mute" + ChatColor.AQUA + " <player>" + ChatColor.YELLOW
               + " : Toggle mute for a player.");
      }
      if(perm.has(sender, PermissionNode.LIST)) {
         sender.sendMessage(ChatColor.GREEN + "/kj list" + ChatColor.LIGHT_PURPLE + " [page]" + ChatColor.YELLOW
               + " : List jailed players.");
         sender.sendMessage(ChatColor.GREEN + "/kj prev" + ChatColor.YELLOW + " : Previous page. Alias: /jprev");
         sender.sendMessage(ChatColor.GREEN + "/kj next" + ChatColor.YELLOW + " : Next page. Alias: /jnext");
      }
      if(perm.has(sender, PermissionNode.HISTORY_VIEW)) {
         sender.sendMessage(ChatColor.GREEN + "/kj history" + ChatColor.LIGHT_PURPLE + " [args]" + ChatColor.YELLOW
               + " : Jail history command.");
      }
      if(perm.has(sender, PermissionNode.INVENTORY_VIEW)) {
         sender.sendMessage(ChatColor.GREEN + "/kj inv" + ChatColor.AQUA + " <player>" + ChatColor.YELLOW
               + " : Open inventory of jailed player.");
      }
      if(perm.has(sender, PermissionNode.WARP_LAST)) {
         sender.sendMessage(ChatColor.GREEN + "/kj last" + ChatColor.AQUA + " <player>" + ChatColor.YELLOW
               + " : Warp to last known postion of player");
      }
      if(perm.has(sender, PermissionNode.SETJAIL)) {
         sender.sendMessage(ChatColor.GREEN + "/kj setjail" + ChatColor.LIGHT_PURPLE + " [x] [y] [z] [world]" + ChatColor.YELLOW
               + " : Set jail teleport to current pos or given pos");
         sender.sendMessage(ChatColor.GREEN + "/kj setunjail" + ChatColor.LIGHT_PURPLE + " [x] [y] [z] [world]" + ChatColor.YELLOW
               + " : Set unjail teleport to current pos or given pos");
      }
      if(perm.has(sender, PermissionNode.JAILSTATUS)) {
         sender.sendMessage(ChatColor.GREEN + "/kj status" + ChatColor.LIGHT_PURPLE + " [player]" + ChatColor.YELLOW
               + " : Get jail status.");
      }
      sender.sendMessage(ChatColor.GREEN + "/kj version" + ChatColor.YELLOW + " : Plugin version and config info. Alias: /jversion");
   }

   /**
    * Lists the players in jail
    * 
    * @param sender
    *           of command
    * @param Page
    *           adjustment
    */
   public void listJailed(CommandSender sender, int pageAdjust) {
      RootConfig config = plugin.getModuleForClass(RootConfig.class);
      DBHandler database = plugin.getModuleForClass(DBHandler.class);
      // Update cache of jailed players
      ResultSet rs = null;
      try {
         // TODO order by date
         rs = database.query("SELECT * FROM " + Table.JAILED.getName() + " WHERE status='" + JailStatus.JAILED + "' OR status='"
               + JailStatus.PENDINGJAIL + "';");
         if(rs.next()) {
            do {
               String name = rs.getString("playername");
               String jailer = rs.getString("jailer");
               if(rs.wasNull()) {
                  jailer = "NOBODY";
               }
               String date = rs.getString("date");
               if(rs.wasNull()) {
                  date = "NO DATE";
               }
               String reason = rs.getString("reason");
               if(rs.wasNull()) {
                  reason = "";
               }
               long time = rs.getLong("time");
               if(rs.wasNull()) {
                  time = 0;
               }
               int muteInt = rs.getInt("muted");
               if(rs.wasNull()) {
                  muteInt = 0;
               }
               boolean muted = false;
               if(muteInt == 1) {
                  muted = true;
               }
               cache.put(name, new PrisonerInfo(name, jailer, date, reason, time, muted));
               // Update the time if necessary
               if(KarmicJail.getJailThreads().containsKey(name)) {
                  cache.get(name).updateTime(KarmicJail.getJailThreads().get(name).remainingTime());
               }
            } while(rs.next());
         }
      } catch(SQLException e) {
         plugin.getLogger().log(Level.SEVERE, KarmicJail.TAG + " SQL Exception", e);
         e.printStackTrace();
      } finally {
         database.cleanup(rs, null);
      }
      if(cache.isEmpty()) {
         sender.sendMessage(ChatColor.RED + KarmicJail.TAG + " No jailed players");
         return;
      }
      if(!page.containsKey(sender.getName())) {
         page.put(sender.getName(), 0);
      } else {
         if(pageAdjust != 0) {
            int adj = page.get(sender.getName()).intValue() + pageAdjust;
            page.put(sender.getName(), adj);
         }
      }
      PrisonerInfo[] array = cache.values().toArray(new PrisonerInfo[0]);
      boolean valid = true;
      // Caluclate amount of pages
      int num = array.length / 8;
      double rem = (double) array.length % (double) config.limit;
      if(rem != 0) {
         num++;
      }
      if(page.get(sender.getName()).intValue() < 0) {
         // They tried to use /ks prev when they're on page 0
         sender.sendMessage(ChatColor.YELLOW + KarmicJail.TAG + " Page does not exist");
         // reset their current page back to 0
         page.put(sender.getName(), 0);
         valid = false;
      } else if((page.get(sender.getName()).intValue()) * config.limit > array.length) {
         // They tried to use /ks next at the end of the list
         sender.sendMessage(ChatColor.YELLOW + KarmicJail.TAG + " Page does not exist");
         // Revert to last page
         page.put(sender.getName(), num - 1);
         valid = false;
      }
      if(valid) {
         // Header with amount of pages
         sender.sendMessage(ChatColor.BLUE + "===" + ChatColor.GRAY + "Jailed" + ChatColor.BLUE + "===" + ChatColor.GRAY + "Page: "
               + ((page.get(sender.getName()).intValue()) + 1) + ChatColor.BLUE + " of " + ChatColor.GRAY + num + ChatColor.BLUE + "===");
         // list
         for(int i = ((page.get(sender.getName()).intValue()) * config.limit); i < ((page.get(sender.getName()).intValue()) * config.limit)
               + config.limit; i++) {
            // Don't try to pull something beyond the bounds
            if(i < array.length) {
               StringBuilder sb = new StringBuilder();
               Player player = plugin.getServer().getPlayer(array[i].name);
               // Grab player and colorize name if they're online or not
               if(player == null) {
                  sb.append(ChatColor.RED + array[i].name + ChatColor.GRAY + " - ");
               } else {
                  sb.append(ChatColor.GREEN + array[i].name + ChatColor.GRAY + " - ");
               }
               // Grab date
               try {
                  sb.append(ChatColor.GOLD + array[i].date.substring(0, 10) + ChatColor.GRAY + " - ");
               } catch(StringIndexOutOfBoundsException e) {
                  // Incorrect format stored, so just give the date as is
                  sb.append(ChatColor.GOLD + array[i].date + ChatColor.GRAY + " - ");
               }
               // Give jailer name
               sb.append(ChatColor.AQUA + array[i].jailer);
               // Grab time if applicable
               if(array[i].time > 0) {
                  double temp = Math.floor(((double) array[i].time / (double) KarmicJail.minutesToTicks) + 0.5f);
                  sb.append(ChatColor.GRAY + " - " + ChatColor.BLUE + "" + plugin.prettifyMinutes((int) temp));
               }
               // Grab reason if there was one given
               if(!array[i].reason.equals("")) {
                  sb.append(ChatColor.GRAY + " - " + ChatColor.GRAY + ChatColor.translateAlternateColorCodes('&', array[i].reason));
               }
               // Grab if muted
               if(array[i].mute) {
                  sb.append(ChatColor.GRAY + " - " + ChatColor.DARK_RED + "MUTED");
               }
               sender.sendMessage(sb.toString());
            } else {
               break;
            }
         }
      }
   }

   public Map<String, JailInventoryHolder> getInventoryHolders() {
      return inventoryHolders;
   }

   public Map<String, Integer> getPage() {
      return page;
   }

   public Map<String, PrisonerInfo> getCache() {
      return cache;
   }

   @Override
   public boolean noArgs(CommandSender sender, Command command, String label) {
      showHelp(sender);
      return true;
   }

   @Override
   public boolean unknownCommand(CommandSender sender, Command command, String label, String[] args) {
      sender.sendMessage(ChatColor.YELLOW + KarmicJail.TAG + " Invalid command '" + args[0] + "', use /kj help.");
      return true;
   }

   private class HelpCommand implements JailCommand {

      @Override
      public boolean execute(KarmicJail plugin, CommandSender sender, Command command, String label, String[] args) {
         showHelp(sender);
         return true;
      }

   }
}
