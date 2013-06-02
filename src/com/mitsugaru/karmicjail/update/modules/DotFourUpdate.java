package com.mitsugaru.karmicjail.update.modules;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import com.mitsugaru.karmicjail.KarmicJail;
import com.mitsugaru.karmicjail.config.RootConfig;
import com.mitsugaru.karmicjail.database.DBHandler;
import com.mitsugaru.karmicjail.database.Table;
import com.mitsugaru.karmicjail.jail.JailStatus;
import com.mitsugaru.karmicjail.services.Version;
import com.mitsugaru.karmicjail.update.UpdateModule;
import com.mitsugaru.karmicjail.update.holders.PointThreeObject;

/**
 * Handles the 0.4 update.
 * 
 * @author Mitsugaru
 */
public class DotFourUpdate extends UpdateModule {

    /**
     * Constructor.
     * 
     * @param plugin
     *            - Plugin instance.
     */
    public DotFourUpdate(KarmicJail plugin) {
        super(plugin);
        targetVersion = new Version("0.4");
        targetVersion.setIgnorePatch(true);
    }

    @Override
    public void update() {
        RootConfig config = plugin.getModuleForClass(RootConfig.class);
        DBHandler database = plugin.getModuleForClass(DBHandler.class);
        String query = "";
        try {
            final Set<PointThreeObject> entries = new HashSet<PointThreeObject>();
            plugin.getLogger().info(
                    KarmicJail.TAG + " Converting table '"
                            + Table.JAILED.getName() + "' ...");
            // Grab old entries
            final ResultSet rs = database.query("SELECT * FROM "
                    + Table.JAILED.getName() + ";");
            if(rs.next()) {
                do {
                    // save entry
                    String name = rs.getString("playername");
                    String status = rs.getString("status");
                    if(rs.wasNull()) {
                        status = JailStatus.FREED + "";
                    }
                    double time = rs.getDouble("time");
                    if(rs.wasNull()) {
                        time = -1;
                    }
                    String groups = rs.getString("groups");
                    if(rs.wasNull()) {
                        groups = "";
                    }
                    String jailer = rs.getString("jailer");
                    if(rs.wasNull()) {
                        jailer = "";
                    }
                    String date = rs.getString("date");
                    if(rs.wasNull()) {
                        date = "";
                    }
                    String reason = rs.getString("reason");
                    if(rs.wasNull()) {
                        reason = "";
                    }
                    int muted = rs.getInt("muted");
                    if(rs.wasNull()) {
                        muted = 0;
                    }
                    entries.add(new PointThreeObject(name, status, groups,
                            jailer, date, reason, time, muted));
                } while(rs.next());
            }
            database.cleanup(rs, null);
            // Drop old table
            ResultSet second = database.query("DROP TABLE "
                    + Table.JAILED.getName() + ";");
            database.cleanup(second, null);
            // Create new table
            if(config.useMySQL) {
                query = "CREATE TABLE "
                        + Table.JAILED.getName()
                        + " (id INT UNSIGNED NOT NULL AUTO_INCREMENT, playername varchar(32) NOT NULL, status TEXT NOT NULL, time REAL NOT NULL, groups TEXT, jailer varchar(32), date TEXT, reason TEXT, muted INT, lastpos TEXT, UNIQUE (playername), PRIMARY KEY(id));";
            } else {
                query = "CREATE TABLE "
                        + Table.JAILED.getName()
                        + " (id INTEGER PRIMARY KEY, playername varchar(32) NOT NULL, status TEXT NOT NULL, time REAL NOT NULL, groups TEXT, jailer varchar(32), date TEXT, reason TEXT, muted INTEGER, lastpos TEXT, UNIQUE (playername));";
            }
            ResultSet third = database.query(query);
            database.cleanup(third, null);
            // Add back entries
            if(!entries.isEmpty()) {
                PreparedStatement statement = database
                        .prepare("INSERT INTO "
                                + Table.JAILED.getName()
                                + " (playername,status,time,groups,jailer,date,reason,muted,lastpos) VALUES(?,?,?,?,?,?,?,?,?)");
                for(PointThreeObject entry : entries) {
                    statement.setString(1, entry.playername);
                    statement.setString(2, entry.status);
                    statement.setDouble(3, entry.time);
                    statement.setString(4, entry.groups);
                    statement.setString(5, entry.jailer);
                    statement.setString(6, entry.date);
                    statement.setString(7, entry.reason);
                    statement.setInt(8, entry.mute);
                    statement.setString(9, "");
                    ResultSet fourth = null;
                    try {
                        fourth = database.query(statement);
                    } catch(SQLException e) {
                        plugin.getLogger().log(Level.SEVERE,
                                KarmicJail.TAG + " SQLException", e);
                    } finally {
                        database.cleanup(fourth, null);
                    }
                }
                database.cleanup(null, statement);
            }
            plugin.getLogger().info(
                    KarmicJail.TAG + " Conversion of table '"
                            + Table.JAILED.getName() + "' finished.");
        } catch(SQLException e) {
            plugin.getLogger().warning(
                    KarmicJail.TAG + " SQL Exception on 0.4 update");
            e.printStackTrace();
        }
    }

}
