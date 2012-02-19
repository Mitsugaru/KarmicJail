package com.mitsugaru.karmicjail;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.mitsugaru.karmicjail.KarmicJail.JailStatus;

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
				plugin.log.info(KarmicJail.prefix + " Created jailed table");
				// Jail table
				mysql.createTable("CREATE TABLE `" + config.tablePrefix +"jailed` (`playername` varchar(32) NOT NULL, `status` TEXT, `time` REAL, `groups` TEXT, `jailer` varchar(32), `date` TEXT, `reason` TEXT, `muted` INTEGER, UNIQUE (`playername`));");
			}
		} else {
			// Connect to sql database
			sqlite = new SQLite(plugin.log, KarmicJail.prefix, "jail", plugin
					.getDataFolder().getAbsolutePath());
			// Check if jailed table exists
			if (!sqlite.checkTable(config.tablePrefix +"jailed")) {
				plugin.log.info(KarmicJail.prefix + " Created jailed table");
				// Jail table
				sqlite.createTable("CREATE TABLE `"+ config.tablePrefix +"jailed` (`playername` varchar(32) NOT NULL, `status` TEXT, `time` REAL, `groups` TEXT, `jailer` varchar(32), `date` TEXT, `reason` TEXT, `muted` INTEGER, UNIQUE (`playername`));");
			}
		}
	}

	private void importSQL() {
		// Connect to sql database
		try {
			StringBuilder sb = new StringBuilder();
			// Grab local SQLite database
			sqlite = new SQLite(plugin.getLogger(), KarmicJail.prefix, "jail",
					plugin.getDataFolder().getAbsolutePath());
			// Copy items
			ResultSet rs = sqlite.select("SELECT * FROM " + config.tablePrefix
					+ "jailed;");
			if (rs.next()) {
				plugin.getLogger().info(
						KarmicJail.prefix + " Importing jailed players...");
				do {
					String name = rs.getString("playername");
					String status = rs.getString("status");
					if (!rs.wasNull()) {
						status = JailStatus.JAILED + "";
					}
					long time = rs.getLong("time");
					if (!rs.wasNull()) {
						time = -1;
					}
					String groups = rs.getString("groups");
					if (!rs.wasNull()) {
						groups = "";
					}
					String jailer = rs.getString("jailer");
					if (!rs.wasNull()) {
						jailer = "";
					}
					String date = rs.getString("date");
					if (!rs.wasNull()) {
						date = "";
					}
					String reason = rs.getString("reason");
					if (!rs.wasNull()) {
						reason = "";
					}
					int muted = rs.getInt("muted");
					if (!rs.wasNull()) {
						muted = 0;
					}
					sb.append("INSERT INTO ");
					sb.append(config.tablePrefix);
					sb.append("jailed (playername, status, time, groups, jailer, date, reason, muted) VALUES('");
					sb.append(name);
					sb.append("','");
					sb.append(status);
					sb.append("','");
					sb.append(time);
					sb.append("','");
					sb.append(groups);
					sb.append("','");
					sb.append(jailer);
					sb.append("','");
					sb.append(date);
					sb.append("','");
					sb.append(reason);
					sb.append("','");
					sb.append(muted);
					sb.append("');");
					final String query = sb.toString();
					mysql.standardQuery(query);
					sb = new StringBuilder();
				} while (rs.next());
			}
			rs.close();
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

	public ResultSet select(String query) {
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
}
