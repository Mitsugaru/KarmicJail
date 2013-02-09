package com.mitsugaru.karmicjail.update;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.configuration.ConfigurationSection;

import com.mitsugaru.karmicjail.KarmicJail;
import com.mitsugaru.karmicjail.config.RootConfig;
import com.mitsugaru.karmicjail.database.DBHandler;
import com.mitsugaru.karmicjail.database.Table;
import com.mitsugaru.karmicjail.jail.JailStatus;
import com.mitsugaru.karmicjail.update.holders.PointFourFourObject;
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
      RootConfig config = plugin.getModuleForClass(RootConfig.class);
      DBHandler database = plugin.getModuleForClass(DBHandler.class);
      // Grab current version
      double ver = Double.parseDouble(plugin.getConfig().getString("version"));
      String query = "";
      // Update to 0.2
      if(ver < 0.2) {
         // Add mute column
         query = "ALTER TABLE jailed ADD muted INTEGER;";
         ResultSet rs = null;
         try {
            rs = database.query(query);
         } catch(SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to alter jailed table with muted column.", e);
         } finally {
            database.cleanup(rs, null);
         }
      }
      // Update to 0.3
      if(ver < 0.3) {
         // Drop newly created tables
         plugin.getLogger().info(KarmicJail.TAG + " Dropping empty tables.");
         ResultSet rs = null;
         try {
            rs = database.query("DROP TABLE " + Table.JAILED.getName() + ";");
         } catch(SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to drop jailed table.", e);
         } finally {
            database.cleanup(rs, null);
         }
         // Update tables to have prefix
         plugin.getLogger().info(KarmicJail.TAG + " Renaming jailed table to '" + Table.JAILED.getName() + "'.");
         query = "ALTER TABLE jailed RENAME TO " + Table.JAILED.getName() + ";";
         ResultSet second = null;
         try {
            second = database.query(query);
         } catch(SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to alter jailed table with prefix.", e);
         } finally {
            database.cleanup(second, null);
         }
      }
      // Update to 0.4
      if(ver < 0.4) {
         try {
            final Set<PointThreeObject> entries = new HashSet<PointThreeObject>();
            plugin.getLogger().info(KarmicJail.TAG + " Converting table '" + Table.JAILED.getName() + "' ...");
            // Grab old entries
            final ResultSet rs = database.query("SELECT * FROM " + Table.JAILED.getName() + ";");
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
            database.cleanup(rs, null);
            // Drop old table
            ResultSet second = database.query("DROP TABLE " + Table.JAILED.getName() + ";");
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
               PreparedStatement statement = database.prepare("INSERT INTO " + Table.JAILED.getName()
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
                     plugin.getLogger().log(Level.SEVERE, KarmicJail.TAG + " SQLException", e);
                  } finally {
                     database.cleanup(fourth, null);
                  }
               }
               database.cleanup(null, statement);
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
         config.jailGroup = plugin.getConfig().getString("jailgroup", "Jailed");
         plugin.getConfig().set("group.jail.group", config.jailGroup);
         plugin.getConfig().set("jailgroup", null);
         config.removeGroups = plugin.getConfig().getBoolean("removegroups", true);
         plugin.getConfig().set("group.removeOnJail", config.removeGroups);
         plugin.getConfig().set("removegroups", null);
      }
      if(ver < 0.44) {
         plugin.getLogger().info("Update item database");
         final Set<PointFourFourObject> entries = new HashSet<PointFourFourObject>();
         ResultSet rs = null;
         try {
            rs = database.query("SELECT * FROM " + Table.INVENTORY.getName() + ";");
            if(rs.next()) {
               do {
                  PointFourFourObject tempInv = new PointFourFourObject();
                  tempInv.row = rs.getInt("row");
                  tempInv.id = rs.getInt("id");
                  tempInv.slot = rs.getInt("slot");
                  tempInv.itemId = rs.getInt("itemId");
                  tempInv.amount = rs.getInt("amount");
                  tempInv.durability = Short.valueOf(rs.getString("durability"));
                  tempInv.enchantments = rs.getString("enchantments");
                  entries.add(tempInv);
               } while(rs.next());
            }
            database.cleanup(rs, null);
            // Drop old table
            ResultSet second = database.query("DROP TABLE " + Table.INVENTORY.getName() + ";");
            database.cleanup(second, null);
            // Create new table
            if(config.useMySQL) {
               query = "CREATE TABLE "
                     + Table.INVENTORY.getName()
                     + " (row INT UNSIGNED NOT NULL AUTO_INCREMENT, id INT UNSIGNED NOT NULL, slot INT NOT NULL, itemid SMALLINT UNSIGNED NOT NULL, amount INT NOT NULL, durability TINYTEXT NOT NULL, enchantments TEXT, PRIMARY KEY(row));";
            } else {
               query = "CREATE TABLE "
                     + Table.INVENTORY.getName()
                     + " (row INTEGER PRIMARY KEY, id INTEGER NOT NULL, slot INTEGER NOT NULL, itemid INTEGER NOT NULL, amount INTEGER NOT NULL, durability TEXT NOT NULL, enchantments TEXT);";
            }
            ResultSet third = database.query(query);
            database.cleanup(third, null);
            // Add back entries
            try {
               if(!entries.isEmpty()) {
                  PreparedStatement statement = database.prepare("INSERT INTO " + Table.INVENTORY.getName()
                        + " (row,id,slot,itemid,amount,durability,enchantments) VALUES(?,?,?,?,?,?,?)");
                  for(PointFourFourObject entry : entries) {
                     statement.setInt(1, entry.row);
                     statement.setInt(2, entry.id);
                     statement.setInt(3, entry.slot);
                     statement.setInt(4, entry.itemId);
                     statement.setInt(5, entry.amount);
                     statement.setString(6, entry.durability + "");
                     statement.setString(7, entry.enchantments);
                     ResultSet fourth = null;
                     try {
                        fourth = database.query(statement);
                     } catch(SQLException e) {
                        plugin.getLogger().log(Level.SEVERE, KarmicJail.TAG + " SQLException", e);
                     } finally {
                        database.cleanup(fourth, null);
                     }
                  }
                  database.cleanup(null, statement);
               }
            } catch(SQLException e) {
               plugin.getLogger().warning(KarmicJail.TAG + " SQL Exception on 0.44 update");
            }
         } catch(SQLException e) {
            plugin.getLogger().warning(KarmicJail.TAG + " SQL Exception on 0.44 update");
         }
      }
      // Update version number in config.yml
      plugin.getConfig().set("version", plugin.getDescription().getVersion());
      plugin.saveConfig();
   }
}
