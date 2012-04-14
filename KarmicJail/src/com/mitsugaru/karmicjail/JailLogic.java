package com.mitsugaru.karmicjail;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import lib.Mitsugaru.SQLibrary.Database.Query;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import com.mitsugaru.karmicjail.KarmicJail.JailStatus;
import com.mitsugaru.karmicjail.KarmicJail.PrisonerInfo;
import com.mitsugaru.karmicjail.events.JailEvent;
import com.mitsugaru.utils.Config;
import com.mitsugaru.utils.PermCheck;
import com.platymuus.bukkit.permissions.Group;
import com.platymuus.bukkit.permissions.PermissionsPlugin;

public class JailLogic
{
	private static KarmicJail plugin;
	private static Config config;
	private static PermCheck perm;
	private static DBHandler database;
	private static ConsoleCommandSender console;
	
	

	public static void init(KarmicJail plugin)
	{
		JailLogic.plugin = plugin;
		JailLogic.perm = plugin.getPermissions();
		JailLogic.config = plugin.getPluginConfig();
		JailLogic.database = plugin.getDatabaseHandler();
		console = plugin.getServer().getConsoleSender();
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
	public static void jailPlayer(CommandSender sender, String inName, String reason,
			int minutes, boolean timed) {
		// Check if player is already jailed:
		if (playerIsJailed(inName) || playerIsPendingJail(inName)) {
			sender.sendMessage(ChatColor.RED
					+ "That player is already in jail!");
		} else {

			// Check if player is in database
			String name = playerInDatabase(inName);
			if (name == null) {
				sender.sendMessage(ChatColor.YELLOW + " Player '"
						+ ChatColor.GREEN + inName + ChatColor.YELLOW
						+ "' has never been on server! Adding to database...");
				// Player has never been on server, adding to list
				addPlayerToDatabase(inName);
				name = inName;
			}
			if (config.removeGroups) {
				// Save groups
				savePlayerGroups(name);
				// Remove all groups
				removePlayerGroups(name);
			}
			// Add to jail group
			perm.playerAddGroup(config.jailLoc.getWorld().getName(), name,
					config.jailGroup);

			// Grab duration
			long duration = 0;
			if (timed) {
				duration = minutes * KarmicJail.minutesToTicks;
				updatePlayerTime(name, duration);
			}

			// Grab player from server if they are online
			final Player player = plugin.getServer().getPlayer(name);
			if (player != null) {
				if (player.isOnline()) {
					// Move to jail
					player.teleport(config.jailLoc);
					// Set status to jailed
					setPlayerStatus(JailStatus.JAILED, name);
					// Notify player
					if (reason.equals("")) {
						player.sendMessage(ChatColor.RED + "Jailed by "
								+ ChatColor.AQUA + sender.getName()
								+ ChatColor.RED);
					} else {
						player.sendMessage(ChatColor.RED + "Jailed by "
								+ ChatColor.AQUA + sender.getName()
								+ ChatColor.RED + " for: " + ChatColor.GRAY
								+ plugin.colorizeText(reason));
					}
					if (timed) {
						player.sendMessage(ChatColor.AQUA + "Time in jail: "
								+ ChatColor.GOLD
								+ plugin.prettifyMinutes(minutes));
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
				final String date = new Date().toString();
				final PreparedStatement statement = database
						.prepare("UPDATE "
								+ config.tablePrefix
								+ "jailed SET jailer=?,date=?,reason=?, muted=? WHERE playername=?;");
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
				plugin.getFakeCommander().addToCache(name, pi);
				// Throw jail event
				plugin.getServer().getPluginManager()
						.callEvent(new JailEvent("JailEvent", pi));
				// Broadcast if necessary
				if (config.broadcastJail) {
					// Setup broadcast string
					final StringBuilder sb = new StringBuilder();
					sb.append(ChatColor.AQUA + pi.name + ChatColor.RED
							+ " was jailed on " + ChatColor.GREEN + pi.date
							+ ChatColor.RED + " by " + ChatColor.GOLD
							+ pi.jailer);
					if (!pi.reason.equals("")) {
						sb.append(ChatColor.RED + " for " + ChatColor.GRAY
								+ plugin.colorizeText(pi.reason));
					}
					if (pi.mute) {
						sb.append(ChatColor.GRAY + " - " + ChatColor.DARK_RED
								+ "MUTED");
					}
					// Broadcast
					if (config.broadcastPerms) {
						plugin.getServer().broadcast(sb.toString(),
								"KarmicJail.broadcast");
					} else {
						plugin.getServer().broadcastMessage(sb.toString());
					}
				}
			} catch (SQLException e) {
				plugin.getLogger().warning("SQL Exception on jail command");
				e.printStackTrace();
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
	public static void unjailPlayer(CommandSender sender, String inName,
			boolean fromTempJail) {
		String name = playerInDatabase(inName);
		if (name == null) {
			name = inName;
		}
		// Check if player is in jail:
		if (getPlayerStatus(name).equals("" + JailStatus.FREED)) {
			sender.sendMessage(ChatColor.RED + "That player is not in jail!");
			return;
		}

		// Grab player if on server
		Player player = plugin.getServer().getPlayer(name);
		// Remove jail group
		perm.playerRemoveGroup(config.jailLoc.getWorld(), name,
				config.jailGroup);
		if (config.removeGroups) {
			// Return previous groups
			returnGroups(name);
		}

		// Clear other columns
		database.standardQuery("UPDATE "
				+ config.tablePrefix
				+ "jailed SET time='0',jailer='',date='',reason='' WHERE playername='"
				+ name + "';");
		plugin.getFakeCommander().removeFromCache(name);
		// Check if player is offline:
		if (player == null) {
			setPlayerStatus(JailStatus.PENDINGFREE, name);
			sender.sendMessage(ChatColor.GOLD + name + ChatColor.AQUA
					+ " will be released from jail on getLogger()in.");
			return;
		}

		// Move player out of jail
		if (config.unjailTeleport) {
			player.teleport(config.unjailLoc);
		}
		// Change status
		setPlayerStatus(JailStatus.FREED, name);

		// Remove task
		if (KarmicJail.getJailThreads().containsKey(name)) {
			int id = KarmicJail.getJailThreads().get(name).getId();
			if (id != -1) {
				plugin.getServer().getScheduler().cancelTask(id);
			}
			KarmicJail.removeTask(name);
		}
		player.sendMessage(ChatColor.AQUA + "You have been released from jail!");
		if (fromTempJail) {
			// Also notify jailer if they're online
			Player jailer = plugin.getServer().getPlayer(getJailer(name));
			if (jailer != null) {
				jailer.sendMessage(ChatColor.GOLD + player.getName()
						+ ChatColor.AQUA + " auto-unjailed.");
			}
			// Notify sender
			sender.sendMessage(ChatColor.GOLD + player.getName()
					+ ChatColor.AQUA + " auto-unjailed.");
		} else {
			sender.sendMessage(ChatColor.GOLD + name + ChatColor.AQUA
					+ " removed from jail.");
		}
		// Broadcast if necessary
		if (config.broadcastUnjail) {
			// Setup broadcast string
			final StringBuilder sb = new StringBuilder();
			sb.append(ChatColor.AQUA + name);
			if (fromTempJail) {
				sb.append(ChatColor.RED + " was auto-unjailed by ");
			} else {
				sb.append(ChatColor.RED + " was unjailed by ");
			}
			sb.append(ChatColor.GOLD + sender.getName());
			// Broadcast
			if (config.broadcastPerms) {
				plugin.getServer().broadcast(sb.toString(), "KarmicJail.broadcast");
			} else {
				plugin.getServer().broadcastMessage(sb.toString());
			}
		}
	}

	public static void unjailPlayer(CommandSender sender, String name) {
		unjailPlayer(sender, name, false);
	}

	
	/**
	 * Checks if the player was jailed while offline
	 * 
	 * @param Name
	 *            of player
	 * @return True if pending jailed, else false
	 */
	public static boolean playerIsPendingJail(String player) {
		boolean jailed = false;
		String name = playerInDatabase(player);
		if (name == null) {
			name = player;
		}
		try {
			Query rs = database.select("SELECT * FROM " + config.tablePrefix
					+ "jailed WHERE playername='" + name + "';");
			if (rs.getResult().next()) {
				final String status = rs.getResult().getString("status");
				if (status.equals("" + JailStatus.PENDINGJAIL)) {
					jailed = true;
				}
			}
			rs.closeQuery();
		} catch (SQLException e) {
			plugin.getLogger().warning(KarmicJail.prefix + " SQL Exception");
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
	public static boolean playerIsJailed(String player) {
		boolean jailed = false;
		boolean missing = false;
		String name = playerInDatabase(player);
		if (name == null) {
			name = player;
		}
		try {
			Query rs = database.select("SELECT * FROM " + config.tablePrefix
					+ "jailed WHERE playername='" + name + "';");
			if (rs.getResult().next()) {
				final String status = rs.getResult().getString("status");
				if (rs.getResult().wasNull()) {
					plugin.getLogger().severe(KarmicJail.prefix + " MISSING STATUS FOR: " + name);
					missing = true;
				} else if (status.equals("" + JailStatus.JAILED)) {
					jailed = true;
				}
			}
			rs.closeQuery();
			if (missing) {
				setPlayerStatus(JailStatus.FREED, name);
			}
		} catch (SQLException e) {
			plugin.getLogger().warning(KarmicJail.prefix + " SQL Exception");
			e.printStackTrace();
		}
		return jailed;
	}
	
	/**
	 * Grabs player's time left in jail
	 * 
	 * @param name
	 *            of player
	 * @return long of time left to serve
	 */
	public static long getPlayerTime(String player) {
		long time = 0;
		String name = playerInDatabase(player);
		if (name == null) {
			name = player;
		}
		try {
			Query rs = database.select("SELECT * FROM " + config.tablePrefix
					+ "jailed WHERE playername='" + name + "';");
			if (rs.getResult().next()) {
				time = rs.getResult().getLong("time");
				if (rs.getResult().wasNull()) {
					time = 0;
				}
			}
			rs.closeQuery();
		} catch (SQLException e) {
			plugin.getLogger().warning(KarmicJail.prefix + " SQL Exception");
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
	public static void updatePlayerTime(String player, long duration) {
		String name = playerInDatabase(player);
		if (name == null) {
			name = player;
		}
		database.standardQuery("UPDATE " + config.tablePrefix
				+ "jailed SET time='" + duration + "' WHERE playername='"
				+ name + "';");
	}
	
	/**
	 * Check if player exists in master database
	 * 
	 * @param name
	 *            of player
	 * @return true if player is in database, else false
	 */
	public static String playerInDatabase(String name) {
		String has = null;
		try {
			Query rs = database.select("SELECT * FROM " + config.tablePrefix
					+ "jailed;");
			if (rs.getResult().next()) {
				do {
					if (name.equalsIgnoreCase(rs.getResult().getString(
							"playername"))) {
						has = rs.getResult().getString("playername");
						break;
					}
				} while (rs.getResult().next());
			}
			rs.closeQuery();
		} catch (SQLException e) {
			plugin.getLogger().warning(KarmicJail.prefix + " SQL Exception");
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
	public static void addPlayerToDatabase(String name) {
		try {
			boolean has = false;
			Query rs = database.select("SELECT COUNT(*) FROM "
					+ config.tablePrefix + "jailed WHERE playername='" + name
					+ "';");
			if (rs.getResult().next()) {
				final int count = rs.getResult().getInt(1);
				if (!rs.getResult().wasNull()) {
					if (count > 0) {
						has = true;
					}
				}
			}
			rs.closeQuery();
			if (!has) {
				// Add to database
				database.standardQuery("INSERT INTO " + config.tablePrefix
						+ "jailed (playername,status,time) VALUES ('" + name
						+ "', '" + JailStatus.FREED + "', '0');");
			}
		} catch (SQLException e) {
			plugin.getLogger().warning(KarmicJail.prefix + " SQL Exception");
			e.printStackTrace();
		}
	}
	
	/**
	 * Checks to see if the player has a time associated with their jail
	 * sentence
	 * 
	 * @param Name
	 *            of player
	 * @return true if player has a valid time, else false
	 */
	public static boolean playerIsTempJailed(String player) {
		double time = 0;
		boolean missing = false;
		String name = playerInDatabase(player);
		if (name == null) {
			name = player;
		}
		try {
			Query rs = database.select("SELECT * FROM " + config.tablePrefix
					+ "jailed WHERE playername='" + name + "';");
			if (rs.getResult().next()) {
				time = rs.getResult().getDouble("time");
				if (rs.getResult().wasNull()) {
					time = 0;
					missing = true;
				}
			}
			rs.closeQuery();
			if (missing) {
				setJailTime(console, name, 0);
				plugin.getLogger().warning(
						KarmicJail.prefix + " " + name
								+ "'s Time was missing. Reset to 0.");
			}
		} catch (SQLException e) {
			plugin.getLogger().warning(KarmicJail.prefix + " SQL Exception");
			e.printStackTrace();
		}
		if (time > 0) {
			return true;
		}
		return false;
	}
	
	public static void setJailTime(CommandSender sender, String name, int minutes) {
		// Check if player is in jail:
		if (!playerIsJailed(name) && !playerIsPendingJail(name)) {
			sender.sendMessage(ChatColor.RED + "That player is not in jail!");
			return;
		}

		// Grab player if on server
		Player player = plugin.getServer().getPlayer(name);
		// Remove task
		if (KarmicJail.getJailThreads().containsKey(name)) {
			int id = KarmicJail.getJailThreads().get(name).getId();
			if (id != -1) {
				plugin.getServer().getScheduler().cancelTask(id);
			}
			KarmicJail.removeTask(name);
		}
		// Jail indefinitely if 0 or negative
		if (minutes <= 0) {
			updatePlayerTime(name, minutes);
			sender.sendMessage(ChatColor.RED + name + ChatColor.AQUA
					+ " is jailed forever.");
			if (player != null) {
				player.sendMessage(ChatColor.AQUA + "Jailed forever.");
			}
		} else {
			// Calculate time
			long duration = 0;
			duration = minutes * KarmicJail.minutesToTicks;
			updatePlayerTime(name, duration);
			if (player != null) {
				// Create thread to release player
				KarmicJail.getJailThreads().put(name, new JailTask(plugin, name, duration));
			}
			sender.sendMessage(ChatColor.AQUA + "Time set to " + ChatColor.GOLD
					+ minutes + ChatColor.AQUA + " for " + ChatColor.RED + name
					+ ChatColor.AQUA + ".");
			if (player != null) {
				player.sendMessage(ChatColor.AQUA + "Time set to "
						+ ChatColor.GOLD + minutes + ChatColor.AQUA + ".");
			}
		}

	}
	
	public static void setPlayerReason(String inName, String reason) {
		String name = playerInDatabase(inName);
		if (name == null) {
			name = inName;
		}
		try {
			final PreparedStatement statement = database.prepare("UPDATE "
					+ config.tablePrefix
					+ "jailed SET reason=? WHERE playername='" + name + "';");
			statement.setString(1, reason);
			statement.executeUpdate();
			statement.close();
			// broadcast
			if (config.broadcastReason) {
				final String out = ChatColor.AQUA + name + ChatColor.RED
						+ " for " + ChatColor.GRAY + plugin.colorizeText(reason);
				if (config.broadcastPerms) {
					plugin.getServer().broadcast(out, "KarmicJail.broadcast");
				} else {
					plugin.getServer().broadcastMessage(out);
				}
			}
		} catch (SQLException e) {
			plugin.getLogger().warning("SQL Exception on jail command");
			e.printStackTrace();
		}
	}
	
	/**
	 * Grabs the reason for being in jail
	 * 
	 * @param name
	 *            of player
	 * @return String of jailer's reason
	 */
	public static String getJailReason(String player) {
		String reason = "";
		String name = playerInDatabase(player);
		if (name == null) {
			name = player;
		}
		try {
			Query rs = database.select("SELECT * FROM " + config.tablePrefix
					+ "jailed WHERE playername='" + name + "';");
			if (rs.getResult().next()) {
				reason = rs.getResult().getString("reason");
				if (rs.getResult().wasNull()) {
					reason = "";
				}
			}
			rs.closeQuery();
		} catch (SQLException e) {
			plugin.getLogger().warning(KarmicJail.prefix + " SQL Exception");
			e.printStackTrace();
		}
		return reason;
	}
	
	public static boolean playerIsMuted(String player) {
		boolean mute = false;
		String name = playerInDatabase(player);
		if (name == null) {
			name = player;
		}
		try {
			Query rs = database.select("SELECT * FROM " + config.tablePrefix
					+ "jailed WHERE playername='" + name + "';");
			if (rs.getResult().next()) {
				int muteInt = rs.getResult().getInt("muted");
				if (!rs.getResult().wasNull()) {
					if (muteInt == 1) {
						mute = true;
					}
				}
			}
			rs.closeQuery();
		} catch (SQLException e) {
			plugin.getLogger().warning(KarmicJail.prefix + " SQL Exception");
			e.printStackTrace();
		}
		return mute;
	}
	
	public static void mutePlayer(CommandSender sender, String player) {
		String name = playerInDatabase(player);
		if (name == null) {
			name = player;
		}
		// Check if player is in jail:
		if (!playerIsJailed(name) && !playerIsPendingJail(name)) {
			sender.sendMessage(ChatColor.RED + "That player is not in jail!");
			return;
		}
		if (playerIsMuted(name)) {
			database.standardQuery("UPDATE " + config.tablePrefix
					+ "jailed SET muted='0' WHERE playername='" + name + "';");
			sender.sendMessage(ChatColor.GOLD + name + ChatColor.GREEN
					+ " unmuted");
		} else {
			database.standardQuery("UPDATE " + config.tablePrefix
					+ "jailed SET muted='1' WHERE playername='" + name + "';");
			sender.sendMessage(ChatColor.GOLD + name + ChatColor.RED + " muted");
		}
	}
	
	/**
	 * Returns the player's current status
	 * 
	 * @param name
	 *            of player
	 * @return String of the player's JailStatus
	 */
	public static String getPlayerStatus(String inName) {
		boolean found = true;
		String status = "" + JailStatus.FREED;
		String name = playerInDatabase(inName);
		if (name == null) {
			name = inName;
		}
		try {
			Query rs = database.select("SELECT * FROM " + config.tablePrefix
					+ "jailed WHERE playername='" + name + "';");
			if (rs.getResult().next()) {
				status = rs.getResult().getString("status");
				if (rs.getResult().wasNull()) {
					status = "" + JailStatus.FREED;
					found = false;
				}
			} else {
				found = false;
			}
			rs.closeQuery();

		} catch (SQLException e) {
			plugin.getLogger().warning(KarmicJail.prefix + " SQL Exception");
			e.printStackTrace();
		}
		if (found) {
			if (status.equals("" + JailStatus.JAILED)) {
				status = "" + JailStatus.JAILED;
			} else if (status.equals("" + JailStatus.PENDINGFREE)) {
				status = "" + JailStatus.PENDINGFREE;
			} else if (status.equals("" + JailStatus.PENDINGJAIL)) {
				status = "" + JailStatus.PENDINGJAIL;
			}
		} else {
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
	public static void setPlayerStatus(JailStatus status, String inName) {
		String name = playerInDatabase(inName);
		if (name == null) {
			name = inName;
		}
		database.standardQuery("UPDATE " + config.tablePrefix
				+ "jailed SET status='" + status + "' WHERE playername='"
				+ name + "';");
	}
	
	/**
	 * Saves the player's groups into database
	 * 
	 * @param name
	 *            of player
	 */
	private static void savePlayerGroups(String name) {
		StringBuilder sb = new StringBuilder();
		boolean append = false;
		for (String s : getGroups(name)) {
			sb.append(s + "&");
			append = true;
		}
		if (append) {
			sb.deleteCharAt(sb.length() - 1);
		}
		database.standardQuery("UPDATE " + config.tablePrefix
				+ "jailed SET groups='" + sb.toString()
				+ "' WHERE playername='" + name + "';");
	}

	/**
	 * Removes all groups of a player
	 * 
	 * @param name
	 *            of player
	 */
	private static void removePlayerGroups(String name) {
		if (perm.getName().equals("PermissionsBukkit")) {
			final PermissionsPlugin permission = (PermissionsPlugin) plugin
					.getServer().getPluginManager()
					.getPlugin("PermissionsBukkit");
			for (Group group : permission.getGroups(name)) {
				perm.playerRemoveGroup(plugin.getServer().getWorlds().get(0),
						name, group.getName());
			}
		} else {
			for (World w : plugin.getServer().getWorlds()) {
				String[] groups = perm.getPlayerGroups(w, name);
				for (String group : groups) {
					perm.playerRemoveGroup(w, name, group);
				}
			}
		}
	}

	/**
	 * Returns a list of all the groups a player has
	 * 
	 * @param name
	 *            of player
	 * @return List of groups with associated world
	 */
	public static List<String> getGroups(String player)
	{
		List<String> list = new ArrayList<String>();
		if (perm.getName().equals("PermissionsBukkit"))
		{
			final PermissionsPlugin permission = (PermissionsPlugin) plugin
					.getServer().getPluginManager()
					.getPlugin("PermissionsBukkit");
			for (Group group : permission.getGroups(player))
			{
				final String s = group.getName() + "!"
						+ plugin.getServer().getWorlds().get(0).getName();
				list.add(s);
			}
		}
		else
		{
			for (World w : plugin.getServer().getWorlds())
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
	 * Restores the players groups from database storage
	 * 
	 * @param name
	 *            of player
	 */
	private static void returnGroups(String name) {
		try {
			Query rs = database.select("SELECT * FROM "
					+ config.tablePrefix + "jailed WHERE playername='" + name
					+ "';");
			if (rs.getResult().next()) {
				String groups = rs.getResult().getString("groups");
				if (!rs.getResult().wasNull() && !groups.equals(""))

					if (groups.contains("&")) {
						String[] cut = groups.split("&");
						for (String group : cut) {
							String[] split = group.split("!");
							perm.playerAddGroup(split[1], name, split[0]);
						}
					} else {
						String[] split = groups.split("!");
						perm.playerAddGroup(split[1], name, split[0]);
					}
			}
			rs.closeQuery();
		} catch (SQLException e) {
			plugin.getLogger().warning(KarmicJail.prefix + " SQL Exception");
			e.printStackTrace();
		}
	}

	/**
	 * Gets the status of a player
	 * 
	 * @param sender
	 *            of command
	 * @param arguments
	 *            of command
	 */
	public static void jailStatus(CommandSender sender, String[] args) {
		if (!(sender instanceof Player) && args.length == 0) {
			sender.sendMessage(ChatColor.RED + "Must specify a player.");
			return;
		}
		final Player player = (args.length == 0) ? (Player) sender : plugin
				.getServer().getPlayer(args[0]);
		String name = "";
		if (player == null) {
			name = args[0];
		} else {
			name = player.getName();
		}

		if (!playerIsJailed(name) && !playerIsPendingJail(name)) {
			if (args.length == 0)
				sender.sendMessage(ChatColor.RED + "You are not jailed.");
			else
				sender.sendMessage(ChatColor.AQUA + name + ChatColor.RED
						+ " is not jailed.");
			return;
		}

		final StringBuilder sb = new StringBuilder();
		final String date = getJailDate(name);
		final String jailer = getJailer(name);
		final String reason = getJailReason(name);
		final boolean muted = playerIsMuted(name);
		if (args.length == 0) {
			sb.append(ChatColor.RED + "Jailed on " + ChatColor.GREEN + date
					+ ChatColor.RED + " by " + ChatColor.GOLD + jailer);
		} else {
			sb.append(ChatColor.AQUA + name + ChatColor.RED + " was jailed on "
					+ ChatColor.GREEN + date + ChatColor.RED + " by "
					+ ChatColor.GOLD + jailer);
		}
		if (!reason.equals("")) {
			sb.append(ChatColor.RED + " for " + ChatColor.GRAY
					+ plugin.colorizeText(reason));
		}
		if (muted) {
			sb.append(ChatColor.GRAY + " - " + ChatColor.DARK_RED + "MUTED");
		}
		sender.sendMessage(sb.toString());
		if (playerIsTempJailed(name)) {
			int minutes = (int) ((getPlayerTime(name) / KarmicJail.minutesToTicks));
			if (player == null) {
				sender.sendMessage(ChatColor.AQUA + "Remaining jail time: "
						+ ChatColor.GOLD + plugin.prettifyMinutes(minutes));
			} else {
				// Player is online, check the thread for their remaining time
				if (KarmicJail.getJailThreads().containsKey(name)) {
					minutes = (int) (KarmicJail.getJailThreads().get(name).remainingTime() / KarmicJail.minutesToTicks);
					sender.sendMessage(ChatColor.AQUA + "Remaining jail time: "
							+ plugin.prettifyMinutes(minutes));
				}
			}
		}
	}
	
	/**
	 * Gets name of the jailer
	 * 
	 * @param name
	 *            of person in jail
	 * @return name of jailer
	 */
	private static String getJailer(String name) {
		String jailer = "NOBODY";
		try {
			Query rs = database.select("SELECT * FROM " + config.tablePrefix
					+ "jailed WHERE playername='" + name + "';");
			if (rs.getResult().next()) {
				jailer = rs.getResult().getString("jailer");
				if (rs.getResult().wasNull()) {
					jailer = "NOBODY";
				}
			}
			rs.closeQuery();
		} catch (SQLException e) {
			plugin.getLogger().warning(KarmicJail.prefix + " SQL Exception");
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
	private static String getJailDate(String name) {
		String date = "";
		try {
			Query rs = database.select("SELECT * FROM " + config.tablePrefix
					+ "jailed WHERE playername='" + name + "';");
			if (rs.getResult().next()) {
				date = rs.getResult().getString("date");
				if (rs.getResult().wasNull()) {
					date = "NO DATE";
				}
			}
			rs.closeQuery();
		} catch (SQLException e) {
			plugin.getLogger().warning(KarmicJail.prefix + " SQL Exception");
			e.printStackTrace();
		}
		return date;
	}
	
	/**
	 * Sets the jail location
	 * 
	 * @param sender
	 *            of command
	 * @param arguments
	 *            of command
	 */
	public static void setJail(CommandSender sender, String[] args) {
		if (!(sender instanceof Player) && args.length != 4) {
			sender.sendMessage(ChatColor.RED + "Only players can use that.");
			return;
		}
		if (args.length == 0) {
			Player player = (Player) sender;
			config.jailLoc = player.getLocation();
		} else {
			if (!(new Scanner(args[0]).hasNextInt())
					|| !(new Scanner(args[1]).hasNextInt())
					|| !(new Scanner(args[2]).hasNextInt())) {
				sender.sendMessage(ChatColor.RED + "Invalid coordinate.");
				return;
			}
			config.jailLoc = new Location(plugin.getServer().getWorld(args[3]),
					Integer.parseInt(args[0]), Integer.parseInt(args[1]),
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
	 *            of command
	 * @param arguments
	 *            of command
	 */
	public static void setUnjail(CommandSender sender, String[] args) {
		if (!(sender instanceof Player) && args.length != 4) {
			sender.sendMessage(ChatColor.RED + "Only players can use that.");
			return;
		}
		if (args.length == 0) {
			Player player = (Player) sender;
			config.unjailLoc = player.getLocation();
		} else {
			if (!(new Scanner(args[0]).hasNextInt())
					|| !(new Scanner(args[1]).hasNextInt())
					|| !(new Scanner(args[2]).hasNextInt())) {
				sender.sendMessage(ChatColor.RED + "Invalid coordinate.");
				return;
			}
			config.unjailLoc = new Location(plugin.getServer().getWorld(args[3]),
					Integer.parseInt(args[0]), Integer.parseInt(args[1]),
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
	public static Location getJailLocation() {
		return config.jailLoc;
	}

	/**
	 * 
	 * @return location of unjail
	 */
	public static Location getUnjailLocation() {
		return config.unjailLoc;
	}
	
	/**
	 * Teleports a player to unjail locaiton
	 * 
	 * @param name
	 *            of player to be teleported
	 */
	public static void teleportOut(String name) {
		final Player player = plugin.getServer().getPlayer(name);
		if (player != null) {
			player.teleport(config.unjailLoc);
		}
	}
	
}
