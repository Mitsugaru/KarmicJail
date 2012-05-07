package com.mitsugaru.karmicjail;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import lib.Mitsugaru.SQLibrary.Database.Query;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import com.mitsugaru.karmicjail.DBHandler.Table;
import com.mitsugaru.karmicjail.KarmicJail.JailStatus;

public class Config
{
	// Class variables
	private KarmicJail plugin;
	public String host, port, database, user, password, tablePrefix;
	public boolean useMySQL, debugLog, debugEvents, debugTime, debugGroups,
			importSQL, unjailTeleport, jailTeleport, jailTeleportRespawn,
			removeGroups, returnGroups, broadcastJail, broadcastUnjail,
			broadcastReason, broadcastPerms, broadcastJoin, debugUnhandled,
			clearInventory, returnInventory, modifyInventory, timePerm,
			warpAllOnJoin, useJailGroup, useUnjailGroup, denyBlockPlace,
			denyBlockBreak, denyInteract, denyInventory, denyItemPickup,
			denyItemDrop, denyItemCraft, denyItemEnchant, denyChat, denyCommands, denyMove;
	public Location jailLoc, unjailLoc;
	public String jailGroup, unjailGroup;
	public int limit;

	// TODO configuration files per jail location
	/**
	 * Loads config from yaml file
	 */
	public Config(KarmicJail karmicJail)
	{
		plugin = karmicJail;
		// Init config files:
		ConfigurationSection config = plugin.getConfig();
		final Map<String, Object> defaults = new LinkedHashMap<String, Object>();
		// TODO player settings to put jailgroup and event cancelling (such as
		// building/destroying)
		defaults.put("timedJailNeedsPermission", false);
		defaults.put("entrylimit", 10);
		defaults.put("deny.block.break", true);
		defaults.put("deny.block.place", true);
		defaults.put("deny.chat", false);
		defaults.put("deny.commands", true);
		defaults.put("deny.interact", true);
		defaults.put("deny.inventory", false);
		defaults.put("deny.item.craft", true);
		defaults.put("deny.item.drop", true);
		defaults.put("deny.item.enchant", true);
		defaults.put("deny.item.pickup", true);
		defaults.put("deny.move", false);
		defaults.put("group.removeOnJail", true);
		defaults.put("group.returnOnUnjail", true);
		defaults.put("group.jail.group", "Jailed");
		defaults.put("group.jail.use", true);
		defaults.put("group.unjail.group", "Default");
		defaults.put("group.unjail.use", false);
		defaults.put("jail.world", plugin.getServer().getWorlds().get(0)
				.getName());
		defaults.put("jail.x", 0);
		defaults.put("jail.y", 0);
		defaults.put("jail.z", 0);
		defaults.put("jail.warpAllOnJoin", false);
		// TODO separate this for login and command as well instead of both
		defaults.put("jail.teleport", true);
		defaults.put("jail.teleportRespawn", true);
		defaults.put("unjail.world", plugin.getServer().getWorlds().get(0)
				.getName());
		defaults.put("unjail.x", 0);
		defaults.put("unjail.y", 0);
		defaults.put("unjail.z", 0);
		defaults.put("unjail.teleport", true);
		defaults.put("inventory.clearOnJail", true);
		defaults.put("inventory.returnOnUnjail", true);
		defaults.put("inventory.modify", true);
		defaults.put("broadcast.jail", false);
		defaults.put("broadcast.unjail", false);
		defaults.put("broadcast.reasonChange", false);
		defaults.put("broadcast.onjoin", false);
		defaults.put("broadcast.ignorePermission", false);
		defaults.put("mysql.use", false);
		defaults.put("mysql.host", "localhost");
		defaults.put("mysql.port", 3306);
		defaults.put("mysql.database", "minecraft");
		defaults.put("mysql.user", "username");
		defaults.put("mysql.password", "pass");
		defaults.put("mysql.tablePrefix", "kj_");
		defaults.put("mysql.import", false);
		defaults.put("debug.logToConsole", false);
		defaults.put("debug.events", false);
		defaults.put("debug.groups", false);
		defaults.put("debug.time", false);
		defaults.put("debug.unhandled", false);
		defaults.put("version", plugin.getDescription().getVersion());

		// Insert defaults into config file if they're not present
		for (final Entry<String, Object> e : defaults.entrySet())
		{
			if (!config.contains(e.getKey()))
			{
				config.set(e.getKey(), e.getValue());
			}
		}
		// Save config
		plugin.saveConfig();

		// Load sql info from config
		useMySQL = config.getBoolean("mysql.use", false);
		host = config.getString("mysql.host", "localhost");
		port = config.getString("mysql.port", "3306");
		database = config.getString("mysql.database", "minecraft");
		user = config.getString("mysql.user", "user");
		password = config.getString("mysql.password", "password");
		tablePrefix = config.getString("mysql.prefix", "kj_");
		importSQL = config.getBoolean("mysql.import", false);
		// Load variables
		loadVariables();
		boundsCheck();
	}

