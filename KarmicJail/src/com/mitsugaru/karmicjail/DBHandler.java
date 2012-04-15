package com.mitsugaru.karmicjail;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.mitsugaru.karmicjail.KarmicJail.JailStatus;
import com.mitsugaru.utils.Config;

import lib.Mitsugaru.SQLibrary.Database.Query;
import lib.Mitsugaru.SQLibrary.MySQL;
import lib.Mitsugaru.SQLibrary.SQLite;

public class DBHandler
{
	// Class Variables
	private KarmicJail plugin;
	private static Config config;
	private SQLite sqlite;
	private MySQL mysql;
	private boolean useMySQL;

	public DBHandler(KarmicJail ks, Config conf)
	{
		plugin = ks;
		config = conf;
		useMySQL = config.useMySQL;
		checkTables();
		if (config.importSQL)
		{
			if (useMySQL)
			{
				importSQL();
			}
			config.set("mysql.import", false);
		}
	}

	private void checkTables()
	{
		if (useMySQL)
		{
			// Connect to mysql database
			mysql = new MySQL(plugin.getLogger(), KarmicJail.prefix,
					config.host, config.port, config.database, config.user,
					config.password);
			// Check if jailed table exists
			if (!mysql.checkTable(Table.JAILED.getName()))
			{
				plugin.getLogger().info(
						KarmicJail.prefix + " Created jailed table");
				// Jail table
				mysql.createTable("CREATE TABLE "
						+ Table.JAILED.getName()
						+ " (id INT UNSIGNED NOT NULL AUTO_INCREMENT, playername varchar(32) NOT NULL, status TEXT NOT NULL, time REAL NOT NULL, groups TEXT, jailer varchar(32), date TEXT, reason TEXT, muted INT, UNIQUE (playername), PRIMARY KEY(id));");
			}
		}
		else
		{
			// Connect to sql database
			sqlite = new SQLite(plugin.getLogger(), KarmicJail.prefix, "jail",
					plugin.getDataFolder().getAbsolutePath());
			// Check if jailed table exists
			if (!sqlite.checkTable(Table.JAILED.getName()))
			{
				plugin.getLogger().info(
						KarmicJail.prefix + " Created jailed table");
				// Jail table
				sqlite.createTable("CREATE TABLE "
						+ Table.JAILED.getName()
						+ " (id INTEGER PRIMARY KEY, playername varchar(32) NOT NULL, status TEXT NOT NULL, time REAL NOT NULL, groups TEXT, jailer varchar(32), date TEXT, reason TEXT, muted INTEGER, UNIQUE (playername));");
			}
		}
	}

	private void importSQL()
	{
		// Connect to sql database
		try
		{
			// Grab local SQLite database
			sqlite = new SQLite(plugin.getLogger(), KarmicJail.prefix, "jail",
					plugin.getDataFolder().getAbsolutePath());
			// Copy items
			Query rs = sqlite.select("SELECT * FROM " + config.tablePrefix
					+ "jailed;");
			if (rs.getResult().next())
			{
				plugin.getLogger().info(
						KarmicJail.prefix + " Importing jailed players...");
				PreparedStatement statement = mysql
						.prepare("INSERT INTO "
								+ config.tablePrefix
								+ "jailed (playername, status, time, groups, jailer, date, reason, muted) VALUES(?,?,?,?,?,?,?,?);");
				do
				{
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
					statement.setString(1, name);
					statement.setString(2, status);
					statement.setDouble(3, time);
					statement.setString(4, groups);
					statement.setString(5, jailer);
					statement.setString(6, date);
					statement.setString(7, reason);
					statement.setInt(8, muted);
					try
					{
						statement.executeUpdate();
					}
					catch (SQLException e)
					{
						plugin.getLogger().warning(
								KarmicJail.prefix + " SQL Exception on Import");
						e.printStackTrace();
					}
				} while (rs.getResult().next());
				statement.close();
			}
			rs.closeQuery();
			// TODO import inventory
			plugin.getLogger().info(
					KarmicJail.prefix + " Done importing SQLite into MySQL");
		}
		catch (SQLException e)
		{
			plugin.getLogger().warning(
					KarmicJail.prefix + " SQL Exception on Import");
			e.printStackTrace();
		}

	}

