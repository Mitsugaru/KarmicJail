package com.mitsugaru.karmicjail.command;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mitsugaru.karmicjail.KarmicJail;
import com.mitsugaru.karmicjail.database.DBHandler;
import com.mitsugaru.karmicjail.database.Table;
import com.mitsugaru.karmicjail.jail.JailLogic;
import com.mitsugaru.karmicjail.jail.JailStatus;
import com.mitsugaru.karmicjail.jail.PrisonerInfo;
import com.mitsugaru.karmicjail.config.RootConfig;
import com.mitsugaru.karmicjail.inventory.JailInventoryHolder;
import com.mitsugaru.karmicjail.permissions.PermCheck;
import com.mitsugaru.karmicjail.permissions.PermissionNode;
import com.mitsugaru.karmicjail.services.CommandHandler;

public class Commander extends CommandHandler {
   private KarmicJail plugin;
   private static final String bar = "======================";
   private final Map<String, Integer> page = new HashMap<String, Integer>();
   private final Map<String, PrisonerInfo> cache = new HashMap<String, PrisonerInfo>();
   public static final Map<String, JailInventoryHolder> inv = new HashMap<String, JailInventoryHolder>();

   public Commander(KarmicJail plugin) {
      super(plugin, "kj");
      // Register commands
      plugin.getCommand("jail").setExecutor(this);
      plugin.getCommand("j").setExecutor(this);
      plugin.getCommand("unjail").setExecutor(this);
      plugin.getCommand("setjail").setExecutor(this);
      plugin.getCommand("setunjail").setExecutor(this);
      plugin.getCommand("jailstatus").setExecutor(this);
      plugin.getCommand("jstatus").setExecutor(this);
      plugin.getCommand("jailhelp").setExecutor(this);
      plugin.getCommand("jhelp").setExecutor(this);
      plugin.getCommand("jaillist").setExecutor(this);
      plugin.getCommand("jlist").setExecutor(this);
      plugin.getCommand("jailprev").setExecutor(this);
      plugin.getCommand("jprev").setExecutor(this);
      plugin.getCommand("jailnext").setExecutor(this);
      plugin.getCommand("jnext").setExecutor(this);
      plugin.getCommand("jailmute").setExecutor(this);
      plugin.getCommand("jmute").setExecutor(this);
      plugin.getCommand("jailtime").setExecutor(this);
      plugin.getCommand("jtime").setExecutor(this);
      plugin.getCommand("jailreason").setExecutor(this);
      plugin.getCommand("jreason").setExecutor(this);
      plugin.getCommand("jlast").setExecutor(this);
      plugin.getCommand("jaillast").setExecutor(this);
      plugin.getCommand("jinv").setExecutor(this);
      plugin.getCommand("jailinv").setExecutor(this);
      plugin.getCommand("jwarp").setExecutor(this);
      plugin.getCommand("jailwarp").setExecutor(this);
      plugin.getCommand("jailversion").setExecutor(this);
      plugin.getCommand("jversion").setExecutor(this);
      plugin.getCommand("jailreload").setExecutor(this);
      plugin.getCommand("jreload").setExecutor(this);
   }

