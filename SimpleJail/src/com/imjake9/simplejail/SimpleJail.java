package com.imjake9.simplejail;

import java.sql.ResultSet;
import java.sql.SQLException;
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

public class SimpleJail extends JavaPlugin {

	public final Logger log = Logger.getLogger("Minecraft");
	public static final String prefix = "[SimpleJail]";
	private static final long minutesToTicks = 1200;
	public ConsoleCommandSender console;
	private Location jailLoc;
	private Location unjailLoc;
	private String jailGroup;
	private SimpleJailPlayerListener listener;
	private Permission perm;
	private SQLite database;
	private Map<String, JailTask> threads = new HashMap<String, JailTask>();

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
		listener = new SimpleJailPlayerListener(this);
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

		if (commandLabel.equalsIgnoreCase("jail"))
		{
			if (!perm.has(sender, "SimpleJail.jail"))
			{
				return true;
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
						if(time > 0)
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
			for (String name : players)
			{
				this.jailPlayer(sender, name, reason, time, timed);
			}
			return true;
		}
		else if (commandLabel.equalsIgnoreCase("unjail") && args.length == 1)
		{
			if (!perm.has(sender, "SimpleJail.unjail"))
				return true;
			this.unjailPlayer(sender, args);
			return true;
		}
		else if (commandLabel.equalsIgnoreCase("setjail")
				&& (args.length == 0 || args.length == 4))
		{
			if (!perm.has(sender, "SimpleJail.setjail"))
				return true;
			this.setJail(sender, args);
			return true;
		}
		else if (commandLabel.equalsIgnoreCase("setunjail")
				&& (args.length == 0 || args.length == 4))
		{
			if (!perm.has(sender, "SimpleJail.setjail"))
				return true;
			this.setUnjail(sender, args);
			return true;
		}
		else if (commandLabel.equalsIgnoreCase("jailstatus")
				&& args.length <= 1)
		{
			if (!perm.has(sender, "SimpleJail.jailstatus"))
				return true;
			this.jailTime(sender, args);
			return true;
		}
		else
		{
			if (!perm.has(sender, "SimpleJail.jail"))
				return true;
			if (!perm.has(sender, "SimpleJail.unjail"))
				return true;
			if (!perm.has(sender, "SimpleJail.setjail"))
				return true;
			if (!perm.has(sender, "SimpleJail.jailstatus"))
				return true;
			return false;
		}

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
		database.standardQuery("UPDATE jailed SET jailer='" + sender.getName()
				+ "',date='" + new Date().toString() + "',reason='" + reason
				+ "' WHERE playername='"+name+"';");
		sender.sendMessage(ChatColor.RED + name + ChatColor.AQUA
				+ " sent to jail.");
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

		//Clear other columns
		database.standardQuery("UPDATE jailed SET time='',jailer='',date='',reason='' WHERE playername='"+name+"';");
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

		//Remove task
		if(threads.containsKey(name))
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
			sender.sendMessage(ChatColor.GOLD + name + ChatColor.AQUA + " removed from jail.");
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

	public void jailTime(CommandSender sender, String[] args) {
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
						sender.sendMessage(ChatColor.RED + "Jailed on " + ChatColor.GREEN
								+ this.getJailDate(name) + ChatColor.RED + " by "
								+ ChatColor.GOLD + this.getJailer(name) + ChatColor.RED +" for " + ChatColor.GRAY + reason);
					else
						sender.sendMessage(ChatColor.AQUA + name
								+ ChatColor.RED + " was jailed on " + ChatColor.GREEN
								+ this.getJailDate(name) + ChatColor.RED + " by "
								+ ChatColor.GOLD + this.getJailer(name)+ ChatColor.RED +" for " + ChatColor.GRAY + reason);
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
				sender.sendMessage(ChatColor.RED + "Jailed on " + ChatColor.GREEN
						+ this.getJailDate(name) + ChatColor.RED + " by "
						+ ChatColor.GOLD + this.getJailer(name)+ ChatColor.RED +" for " + ChatColor.GRAY + reason);
			else
				sender.sendMessage(ChatColor.AQUA + name + ChatColor.RED
						+ " is jailed on" + ChatColor.GREEN
						+ this.getJailDate(name) + ChatColor.RED + " by "
						+ ChatColor.GOLD + this.getJailer(name)+ ChatColor.RED +" for " + ChatColor.GRAY + reason);
			int minutes = (int) ((this.getTempJailTime(player) / minutesToTicks));
			sender.sendMessage(ChatColor.AQUA + "Remaining jail time: "
					+ this.prettifyMinutes(minutes));
		}
		else
		{
			if (args.length == 0)
				sender.sendMessage(ChatColor.RED + "Jailed on " + ChatColor.GREEN
						+ this.getJailDate(name) + ChatColor.RED + " by "
						+ ChatColor.GOLD + this.getJailer(name));
			else
				sender.sendMessage(ChatColor.AQUA + name + ChatColor.RED
						+ " is jailed on" + ChatColor.GREEN
						+ this.getJailDate(name) + ChatColor.RED + " by "
						+ ChatColor.GOLD + this.getJailer(name));
			int minutes = (int) ((this.getTempJailTime(player) / minutesToTicks));
			sender.sendMessage(ChatColor.AQUA + "Remaining jail time: "
					+ this.prettifyMinutes(minutes));
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

	}

	private void setupPermissions() {
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

	public boolean playerIsPendingJail(String player)
	{
		boolean jailed = false;
		try
		{
			ResultSet rs = database
					.select("SELECT * FROM jailed WHERE playername='" + player
							+ "';");
			if (rs.next())
			{
				final String status = rs.getString("status");
				if (status.equals(""+JailStatus.PENDINGJAIL))
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
				if (status.equals(""+JailStatus.JAILED))
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
			if (status.equals(""+JailStatus.JAILED))
			{
				status =  "" +JailStatus.JAILED;
			}
			else if (status.equals(""+JailStatus.PENDINGFREE))
			{
				status = ""+ JailStatus.PENDINGFREE;
			}
			else if (status.equals(""+JailStatus.PENDINGJAIL))
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

	public long getPlayerTime(String name)
	{
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

	public void addThread(String name)
	{
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
}
