package com.mitsugaru.karmicjail.update;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;


import org.bukkit.configuration.ConfigurationSection;

import com.mitsugaru.karmicjail.KarmicJail;
import com.mitsugaru.karmicjail.database.Table;
import com.mitsugaru.karmicjail.KarmicJail.JailStatus;
import com.mitsugaru.karmicjail.database.SQLibrary.Database.Query;
import com.mitsugaru.karmicjail.update.holders.PointThreeObject;

public class Update
{
	private static KarmicJail plugin;
	
	public static void init(KarmicJail kj)
	{
		plugin = kj;
	}
	
	/**
	 * Check if updates are necessary
	 */
	public static void checkUpdate()
	{
		// Check if need to update
		ConfigurationSection config = plugin.getConfig();
		if (Double.parseDouble(plugin.getDescription().getVersion()) > Double
				.parseDouble(config.getString("version")))
		{
			// Update to latest version
			plugin.getLogger().info(
					"Updating to v" + plugin.getDescription().getVersion());
			update();
		}
	}
	
	/**
	 * This method is called to make the appropriate changes, most likely only
	 * necessary for database schema modification, for a proper update.
	 */
	private static void update()
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
					KarmicJail.TAG + " Dropping empty tables.");
			plugin.getDatabaseHandler().standardQuery(
					"DROP TABLE " + Table.JAILED.getName() + ";");
			// Update tables to have prefix
			plugin.getLogger().info(
					KarmicJail.TAG + " Renaming jailed table to '"
							+ Table.JAILED.getName() + "'.");
			query = "ALTER TABLE jailed RENAME TO " + Table.JAILED.getName() + ";";
			plugin.getDatabaseHandler().standardQuery(query);
		}
		// Update to 0.4
		if (ver < 0.4)
		{
			try
			{
				final Set<PointThreeObject> entries = new HashSet<PointThreeObject>();
				plugin.getLogger().info(
						KarmicJail.TAG + " Converting table '"
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
				if (plugin.getPluginConfig().useMySQL)
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
									KarmicJail.TAG + " SQLException");
							s.printStackTrace();
						}
					}
					statement.close();
				}
				plugin.getLogger().info(
						KarmicJail.TAG + " Conversion of table '"
								+ Table.JAILED.getName() + "' finished.");
			}
			catch (SQLException e)
			{
				plugin.getLogger().warning(
						KarmicJail.TAG + " SQL Exception on 0.4 update");
				e.printStackTrace();
			}

		}
		if (ver < 0.43)
		{
			// Update config
			plugin.getLogger().info("Updating config");
			plugin.getPluginConfig().jailGroup = plugin.getConfig().getString("jailgroup", "Jailed");
			plugin.getConfig().set("group.jail.group", plugin.getPluginConfig().jailGroup);
			plugin.getConfig().set("jailgroup", null);
			plugin.getPluginConfig().removeGroups = plugin.getConfig().getBoolean("removegroups", true);
			plugin.getConfig().set("group.removeOnJail", plugin.getPluginConfig().removeGroups);
			plugin.getConfig().set("removegroups", null);
		}
		// Update version number in config.yml
		plugin.getConfig().set("version", plugin.getDescription().getVersion());
		plugin.saveConfig();
	}
}