   @Override
   public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
      RootConfig config = plugin.getModuleForClass(RootConfig.class);
      JailLogic logic = plugin.getModuleForClass(JailLogic.class);
      PermCheck perm = plugin.getModuleForClass(PermCheck.class);
      boolean com = false;
      long dTime = 0;
      if(config.debugTime) {
         dTime = System.nanoTime();
      }
      if(commandLabel.equalsIgnoreCase("jail") || commandLabel.equalsIgnoreCase("j")) {
         if(!perm.has(sender, PermissionNode.JAIL)) {
            sender.sendMessage(ChatColor.RED + "Lack Permission: " + PermissionNode.JAIL.getNode());
            com = true;
         } else {
            // All numeric player name must be the first name
            boolean timed = false;
            boolean done = false;
            int time = 0;
            StringBuilder sb = new StringBuilder();
            String reason = "";
            final Set<String> players = new HashSet<String>();
            try {
               String first = plugin.expandName(args[0]);
               if(first == null) {
                  // expand failed
                  first = args[0];
               }
               players.add(first);
               for(int i = 1; i < args.length; i++) {
                  if(!done) {
                     try {
                        // Attempt to grab time
                        time = Integer.parseInt(args[i]);
                        // Attempt to grab player name if its all
                        // numbers
                        if(time > 0) {
                           timed = true;
                        }
                        done = true;
                     } catch(NumberFormatException e) {
                        // Attempt to grab name and add to list
                        String name = plugin.expandName(args[i]);
                        if(name != null) {
                           players.add(name);
                        } else {
                           players.add(args[i]);
                        }
                     }
                  } else {
                     // attempt to grab reason if it exists
                     sb.append(args[i] + " ");
                  }
               }
               if(sb.length() > 0) {
                  // Remove all trailing whitespace
                  reason = sb.toString().replaceAll("\\s+$", "");
               }
               for(String name : players) {
                  logic.jailPlayer(sender, name, reason, time, timed);
               }
            } catch(ArrayIndexOutOfBoundsException e) {
               // no player name given, error
               sender.sendMessage(ChatColor.RED + "Missing paramters");
               sender.sendMessage(ChatColor.RED + "/j <player> [player2] ... [time] [reason]");
            }
         }
         com = true;
      } else if(commandLabel.equalsIgnoreCase("unjail")) {
         if(!perm.has(sender, PermissionNode.UNJAIL)) {
            sender.sendMessage(ChatColor.RED + "Lack Permission: " + PermissionNode.UNJAIL.getNode());
         } else {
            final Vector<String> players = new Vector<String>();
            for(int i = 0; i < args.length; i++) {
               // Attempt to grab name and add to list
               String name = plugin.expandName(args[i]);
               if(name != null) {
                  players.add(name);
               } else {
                  players.add(args[i]);
               }
            }
            if(players.isEmpty()) {
               sender.sendMessage(ChatColor.RED + "Missing paramters");
               sender.sendMessage(ChatColor.RED + "/unjail <player> [player2]");
            }
            for(String name : players) {
               logic.unjailPlayer(sender, name);
            }
         }
         com = true;
      } else if(commandLabel.equalsIgnoreCase("setjail") && (args.length == 0 || args.length == 4)) {
         if(!perm.has(sender, PermissionNode.SETJAIL)) {
            sender.sendMessage(ChatColor.RED + "Lack Permission: " + PermissionNode.SETJAIL.getNode());
         } else {
            logic.setJail(sender, args);
         }
         com = true;
      } else if(commandLabel.equalsIgnoreCase("setunjail") && (args.length == 0 || args.length == 4)) {
         if(!perm.has(sender, PermissionNode.SETJAIL)) {
            sender.sendMessage(ChatColor.RED + "Lack Permission: " + PermissionNode.SETJAIL.getNode());
         } else {
            logic.setUnjail(sender, args);
         }
         com = true;
      } else if((commandLabel.equalsIgnoreCase("jailstatus") || commandLabel.equalsIgnoreCase("jstatus")
            || commandLabel.equalsIgnoreCase("jailcheck") || commandLabel.equalsIgnoreCase("jcheck"))
            && args.length <= 1) {
         if(!perm.has(sender, PermissionNode.JAILSTATUS)) {
            sender.sendMessage(ChatColor.RED + "Lack Permission: " + PermissionNode.JAILSTATUS.getNode());
         } else {
            logic.jailStatus(sender, args);
         }
         com = true;
      } else if(commandLabel.equalsIgnoreCase("jailversion") || commandLabel.equalsIgnoreCase("jversion")) {
         showVersion(sender);
         com = true;
      } else if(commandLabel.equalsIgnoreCase("jailhelp") || commandLabel.equalsIgnoreCase("jhelp")) {
         showHelp(sender);
         com = true;
      } else if(commandLabel.equalsIgnoreCase("jaillist") || commandLabel.equalsIgnoreCase("jlist")) {
         if(!perm.has(sender, PermissionNode.LIST)) {
            sender.sendMessage(ChatColor.RED + "Lack Permission: " + PermissionNode.LIST.getNode());
         } else {
            // list jailed people
            if(args.length > 0) {
               // If they provided a page number
               try {
                  // Attempt to parse argument for page number
                  int pageNum = Integer.parseInt(args[0]);
                  // Set current page to given number
                  page.put(sender.getName(), pageNum - 1);
                  // Show page if possible
                  this.listJailed(sender, 0);
               } catch(NumberFormatException e) {
                  sender.sendMessage(ChatColor.YELLOW + KarmicJail.TAG + " Invalid integer for page number");
               }
            } else {
               // List with current page
               this.listJailed(sender, 0);
            }
         }
         com = true;
      } else if(commandLabel.equals("jailprev") || commandLabel.equals("jprev")) {
         if(!perm.has(sender, PermissionNode.LIST)) {
            sender.sendMessage(ChatColor.RED + "Lack Permission: " + PermissionNode.LIST.getNode());
         } else {
            // List, with previous page
            this.listJailed(sender, -1);
         }
         com = true;
      }
      // Next page of item pool
      else if(commandLabel.equals("jailnext") || commandLabel.equals("jnext")) {
         if(!perm.has(sender, PermissionNode.LIST)) {
            sender.sendMessage(ChatColor.RED + "Lack Permission: " + PermissionNode.LIST.getNode());
         } else {
            // List with next page
            this.listJailed(sender, 1);
         }
         com = true;
      } else if(commandLabel.equals("jailmute") || commandLabel.equals("jmute")) {
         if(!perm.has(sender, PermissionNode.MUTE)) {
            sender.sendMessage(ChatColor.RED + "Lack Permission: " + PermissionNode.MUTE.getNode());
         } else {
            final Vector<String> players = new Vector<String>();
            for(int i = 0; i < args.length; i++) {
               // Attempt to grab name and add to list
               String name = plugin.expandName(args[i]);
               if(name != null) {
                  players.add(name);
               } else {
                  players.add(args[i]);
               }
            }
            if(players.isEmpty()) {
               sender.sendMessage(ChatColor.RED + "Missing paramters");
               sender.sendMessage(ChatColor.RED + "/jmute <player> [player2] ...");
            }
            for(String name : players) {
               logic.mutePlayer(sender, name);
            }
         }
         com = true;
      } else if(commandLabel.equals("jaillast") || commandLabel.equals("jlast")) {
         if(!perm.has(sender, PermissionNode.WARP_LAST)) {
            sender.sendMessage(ChatColor.RED + "Lack Permission: " + PermissionNode.WARP_LAST.getNode());
         } else {
            if(sender instanceof Player) {
               final Player player = (Player) sender;
               if(args.length > 0) {
                  String name = plugin.expandName(args[0]);
                  final Location last = logic.getPlayerLastLocation(name);
                  if(last != null) {
                     player.teleport(last);
                     sender.sendMessage(ChatColor.GREEN + KarmicJail.TAG + " Warp to last location of " + ChatColor.AQUA + name);
                  } else {
                     sender.sendMessage(ChatColor.RED + KarmicJail.TAG + " No last location for " + ChatColor.AQUA + name);
                  }
               } else {
                  sender.sendMessage(ChatColor.RED + "Missing name");
                  sender.sendMessage(ChatColor.RED + "/jlast <player>");
               }
            } else {
               sender.sendMessage(ChatColor.RED + "Cannot use command as console.");
            }
         }
         com = true;
      } else if(commandLabel.equals("jailwarp") || commandLabel.equals("jwarp")) {
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
         com = true;
      } else if(commandLabel.equals("jailinv") || commandLabel.equals("jinv")) {
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
                     for(JailInventoryHolder h : inv.values()) {
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
                     inv.put(player.getName(), holder);
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
         com = true;
      } else if(commandLabel.equals("jailtime") || commandLabel.equals("jtime")) {
         boolean hasPerm = true;
         if(!perm.has(sender, PermissionNode.JAIL)) {
            sender.sendMessage(ChatColor.RED + "Lack Permission: " + PermissionNode.JAIL.getNode());
            hasPerm = false;
         }
         if(config.timePerm) {
            if(!perm.has(sender, PermissionNode.TIMED)) {
               sender.sendMessage(ChatColor.RED + "Lack Permission: " + PermissionNode.TIMED.getNode());
               hasPerm = false;
            }
         }
         if(hasPerm) {
            boolean done = false;
            int time = 0;
            final Vector<String> players = new Vector<String>();
            for(int i = 0; i < args.length; i++) {
               if(!done) {
                  try {
                     // Attempt to grab time
                     time = Integer.parseInt(args[i]);
                     done = true;
                  } catch(NumberFormatException e) {
                     // Attempt to grab name and add to list
                     String name = plugin.expandName(args[i]);
                     if(name != null) {
                        players.add(name);
                     } else {
                        players.add(args[i]);
                     }
                  }
               }
            }
            if(players.isEmpty()) {
               sender.sendMessage(ChatColor.RED + "Missing paramters");
               sender.sendMessage(ChatColor.RED + "/jtime <player> [player2] ... <time>");
            }
            for(String name : players) {
               logic.setJailTime(sender, name, time);
            }
         }
         com = true;
      } else if(commandLabel.equals("jailreload") || commandLabel.equals("jreload")) {
         if(perm.has(sender, PermissionNode.JAIL) || perm.has(sender, PermissionNode.UNJAIL) || perm.has(sender, PermissionNode.SETJAIL)) {
            config.reload();
            sender.sendMessage(ChatColor.GREEN + KarmicJail.TAG + " Config reloaded.");
         } else {
            sender.sendMessage(ChatColor.RED + "Lack permission to reload");
         }
         com = true;
      } else if(commandLabel.equals("jailreason") || commandLabel.equals("jreason")) {
         if(!perm.has(sender, PermissionNode.JAIL)) {
            sender.sendMessage(ChatColor.RED + "Lack Permission: " + PermissionNode.JAIL.getNode());
         } else {
            if(args.length > 0) {
               String name = plugin.expandName(args[0]);
               final StringBuilder sb = new StringBuilder();
               for(int i = 1; i < args.length; i++) {
                  sb.append(args[i] + " ");
               }
               String reason = "";
               if(sb.length() > 0) {
                  // Remove all trailing whitespace
                  reason = sb.toString().replaceAll("\\s+$", "");
               }
               if(logic.playerIsJailed(name) || logic.playerIsPendingJail(name)) {
                  logic.setPlayerReason(name, reason);
                  sender.sendMessage(ChatColor.GREEN + KarmicJail.TAG + " Set reason for " + ChatColor.AQUA + name + ChatColor.GREEN + " to: "
                        + ChatColor.GRAY + reason);
               } else {
                  sender.sendMessage(ChatColor.RED + KarmicJail.TAG + " Player '" + ChatColor.AQUA + name + ChatColor.RED + "' not jailed.");
               }
            } else {
               sender.sendMessage(ChatColor.RED + "Missing name");
               sender.sendMessage(ChatColor.RED + "/jtime <player> <reason>");
            }
         }
         com = true;
      } else {
         // TODO don't think this is necessary at all...
         // Honestly, this section probably is for unrecognized commands
         if(!perm.has(sender, "KarmicJail.jail"))
            com = true;
         if(!perm.has(sender, "KarmicJail.unjail"))
            com = true;
         if(!perm.has(sender, "KarmicJail.setjail"))
            com = true;
         if(!perm.has(sender, "KarmicJail.jailstatus"))
            com = true;
      }

      if(com) {
         if(config.debugTime) {
            this.debugTime(sender, dTime);
         }
         return true;
      }
      return false;
   }