	public void reload()
	{
		// Reload
		plugin.reloadConfig();
		loadVariables();
		boundsCheck();
	}

	public void loadVariables()
	{
		// Grab config
		ConfigurationSection config = plugin.getConfig();
		// Load variables from config
		jailLoc = new Location(plugin.getServer().getWorld(
				config.getString("jail.world", plugin.getServer().getWorlds()
						.get(0).getName())), config.getInt("jail.x", 0),
				config.getInt("jail.y", 0), config.getInt("jail.z", 0));
		unjailLoc = new Location(plugin.getServer().getWorld(
				config.getString("unjail.world", plugin.getServer().getWorlds()
						.get(0).getName())), config.getInt("unjail.x", 0),
				config.getInt("unjail.y", 0), config.getInt("unjail.z", 0));
		jailGroup = config.getString("group.jail.group", "Jailed");
		useJailGroup = config.getBoolean("group.jail.use", true);
		unjailGroup = config.getString("group.unjail.group", "Default");
		useUnjailGroup = config.getBoolean("group.unjail.use", false);
		debugLog = config.getBoolean("debug.logToConsole", false);
		debugEvents = config.getBoolean("debug.events", false);
		debugGroups = config.getBoolean("debug.groups", false);
		debugTime = config.getBoolean("debug.time", false);
		debugUnhandled = config.getBoolean("debug.unhandled", false);
		limit = config.getInt("entrylimit", 10);
		jailTeleport = config.getBoolean("unjail.teleport", true);
		jailTeleportRespawn = config.getBoolean("jail.teleportRespawn", true);
		unjailTeleport = config.getBoolean("unjail.teleport", true);
		broadcastJail = config.getBoolean("broadcast.jail", false);
		broadcastUnjail = config.getBoolean("broadcast.unjail", false);
		broadcastReason = config.getBoolean("broadcast.reason", false);
		broadcastPerms = !config
				.getBoolean("broadcast.ignorePermission", false);
		broadcastJoin = config.getBoolean("broadcast.onjoin", false);
		removeGroups = config.getBoolean("group.removeOnJail", true);
		returnGroups = config.getBoolean("group.returnOnUnjail", true);
		clearInventory = config.getBoolean("inventory.clearOnJail", true);
		returnInventory = config.getBoolean("inventory.returnOnUnjail", true);
		modifyInventory = config.getBoolean("inventory.modify", true);
		timePerm = config.getBoolean("timedJailNeedsPermission", false);
		warpAllOnJoin = config.getBoolean("jail.warpAllOnJoin", false);
		denyBlockBreak = config.getBoolean("deny.block.break", true);
		denyBlockPlace = config.getBoolean("deny.block.place", true);
		denyChat = config.getBoolean("deny.chat", false);
		denyCommands = config.getBoolean("deny.commands", true);
		denyInteract = config.getBoolean("deny.interact", true);
		denyInventory = config.getBoolean("deny.inventory", false);
		denyItemCraft = config.getBoolean("deny.item.craft", true);
		denyItemDrop = config.getBoolean("deny.item.drop", true);
		denyItemEnchant = config.getBoolean("deny.item.enchant", true);
		denyItemPickup = config.getBoolean("deny.item.pickup", true);
		denyMove = config.getBoolean("deny.move", false);
	}

