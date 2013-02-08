package com.mitsugaru.karmicjail.update;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.configuration.ConfigurationSection;

import com.mitsugaru.karmicjail.KarmicJail;
import com.mitsugaru.karmicjail.database.Table;
import com.mitsugaru.karmicjail.jail.JailStatus;
import com.mitsugaru.karmicjail.update.holders.PointThreeObject;

public class Update {
   private static KarmicJail plugin;

   public static void init(KarmicJail kj) {
      plugin = kj;
   }

   /**
    * Check if updates are necessary
    */
   public static void checkUpdate() {
      // Check if need to update
      ConfigurationSection config = plugin.getConfig();
      if(Double.parseDouble(plugin.getDescription().getVersion()) > Double.parseDouble(config.getString("version"))) {
         // Update to latest version
         plugin.getLogger().info("Updating to v" + plugin.getDescription().getVersion());
         update();
      }
   }

   /**
    * This method is called to make the appropriate changes, most likely only
    * necessary for database schema modification, for a proper update.
    */
   private static void update() {
      // Grab current version
      double ver = Double.parseDouble(plugin.getConfig().getString("version"));
      String query = "";
      // Update to 0.2
      if(ver < 0.2) {
         // Add mute column
         query = "ALTER TABLE jailed ADD muted INTEGER;";
         ResultSet rs = null;
         try {
            rs = plugin.getDatabaseHandler().query(query);
         } catch(SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to alter jailed table with muted column.", e);
         } finally {
            plugin.getDatabaseHandler().cleanup(rs, null);
         }
      }
      // Update to 0.3
      if(ver < 0.3) {
         // Drop newly created tables
         plugin.getLogger().info(KarmicJail.TAG + " Dropping empty tables.");
         ResultSet rs = null;
         try {
            rs = plugin.getDatabaseHandler().query("DROP TABLE " + Table.JAILED.getName() + ";");
         } catch(SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to drop jailed table.", e);
         } finally {
            plugin.getDatabaseHandler().cleanup(rs, null);
         }
         // Update tables to have prefix
         plugin.getLogger().info(KarmicJail.TAG + " Renaming jailed table to '" + Table.JAILED.getName() + "'.");
         query = "ALTER TABLE jailed RENAME TO " + Table.JAILED.getName() + ";";
         ResultSet second = null;
         try {
            second = plugin.getDatabaseHandler().query(query);
         } catch(SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to alter jailed table with prefix.", e);
         } finally {
            plugin.getDatabaseHandler().cleanup(second, null);
         }
      }
      // Update to 0.4
      if(ver < 0.4) {
         try {
            final Set<PointThreeObject> entries = new HashSet<PointThreeObject>();
            plugin.getLogger().info(KarmicJail.TAG + " Converting table '" + Table.JAILED.getName() + "' ...");
            // Grab old entries
            final ResultSet rs = plugin.getDatabaseHandler().query("SELECT * FROM " + Table.JAILED.getName() + ";");
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
                  entries.add(new PointThreeObject(name, status, groups, jailer, date, reason, time, muted));
               } while(rs.next());
            }
            plugin.getDatabaseHandler().cleanup(rs, null);
            // Drop old table
            ResultSet second = plugin.getDatabaseHandler().query("DROP TABLE " + Table.JAILED.getName() + ";");
            plugin.getDatabaseHandler().cleanup(second, null);
            // Create new table
            if(plugin.getPluginConfig().useMySQL) {
               query = "CREATE TABLE "
                     + Table.JAILED.getName()
                     + " (id INT UNSIGNED NOT NULL AUTO_INCREMENT, playername varchar(32) NOT NULL, status TEXT NOT NULL, time REAL NOT NULL, groups TEXT, jailer varchar(32), date TEXT, reason TEXT, muted INT, lastpos TEXT, UNIQUE (playername), PRIMARY KEY(id));";
            } else {
               query = "CREATE TABLE "
                     + Table.JAILED.getName()
                     + " (id INTEGER PRIMARY KEY, playername varchar(32) NOT NULL, status TEXT NOT NULL, time REAL NOT NULL, groups TEXT, jailer varchar(32), date TEXT, reason TEXT, muted INTEGER, lastpos TEXT, UNIQUE (playername));";
            }
            ResultSet third = plugin.getDatabaseHandler().query(query);
            plugin.getDatabaseHandler().cleanup(third, null);
            // Add back entries
            if(!entries.isEmpty()) {
               PreparedStatement statement = plugin.getDatabaseHandler().prepare(
                     "INSERT INTO " + Table.JAILED.getName()
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
                  try {
                     statement.executeUpdate();
                  } catch(SQLException s) {
                     plugin.getLogger().warning(KarmicJail.TAG + " SQLException");
                     s.printStackTrace();
                  }
               }
               statement.close();
            }
            plugin.getLogger().info(KarmicJail.TAG + " Conversion of table '" + Table.JAILED.getName() + "' finished.");
         } catch(SQLException e) {
            plugin.getLogger().warning(KarmicJail.TAG + " SQL Exception on 0.4 update");
            e.printStackTrace();
         }

      }
      if(ver < 0.43) {
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