   /**
    * Method that messages how long a command took to complete
    * 
    * @param sender
    *           of command
    * @param time
    *           when command was issued
    */
   private void debugTime(CommandSender sender, long time) {
      time = System.nanoTime() - time;
      sender.sendMessage("[Debug]" + KarmicJail.TAG + "Process time: " + time);
   }

   public void removeFromCache(String name) {
      cache.remove(name);
   }

   public void addToCache(String name, PrisonerInfo info) {
      cache.put(name, info);
   }

   public void showVersion(CommandSender sender) {
      RootConfig config = plugin.getModuleForClass(RootConfig.class);
      // Version
      sender.sendMessage(ChatColor.BLUE + "==================" + ChatColor.GREEN + "KarmicJail v" + plugin.getDescription().getVersion()
            + ChatColor.BLUE + "=================");
      sender.sendMessage(ChatColor.GREEN + "Coded by Mitsugaru" + ChatColor.WHITE + " - " + ChatColor.AQUA + "Fork of imjake9's SimpleJail project");
      sender.sendMessage(ChatColor.WHITE + "Shout outs: " + ChatColor.GOLD + "@khanjal");
      sender.sendMessage(ChatColor.BLUE + bar + ChatColor.GRAY + "Config" + ChatColor.BLUE + bar);
      DecimalFormat twoDForm = new DecimalFormat("#.##");
      sender.sendMessage(ChatColor.LIGHT_PURPLE + "Jail: " + ChatColor.GRAY + config.jailLoc.getWorld().getName() + ChatColor.BLUE + " : ("
            + ChatColor.WHITE + Double.valueOf(twoDForm.format(config.jailLoc.getX())) + ChatColor.BLUE + ", " + ChatColor.WHITE
            + Double.valueOf(twoDForm.format(config.jailLoc.getY())) + ChatColor.BLUE + ", " + ChatColor.WHITE
            + Double.valueOf(twoDForm.format(config.jailLoc.getZ())) + ChatColor.BLUE + ")");
      sender.sendMessage(ChatColor.LIGHT_PURPLE + "UnJail: " + ChatColor.GRAY + config.unjailLoc.getWorld().getName() + ChatColor.BLUE + " : ("
            + ChatColor.WHITE + Double.valueOf(twoDForm.format(config.unjailLoc.getX())) + ChatColor.BLUE + ", " + ChatColor.WHITE
            + Double.valueOf(twoDForm.format(config.unjailLoc.getY())) + ChatColor.BLUE + ", " + ChatColor.WHITE
            + Double.valueOf(twoDForm.format(config.unjailLoc.getZ())) + ChatColor.BLUE + ")");
      // TODO show other config options here too
   }