	public boolean checkConnection()
	{
		boolean connected = false;
		if (useMySQL)
		{
			connected = mysql.checkConnection();
		}
		else
		{
			connected = sqlite.checkConnection();
		}
		return connected;
	}

	public void close()
	{
		if (useMySQL)
		{
			mysql.close();
		}
		else
		{
			sqlite.close();
		}
	}

	public Query select(String query)
	{
		if (useMySQL)
		{
			return mysql.select(query);
		}
		else
		{
			return sqlite.select(query);
		}
	}

	public void standardQuery(String query)
	{
		if (useMySQL)
		{
			mysql.standardQuery(query);
		}
		else
		{
			sqlite.standardQuery(query);
		}
	}

	public void createTable(String query)
	{
		if (useMySQL)
		{
			mysql.createTable(query);
		}
		else
		{
			sqlite.createTable(query);
		}
	}

	public PreparedStatement prepare(String statement)
	{
		if (useMySQL)
		{
			return mysql.prepare(statement);
		}
		else
		{
			return sqlite.prepare(statement);
		}
	}

	public int getPlayerId(String playername)
	{
		int id = -1;
		try
		{
			final Query query = select("SELECT * FROM " + Table.JAILED.getName()
					+ " WHERE playername='" + playername + "';");
			if (query.getResult().next())
			{
				id = query.getResult().getInt("id");
			}
			query.closeQuery();
		}
		catch (SQLException e)
		{
			plugin.getLogger().warning("SQL Exception on grabbing player ID");
			e.printStackTrace();
		}
		return id;
	}

	public void resetPlayer(String name)
	{
		int id = getPlayerId(name);
		if (id == -1)
		{
			// Unknown player?
			JailLogic.addPlayerToDatabase(name);
		}
		id = getPlayerId(name);
		if (id != -1)
		{
			standardQuery("UPDATE " + Table.JAILED.getName()
					+ " SET time='-1',jailer='',date='',reason='' WHERE id='"
					+ id + "';");
			// TODO drop inventory table entries of their id
		}
		else
		{
			plugin.getLogger().warning("Could not reset player '" + name + "'");
		}
	}

	public void setField(Field field, String playername, String entry, int i,
			double d)
	{
		int id = getPlayerId(playername);
		if (id == -1)
		{
			// Unknown player?
			JailLogic.addPlayerToDatabase(playername);
		}
		id = getPlayerId(playername);
		if (id != -1)
		{
			try
			{
				final PreparedStatement statement = prepare("UPDATE "
						+ field.getTable().getName() + " SET "
						+ field.getColumnName() + "=? WHERE id='" + id + "';");
				boolean execute = false;
				switch (field.getType())
				{

					case STRING:
					{
						if (entry != null)
						{
							statement.setString(1, entry);
							execute = true;
						}
						else
						{
							plugin.getLogger().warning(
									"String cannot be null for field: "
											+ field.name());
						}
						break;
					}
					case INT:
					{
						statement.setInt(1, i);
						execute = true;
					}
					case DOUBLE:
					{
						statement.setDouble(1, d);
						execute = true;
					}
					default:
					{
						if (config.debugUnhandled)
						{
							plugin.getLogger().warning(
									"Unhandled getStringField for field "
											+ field);
						}
						break;
					}
				}
				if (execute)
				{
					statement.executeUpdate();
				}
				statement.close();
			}
			catch (SQLException e)
			{
				plugin.getLogger().warning(
						"SQL Exception on setting field " + field.name()
								+ " for '" + playername + "'");
				e.printStackTrace();
			}
		}
		else
		{
			plugin.getLogger().warning(
					"Could not set field " + field.name() + " for player '"
							+ playername + "' to: " + entry);
		}
	}

	public String getStringField(Field field, String playername)
	{
		String out = "";
		int id = getPlayerId(playername);
		if (id == -1)
		{
			// Unknown player?
			JailLogic.addPlayerToDatabase(playername);
		}
		id = getPlayerId(playername);
		if (id != -1)
		{
			switch (field.getType())
			{
				case STRING:
				{
					try
					{
						final Query query = select("SELECT * FROM "
								+ field.getTable().getName() + " WHERE id ='"
								+ id + "'");
						if (query.getResult().next())
						{
							out = query.getResult().getString(field.columnname);
							if (query.getResult().wasNull())
							{
								out = "";
							}
						}
						query.closeQuery();
					}
					catch (SQLException e)
					{
						plugin.getLogger().warning(
								"SQL Exception on grabbing field "
										+ field.name() + " for player '"
										+ playername + "'");
						e.printStackTrace();
					}
				}
				default:
				{
					if (config.debugUnhandled)
					{
						plugin.getLogger().warning(
								"Unhandled getStringField for field " + field);
					}
					break;
				}
			}
		}
		else
		{
			plugin.getLogger().warning(
					"Could not get field " + field.name() + " for player '"
							+ playername + "'");
		}
		return out;
	}

