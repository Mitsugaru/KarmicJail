package com.mitsugaru.karmicjail;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.mitsugaru.karmicjail.KarmicJail.JailStatus;

import lib.Mitsugaru.SQLibrary.Database.Query;
import lib.Mitsugaru.SQLibrary.MySQL;
import lib.Mitsugaru.SQLibrary.SQLite;

public class DBHandler {
	// Class Variables
	private KarmicJail plugin;
	private Config config;
	private SQLite sqlite;
	private MySQL mysql;
	private boolean useMySQL;

	public DBHandler(KarmicJail ks, Config conf) {
		plugin = ks;
		config = conf;
		useMySQL = config.useMySQL;
		checkTables();
		if (config.importSQL) {
			if (useMySQL) {
				importSQL();
			}
			config.set("mysql.import", false);
		}
	}

	private void checkTables() {
		if (useMySQL) {
			// Connect to mysql database
			mysql = new MySQL(plugin.getLogger(), KarmicJail.prefix,
					config.host, config.port, config.database, config.user,
					config.password);
			// Check if jailed table exists
			if (!mysql.checkTable(config.tablePrefix +"jailed")) {
				plugin.getLogger().info(KarmicJail.prefix + " Created jailed table");
				// Jail table
				mysql.createTable("CREATE TABLE `" + config.tablePrefix +"jailed` (`playername` varchar(32) NOT NULL, `status` TEXT, `time` REAL, `groups` TEXT, `jailer` varchar(32), `date` TEXT, `reason` TEXT, `muted` INTEGER, UNIQUE (`playername`));");
			}
		} else {
			// Connect to sql database
			sqlite = new SQLite(plugin.getLogger(), KarmicJail.prefix, "jail", plugin
					.getDataFolder().getAbsolutePath());
			// Check if jailed table exists
			if (!sqlite.checkTable(config.tablePrefix +"jailed")) {
				plugin.getLogger().info(KarmicJail.prefix + " Created jailed table");
				// Jail table
				sqlite.createTable("CREATE TABLE `"+ config.tablePrefix +"jailed` (`playername` varchar(32) NOT NULL, `status` TEXT, `time` REAL, `groups` TEXT, `jailer` varchar(32), `date` TEXT, `reason` TEXT, `muted` INTEGER, UNIQUE (`playername`));");
			}
		}
	}

	private void importSQL() {
		// Connect to sql database
		try {
			// Grab local SQLite database
			sqlite = new SQLite(plugin.getLogger(), KarmicJail.prefix, "jail",
					plugin.getDataFolder().getAbsolutePath());
			// Copy items
			Query rs = sqlite.select("SELECT * FROM " + config.tablePrefix
					+ "jailed;");
			if (rs.getResult().next()) {
				plugin.getLogger().info(
						KarmicJail.prefix + " Importing jailed players...");
				PreparedStatement statement = mysql.prepare("INSERT INTO " + config.tablePrefix + "jailed (playername, status, time, groups, jailer, date, reason, muted) VALUES(?,?,?,?,?,?,?,?);");
				do {
					String name = rs.getResult().getString("playername");
					String status = rs.getResult().getString("status");
					if (rs.getResult().wasNull()) {
						status = JailStatus.JAILED + "";
					}
					long time = rs.getResult().getLong("time");
					if (rs.getResult().wasNull()) {
						time = -1;
					}
					String groups = rs.getResult().getString("groups");
					if (rs.getResult().wasNull()) {
						groups = "";
					}
					String jailer = rs.getResult().getString("jailer");
					if (rs.getResult().wasNull()) {
						jailer = "";
					}
					String date = rs.getResult().getString("date");
					if (rs.getResult().wasNull()) {
						date = "";
					}
					String reason = rs.getResult().getString("reason");
					if (rs.getResult().wasNull()) {
						reason = "";
					}
					int muted = rs.getResult().getInt("muted");
					if (rs.getResult().wasNull()) {
						muted = 0;
					}
					statement.setString(1,name);
					statement.setString(2,status);
					statement.setLong(3,time);
					statement.setString(4,groups);
					statement.setString(5,jailer);
					statement.setString(6,date);
					statement.setString(7,reason);
					statement.setInt(8,muted);
					statement.executeUpdate();
				} while (rs.getResult().next());
			}
			rs.closeQuery();
			plugin.getLogger().info(
					KarmicJail.prefix + " Done importing SQLite into MySQL");
		} catch (SQLException e) {
			plugin.getLogger().warning(
					KarmicJail.prefix + " SQL Exception on Import");
			e.printStackTrace();
		}

	}

	public boolean checkConnection() {
		boolean connected = false;
		if (useMySQL) {
			connected = mysql.checkConnection();
		} else {
			connected = sqlite.checkConnection();
		}
		return connected;
	}

	public void close() {
		if (useMySQL) {
			mysql.close();
		} else {
			sqlite.close();
		}
	}

	public Query select(String query) {
		if (useMySQL) {
			return mysql.select(query);
		} else {
			return sqlite.select(query);
		}
	}

	public void standardQuery(String query) {
		if (useMySQL) {
			mysql.standardQuery(query);
		} else {
			sqlite.standardQuery(query);
		}
	}

	public void createTable(String query) {
		if (useMySQL) {
			mysql.createTable(query);
		} else {
			sqlite.createTable(query);
		}
	}
	
	public PreparedStatement prepare(String statement)
	{
		if(useMySQL)
		{
			return mysql.prepare(statement);
		}
		else
		{
			return sqlite.prepare(statement);
		}
	}
}