	private void boundsCheck()
	{
		// Grab config
		ConfigurationSection config = plugin.getConfig();
		// Bounds check on the limit
		if (limit <= 0 || limit > 16)
		{
			plugin.getLogger()
					.warning(
							KarmicJail.prefix
									+ " Entry limit is <= 0 || > 16. Reverting to default: 10");
			limit = 10;
			config.set("entrylimit", 10);
		}
	}

	/**
	 * Check if updates are necessary
	 */
	public void checkUpdate()
	{
		// Check if need to update
		ConfigurationSection config = plugin.getConfig();
		if (Double.parseDouble(plugin.getDescription().getVersion()) > Double
				.parseDouble(config.getString("version")))
		{
			// Update to latest version
			plugin.getLogger().info(
					KarmicJail.prefix + " Updating to v"
							+ plugin.getDescription().getVersion());
			update();
		}
	}

	/**
	 * This method is called to make the appropriate changes, most likely only
	 * necessary for database schema modification, for a proper update.
	 */
	private void update()
	{
		// Grab current version
		double ver = Double
				.parseDouble(plugin.getConfig().getString("version"));
		String query = "";
		// Update to 0.2
		if (ver < 0.2)
		{
			// Add mute column
			query = "ALTER TABLE jailed ADD muted INTEGER;";
			plugin.getDatabaseHandler().standardQuery(query);
		}
		// Update to 0.3
		if (ver < 0.3)
		{
			// Drop newly created tables
			plugin.getLogger().info(
					KarmicJail.prefix + " Dropping empty tables.");
			plugin.getDatabaseHandler().standardQuery(
					"DROP TABLE " + tablePrefix + "jailed;");
			// Update tables to have prefix
			plugin.getLogger().info(
					KarmicJail.prefix + " Renaming jailed table to '"
							+ tablePrefix + "jailed'.");
			query = "ALTER TABLE jailed RENAME TO " + tablePrefix + "jailed;";
			plugin.getDatabaseHandler().standardQuery(query);
		}
		// Update to 0.4
		if (ver < 0.4)
		{
			try
			{
				final Set<PointThreeObject> entries = new HashSet<PointThreeObject>();
				plugin.getLogger().info(
						KarmicJail.prefix + " Converting table '"
								+ Table.JAILED.getName() + "' ...");
				// Grab old entries
				final Query rs = plugin.getDatabaseHandler().select(
						"SELECT * FROM " + Table.JAILED.getName() + ";");
				if (rs.getResult().next())
				{
					do
					{
						// save entry
						String name = rs.getResult().getString("playername");
						String status = rs.getResult().getString("status");
						if (rs.getResult().wasNull())
						{
							status = JailStatus.FREED + "";
						}
						double time = rs.getResult().getDouble("time");
						if (rs.getResult().wasNull())
						{
							time = -1;
						}
						String groups = rs.getResult().getString("groups");
						if (rs.getResult().wasNull())
						{
							groups = "";
						}
						String jailer = rs.getResult().getString("jailer");
						if (rs.getResult().wasNull())
						{
							jailer = "";
						}
						String date = rs.getResult().getString("date");
						if (rs.getResult().wasNull())
						{
							date = "";
						}
						String reason = rs.getResult().getString("reason");
						if (rs.getResult().wasNull())
						{
							reason = "";
						}
						int muted = rs.getResult().getInt("muted");
						if (rs.getResult().wasNull())
						{
							muted = 0;
						}
						entries.add(new PointThreeObject(name, status, groups,
								jailer, date, reason, time, muted));
					} while (rs.getResult().next());
				}
				rs.closeQuery();
				// Drop old table
				plugin.getDatabaseHandler().standardQuery(
						"DROP TABLE " + Table.JAILED.getName() + ";");
				// Create new table
				if (useMySQL)
				{
					query = "CREATE TABLE "
							+ Table.JAILED.getName()
							+ " (id INT UNSIGNED NOT NULL AUTO_INCREMENT, playername varchar(32) NOT NULL, status TEXT NOT NULL, time REAL NOT NULL, groups TEXT, jailer varchar(32), date TEXT, reason TEXT, muted INT, lastpos TEXT, UNIQUE (playername), PRIMARY KEY(id));";
				}
				else
				{
					query = "CREATE TABLE "
							+ Table.JAILED.getName()
							+ " (id INTEGER PRIMARY KEY, playername varchar(32) NOT NULL, status TEXT NOT NULL, time REAL NOT NULL, groups TEXT, jailer varchar(32), date TEXT, reason TEXT, muted INTEGER, lastpos TEXT, UNIQUE (playername));";
				}
				plugin.getDatabaseHandler().createTable(query);
				// Add back entries
				if (!entries.isEmpty())
				{
					PreparedStatement statement = plugin
							.getDatabaseHandler()
							.prepare(
									"INSERT INTO "
											+ Table.JAILED.getName()
											+ " (playername,status,time,groups,jailer,date,reason,muted,lastpos) VALUES(?,?,?,?,?,?,?,?,?)");
					for (PointThreeObject entry : entries)
					{
						statement.setString(1, entry.playername);
						statement.setString(2, entry.status);
						statement.setDouble(3, entry.time);
						statement.setString(4, entry.groups);
						statement.setString(5, entry.jailer);
						statement.setString(6, entry.date);
						statement.setString(7, entry.reason);
						statement.setInt(8, entry.mute);
						statement.setString(9, "");
						try
						{
							statement.executeUpdate();
						}
						catch (SQLException s)
						{
							plugin.getLogger().warning(
									KarmicJail.prefix + " SQLException");
							s.printStackTrace();
						}
					}
					statement.close();
				}
				plugin.getLogger().info(
						KarmicJail.prefix + " Conversion of table '"
								+ Table.JAILED.getName() + "' finished.");
			}
			catch (SQLException e)
			{
				plugin.getLogger().warning(
						KarmicJail.prefix + " SQL Exception on 0.4 update");
				e.printStackTrace();
			}

		}
		if (ver < 0.43)
		{
			// Update config
			plugin.getLogger().info("Updating config");
			jailGroup = plugin.getConfig().getString("jailgroup", "Jailed");
			plugin.getConfig().set("group.jail.group", jailGroup);
			plugin.getConfig().set("jailgroup", null);
			removeGroups = plugin.getConfig().getBoolean("removegroups", true);
			plugin.getConfig().set("group.removeOnJail", removeGroups);
			plugin.getConfig().set("removegroups", null);
		}
		// Update version number in config.yml
		plugin.getConfig().set("version", plugin.getDescription().getVersion());
		plugin.saveConfig();
	}

	public void set(String path, Object o)
	{
		final ConfigurationSection config = plugin.getConfig();
		config.set(path, o);
		plugin.saveConfig();
	}

	private class PointThreeObject
	{
		public String playername, status, groups, jailer, date, reason;
		public double time;
		public int mute;

		public PointThreeObject(String name, String status, String groups,
				String jailer, String date, String reason, double time, int mute)
		{
			this.playername = name;
			this.status = status;
			this.groups = groups;
			this.jailer = jailer;
			this.date = date;
			this.reason = reason;
			this.time = time;
			this.mute = mute;
		}
	}
}