	public int getIntField(Field field, String playername)
	{
		int out = -1;
		int id = getPlayerId(playername);
		if (id == -1)
		{
			// Unknown player?
			JailLogic.addPlayerToDatabase(playername);
		}
		id = getPlayerId(playername);
		if (id != -1)
		{
			switch (field.getType())
			{
				case INT:
				{
					try
					{
						final Query query = select("SELECT * FROM "
								+ field.getTable().getName() + " WHERE id ='"
								+ id + "'");
						if (query.getResult().next())
						{
							out = query.getResult().getInt(field.columnname);
							if (query.getResult().wasNull())
							{
								out = -1;
							}
						}
						query.closeQuery();
					}
					catch (SQLException e)
					{
						plugin.getLogger().warning(
								"SQL Exception on grabbing field "
										+ field.name() + " for player '"
										+ playername + "'");
						e.printStackTrace();
					}
				}
				default:
				{
					if (config.debugUnhandled)
					{
						plugin.getLogger().warning(
								"Unhandled getIntField for field " + field);
					}
					break;
				}
			}
		}
		else
		{
			plugin.getLogger().warning(
					"Could not get field " + field.name() + " for player '"
							+ playername + "'");
		}
		return out;
	}

	public double getDoubleField(Field field, String playername)
	{
		double out = -1;
		int id = getPlayerId(playername);
		if (id == -1)
		{
			// Unknown player?
			JailLogic.addPlayerToDatabase(playername);
		}
		id = getPlayerId(playername);
		if (id != -1)
		{
			switch (field.getType())
			{
				case DOUBLE:
				{
					try
					{
						final Query query = select("SELECT * FROM "
								+ field.getTable().getName() + " WHERE id ='"
								+ id + "'");
						if (query.getResult().next())
						{
							out = query.getResult().getDouble(field.columnname);
							if (query.getResult().wasNull())
							{
								out = -1;
							}
						}
						query.closeQuery();
					}
					catch (SQLException e)
					{
						plugin.getLogger().warning(
								"SQL Exception on grabbing field "
										+ field.name() + " for player '"
										+ playername + "'");
						e.printStackTrace();
					}
				}
				default:
				{
					if (config.debugUnhandled)
					{
						plugin.getLogger().warning(
								"Unhandled getDoubleField for field " + field);
					}
					break;
				}
			}
		}
		else
		{
			plugin.getLogger().warning(
					"Could not get field " + field.name() + " for player '"
							+ playername + "'");
		}
		return out;
	}

	public enum Field
	{
		PlAYERNAME(Table.JAILED, "playername", Type.STRING), STATUS(
				Table.JAILED, "status", Type.STRING), TIME(Table.JAILED,
				"time", Type.DOUBLE), GROUPS(Table.JAILED, "groups",
				Type.STRING), JAILER(Table.JAILED, "jailer", Type.STRING), DATE(
				Table.JAILED, "date", Type.STRING), REASON(Table.JAILED,
				"reason", Type.STRING), MUTE(Table.JAILED, "muted", Type.INT);
		private final Table table;
		private final Type type;
		private final String columnname;

		private Field(Table table, String column, Type type)
		{
			this.table = table;
			this.columnname = column;
			this.type = type;
		}

		public Table getTable()
		{
			return table;
		}

		public String getColumnName()
		{
			return columnname;
		}

		public Type getType()
		{
			return type;
		}
	}

	public enum Type
	{
		STRING, INT, DOUBLE;
	}

	public enum Table
	{
		JAILED(config.tablePrefix + "jailed"), INVENTORY(config.tablePrefix
				+ "inventory");
		private final String table;

		private Table(String table)
		{
			this.table = table;
		}

		public String getName()
		{
			return table;
		}
	}
}