   public void showHelp(CommandSender sender) {
      PermCheck perm = plugin.getModuleForClass(PermCheck.class);
      sender.sendMessage(ChatColor.BLUE + "=====" + ChatColor.GREEN + "KarmicJail" + ChatColor.BLUE + "=====");
      if(perm.has(sender, PermissionNode.JAIL)) {
         sender.sendMessage(ChatColor.GREEN + "/jail " + ChatColor.AQUA + "<player> " + ChatColor.LIGHT_PURPLE + "[player2]... [time] [reason]"
               + ChatColor.YELLOW + " : Jails player(s)");
         sender.sendMessage(ChatColor.YELLOW + "Note - Names auto-complete if player is online. Alias: /j");
         sender.sendMessage(ChatColor.GREEN + "/jailtime" + ChatColor.AQUA + " <player> <time>" + ChatColor.YELLOW
               + " : Sets time for jailed player. Alias: /jtime");
         sender.sendMessage(ChatColor.GREEN + "/jailreason" + ChatColor.AQUA + " <player> " + ChatColor.LIGHT_PURPLE + "[reason]" + ChatColor.YELLOW
               + " : Sets jail reason for player. Alias: /jreason");
      }
      if(perm.has(sender, PermissionNode.UNJAIL)) {
         sender.sendMessage(ChatColor.GREEN + "/unjail" + ChatColor.AQUA + " <player>" + ChatColor.YELLOW + " : Unjail player");
      }
      if(perm.has(sender, PermissionNode.MUTE)) {
         sender.sendMessage(ChatColor.GREEN + "/jailmute" + ChatColor.AQUA + " <player>" + ChatColor.YELLOW
               + " : Toggle mute for a player. Alias: /jmute");
      }
      if(perm.has(sender, PermissionNode.LIST)) {
         sender.sendMessage(ChatColor.GREEN + "/jaillist" + ChatColor.LIGHT_PURPLE + " [page]" + ChatColor.YELLOW
               + " : List jailed players. Alias: /jlist");
         sender.sendMessage(ChatColor.GREEN + "/jailprev" + ChatColor.YELLOW + " : Previous page. Alias: /jprev");
         sender.sendMessage(ChatColor.GREEN + "/jailnext" + ChatColor.YELLOW + " : Next page. Alias: /jnext");
      }
      if(perm.has(sender, PermissionNode.HISTORY_VIEW)) {
         sender.sendMessage(ChatColor.GREEN + "/jailhistory" + ChatColor.LIGHT_PURPLE + " [args]" + ChatColor.YELLOW
               + " : Jail history command. Alias: /jhistory");
      }
      if(perm.has(sender, PermissionNode.INVENTORY_VIEW)) {
         sender.sendMessage(ChatColor.GREEN + "/jailinv" + ChatColor.AQUA + " <player>" + ChatColor.YELLOW
               + " : Open inventory of jailed player. Alias: /jinv");
      }
      if(perm.has(sender, PermissionNode.WARP_LAST)) {
         sender.sendMessage(ChatColor.GREEN + "/jaillast" + ChatColor.AQUA + " <player>" + ChatColor.YELLOW
               + " : Warp to last known postion of player");
      }
      if(perm.has(sender, PermissionNode.SETJAIL)) {
         sender.sendMessage(ChatColor.GREEN + "/setjail" + ChatColor.LIGHT_PURPLE + " [x] [y] [z] [world]" + ChatColor.YELLOW
               + " : Set jail teleport to current pos or given pos");
         sender.sendMessage(ChatColor.GREEN + "/setunjail" + ChatColor.LIGHT_PURPLE + " [x] [y] [z] [world]" + ChatColor.YELLOW
               + " : Set unjail teleport to current pos or given pos");
      }
      if(perm.has(sender, PermissionNode.JAILSTATUS)) {
         sender.sendMessage(ChatColor.GREEN + "/jailstatus" + ChatColor.LIGHT_PURPLE + " [player]" + ChatColor.YELLOW
               + " : Get jail status. Alias: /jstatus");
      }
      sender.sendMessage(ChatColor.GREEN + "/jailversion" + ChatColor.YELLOW + " : Plugin version and config info. Alias: /jversion");
   }

   /**
    * Lists the players in jail
    * 
    * @param sender
    *           of command
    * @param Page
    *           adjustment
    */
   private void listJailed(CommandSender sender, int pageAdjust) {
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

   @Override
   public boolean noArgs(CommandSender sender, Command command, String label) {
      showHelp(sender);
      return true;
   }

   @Override
   public boolean unknownCommand(CommandSender sender, Command command, String label, String[] args) {
      // TODO Auto-generated method stub
      return false;
   }
}
