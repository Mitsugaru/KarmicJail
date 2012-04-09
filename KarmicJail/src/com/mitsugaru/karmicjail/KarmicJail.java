/**
 * Jail plugin custom tailed for the needs of Mine-RP. Built upon the SimpleJail
 * project, created by imjake9. https://github.com/imjake9/SimpleJail
 * 
 * @author imjake9
 * @author Mitsugaru
 */

package com.mitsugaru.karmicjail;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Vector;
import java.util.logging.Logger;

import lib.Mitsugaru.SQLibrary.Database.Query;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.platymuus.bukkit.permissions.Group;
import com.platymuus.bukkit.permissions.PermissionsPlugin;

public class KarmicJail extends JavaPlugin
{
	// Class Variables
	public final Logger log = Logger.getLogger("Minecraft");
	public static final String prefix = "[KarmicJail]";
	private static final String bar = "======================";
	private static final long minutesToTicks = 1200;
	private Config config;
	public ConsoleCommandSender console;
	private PermCheck perm;
	private DBHandler database;
	private final Map<String, JailTask> threads = new HashMap<String, JailTask>();
	private final Map<String, Integer> page = new HashMap<String, Integer>();
	private final Map<String, PrisonerInfo> cache = new HashMap<String, PrisonerInfo>();

	@Override
	public void onDisable()
	{
		// Stop all running threads
		this.log.info(prefix + " Stopping all jail threads...");
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
	public void onEnable()
	{
		// Get console:
		console = this.getServer().getConsoleSender();

		// Grab config
		config = new Config(this);
		// Grab database
		database = new DBHandler(this, config);

		// Check if any updates are necessary
		config.checkUpdate();

		// Get permissions plugin:
		perm = new PermCheck(this);

		// Setup listener
		this.getServer().getPluginManager()
				.registerEvents(new KarmicJailListener(this), this);

		log.info(prefix + " " + this.getDescription().getName() + " v"
				+ this.getDescription().getVersion() + " enabled.");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd,
			String commandLabel, String[] args)
	{
		boolean com = false;
		long dTime = 0;
		if (config.debugTime)
		{
			dTime = System.nanoTime();
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
			else
			{
				// All numeric player name must be the first name
				boolean timed = false;
				boolean done = false;
				int time = 0;
				StringBuilder sb = new StringBuilder();
				String reason = "";
				final Vector<String> players = new Vector<String>();
				try
				{
					String first = expandName(args[0]);
					if (first == null)
					{
						// expand failed
						first = args[0];
					}
					players.add(first);
					for (int i = 1; i < args.length; i++)
					{
						if (!done)
						{
							try
							{
								// Attempt to grab time
								time = Integer.parseInt(args[i]);
								// Attempt to grab player name if its all
								// numbers
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
							sb.append(args[i] + " ");
						}
					}
					if (sb.length() > 0)
					{
						// Remove all trailing whitespace
						reason = sb.toString().replaceAll("\\s+$", "");
					}
					for (String name : players)
					{
						this.jailPlayer(sender, name,
								reason, time, timed);
					}
				}
				catch (ArrayIndexOutOfBoundsException e)
				{
					// no player name given, error
					sender.sendMessage(ChatColor.RED + "Missing paramters");
					sender.sendMessage(ChatColor.RED
							+ "/j <player> [player2] ... [time] [reason]");
				}
			}
			com = true;
		}
		else if (commandLabel.equalsIgnoreCase("unjail"))
		{
			if (!perm.has(sender, "KarmicJail.unjail"))
			{
				sender.sendMessage(ChatColor.RED
						+ "Lack Permission: KarmicJail.unjail");
			}
			else
			{
				final Vector<String> players = new Vector<String>();
				for (int i = 0; i < args.length; i++)
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
				if (players.isEmpty())
				{
					sender.sendMessage(ChatColor.RED + "Missing paramters");
					sender.sendMessage(ChatColor.RED
							+ "/unjail <player> [player2]");
				}
				for (String name : players)
				{
					this.unjailPlayer(sender, name);
				}
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
		else if ((commandLabel.equalsIgnoreCase("jailstatus")
				|| commandLabel.equalsIgnoreCase("jstatus")
				|| commandLabel.equalsIgnoreCase("jailcheck") || commandLabel
					.equalsIgnoreCase("jcheck")) && args.length <= 1)
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
			sender.sendMessage(ChatColor.BLUE + "=============="
					+ ChatColor.GRAY + "Config" + ChatColor.BLUE
					+ "=============");
			DecimalFormat twoDForm = new DecimalFormat("#.##");
			sender.sendMessage(ChatColor.LIGHT_PURPLE + "Jail: "
					+ ChatColor.GRAY + config.jailLoc.getWorld().getName()
					+ ChatColor.BLUE + ":(" + ChatColor.GOLD
					+ Double.valueOf(twoDForm.format(config.jailLoc.getX()))
					+ ChatColor.BLUE + "," + ChatColor.GOLD
					+ Double.valueOf(twoDForm.format(config.jailLoc.getY()))
					+ ChatColor.BLUE + "," + ChatColor.GOLD
					+ Double.valueOf(twoDForm.format(config.jailLoc.getZ()))
					+ ChatColor.BLUE + ")");
			sender.sendMessage(ChatColor.LIGHT_PURPLE + "UnJail: "
					+ ChatColor.GRAY + config.unjailLoc.getWorld().getName()
					+ ChatColor.BLUE + ":(" + ChatColor.GOLD
					+ Double.valueOf(twoDForm.format(config.unjailLoc.getX()))
					+ ChatColor.BLUE + "," + ChatColor.GOLD
					+ Double.valueOf(twoDForm.format(config.unjailLoc.getY()))
					+ ChatColor.BLUE + "," + ChatColor.GOLD
					+ Double.valueOf(twoDForm.format(config.unjailLoc.getZ()))
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
				sender.sendMessage(ChatColor.GREEN + "/jail " + ChatColor.AQUA
						+ "<player> " + ChatColor.LIGHT_PURPLE
						+ "[player2]... [time] [reason]" + ChatColor.YELLOW
						+ " : Jails player(s)");
				sender.sendMessage(ChatColor.YELLOW
						+ "Note - Names auto-complete if player is online. Alias: /j");
			}
			if (perm.has(sender, "KarmicJail.unjail"))
			{
				sender.sendMessage(ChatColor.GREEN + "/unjail" + ChatColor.AQUA
						+ " <player>" + ChatColor.YELLOW + " : Unjail player");
			}
			if (perm.has(sender, "KarmicJail.jail"))
			{
				sender.sendMessage(ChatColor.GREEN + "/jailtime"
						+ ChatColor.AQUA + " <player> <time>" + ChatColor.YELLOW
						+ " : Sets time for jailed player. Alias: /jtime");
				sender.sendMessage(ChatColor.GREEN + "/jailreason"
						+ ChatColor.AQUA + " <player> "
						+ ChatColor.LIGHT_PURPLE + "[reason]"
						+ ChatColor.YELLOW
						+ " : Sets jail reason for player. Alias: /jreason");
			}
			if (perm.has(sender, "KarmicJail.mute"))
			{
				sender.sendMessage(ChatColor.GREEN + "/jailmute"
						+ ChatColor.AQUA + " <player>" + ChatColor.YELLOW
						+ " : Toggle mute for a player. Alias: /jmute");
			}
			if (perm.has(sender, "KarmicJail.list"))
			{
				sender.sendMessage(ChatColor.GREEN + "/jaillist"
						+ ChatColor.LIGHT_PURPLE + " [page]" + ChatColor.YELLOW
						+ " : List jailed players. Alias: /jlist");
				sender.sendMessage(ChatColor.GREEN + "/jailprev"
						+ ChatColor.YELLOW + " : Previous page. Alias: /jprev");
				sender.sendMessage(ChatColor.GREEN + "/jailnext"
						+ ChatColor.YELLOW + " : Next page. Alias: /jnext");
			}
			if (perm.has(sender, "KarmicJail.setjail"))
			{
				sender.sendMessage(ChatColor.GREEN + "/setjail"
						+ ChatColor.LIGHT_PURPLE + " [x] [y] [z] [world]"
						+ ChatColor.YELLOW
						+ " : Set jail teleport to current pos or given pos");
				sender.sendMessage(ChatColor.GREEN + "/setunjail"
						+ ChatColor.LIGHT_PURPLE + " [x] [y] [z] [world]"
						+ ChatColor.YELLOW
						+ " : Set unjail teleport to current pos or given pos");
			}
			if (perm.has(sender, "KarmicJail.jailstatus"))
			{
				sender.sendMessage(ChatColor.GREEN + "/jailstatus"
						+ ChatColor.LIGHT_PURPLE + " [player]"
						+ ChatColor.YELLOW
						+ " : Get jail status. Alias: /jstatus");
			}
			sender.sendMessage(ChatColor.GREEN + "/jailversion"
					+ ChatColor.YELLOW
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
		else if (commandLabel.equals("jailprev")
				|| commandLabel.equals("jprev"))
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
		else if (commandLabel.equals("jailnext")
				|| commandLabel.equals("jnext"))
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
		else if (commandLabel.equals("jailmute")
				|| commandLabel.equals("jmute"))
		{
			if (!perm.has(sender, "KarmicJail.mute"))
			{
				sender.sendMessage(ChatColor.RED
						+ "Lack Permission: KarmicJail.mute");
			}
			else
			{
				final Vector<String> players = new Vector<String>();
				for (int i = 0; i < args.length; i++)
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
				if (players.isEmpty())
				{
					sender.sendMessage(ChatColor.RED + "Missing paramters");
					sender.sendMessage(ChatColor.RED
							+ "/jmute <player> [player2] ...");
				}
				for (String name : players)
				{
					this.mutePlayer(sender, name);
				}
			}
			com = true;
		}
		else if (commandLabel.equals("jailtime")
				|| commandLabel.equals("jtime"))
		{
			if (!perm.has(sender, "KarmicJail.jail"))
			{
				sender.sendMessage(ChatColor.RED
						+ "Lack Permission: KarmicJail.jail");
			}
			else
			{
				boolean done = false;
				int time = 0;
				final Vector<String> players = new Vector<String>();
				for (int i = 0; i < args.length; i++)
				{
					if (!done)
					{
						try
						{
							// Attempt to grab time
							time = Integer.parseInt(args[i]);
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
				}
				if (players.isEmpty())
				{
					sender.sendMessage(ChatColor.RED + "Missing paramters");
					sender.sendMessage(ChatColor.RED
							+ "/jtime <player> [player2] ... <time>");
				}
				for (String name : players)
				{
					this.setJailTime(sender, name, time);
				}
			}
			com = true;
		}
		else if (commandLabel.equals("jailreload")
				|| commandLabel.equals("jreload"))
		{
			if (perm.has(sender, "KarmicJail.jail")
					|| perm.has(sender, "KarmicJail.unjail")
					|| perm.has(sender, "KarmicJail.setjail"))
			{
				config.reload();
				sender.sendMessage(ChatColor.GREEN + prefix
						+ " Config reloaded.");
			}
			else
			{
				sender.sendMessage(ChatColor.RED + "Lack permission to reload");
			}
			com = true;
		}
		else if (commandLabel.equals("jailreason")
				|| commandLabel.equals("jreason"))
		{
			if (!perm.has(sender, "KarmicJail.jail"))
			{
				sender.sendMessage(ChatColor.RED
						+ "Lack Permission: KarmicJail.jail");
			}
			else
			{
				if (args.length > 0)
				{
					String name = expandName(args[0]);
					final StringBuilder sb = new StringBuilder();
					for (int i = 1; i < args.length; i++)
					{
						sb.append(args[i] + " ");
					}
					String reason = "";
					if (sb.length() > 0)
					{
						// Remove all trailing whitespace
						reason = sb.toString().replaceAll("\\s+$", "");
					}
					setPlayerReason(name, reason);
					sender.sendMessage(ChatColor.GREEN + prefix
							+ " Set reason for " + ChatColor.AQUA + name
							+ ChatColor.GREEN + " to: " + ChatColor.GRAY
							+ reason);
				}
				else
				{
					sender.sendMessage(ChatColor.RED + "Missing name");
					sender.sendMessage(ChatColor.RED
							+ "/jtime <player> [player2] ... <time>");
				}
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
			if (config.debugTime)
			{
				this.debugTime(sender, dTime);
			}
			return true;
		}
		return false;
	}

	private void setPlayerReason(String name, String reason)
	{
		try
		{
			final PreparedStatement statement = database.prepare("UPDATE " + config.tablePrefix
					+ "jailed SET reason=? WHERE playername='"
					+ name + "';");
			statement.setString(1, reason);
			statement.executeUpdate();
			statement.close();
		}
		catch (SQLException e)
		{
			getLogger().warning("SQL Exception on jail command");
			e.printStackTrace();
		}
	}

	/**
	 * Method that messages how long a command took to complete
	 * 
	 * @param sender
	 *            of command
	 * @param time
	 *            when command was issued
	 */
	private void debugTime(CommandSender sender, long time)
	{
		time = System.nanoTime() - time;
		sender.sendMessage("[Debug]" + prefix + "Process time: " + time);
	}

	/**
	 * Jails a player
	 * 
	 * @param sender
	 *            of command
	 * @param name
	 *            of player to be jailed
	 * @param reason
	 *            for being jailed
	 * @param minutes
	 *            for how long they're in jail
	 * @param boolean to determine of player has a timed release
	 */
	public void jailPlayer(CommandSender sender, String name, String reason,
			int minutes, boolean timed)
	{
		// Check if player is already jailed:
		if (playerIsJailed(name) || playerIsPendingJail(name))
		{
			sender.sendMessage(ChatColor.RED
					+ "That player is already in jail!");
		}
		else
		{

			// Check if player is in database
			if (!playerInDatabase(name))
			{
				sender.sendMessage(ChatColor.YELLOW
						+ " Player has never been on server! Adding to database...");
				// Player has never been on server, adding to list
				addPlayerToDatabase(name);
			}
			if(config.removeGroups)
			{
				// Save groups
				this.savePlayerGroups(name);
				// Remove all groups
				this.removePlayerGroups(name);
			}
			// Add to jail group
			perm.playerAddGroup(config.jailLoc.getWorld().getName(), name,
					config.jailGroup);

			// Grab duration
			long duration = 0;
			if (timed)
			{
				duration = minutes * minutesToTicks;
				this.updatePlayerTime(name, duration);
				// Create thread to release player
				threads.put(name, new JailTask(this, name, duration));
			}

			// Grab player from server if they are online
			final Player player = this.getServer().getPlayer(name);
			if (player != null)
			{
				// Move to jail
				player.teleport(config.jailLoc);
				// Set status to jailed
				this.setPlayerStatus(JailStatus.JAILED, name);
				// Notify player
				if (reason.equals(""))
				{
					player.sendMessage(ChatColor.RED + "Jailed by "
							+ ChatColor.AQUA + sender.getName() + ChatColor.RED);
				}
				else
				{
					player.sendMessage(ChatColor.RED + "Jailed by "
							+ ChatColor.AQUA + sender.getName() + ChatColor.RED
							+ " for: " + ChatColor.GRAY
							+ this.colorizeText(reason));
				}
				if (timed)
				{
					player.sendMessage(ChatColor.AQUA + "Time in jail: "
							+ ChatColor.GOLD + this.prettifyMinutes(minutes));
				}
			}
			else
			{
				// Set player status to pending
				this.setPlayerStatus(JailStatus.PENDINGJAIL, name);
			}

			try
			{
				final String date = new Date().toString();
				final PreparedStatement statement = database.prepare("UPDATE "
						+ config.tablePrefix + "jailed SET jailer=?,date=?,reason=?, muted=? WHERE playername=?;");
				statement.setString(1, sender.getName());
				statement.setString(2, date);
				statement.setString(3, reason);
				statement.setInt(4, 0);
				statement.setString(5, name);
				statement.executeUpdate();
				statement.close();
				sender.sendMessage(ChatColor.RED + name + ChatColor.AQUA
						+ " sent to jail.");
				final PrisonerInfo pi = new PrisonerInfo(name,
						sender.getName(), date, reason, duration, false);
				cache.put(name, pi);
				// Throw jail event
				this.getServer().getPluginManager()
						.callEvent(new JailEvent("JailEvent", pi));
			}
			catch (SQLException e)
			{
				getLogger().warning("SQL Exception on jail command");
				e.printStackTrace();
			}
		}
	}

	/**
	 * Saves the player's groups into database
	 * 
	 * @param name
	 *            of player
	 */
	private void savePlayerGroups(String name)
	{
		StringBuilder sb = new StringBuilder();
		boolean append = false;
		for (String s : this.getGroups(name))
		{
			sb.append(s + "&");
			append = true;
		}
		if (append)
		{
			sb.deleteCharAt(sb.length() - 1);
		}
		this.database.standardQuery("UPDATE " + config.tablePrefix
				+ "jailed SET groups='" + sb.toString()
				+ "' WHERE playername='" + name + "';");
	}

	/**
	 * Removes all groups of a player
	 * 
	 * @param name
	 *            of player
	 */
	private void removePlayerGroups(String name)
	{
		if (perm.getName().equals("PermissionsBukkit"))
		{
			final PermissionsPlugin permission = (PermissionsPlugin) this
					.getServer().getPluginManager()
					.getPlugin("PermissionsBukkit");
			for (Group group : permission.getGroups(name))
			{
				perm.playerRemoveGroup(this.getServer().getWorlds().get(0),
						name, group.getName());
			}
		}
		else
		{
			for (World w : this.getServer().getWorlds())
			{
				String[] groups = perm.getPlayerGroups(w, name);
				for (String group : groups)
				{
					perm.playerRemoveGroup(w, name, group);
				}
			}
		}
	}

	/**
	 * Unjails a player
	 * 
	 * @param sender
	 *            of command
	 * @param name
	 *            of jailed player
	 * @param fromTempJail
	 *            , if the jailed player's time ran out
	 */
	public void unjailPlayer(CommandSender sender, String name,
			boolean fromTempJail)
	{

		// Check if player is in jail:
		if (!playerIsJailed(name) && !playerIsPendingJail(name))
		{
			sender.sendMessage(ChatColor.RED + "That player is not in jail!");
			return;
		}

		// Grab player if on server
		Player player = this.getServer().getPlayer(name);
		// Remove jail group
		perm.playerRemoveGroup(config.jailLoc.getWorld(), name,
				config.jailGroup);
		if(config.removeGroups)
		{
			// Return previous groups
			this.returnGroups(name);
		}

		// Clear other columns
		database.standardQuery("UPDATE "
				+ config.tablePrefix
				+ "jailed SET time='0',jailer='',date='',reason='' WHERE playername='"
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

		// Move player out of jail
		if (config.unjailTeleport)
		{
			player.teleport(config.unjailLoc);
		}
		// Change status
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
		{
			// Also notify jailer if they're online
			Player jailer = this.getServer().getPlayer(this.getJailer(name));
			if (jailer != null)
			{
				jailer.sendMessage(ChatColor.GOLD + player.getName()
						+ ChatColor.AQUA + " auto-unjailed.");
			}
			// Notify console
			sender.sendMessage(ChatColor.GOLD + player.getName()
					+ ChatColor.AQUA + " auto-unjailed.");
		}
		else
			sender.sendMessage(ChatColor.GOLD + name + ChatColor.AQUA
					+ " removed from jail.");
	}

	/**
	 * Restores the players groups from database storage
	 * 
	 * @param name
	 *            of player
	 */
	public void returnGroups(String name)
	{
		try
		{
			Query rs = this.database.select("SELECT * FROM "
					+ config.tablePrefix + "jailed WHERE playername='" + name
					+ "';");
			if (rs.getResult().next())
			{
				String groups = rs.getResult().getString("groups");
				if (!rs.getResult().wasNull() && !groups.equals(""))

					if (groups.contains("&"))
					{
						String[] cut = groups.split("&");
						for (String group : cut)
						{
							String[] split = group.split("!");
							perm.playerAddGroup(split[1], name, split[0]);
						}
					}
					else
					{
						String[] split = groups.split("!");
						perm.playerAddGroup(split[1], name, split[0]);
					}
			}
			rs.closeQuery();
		}
		catch (SQLException e)
		{
			log.warning(prefix + " SQL Exception");
			e.printStackTrace();
		}
	}

	public void unjailPlayer(CommandSender sender, String name)
	{
		this.unjailPlayer(sender, name, false);
	}

	public void mutePlayer(CommandSender sender, String name)
	{
		// Check if player is in jail:
		if (!playerIsJailed(name) && !playerIsPendingJail(name))
		{
			sender.sendMessage(ChatColor.RED + "That player is not in jail!");
			return;
		}
		if (playerIsMuted(name))
		{
			database.standardQuery("UPDATE " + config.tablePrefix
					+ "jailed SET muted='0' WHERE playername='" + name + "';");
			sender.sendMessage(ChatColor.GOLD + name + ChatColor.GREEN
					+ " unmuted");
		}
		else
		{
			database.standardQuery("UPDATE " + config.tablePrefix
					+ "jailed SET muted='1' WHERE playername='" + name + "';");
			sender.sendMessage(ChatColor.GOLD + name + ChatColor.RED + " muted");
		}
	}

	public void setJailTime(CommandSender sender, String name, int minutes)
	{
		// Check if player is in jail:
		if (!playerIsJailed(name) && !playerIsPendingJail(name))
		{
			sender.sendMessage(ChatColor.RED + "That player is not in jail!");
			return;
		}

		// Grab player if on server
		Player player = this.getServer().getPlayer(name);
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
		// Jail indefinitely if 0 or negative
		if (minutes <= 0)
		{
			this.updatePlayerTime(name, minutes);
			sender.sendMessage(ChatColor.RED + name + ChatColor.AQUA
					+ " is jailed forever.");
			if (player != null)
			{
				player.sendMessage(ChatColor.AQUA + "Jailed forever.");
			}
		}
		else
		{
			// Calculate time
			long duration = 0;
			duration = minutes * minutesToTicks;
			this.updatePlayerTime(name, duration);
			if (player != null)
			{
				// Create thread to release player
				threads.put(name, new JailTask(this, name, duration));
			}
			sender.sendMessage(ChatColor.AQUA + "Time set to " + ChatColor.GOLD
					+ minutes + ChatColor.AQUA + " for " + ChatColor.RED + name
					+ ChatColor.AQUA + ".");
			if (player != null)
			{
				player.sendMessage(ChatColor.AQUA + "Time set to "
						+ ChatColor.GOLD + minutes + ChatColor.AQUA + ".");
			}
		}

	}

	/**
	 * Sets the jail location
	 * 
	 * @param sender
	 *            of command
	 * @param arguments
	 *            of command
	 */
	public void setJail(CommandSender sender, String[] args)
	{
		if (!(sender instanceof Player) && args.length != 4)
		{
			sender.sendMessage(ChatColor.RED + "Only players can use that.");
			return;
		}
		if (args.length == 0)
		{
			Player player = (Player) sender;
			config.jailLoc = player.getLocation();
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
			config.jailLoc = new Location(this.getServer().getWorld(args[3]),
					Integer.parseInt(args[0]), Integer.parseInt(args[1]),
					Integer.parseInt(args[2]));
		}

		config.set("jail.x", (int) config.jailLoc.getX());
		config.set("jail.y", (int) config.jailLoc.getY());
		config.set("jail.z", (int) config.jailLoc.getZ());
		config.set("jail.world", config.jailLoc.getWorld().getName());

		this.saveConfig();

		sender.sendMessage(ChatColor.AQUA + "Jail point saved.");
	}

	/**
	 * Sets the unjail location
	 * 
	 * @param sender
	 *            of command
	 * @param arguments
	 *            of command
	 */
	public void setUnjail(CommandSender sender, String[] args)
	{
		if (!(sender instanceof Player) && args.length != 4)
		{
			sender.sendMessage(ChatColor.RED + "Only players can use that.");
			return;
		}
		if (args.length == 0)
		{
			Player player = (Player) sender;
			config.unjailLoc = player.getLocation();
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
			config.unjailLoc = new Location(this.getServer().getWorld(args[3]),
					Integer.parseInt(args[0]), Integer.parseInt(args[1]),
					Integer.parseInt(args[2]));
		}

		config.set("unjail.x", (int) config.unjailLoc.getX());
		config.set("unjail.y", (int) config.unjailLoc.getY());
		config.set("unjail.z", (int) config.unjailLoc.getZ());
		config.set("unjail.world", config.unjailLoc.getWorld().getName());

		this.saveConfig();

		sender.sendMessage(ChatColor.AQUA + "Unjail point saved.");
	}

	/**
	 * Gets the status of a player
	 * 
	 * @param sender
	 *            of command
	 * @param arguments
	 *            of command
	 */
	public void jailStatus(CommandSender sender, String[] args)
	{
		if (!(sender instanceof Player) && args.length == 0)
		{
			sender.sendMessage(ChatColor.RED + "Must specify a player.");
			return;
		}
		final Player player = (args.length == 0) ? (Player) sender : this
				.getServer().getPlayer(args[0]);
		String name = "";
		if (player == null)
		{
			name = args[0];
		}
		else
		{
			name = player.getName();
		}

		if (!this.playerIsJailed(name) && !this.playerIsPendingJail(name))
		{
			if (args.length == 0)
				sender.sendMessage(ChatColor.RED + "You are not jailed.");
			else
				sender.sendMessage(ChatColor.AQUA + name + ChatColor.RED
						+ " is not jailed.");
			return;
		}

		final StringBuilder sb = new StringBuilder();
		final String date = this.getJailDate(name);
		final String jailer = this.getJailer(name);
		final String reason = this.getJailReason(name);
		final boolean muted = this.playerIsMuted(name);
		if (args.length == 0)
		{
			sb.append(ChatColor.RED + "Jailed on " + ChatColor.GREEN + date
					+ ChatColor.RED + " by " + ChatColor.GOLD + jailer);
		}
		else
		{
			sb.append(ChatColor.AQUA + name + ChatColor.RED + " was jailed on "
					+ ChatColor.GREEN + date + ChatColor.RED + " by "
					+ ChatColor.GOLD + jailer);
		}
		if (!reason.equals(""))
		{
			sb.append(ChatColor.RED + " for " + ChatColor.GRAY
					+ this.colorizeText(reason));
		}
		if (muted)
		{
			sb.append(ChatColor.GRAY + " - " + ChatColor.DARK_RED + "MUTED");
		}
		sender.sendMessage(sb.toString());
		if (this.playerIsTempJailed(name))
		{
			int minutes = (int) ((this.getPlayerTime(name) / minutesToTicks));
			if (player == null)
			{
				sender.sendMessage(ChatColor.AQUA + "Remaining jail time: "
						+ ChatColor.GOLD + this.prettifyMinutes(minutes));
			}
			else
			{
				// Player is online, check the thread for their remaining time
				if (threads.containsKey(name))
				{
					minutes = (int) (threads.get(name).remainingTime() / minutesToTicks);
					sender.sendMessage(ChatColor.AQUA + "Remaining jail time: "
							+ this.prettifyMinutes(minutes));
				}
			}
		}
	}

	/**
	 * 
	 * @return location of jail
	 */
	public Location getJailLocation()
	{
		return config.jailLoc;
	}

	/**
	 * 
	 * @return location of unjail
	 */
	public Location getUnjailLocation()
	{
		return config.unjailLoc;
	}

	/**
	 * Gets name of the jailer
	 * 
	 * @param name
	 *            of person in jail
	 * @return name of jailer
	 */
	private String getJailer(String name)
	{
		String jailer = "NOBODY";
		try
		{
			Query rs = database.select("SELECT * FROM " + config.tablePrefix
					+ "jailed WHERE playername='" + name + "';");
			if (rs.getResult().next())
			{
				jailer = rs.getResult().getString("jailer");
				if (rs.getResult().wasNull())
				{
					jailer = "NOBODY";
				}
			}
			rs.closeQuery();
		}
		catch (SQLException e)
		{
			log.warning(prefix + " SQL Exception");
			e.printStackTrace();
		}
		return jailer;
	}

	/**
	 * Grabs date of when player was originally jailed
	 * 
	 * @param name
	 *            of person jailed
	 * @return String of the date when player was jailed
	 */
	private String getJailDate(String name)
	{
		String date = "";
		try
		{
			Query rs = database.select("SELECT * FROM " + config.tablePrefix
					+ "jailed WHERE playername='" + name + "';");
			if (rs.getResult().next())
			{
				date = rs.getResult().getString("date");
				if (rs.getResult().wasNull())
				{
					date = "NO DATE";
				}
			}
			rs.closeQuery();
		}
		catch (SQLException e)
		{
			log.warning(prefix + " SQL Exception");
			e.printStackTrace();
		}
		return date;
	}

	/**
	 * Checks if the player was jailed while offline
	 * 
	 * @param Name
	 *            of player
	 * @return True if pending jailed, else false
	 */
	public boolean playerIsPendingJail(String player)
	{
		boolean jailed = false;
		try
		{
			Query rs = database.select("SELECT * FROM " + config.tablePrefix
					+ "jailed WHERE playername='" + player + "';");
			if (rs.getResult().next())
			{
				final String status = rs.getResult().getString("status");
				if (status.equals("" + JailStatus.PENDINGJAIL))
				{
					jailed = true;
				}
			}
			rs.closeQuery();
		}
		catch (SQLException e)
		{
			log.warning(prefix + " SQL Exception");
			e.printStackTrace();
		}
		return jailed;
	}

	/**
	 * Checks if the player is in jail
	 * 
	 * @param Name
	 *            of player
	 * @return true if jailed, else false
	 */
	public boolean playerIsJailed(String player)
	{
		boolean jailed = false;
		boolean missing = false;
		try
		{
			Query rs = database.select("SELECT * FROM " + config.tablePrefix
					+ "jailed WHERE playername='" + player + "';");
			if (rs.getResult().next())
			{
				final String status = rs.getResult().getString("status");
				if (rs.getResult().wasNull())
				{
					log.severe(prefix + " MISSING STATUS FOR: " + player);
					missing = true;
				}
				else if (status.equals("" + JailStatus.JAILED))
				{
					jailed = true;
				}
			}
			rs.closeQuery();
			if (missing)
			{
				setPlayerStatus(JailStatus.FREED, player);
			}
		}
		catch (SQLException e)
		{
			log.warning(prefix + " SQL Exception");
			e.printStackTrace();
		}
		return jailed;
	}

	/**
	 * Checks to see if the player has a time associated with their jail
	 * sentence
	 * 
	 * @param Name
	 *            of player
	 * @return true if player has a valid time, else false
	 */
	public boolean playerIsTempJailed(String player)
	{
		double time = 0;
		boolean missing = false;
		try
		{
			Query rs = database.select("SELECT * FROM " + config.tablePrefix
					+ "jailed WHERE playername='" + player + "';");
			if (rs.getResult().next())
			{
				time = rs.getResult().getDouble("time");
				if (rs.getResult().wasNull())
				{
					time = 0;
					missing = true;
				}
			}
			rs.closeQuery();
			if (missing)
			{
				setJailTime(console, player, 0);
				log.warning(prefix + " " + player
						+ "'s Time was missing. Reset to 0.");
			}
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

	/**
	 * Lists the players in jail
	 * 
	 * @param sender
	 *            of command
	 * @param Page
	 *            adjustment
	 */
	private void listJailed(CommandSender sender, int pageAdjust)
	{
		// Update cache of jailed players
		try
		{
			Query rs = database.select("SELECT * FROM " + config.tablePrefix
					+ "jailed WHERE status='" + JailStatus.JAILED
					+ "' OR status='" + JailStatus.PENDINGJAIL + "';");
			if (rs.getResult().next())
			{
				do
				{
					String name = rs.getResult().getString("playername");
					String jailer = rs.getResult().getString("jailer");
					if (rs.getResult().wasNull())
					{
						jailer = "NOBODY";
					}
					String date = rs.getResult().getString("date");
					if (rs.getResult().wasNull())
					{
						date = "NO DATE";
					}
					String reason = rs.getResult().getString("reason");
					if (rs.getResult().wasNull())
					{
						reason = "";
					}
					long time = rs.getResult().getLong("time");
					if (rs.getResult().wasNull())
					{
						time = 0;
					}
					int muteInt = rs.getResult().getInt("muted");
					if (rs.getResult().wasNull())
					{
						muteInt = 0;
					}
					boolean muted = false;
					if (muteInt == 1)
					{
						muted = true;
					}
					cache.put(name, new PrisonerInfo(name, jailer, date,
							reason, time, muted));
					// Update the time if necessary
					if (threads.containsKey(name))
					{
						cache.get(name).updateTime(
								threads.get(name).remainingTime());
					}
				} while (rs.getResult().next());
			}
			rs.closeQuery();
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
		double rem = (double) array.length % (double) config.limit;
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
		else if ((page.get(sender.getName()).intValue()) * config.limit > array.length)
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
			// FIXME not going into for loop for whatever reason
			for (int i = ((page.get(sender.getName()).intValue()) * config.limit); i < ((page
					.get(sender.getName()).intValue()) * config.limit)
					+ config.limit; i++)
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
						double temp = Math
								.floor(((double) array[i].time / (double) minutesToTicks) + 0.5f);
						sb.append(ChatColor.GRAY + " - " + ChatColor.BLUE + ""
								+ this.prettifyMinutes((int) temp));
					}
					// Grab reason if there was one given
					if (!array[i].reason.equals(""))
					{
						sb.append(ChatColor.GRAY + " - " + ChatColor.GRAY
								+ this.colorizeText(array[i].reason));
					}
					// Grab if muted
					if (array[i].mute)
					{
						sb.append(ChatColor.GRAY + " - " + ChatColor.DARK_RED
								+ "MUTED");
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

	/**
	 * Grabs the reason for being in jail
	 * 
	 * @param name
	 *            of player
	 * @return String of jailer's reason
	 */
	private String getJailReason(String name)
	{
		String reason = "";
		try
		{
			Query rs = database.select("SELECT * FROM " + config.tablePrefix
					+ "jailed WHERE playername='" + name + "';");
			if (rs.getResult().next())
			{
				reason = rs.getResult().getString("reason");
				if (rs.getResult().wasNull())
				{
					reason = "";
				}
			}
			rs.closeQuery();
		}
		catch (SQLException e)
		{
			log.warning(prefix + " SQL Exception");
			e.printStackTrace();
		}
		return reason;
	}

	public boolean playerIsMuted(String name)
	{
		boolean mute = false;
		try
		{
			Query rs = database.select("SELECT * FROM " + config.tablePrefix
					+ "jailed WHERE playername='" + name + "';");
			if (rs.getResult().next())
			{
				int muteInt = rs.getResult().getInt("muted");
				if (!rs.getResult().wasNull())
				{
					if (muteInt == 1)
					{
						mute = true;
					}
				}
			}
			rs.closeQuery();
		}
		catch (SQLException e)
		{
			log.warning(prefix + " SQL Exception");
			e.printStackTrace();
		}
		return mute;
	}

	/**
	 * Returns the player's current status
	 * 
	 * @param name
	 *            of player
	 * @return String of the player's JailStatus
	 */
	public String getPlayerStatus(String name)
	{
		boolean found = true;
		String status = "" + JailStatus.FREED;
		try
		{
			Query rs = database.select("SELECT * FROM " + config.tablePrefix
					+ "jailed WHERE playername='" + name + "';");
			if (rs.getResult().next())
			{
				status = rs.getResult().getString("status");
				if (rs.getResult().wasNull())
				{
					status = "" + JailStatus.FREED;
					found = false;
				}
			}
			else
			{
				found = false;
			}
			rs.closeQuery();

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
		else
		{
			setPlayerStatus(JailStatus.FREED, name);
		}
		return status;
	}

	/**
	 * Sets a player's status
	 * 
	 * @param JailStatus
	 *            to set to
	 * @param name
	 *            of player
	 */
	public void setPlayerStatus(JailStatus status, String name)
	{
		this.database.standardQuery("UPDATE " + config.tablePrefix
				+ "jailed SET status='" + status + "' WHERE playername='"
				+ name + "';");
	}

	/**
	 * Returns a list of all the groups a player has
	 * 
	 * @param name
	 *            of player
	 * @return List of groups with associated world
	 */
	public List<String> getGroups(String player)
	{
		List<String> list = new ArrayList<String>();
		if (perm.getName().equals("PermissionsBukkit"))
		{
			final PermissionsPlugin permission = (PermissionsPlugin) this
					.getServer().getPluginManager()
					.getPlugin("PermissionsBukkit");
			for (Group group : permission.getGroups(player))
			{
				final String s = group.getName() + "!"
						+ this.getServer().getWorlds().get(0).getName();
				list.add(s);
			}
		}
		else
		{
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
		}

		return list;
	}

	/**
	 * Makes the minutes readable
	 * 
	 * @param minutes
	 * @return String of readable minutes
	 */
	public String prettifyMinutes(int minutes)
	{
		if (minutes < 1)
		{
			return "about less than a minute";
		}
		if (minutes == 1)
			return "about one minute";
		if (minutes < 60)
			return "about " + minutes + " minutes";
		if (minutes % 60 == 0)
		{
			if (minutes / 60 == 1)
				return "about one hour";
			else
				return "about " + (minutes / 60) + " hours";
		}
		int m = minutes % 60;
		int h = (minutes - m) / 60;
		return "about " + h + "h" + m + "m";
	}

	/**
	 * Grabs player's time left in jail
	 * 
	 * @param name
	 *            of player
	 * @return long of time left to serve
	 */
	public long getPlayerTime(String name)
	{
		long time = 0;
		try
		{
			Query rs = database.select("SELECT * FROM " + config.tablePrefix
					+ "jailed WHERE playername='" + name + "';");
			if (rs.getResult().next())
			{
				time = rs.getResult().getLong("time");
				if (rs.getResult().wasNull())
				{
					time = 0;
				}
			}
			rs.closeQuery();
		}
		catch (SQLException e)
		{
			log.warning(prefix + " SQL Exception");
			e.printStackTrace();
		}
		return time;
	}

	/**
	 * Sets a player's time
	 * 
	 * @param name
	 *            of player
	 * @param duration
	 *            of time
	 */
	public void updatePlayerTime(String name, long duration)
	{
		database.standardQuery("UPDATE " + config.tablePrefix
				+ "jailed SET time='" + duration + "' WHERE playername='"
				+ name + "';");
	}

	/**
	 * Removes a task
	 * 
	 * @param name
	 */
	public void removeTask(String name)
	{
		if (threads.containsKey(name))
		{
			threads.remove(name);
		}
	}

	/**
	 * Teleports a player to unjail locaiton
	 * 
	 * @param name
	 *            of player to be teleported
	 */
	public void teleportOut(String name)
	{
		Player player = this.getServer().getPlayer(name);
		if (player != null)
		{
			player.teleport(config.unjailLoc);
		}
	}

	public DBHandler getDatabaseHandler()
	{
		return database;
	}

	/**
	 * Stops a player's timed task
	 * 
	 * @param name
	 *            of player
	 */
	public boolean stopTask(String name)
	{
		if (threads.containsKey(name))
		{
			return threads.get(name).stop();
		}
		return false;
	}

	/**
	 * Check if player exists in master database
	 * 
	 * @param name
	 *            of player
	 * @return true if player is in database, else false
	 */
	public boolean playerInDatabase(String name)
	{
		boolean has = false;
		try
		{
			Query rs = database.select("SELECT COUNT(*) FROM "
					+ config.tablePrefix + "jailed WHERE playername='" + name
					+ "';");
			if (rs.getResult().next())
			{
				final int count = rs.getResult().getInt(1);
				if (!rs.getResult().wasNull())
				{
					if (count > 0)
					{
						has = true;
					}
				}
			}
			rs.closeQuery();
		}
		catch (SQLException e)
		{
			log.warning(prefix + " SQL Exception");
			e.printStackTrace();
		}
		return has;
	}

	/**
	 * Adds a player to the database if they do not exist
	 * 
	 * @param name
	 *            of player
	 */
	public void addPlayerToDatabase(String name)
	{
		try
		{
			boolean has = false;
			Query rs = database.select("SELECT COUNT(*) FROM "
					+ config.tablePrefix + "jailed WHERE playername='" + name
					+ "';");
			if (rs.getResult().next())
			{
				final int count = rs.getResult().getInt(1);
				if (!rs.getResult().wasNull())
				{
					if (count > 0)
					{
						has = true;
					}
				}
			}
			rs.closeQuery();
			if (!has)
			{
				// Add to database
				database.standardQuery("INSERT INTO " + config.tablePrefix
						+ "jailed (playername,status,time) VALUES ('" + name
						+ "', '" + JailStatus.FREED + "', '0');");
			}
		}
		catch (SQLException e)
		{
			log.warning(prefix + " SQL Exception");
			e.printStackTrace();
		}
	}

	/**
	 * Creates a new thread for a player to get auto-released
	 * 
	 * @param name
	 *            of player
	 */
	public void addThread(String name, long time)
	{
		threads.put(name, new JailTask(this, name, time));
	}

	/**
	 * Attempts to look up full name based on who's on the server Given a
	 * partial name
	 * 
	 * @author Frigid, edited by Raphfrk and petteyg359
	 */
	public String expandName(String Name)
	{
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

	/**
	 * Colorizes a given string to Bukkit standards
	 * 
	 * http://forums.bukkit.org/threads/methode-to-colorize.69543/#post-1063437
	 * 
	 * @param string
	 * @return String with appropriate Bukkit ChatColor in them
	 * @author AmberK
	 */
	public String colorizeText(String string)
	{
		string = string.replaceAll("&0", ""+ChatColor.BLACK);
	    string = string.replaceAll("&1", ""+ChatColor.DARK_BLUE);
	    string = string.replaceAll("&2", ""+ChatColor.DARK_GREEN);
	    string = string.replaceAll("&3", ""+ChatColor.DARK_AQUA);
	    string = string.replaceAll("&4", ""+ChatColor.DARK_RED);
	    string = string.replaceAll("&5", ""+ChatColor.DARK_PURPLE);
	    string = string.replaceAll("&6", ""+ChatColor.GOLD);
	    string = string.replaceAll("&7", ""+ChatColor.GRAY);
	    string = string.replaceAll("&8", ""+ChatColor.DARK_GRAY);
	    string = string.replaceAll("&9", ""+ChatColor.BLUE);
	    string = string.replaceAll("&a", ""+ChatColor.GREEN);
	    string = string.replaceAll("&b", ""+ChatColor.AQUA);
	    string = string.replaceAll("&c", ""+ChatColor.RED);
	    string = string.replaceAll("&d", ""+ChatColor.LIGHT_PURPLE);
	    string = string.replaceAll("&e", ""+ChatColor.YELLOW);
	    string = string.replaceAll("&f", ""+ChatColor.WHITE);
	    return string;
	}

	public enum JailStatus
	{
		JAILED, PENDINGJAIL, PENDINGFREE, FREED;
	}

	public static class PrisonerInfo
	{
		public String name, jailer, date, reason;
		public long time;
		public boolean mute;

		public PrisonerInfo(String n, String j, String d, String r, long t,
				boolean m)
		{
			name = n;
			jailer = j;
			date = d;
			reason = r;
			time = t;
			mute = m;
		}

		public void updateTime(long t)
		{
			time = t;
		}
	}

}