/**
 * Jail plugin custom tailed for the needs of Mine-RP.
 * Built upon the SimpleJail project, created by imjake9.
 * https://github.com/imjake9/SimpleJail
 *
 * @author imjake9
 * @author Mitsugaru
 */

package com.mitsugaru.karmicjail;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.logging.Logger;

import lib.PatPeter.SQLibrary.SQLite;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class KarmicJail extends JavaPlugin {

	public static final Logger log = Logger.getLogger("Minecraft");
	public static final String prefix = "[KarmicJail]";
	private static final String bar = "======================";
	private static final long minutesToTicks = 1200;
	public ConsoleCommandSender console;
	private Location jailLoc;
	private Location unjailLoc;
	private String jailGroup;
	private Listener listener;
	private Permission perm;
	private SQLite database;
	private Map<String, JailTask> threads = new HashMap<String, JailTask>();
	private final Map<String, Integer> page = new HashMap<String, Integer>();
	private Map<String, PrisonerInfo> cache = new HashMap<String, PrisonerInfo>();
	private long time;
	private boolean debugTime;
	private int limit = 8;

	@Override
	public void onDisable() {
		// Stop all running threads
		log.info(prefix + " Stopping all jail threads...");
		for (JailTask task : threads.values())
		{
			task.stop();
		}
		// Disconnect from sql database
		if (database.checkConnection())
		{
			// Close connection
			database.close();
		}
		log.info(prefix + " " + this.getDescription().getName() + " v"
				+ this.getDescription().getVersion() + " disabled.");
	}

	@Override
	public void onEnable() {
		// Get console:
		console = this.getServer().getConsoleSender();

		// Load configuration:
		this.loadConfig();

		// Grab database
		database = new SQLite(log, prefix, "jail", this.getDataFolder()
				.getAbsolutePath());
		if (!database.checkTable("jailed"))
		{
			log.info(prefix + " Created jailed table");
			// Jail table
			database.createTable("CREATE TABLE `jailed` (`playername` varchar(32) NOT NULL, `status` TEXT, `time` REAL, `groups` TEXT, `jailer` varchar(32), `date` TEXT, `reason` TEXT);");
		}

		// Get permissions plugin:
		this.setupPermissions();

		// Setup listner
		listener = new Listener(this);
		this.getServer()
				.getPluginManager()
				.registerEvent(Event.Type.PLAYER_RESPAWN, listener,
						Priority.High, this);
		this.getServer()
				.getPluginManager()
				.registerEvent(Event.Type.PLAYER_JOIN, listener,
						Priority.Normal, this);
		this.getServer()
				.getPluginManager()
				.registerEvent(Event.Type.PLAYER_QUIT, listener,
						Priority.Monitor, this);

		log.info(prefix + " " + this.getDescription().getName() + " v"
				+ this.getDescription().getVersion() + " enabled.");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd,
			String commandLabel, String[] args) {
		boolean com = false;
		if (debugTime)
		{
			time = System.nanoTime();
		}
		if (commandLabel.equalsIgnoreCase("jail")
				|| commandLabel.equalsIgnoreCase("j"))
		{
			if (!perm.has(sender, "KarmicJail.jail"))
			{
				sender.sendMessage(ChatColor.RED
						+ "Lack Permission: KarmicJail.jail");
				com = true;
			}

			boolean timed = false;
			boolean done = false;
			int time = 0;
			String reason = "";
			final Vector<String> players = new Vector<String>();
			for (int i = 0; i < args.length; i++)
			{
				if (!done)
				{
					try
					{
						// Attempt to grab time
						time = Integer.parseInt(args[i]);
						if (time > 0)
						{
							timed = true;
						}
						done = true;
					}
					catch (NumberFormatException e)
					{
						// Attempt to grab name and add to list
						String name = this.expandName(args[i]);
						if (name != null)
						{
							players.add(name);
						}
						else
						{
							players.add(args[i]);
						}
					}
				}
				else
				{
					// attempt to grab reason if it exists
					reason += args[i];
				}
			}
			if(players.isEmpty())
			{
				sender.sendMessage(ChatColor.RED
						+ "Missing paramters");
				sender.sendMessage(ChatColor.RED
						+ "/j <player> [player2] ... [time] [reason]");
			}
			for (String name : players)
			{
				this.jailPlayer(sender, name, reason, time, timed);
			}
			com = true;
		}
		else if (commandLabel.equalsIgnoreCase("unjail") && args.length == 1)
		{
			if (!perm.has(sender, "KarmicJail.unjail"))
			{
				sender.sendMessage(ChatColor.RED
						+ "Lack Permission: KarmicJail.unjail");
			}
			else
			{
				this.unjailPlayer(sender, args);
			}
			com = true;
		}
		else if (commandLabel.equalsIgnoreCase("setjail")
				&& (args.length == 0 || args.length == 4))
		{
			if (!perm.has(sender, "KarmicJail.setjail"))
			{
				sender.sendMessage(ChatColor.RED
						+ "Lack Permission: KarmicJail.setjail");
			}
			else
			{
				this.setJail(sender, args);
			}
			com = true;
		}
		else if (commandLabel.equalsIgnoreCase("setunjail")
				&& (args.length == 0 || args.length == 4))
		{
			if (!perm.has(sender, "KarmicJail.setjail"))
			{
				sender.sendMessage(ChatColor.RED
						+ "Lack Permission: KarmicJail.setjail");
			}
			else
			{
				this.setUnjail(sender, args);
			}
			com = true;
		}
		else if ((commandLabel.equalsIgnoreCase("jailstatus") || commandLabel
				.equalsIgnoreCase("jstatus")) && args.length <= 1)
		{
			if (!perm.has(sender, "KarmicJail.jailstatus"))
			{
				sender.sendMessage(ChatColor.RED
						+ "Lack Permission: KarmicJail.jailstatus");
			}
			else
			{
				this.jailStatus(sender, args);
			}
			com = true;
		}
		else if (commandLabel.equalsIgnoreCase("jailversion")
				|| commandLabel.equalsIgnoreCase("jversion"))
		{
			// Version
			sender.sendMessage(ChatColor.BLUE + bar + "==========");
			sender.sendMessage(ChatColor.GREEN + "KarmicJail v"
					+ this.getDescription().getVersion());
			sender.sendMessage(ChatColor.GREEN + "Coded by Mitsugaru");
			sender.sendMessage(ChatColor.AQUA
					+ "Fork of imjake9's SimpleJail project");
			sender.sendMessage(ChatColor.BLUE + "==============" + ChatColor.GRAY
					+ "Config" + ChatColor.BLUE + "=============");
			DecimalFormat twoDForm = new DecimalFormat("#.##");
			sender.sendMessage(ChatColor.LIGHT_PURPLE + "Jail: " + ChatColor.GRAY
					+ jailLoc.getWorld().getName() + ChatColor.BLUE + ":("
					+ ChatColor.GOLD
					+ Double.valueOf(twoDForm.format(jailLoc.getX()))
					+ ChatColor.BLUE + ","+ ChatColor.GOLD
					+ Double.valueOf(twoDForm.format(jailLoc.getY()))
					+ ChatColor.BLUE + ","+ ChatColor.GOLD
					+ Double.valueOf(twoDForm.format(jailLoc.getZ()))
					+ ChatColor.BLUE + ")");
			sender.sendMessage(ChatColor.LIGHT_PURPLE + "UnJail: " + ChatColor.GRAY
					+ unjailLoc.getWorld().getName() + ChatColor.BLUE + ":("
					+ ChatColor.GOLD
					+ Double.valueOf(twoDForm.format(unjailLoc.getX()))
					+ ChatColor.BLUE + ","+ ChatColor.GOLD
					+ Double.valueOf(twoDForm.format(unjailLoc.getY()))
					+ ChatColor.BLUE + ","+ ChatColor.GOLD
					+ Double.valueOf(twoDForm.format(unjailLoc.getZ()))
					+ ChatColor.BLUE + ")");
			com = true;
		}
		else if (commandLabel.equalsIgnoreCase("jailhelp")
				|| commandLabel.equalsIgnoreCase("jhelp"))
		{
			sender.sendMessage(ChatColor.BLUE + "=====" + ChatColor.GREEN
					+ "KarmicJail" + ChatColor.BLUE + "=====");
			if (perm.has(sender, "KarmicJail.jail"))
			{
				sender.sendMessage(ChatColor.GREEN + "/jail " + ChatColor.AQUA + "<player> " + ChatColor.LIGHT_PURPLE + "[player2]... [time] [reason]" + ChatColor.YELLOW
						+ " : Jails player(s)");
				sender.sendMessage(ChatColor.YELLOW + "Note - Names auto-complete if player is online. Alias: /j");
			}
			if (perm.has(sender, "KarmicJail.unjail"))
			{
				sender.sendMessage(ChatColor.GREEN + "/unjail" + ChatColor.AQUA + " <player>" + ChatColor.YELLOW
						+ " : Unjail player");
			}
			if (perm.has(sender, "KarmicJail.list"))
			{
				sender.sendMessage(ChatColor.GREEN + "/jaillist" + ChatColor.LIGHT_PURPLE + " [page]" + ChatColor.YELLOW
						+ " : List jailed players. Alias: /jlist");
				sender.sendMessage(ChatColor.GREEN + "/jailprev" + ChatColor.YELLOW
						+ " : Previous page. Alias: /jprev");
				sender.sendMessage(ChatColor.GREEN + "/jailnext" + ChatColor.YELLOW
						+ " : Next page. Alias: /jnext");
			}
			if (perm.has(sender, "KarmicJail.setjail"))
			{
				sender.sendMessage(ChatColor.GREEN + "/setjail" + ChatColor.LIGHT_PURPLE + " [x] [y] [z] [world]" + ChatColor.YELLOW
						+ " : Set jail teleport to current pos or given pos");
				sender.sendMessage(ChatColor.GREEN + "/setunjail" + ChatColor.LIGHT_PURPLE + " [x] [y] [z] [world]" + ChatColor.YELLOW
						+ " : Set unjail teleport to current pos or given pos");
			}
			if (perm.has(sender, "KarmicJail.jailstatus"))
			{
				sender.sendMessage(ChatColor.GREEN + "/jailstatus" + ChatColor.LIGHT_PURPLE + " [player]" + ChatColor.YELLOW
						+ " : Get jail status. Alias: /jstatus");
			}
			sender.sendMessage(ChatColor.GREEN + "/jailversion" + ChatColor.YELLOW
					+ " : Plugin version and config info. Alias: /jversion");
			com = true;
		}
		else if (commandLabel.equalsIgnoreCase("jaillist")
				|| commandLabel.equalsIgnoreCase("jlist"))
		{
			if (!perm.has(sender, "KarmicJail.list"))
			{
				sender.sendMessage(ChatColor.RED
						+ "Lack Permission: KarmicJail.list");
			}
			else
			{
				// list jailed people
				if (args.length > 0)
				{
					// If they provided a page number
					try
					{
						// Attempt to parse argument for page number
						int pageNum = Integer.parseInt(args[0]);
						// Set current page to given number
						page.put(sender.getName(), pageNum - 1);
						// Show page if possible
						this.listJailed(sender, 0);
					}
					catch (NumberFormatException e)
					{
						sender.sendMessage(ChatColor.YELLOW + prefix
								+ " Invalid integer for page number");
					}
				}
				else
				{
					// List with current page
					this.listJailed(sender, 0);
				}
			}
			com = true;
		}
		else if (commandLabel.equals("jprev"))
		{
			if (!perm.has(sender, "KarmicJail.list"))
			{
				sender.sendMessage(ChatColor.RED
						+ "Lack Permission: KarmicJail.list");
			}
			else
			{
				// List, with previous page
				this.listJailed(sender, -1);
			}
			com = true;
		}
		// Next page of item pool
		else if (commandLabel.equals("jnext"))
		{
			if (!perm.has(sender, "KarmicJail.list"))
			{
				sender.sendMessage(ChatColor.RED
						+ "Lack Permission: KarmicJail.list");
			}
			else
			{
				// List with next page
				this.listJailed(sender, 1);
			}
			com = true;
		}
		else
		{
			if (!perm.has(sender, "KarmicJail.jail"))
				com = true;
			if (!perm.has(sender, "KarmicJail.unjail"))
				com = true;
			if (!perm.has(sender, "KarmicJail.setjail"))
				com = true;
			if (!perm.has(sender, "KarmicJail.jailstatus"))
				com = true;
		}

		if (com)
		{
			if (debugTime)
			{
				this.debugTime(sender, time);
			}
			return true;
		}
		return false;
	}

	private void debugTime(CommandSender sender, long time) {
		time = System.nanoTime() - time;
		sender.sendMessage("[Debug]" + prefix + "Process time: " + time);
	}

	public void jailPlayer(CommandSender sender, String name, String reason,
			int minutes, boolean timed) {
		// Check if player is already jailed:
		if (playerIsJailed(name) || playerIsPendingJail(name))
		{
			sender.sendMessage(ChatColor.RED
					+ "That player is already in jail!");
			return;
		}

		if(!playerInDatabase(name))
		{
			sender.sendMessage(ChatColor.YELLOW
					+ " Player has never been on server! Adding to database...");
			//Player has never been on server, adding to list
			addPlayerToDatabase(name);
		}

		// Save groups
		this.savePlayerGroups(name);
		// Remove all other groups
		this.removePlayerGroups(name);
		// Add to jail group
		perm.playerAddGroup(jailLoc.getWorld(), name, jailGroup);

		// Grab duration
		long duration = 0;
		if (timed)
		{
			duration = minutes * minutesToTicks;
			this.updatePlayerTime(name, duration);
			// Create thread
			threads.put(name, new JailTask(this, name, duration));
		}

		this.setPlayerStatus(JailStatus.PENDINGJAIL, name);

		// Notify player and command sender
		Player player = this.getServer().getPlayer(name);
		if (player != null)
		{
			// Move to jail
			player.teleport(jailLoc);
			this.setPlayerStatus(JailStatus.JAILED, name);
			if (!timed)
			{
				player.sendMessage(ChatColor.RED + "Jailed by "
						+ ChatColor.AQUA + sender.getName() + ChatColor.RED
						+ " for: " + ChatColor.GRAY + reason);
			}
			else
			{
				player.sendMessage(ChatColor.RED + "Jailed by "
						+ ChatColor.AQUA + sender.getName() + ChatColor.RED
						+ " for: " + ChatColor.GRAY + reason);
				player.sendMessage(ChatColor.AQUA + "Time in jail: "
						+ this.prettifyMinutes(minutes));
			}
		}
		String date = new Date().toString();
		database.standardQuery("UPDATE jailed SET jailer='" + sender.getName()
				+ "',date='" + date + "',reason='" + reason
				+ "' WHERE playername='" + name + "';");
		sender.sendMessage(ChatColor.RED + name + ChatColor.AQUA
				+ " sent to jail.");
		cache.put(name, new PrisonerInfo(name, sender.getName(), date, reason,
				duration));
	}

	private void savePlayerGroups(String name) {
		StringBuilder sb = new StringBuilder();
		for (String s : this.getGroups(name))
		{
			sb.append(s + "&");
		}
		sb.deleteCharAt(sb.length() - 1);
		this.database.standardQuery("UPDATE jailed SET groups='"
				+ sb.toString() + "' WHERE playername='" + name + "';");
	}

	private void removePlayerGroups(String name) {
		for (World w : this.getServer().getWorlds())
		{
			String[] groups = perm.getPlayerGroups(w, name);
			for (String group : groups)
			{
				perm.playerRemoveGroup(w, name, group);
			}
		}
	}

	public void unjailPlayer(CommandSender sender, String[] args,
			boolean fromTempJail) {
		String name = args[0];

		// Check if player is in jail:
		if (!playerIsJailed(name) && !playerIsPendingJail(name))
		{
			sender.sendMessage(ChatColor.RED + "That player is not in jail!");
			return;
		}

		// Grab player if on server
		Player player = this.getServer().getPlayer(name);
		// Remove jail group
		perm.playerRemoveGroup(jailLoc.getWorld(), name, jailGroup);
		// Return previous groups
		this.returnGroups(name);

		// Clear other columns
		database.standardQuery("UPDATE jailed SET time='',jailer='',date='',reason='' WHERE playername='"
				+ name + "';");
		cache.remove(name);
		// Check if player is offline:
		if (player == null)
		{
			this.setPlayerStatus(JailStatus.PENDINGFREE, name);
			sender.sendMessage(ChatColor.GOLD + name + ChatColor.AQUA
					+ " will be released from jail on login.");
			return;
		}

		// Move player out of jail:
		player.teleport(unjailLoc);
		this.setPlayerStatus(JailStatus.FREED, name);

		// Remove task
		if (threads.containsKey(name))
		{
			int id = threads.get(name).getId();
			if (id != -1)
			{
				this.getServer().getScheduler().cancelTask(id);
			}
			this.removeTask(name);
		}
		player.sendMessage(ChatColor.AQUA + "You have been released from jail!");
		if (fromTempJail)
			sender.sendMessage(ChatColor.AQUA + player.getName()
					+ " auto-unjailed.");
		else
			sender.sendMessage(ChatColor.GOLD + name + ChatColor.AQUA
					+ " removed from jail.");
	}

	public void returnGroups(String name) {
		try
		{
			ResultSet rs = this.database
					.select("SELECT * FROM jailed WHERE playername='" + name
							+ "';");
			if (rs.next())
			{
				String groups = rs.getString("groups");
				String[] cut = groups.split("&");
				for (String group : cut)
				{
					String[] split = group.split("!");
					perm.playerAddGroup(split[1], name, split[0]);
				}
			}
			rs.close();
		}
		catch (SQLException e)
		{
			log.warning(prefix + " SQL Exception");
			e.printStackTrace();
		}
	}

	public void unjailPlayer(CommandSender sender, String[] args) {
		this.unjailPlayer(sender, args, false);
	}

	public void setJail(CommandSender sender, String[] args) {
		if (!(sender instanceof Player) && args.length != 4)
		{
			sender.sendMessage(ChatColor.RED + "Only players can use that.");
			return;
		}
		if (args.length == 0)
		{
			Player player = (Player) sender;
			jailLoc = player.getLocation();
		}
		else
		{
			if (!(new Scanner(args[0]).hasNextInt())
					|| !(new Scanner(args[1]).hasNextInt())
					|| !(new Scanner(args[2]).hasNextInt()))
			{
				sender.sendMessage(ChatColor.RED + "Invalid coordinate.");
				return;
			}
			jailLoc = new Location(this.getServer().getWorld(args[3]),
					Integer.parseInt(args[0]), Integer.parseInt(args[1]),
					Integer.parseInt(args[2]));
		}

		YamlConfiguration config = (YamlConfiguration) this.getConfig();
		config.set("jail.x", (int) jailLoc.getX());
		config.set("jail.y", (int) jailLoc.getY());
		config.set("jail.z", (int) jailLoc.getZ());
		config.set("jail.world", jailLoc.getWorld().getName());

		this.saveConfig();

		sender.sendMessage(ChatColor.AQUA + "Jail point saved.");
	}

	public void setUnjail(CommandSender sender, String[] args) {
		if (!(sender instanceof Player) && args.length != 4)
		{
			sender.sendMessage(ChatColor.RED + "Only players can use that.");
			return;
		}
		if (args.length == 0)
		{
			Player player = (Player) sender;
			unjailLoc = player.getLocation();
		}
		else
		{
			if (!(new Scanner(args[0]).hasNextInt())
					|| !(new Scanner(args[1]).hasNextInt())
					|| !(new Scanner(args[2]).hasNextInt()))
			{
				sender.sendMessage(ChatColor.RED + "Invalid coordinate.");
				return;
			}
			unjailLoc = new Location(this.getServer().getWorld(args[3]),
					Integer.parseInt(args[0]), Integer.parseInt(args[1]),
					Integer.parseInt(args[2]));
		}

		YamlConfiguration config = (YamlConfiguration) this.getConfig();
		config.set("unjail.x", (int) unjailLoc.getX());
		config.set("unjail.y", (int) unjailLoc.getY());
		config.set("unjail.z", (int) unjailLoc.getZ());
		config.set("unjail.world", unjailLoc.getWorld().getName());

		this.saveConfig();

		sender.sendMessage(ChatColor.AQUA + "Unjail point saved.");
	}

	public void jailStatus(CommandSender sender, String[] args) {
		if (!(sender instanceof Player) && args.length == 0)
		{
			sender.sendMessage(ChatColor.RED + "Must specify a player.");
			return;
		}
		Player player = (args.length == 0) ? (Player) sender : this.getServer()
				.getPlayer(args[0]);
		String name = "";
		if (player == null)
		{
			name = args[0];
		}
		else
		{
			name = player.getName();
		}

		String reason = this.getJailReason(name);
		if (!this.playerIsTempJailed(name))
		{
			if (!this.playerIsJailed(name) && !this.playerIsPendingJail(name))
			{
				if (args.length == 0)
					sender.sendMessage(ChatColor.RED + "You are not jailed.");
				else
					sender.sendMessage(ChatColor.AQUA + name + ChatColor.RED
							+ " is not jailed.");
			}
			else
			{
				if (!reason.equals(""))
				{
					if (args.length == 0)
						sender.sendMessage(ChatColor.RED + "Jailed on "
								+ ChatColor.GREEN + this.getJailDate(name)
								+ ChatColor.RED + " by " + ChatColor.GOLD
								+ this.getJailer(name) + ChatColor.RED
								+ " for " + ChatColor.GRAY + reason);
					else
						sender.sendMessage(ChatColor.AQUA + name
								+ ChatColor.RED + " was jailed on "
								+ ChatColor.GREEN + this.getJailDate(name)
								+ ChatColor.RED + " by " + ChatColor.GOLD
								+ this.getJailer(name) + ChatColor.RED
								+ " for " + ChatColor.GRAY + reason);
				}
				else
				{
					if (args.length == 0)
						sender.sendMessage(ChatColor.RED + "Jailed on "
								+ ChatColor.GREEN + this.getJailDate(name)
								+ ChatColor.RED + " by " + ChatColor.GOLD
								+ this.getJailer(name));
					else
						sender.sendMessage(ChatColor.AQUA + name
								+ ChatColor.RED + " is jailed on"
								+ ChatColor.GREEN + this.getJailDate(name)
								+ ChatColor.RED + " by " + ChatColor.GOLD
								+ this.getJailer(name));
				}
			}
			return;
		}
		if (!reason.equals(""))
		{
			if (args.length == 0)
				sender.sendMessage(ChatColor.RED + "Jailed on "
						+ ChatColor.GREEN + this.getJailDate(name)
						+ ChatColor.RED + " by " + ChatColor.GOLD
						+ this.getJailer(name) + ChatColor.RED + " for "
						+ ChatColor.GRAY + reason);
			else
				sender.sendMessage(ChatColor.AQUA + name + ChatColor.RED
						+ " is jailed on" + ChatColor.GREEN
						+ this.getJailDate(name) + ChatColor.RED + " by "
						+ ChatColor.GOLD + this.getJailer(name) + ChatColor.RED
						+ " for " + ChatColor.GRAY + reason);
			int minutes = (int) ((this.getTempJailTime(player) / minutesToTicks));
			if (player == null)
			{
				sender.sendMessage(ChatColor.AQUA + "Remaining jail time: "
						+ this.prettifyMinutes(minutes));
			}
			else
			{
				if (threads.containsKey(name))
				{
					minutes = (int) (threads.get(name).remainingTime() / minutesToTicks);
					sender.sendMessage(ChatColor.AQUA + "Remaining jail time: "
							+ this.prettifyMinutes(minutes));
				}
			}

		}
		else
		{
			if (args.length == 0)
				sender.sendMessage(ChatColor.RED + "Jailed on "
						+ ChatColor.GREEN + this.getJailDate(name)
						+ ChatColor.RED + " by " + ChatColor.GOLD
						+ this.getJailer(name));
			else
				sender.sendMessage(ChatColor.AQUA + name + ChatColor.RED
						+ " is jailed on" + ChatColor.GREEN
						+ this.getJailDate(name) + ChatColor.RED + " by "
						+ ChatColor.GOLD + this.getJailer(name));
			int minutes = (int) ((this.getTempJailTime(player) / minutesToTicks));
			sender.sendMessage(ChatColor.AQUA + "Remaining jail time: "
					+ this.prettifyMinutes(minutes));
			if (player == null)
			{
				sender.sendMessage(ChatColor.AQUA + "Remaining jail time: "
						+ this.prettifyMinutes(minutes));
			}
			else
			{
				if (threads.containsKey(name))
				{
					minutes = (int) (threads.get(name).remainingTime() / minutesToTicks);
					sender.sendMessage(ChatColor.AQUA + "Remaining jail time: "
							+ this.prettifyMinutes(minutes));
				}
			}
		}
	}

	public void loadConfig() {
		// Init config files:
		ConfigurationSection config = this.getConfig();
		final Map<String, Object> defaults = new HashMap<String, Object>();
		defaults.put("jailgroup", "Jailed");
		defaults.put("jail.world", this.getServer().getWorlds().get(0)
				.getName());
		defaults.put("jail.x", 0);
		defaults.put("jail.y", 0);
		defaults.put("jail.z", 0);
		defaults.put("unjail.world", this.getServer().getWorlds().get(0)
				.getName());
		defaults.put("unjail.x", 0);
		defaults.put("unjail.y", 0);
		defaults.put("unjail.z", 0);

		// Insert defaults into config file if they're not present
		for (final Entry<String, Object> e : defaults.entrySet())
		{
			if (!config.contains(e.getKey()))
			{
				config.set(e.getKey(), e.getValue());
			}
		}
		// Save config
		this.saveConfig();

		// Load variables from config
		jailLoc = new Location(this.getServer().getWorld(
				config.getString("jail.world", this.getServer().getWorlds()
						.get(0).getName())), config.getInt("jail.x", 0),
				config.getInt("jail.y", 0), config.getInt("jail.z", 0));
		unjailLoc = new Location(this.getServer().getWorld(
				config.getString("unjail.world", this.getServer().getWorlds()
						.get(0).getName())), config.getInt("unjail.x", 0),
				config.getInt("unjail.y", 0), config.getInt("unjail.z", 0));
		jailGroup = config.getString("jailgroup", "Jailed");
		debugTime = config.getBoolean("debugTime", false);

	}

	private void setupPermissions() {
		// Make vault optional
		RegisteredServiceProvider<Permission> permissionProvider = this
				.getServer()
				.getServicesManager()
				.getRegistration(net.milkbowl.vault.permission.Permission.class);
		if (permissionProvider != null)
		{
			perm = permissionProvider.getProvider();
		}
		else
		{
			log.info(prefix + " ERROR: Permissions plugin not detected.");
			this.getServer().getPluginManager().disablePlugin(this);
		}
	}

	public Location getJailLocation() {
		return jailLoc;
	}

	public Location getUnjailLocation() {
		return unjailLoc;
	}

	private String getJailer(String name) {
		String jailer = "NOBODY";
		try
		{
			ResultSet rs = database
					.select("SELECT * FROM jailed WHERE playername='" + name
							+ "';");
			if (rs.next())
			{
				jailer = rs.getString("jailer");
			}
			rs.close();
		}
		catch (SQLException e)
		{
			log.warning(prefix + " SQL Exception");
			e.printStackTrace();
		}
		return jailer;
	}

	private String getJailDate(String name) {
		String date = "";
		try
		{
			ResultSet rs = database
					.select("SELECT * FROM jailed WHERE playername='" + name
							+ "';");
			if (rs.next())
			{
				date = rs.getString("date");
			}
			rs.close();
		}
		catch (SQLException e)
		{
			log.warning(prefix + " SQL Exception");
			e.printStackTrace();
		}
		return date;
	}

	public boolean playerIsPendingJail(String player) {
		boolean jailed = false;
		try
		{
			ResultSet rs = database
					.select("SELECT * FROM jailed WHERE playername='" + player
							+ "';");
			if (rs.next())
			{
				final String status = rs.getString("status");
				if (status.equals("" + JailStatus.PENDINGJAIL))
				{
					jailed = true;
				}
			}
			rs.close();
		}
		catch (SQLException e)
		{
			log.warning(prefix + " SQL Exception");
			e.printStackTrace();
		}
		return jailed;
	}

	public boolean playerIsJailed(String player) {
		boolean jailed = false;
		try
		{
			ResultSet rs = database
					.select("SELECT * FROM jailed WHERE playername='" + player
							+ "';");
			if (rs.next())
			{
				final String status = rs.getString("status");
				if (status.equals("" + JailStatus.JAILED))
				{
					jailed = true;
				}
			}
			rs.close();
		}
		catch (SQLException e)
		{
			log.warning(prefix + " SQL Exception");
			e.printStackTrace();
		}
		return jailed;
	}

	public boolean playerIsTempJailed(String player) {
		double time = 0;
		try
		{
			ResultSet rs = database
					.select("SELECT * FROM jailed WHERE playername='" + player
							+ "';");
			if (rs.next())
			{
				time = rs.getDouble("time");
			}
			rs.close();
		}
		catch (SQLException e)
		{
			log.warning(prefix + " SQL Exception");
			e.printStackTrace();
		}
		if (time > 0)
		{
			return true;
		}
		return false;
	}

	private void listJailed(CommandSender sender, int pageAdjust) {
		// Update cache of jailed players
		try
		{
			ResultSet rs = database
					.select("SELECT * FROM jailed WHERE status='"
							+ JailStatus.JAILED + "' OR status='"
							+ JailStatus.PENDINGJAIL + "';");
			if (rs.next())
			{
				do
				{
					String name = rs.getString("playername");
					if (!cache.containsKey(name))
					{
						cache.put(
								name,
								new PrisonerInfo(name, rs.getString("jailer"),
										rs.getString("date"), rs
												.getString("reason"), rs
												.getLong("time")));
					}
					else
					{
						if (threads.containsKey(name))
						{
							// Update the time if necessary
							cache.get(name).updateTime(
									threads.get(name).remainingTime());
						}
					}
				}
				while (rs.next());
			}
			rs.close();
		}
		catch (SQLException e)
		{
			log.warning(prefix + " SQL Exception");
			e.printStackTrace();
		}
		if (cache.isEmpty())
		{
			sender.sendMessage(ChatColor.RED + prefix + " No jailed players");
			return;
		}
		if (!page.containsKey(sender.getName()))
		{
			page.put(sender.getName(), 0);
		}
		else
		{
			if (pageAdjust != 0)
			{
				int adj = page.get(sender.getName()).intValue() + pageAdjust;
				page.put(sender.getName(), adj);
			}
		}
		PrisonerInfo[] array = cache.values().toArray(new PrisonerInfo[0]);
		boolean valid = true;
		// Caluclate amount of pages
		int num = array.length / 8;
		double rem = (double) array.length % (double) limit;
		if (rem != 0)
		{
			num++;
		}
		if (page.get(sender.getName()).intValue() < 0)
		{
			// They tried to use /ks prev when they're on page 0
			sender.sendMessage(ChatColor.YELLOW + prefix
					+ " Page does not exist");
			// reset their current page back to 0
			page.put(sender.getName(), 0);
			valid = false;
		}
		else if ((page.get(sender.getName()).intValue()) * limit > array.length)
		{
			// They tried to use /ks next at the end of the list
			sender.sendMessage(ChatColor.YELLOW + prefix
					+ " Page does not exist");
			// Revert to last page
			page.put(sender.getName(), num - 1);
			valid = false;
		}
		if (valid)
		{
			// Header with amount of pages
			sender.sendMessage(ChatColor.BLUE + "===" + ChatColor.GRAY
					+ "Jailed" + ChatColor.BLUE + "===" + ChatColor.GRAY
					+ "Page: " + ((page.get(sender.getName()).intValue()) + 1)
					+ ChatColor.BLUE + " of " + ChatColor.GRAY + num
					+ ChatColor.BLUE + "===");
			// list
			for (int i = ((page.get(sender.getName()).intValue()) * limit); i < ((page
					.get(sender.getName()).intValue()) * limit) + limit; i++)
			{
				// Don't try to pull something beyond the bounds
				if (i < array.length)
				{
					StringBuilder sb = new StringBuilder();
					Player player = this.getServer().getPlayer(array[i].name);
					// Grab player and colorize name if they're online or not
					if (player == null)
					{
						sb.append(ChatColor.RED + array[i].name
								+ ChatColor.GRAY + " - ");
					}
					else
					{
						sb.append(ChatColor.GREEN + array[i].name
								+ ChatColor.GRAY + " - ");
					}
					// Grab date
					try
					{
						sb.append(ChatColor.GOLD
								+ array[i].date.substring(4, 10)
								+ ChatColor.GRAY + " - ");
					}
					catch (StringIndexOutOfBoundsException e)
					{
						// Incorrect format stored, so just give the date as is
						sb.append(ChatColor.GOLD + array[i].date
								+ ChatColor.GRAY + " - ");
					}
					// Give jailer name
					sb.append(ChatColor.AQUA + array[i].jailer);
					// Grab time if applicable
					if (array[i].time > 0)
					{
						sb.append(ChatColor.GRAY
								+ " - "
								+ ChatColor.BLUE
								+ ""
								+ this.prettifyMinutes((int) Math
										.floor((array[i].time / minutesToTicks) + 0.5f)));
					}
					// Grab reason if there was one given
					if (!array[i].reason.equals(""))
					{
						sb.append(ChatColor.GRAY + " - " + ChatColor.GRAY
								+ array[i].reason);
					}
					sender.sendMessage(sb.toString());
				}
				else
				{
					break;
				}
			}
		}
	}

	public long getTempJailTime(Player player) {
		long time = 0;
		try
		{
			ResultSet rs = database
					.select("SELECT * FROM jailed WHERE playername='" + player
							+ "';");
			if (rs.next())
			{
				time = rs.getLong("time");
			}
			rs.close();
		}
		catch (SQLException e)
		{
			log.warning(prefix + " SQL Exception");
			e.printStackTrace();
		}
		return time;
	}

	private String getJailReason(String name) {
		String reason = "";
		try
		{
			ResultSet rs = database
					.select("SELECT * FROM jailed WHERE playername='" + name
							+ "';");
			if (rs.next())
			{
				reason = rs.getString("reason");
			}
			rs.close();
		}
		catch (SQLException e)
		{
			log.warning(prefix + " SQL Exception");
			e.printStackTrace();
		}
		return reason;
	}

	public String getPlayerStatus(String name) {
		boolean found = false;
		String status = "" + JailStatus.FREED;
		try
		{
			ResultSet rs = database
					.select("SELECT * FROM jailed WHERE playername='" + name
							+ "';");
			if (rs.next())
			{

				found = true;
				status = rs.getString("status");
			}
			rs.close();
		}
		catch (SQLException e)
		{
			log.warning(prefix + " SQL Exception");
			e.printStackTrace();
		}
		if (found)
		{
			if (status.equals("" + JailStatus.JAILED))
			{
				status = "" + JailStatus.JAILED;
			}
			else if (status.equals("" + JailStatus.PENDINGFREE))
			{
				status = "" + JailStatus.PENDINGFREE;
			}
			else if (status.equals("" + JailStatus.PENDINGJAIL))
			{
				status = "" + JailStatus.PENDINGJAIL;
			}
		}
		return status;
	}

	public void setPlayerStatus(JailStatus status, String name) {
		this.database.standardQuery("UPDATE jailed SET status='" + status
				+ "' WHERE playername='" + name + "';");
	}

	public List<String> getGroups(String player) {
		List<String> list = new ArrayList<String>();
		for (World w : this.getServer().getWorlds())
		{
			String[] groups = perm.getPlayerGroups(w, player);
			for (String group : groups)
			{
				String s = group + "!" + w.getName();
				if (!list.contains(s))
				{
					list.add(s);
				}
			}
		}

		return list;
	}

	public String prettifyMinutes(int minutes) {
		if (minutes == 0)
		{
			return "less than a minute";
		}
		if (minutes == 1)
			return "one minute";
		if (minutes < 60)
			return minutes + " minutes";
		if (minutes % 60 == 0)
		{
			if (minutes / 60 == 1)
				return "one hour";
			else
				return (minutes / 60) + " hours";
		}
		int m = minutes % 60;
		int h = (minutes - m) / 60;
		return h + "h" + m + "m";
	}

	public long getPlayerTime(String name) {
		long time = 0;
		try
		{
			ResultSet rs = database
					.select("SELECT * FROM jailed WHERE playername='" + name
							+ "';");
			if (rs.next())
			{
				time = rs.getLong("time");
			}
			rs.close();
		}
		catch (SQLException e)
		{
			log.warning(prefix + " SQL Exception");
			e.printStackTrace();
		}
		return time;
	}

	public void updatePlayerTime(String name, double duration) {
		database.standardQuery("UPDATE jailed SET time='" + duration
				+ "' WHERE playername='" + name + "';");
	}

	public void removeTask(String name) {
		if (threads.containsKey(name))
		{
			threads.remove(name);
		}
	}

	public void teleport(String name) {
		Player player = this.getServer().getPlayer(name);
		if (player != null)
		{
			player.teleport(unjailLoc);
		}
	}

	public SQLite getLiteDB() {
		return database;
	}

	public void stopTask(String name) {
		if (threads.containsKey(name))
		{
			threads.get(name).stop();
		}
	}

	public boolean playerInDatabase(String name)
	{
		boolean has = false;
		try
		{
			ResultSet rs = database
					.select("SELECT COUNT(*) FROM jailed WHERE playername='"
							+ name + "';");
			if (rs.next())
			{
				if (rs.getInt(1) != 0)
				{
					has = true;
				}
			}
			rs.close();
		}
		catch (SQLException e)
		{
			log.warning(prefix + " SQL Exception");
			e.printStackTrace();
		}
		return has;
	}

	public void addPlayerToDatabase(String name) {
		try
		{
			boolean has = false;
			ResultSet rs = database
					.select("SELECT COUNT(*) FROM jailed WHERE playername='"
							+ name + "';");
			if (rs.next())
			{
				if (rs.getInt(1) != 0)
				{
					has = true;
				}
			}
			rs.close();
			if (!has)
			{
				// Add to database
				database.standardQuery("INSERT INTO jailed (playername,status,time) VALUES('"
						+ name + "','" + JailStatus.FREED + "','0');");
			}
		}
		catch (SQLException e)
		{
			log.warning(prefix + " SQL Exception");
			e.printStackTrace();
		}
	}

	public void addThread(String name) {
		threads.put(name, new JailTask(this, name, this.getPlayerTime(name)));
	}

	/**
	 * Attempts to look up full name based on who's on the server Given a
	 * partial name
	 *
	 * @author Frigid, edited by Raphfrk and petteyg359
	 */
	public String expandName(String Name) {
		int m = 0;
		String Result = "";
		for (int n = 0; n < this.getServer().getOnlinePlayers().length; n++)
		{
			String str = this.getServer().getOnlinePlayers()[n].getName();
			if (str.matches("(?i).*" + Name + ".*"))
			{
				m++;
				Result = str;
				if (m == 2)
				{
					return null;
				}
			}
			if (str.equalsIgnoreCase(Name))
				return str;
		}
		if (m == 1)
			return Result;
		if (m > 1)
		{
			return null;
		}
		return Name;
	}

	public enum JailStatus {
		JAILED, PENDINGJAIL, PENDINGFREE, FREED;
	}

	public class PrisonerInfo {
		public String name, jailer, date, reason;
		public long time;

		public PrisonerInfo(String n, String j, String d, String r, long t) {
			name = n;
			jailer = j;
			date = d;
			reason = r;
			time = t;
		}

		public void updateTime(long t) {
			time = t;
		}
	}

}