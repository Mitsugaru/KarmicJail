package com.mitsugaru.karmicjail.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import lib.PatPeter.SQLibrary.MySQL;
import lib.PatPeter.SQLibrary.SQLite;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentWrapper;
import org.bukkit.inventory.ItemStack;

import com.mitsugaru.karmicjail.KarmicJail;
import com.mitsugaru.karmicjail.jail.JailLogic;
import com.mitsugaru.karmicjail.jail.JailStatus;
import com.mitsugaru.karmicjail.services.JailModule;
import com.mitsugaru.karmicjail.config.RootConfig;

public class DBHandler extends JailModule {
   // Class Variables
   private SQLite sqlite;
   private MySQL mysql;
   private boolean useMySQL;

   public DBHandler(KarmicJail ks) {
      super(ks);
   }

   @Override
   public void starting() {
      RootConfig config = plugin.getModuleForClass(RootConfig.class);
      useMySQL = config.useMySQL;
      if(config.importSQL) {
         if(useMySQL) {
            importSQL();
         }
         config.set("mysql.import", false);
      }
      checkTables();
   }

   @Override
   public void closing() {
      // Disconnect from sql database
      if(checkConnection()) {
         // Close connection
         close();
         plugin.getLogger().info("Disconnected from database.");
      }
   }

   public boolean checkTables() {
      RootConfig config = plugin.getModuleForClass(RootConfig.class);
      boolean valid = true;
      if(useMySQL) {
         // Connect to mysql database
         mysql = new MySQL(plugin.getLogger(), KarmicJail.TAG, config.host, Integer.parseInt(config.port), config.database, config.user,
               config.password);
         mysql.open();
         // Check if jailed table exists
         if(!mysql.isTable(Table.JAILED.getName())) {
            plugin.getLogger().info(KarmicJail.TAG + " Created jailed table");
            // Jail table
            try {
               mysql.query("CREATE TABLE "
                     + Table.JAILED.getName()
                     + " (id INT UNSIGNED NOT NULL AUTO_INCREMENT, playername varchar(32) NOT NULL, status TEXT NOT NULL, time REAL NOT NULL, groups TEXT, jailer varchar(32), date TEXT, reason TEXT, muted INT, lastpos TEXT, UNIQUE (playername), PRIMARY KEY(id));");
            } catch(SQLException e) {
               plugin.getLogger().log(Level.SEVERE, "SQLException on creating jailed table.", e);
               valid = false;
            }
         }
         // Check for history table
         if(!mysql.isTable(Table.HISTORY.getName())) {
            plugin.getLogger().info(KarmicJail.TAG + " Created history table");
            // history table
            try {
               mysql.query("CREATE TABLE " + Table.HISTORY.getName()
                     + " (row INT UNSIGNED NOT NULL AUTO_INCREMENT, id INT UNSIGNED NOT NULL, history TEXT NOT NULL, PRIMARY KEY(row));");
            } catch(SQLException e) {
               plugin.getLogger().log(Level.SEVERE, "SQLException on creating history table.", e);
               valid = false;
            }
         }
         // Check inventory table
         if(!mysql.isTable(Table.INVENTORY.getName())) {
            plugin.getLogger().info(KarmicJail.TAG + " Created inventory table");
            // inventory table
            try {
               mysql.query("CREATE TABLE "
                     + Table.INVENTORY.getName()
                     + " (row INT UNSIGNED NOT NULL AUTO_INCREMENT, id INT UNSIGNED NOT NULL, slot INT NOT NULL, itemid SMALLINT UNSIGNED NOT NULL, amount INT NOT NULL, durability TINYTEXT NOT NULL, enchantments TEXT, PRIMARY KEY(row));");
            } catch(SQLException e) {
               plugin.getLogger().log(Level.SEVERE, "SQLException on creating inventory table.", e);
               valid = false;
            }
         }
      } else {
         // Connect to sql database
         sqlite = new SQLite(plugin.getLogger(), KarmicJail.TAG, "jail", plugin.getDataFolder().getAbsolutePath());
         sqlite.open();
         // Check if jailed table exists
         if(!sqlite.isTable(Table.JAILED.getName())) {
            plugin.getLogger().info(KarmicJail.TAG + " Created jailed table");
            // Jail table
            try {
               sqlite.query("CREATE TABLE "
                     + Table.JAILED.getName()
                     + " (id INTEGER PRIMARY KEY, playername varchar(32) NOT NULL, status TEXT NOT NULL, time REAL NOT NULL, groups TEXT, jailer varchar(32), date TEXT, reason TEXT, muted INTEGER, lastpos TEXT, UNIQUE (playername));");
            } catch(SQLException e) {
               plugin.getLogger().log(Level.SEVERE, "SQLException on creating jailed table.", e);
               valid = false;
            }
         }
         // Check for history table
         if(!sqlite.isTable(Table.HISTORY.getName())) {
            plugin.getLogger().info(KarmicJail.TAG + " Created history table");
            // history table
            try {
               sqlite.query("CREATE TABLE " + Table.HISTORY.getName() + " (row INTEGER PRIMARY KEY, id INTEGER NOT NULL, history TEXT NOT NULL);");
            } catch(SQLException e) {
               plugin.getLogger().log(Level.SEVERE, "SQLException on creating history table.", e);
               valid = false;
            }
         }
         // Check inventory table
         if(!sqlite.isTable(Table.INVENTORY.getName())) {
            plugin.getLogger().info(KarmicJail.TAG + " Created inventory table");
            // inventory table
            try {
               sqlite.query("CREATE TABLE "
                     + Table.INVENTORY.getName()
                     + " (row INTEGER PRIMARY KEY, id INTEGER NOT NULL, slot INTEGER NOT NULL, itemid INTEGER NOT NULL, amount INTEGER NOT NULL, durability TEXT NOT NULL, enchantments TEXT);");
            } catch(SQLException e) {
               plugin.getLogger().log(Level.SEVERE, "SQLException on creating inventory table.", e);
               valid = false;
            }
         }
      }
      return valid;
   }

   private void importSQL() {
      // Connect to sql database
      ResultSet rs = null;
      ResultSet statementResult = null;
      PreparedStatement statement = null;
      try {
         // Grab local SQLite database
         sqlite = new SQLite(plugin.getLogger(), KarmicJail.TAG, "jail", plugin.getDataFolder().getAbsolutePath());
         // Copy items
         rs = sqlite.query("SELECT * FROM " + Table.JAILED.getName() + ";");
         if(rs.next()) {
            plugin.getLogger().info(KarmicJail.TAG + " Importing jailed players...");
            statement = mysql.prepare("INSERT INTO " + Table.JAILED.getName()
                  + " (playername, status, time, groups, jailer, date, reason, muted, lastpos) VALUES(?,?,?,?,?,?,?,?,?);");
            do {
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
               String lastpos = "";
               try {
                  lastpos = rs.getString("lastpos");
                  if(rs.wasNull()) {
                     lastpos = "";
                  }
               } catch(SQLException e) {
                  // Ignore. Try catch for < 0.4 databases that don't have
                  // this field
               }
               statement.setString(1, name);
               statement.setString(2, status);
               statement.setDouble(3, time);
               statement.setString(4, groups);
               statement.setString(5, jailer);
               statement.setString(6, date);
               statement.setString(7, reason);
               statement.setInt(8, muted);
               statement.setString(9, lastpos);
               try {
                  statementResult = sqlite.query(statement);
               } catch(SQLException e) {
                  plugin.getLogger().warning(KarmicJail.TAG + " SQL Exception on Import");
                  e.printStackTrace();
               } finally {
                  cleanup(statementResult, null);
               }
            } while(rs.next());
         }
         // TODO import inventory
         // TODO import history
         plugin.getLogger().info(KarmicJail.TAG + " Done importing SQLite into MySQL");
      } catch(SQLException e) {
         plugin.getLogger().log(Level.SEVERE, KarmicJail.TAG + " SQL Exception on Import", e);
      } finally {
         cleanup(statementResult, null);
         cleanup(rs, statement);
      }
   }

   public void cleanup(ResultSet rs, PreparedStatement statement) {
      if(statement != null) {
         try {
            statement.close();
         } catch(SQLException e) {
            plugin.getLogger().log(Level.SEVERE, KarmicJail.TAG + " SQL Exception on cleanup.", e);
         }
      }
      if(rs != null) {
         try {
            rs.close();
         } catch(SQLException e) {
            plugin.getLogger().log(Level.SEVERE, KarmicJail.TAG + " SQL Exception on cleanup.", e);
         }
      }
   }

   public boolean checkConnection() {
      boolean connected = false;
      if(useMySQL) {
         connected = mysql.checkConnection();
      } else {
         connected = sqlite.checkConnection();
      }
      return connected;
   }

   public void close() {
      if(useMySQL) {
         mysql.close();
      } else {
         sqlite.close();
      }
   }

   public ResultSet query(String query) throws SQLException {
      if(useMySQL) {
         return mysql.query(query);
      } else {
         return sqlite.query(query);
      }
   }

   public ResultSet query(PreparedStatement statement) throws SQLException {
      if(useMySQL) {
         return mysql.query(statement);
      } else {
         return sqlite.query(statement);
      }
   }

   public PreparedStatement prepare(String statement) throws SQLException {
      if(useMySQL) {
         return mysql.prepare(statement);
      } else {
         return sqlite.prepare(statement);
      }
   }

   public int getPlayerId(String playername) {
      int id = -1;
      ResultSet query = null;
      try {
         // TODO make this a prepared statement
         query = query("SELECT * FROM " + Table.JAILED.getName() + " WHERE playername='" + playername + "';");
         if(query.next()) {
            id = query.getInt("id");
         }
      } catch(SQLException e) {
         plugin.getLogger().warning("SQL Exception on grabbing player ID");
         e.printStackTrace();
      } finally {
         cleanup(query, null);
      }
      return id;
   }

   public List<String> getPlayerHistory(String name) {
      JailLogic logic = plugin.getModuleForClass(JailLogic.class);
      List<String> list = new ArrayList<String>();
      int id = getPlayerId(name);
      if(id == -1) {
         // Unknown player?
         logic.addPlayerToDatabase(name);
         id = getPlayerId(name);
      }
      ResultSet query = null;
      if(id != -1) {
         try {
            query = query("SELECT * FROM " + Table.HISTORY.getName() + " WHERE id='" + id + "' ORDER BY row DESC;");
            if(query.next()) {
               do {
                  list.add(query.getString(Field.HISTORY.getColumnName()));
               } while(query.next());
            }
         } catch(SQLException e) {
            plugin.getLogger().warning("SQL Exception on grabbing player history:" + name);
            e.printStackTrace();
         } finally {
            cleanup(query, null);
         }
      }
      return list;
   }

   public void addToHistory(String name, String reason) {
      JailLogic logic = plugin.getModuleForClass(JailLogic.class);
      int id = getPlayerId(name);
      if(id == -1) {
         // Unknown player?
         logic.addPlayerToDatabase(name);
         id = getPlayerId(name);
      }
      if(id != -1) {
         try {
            final PreparedStatement statement = prepare("INSERT INTO " + Table.HISTORY.getName() + " (id," + Field.HISTORY.getColumnName()
                  + ") VALUES(?,?);");
            statement.setInt(1, id);
            statement.setString(2, reason);
            statement.executeUpdate();
            statement.close();
         } catch(SQLException e) {
            plugin.getLogger().warning("SQL Exception on inserting player history:" + name);
            e.printStackTrace();
         }
      }
   }

   public void resetPlayer(String name) {
      JailLogic logic = plugin.getModuleForClass(JailLogic.class);
      int id = getPlayerId(name);
      if(id == -1) {
         // Unknown player?
         logic.addPlayerToDatabase(name);
         id = getPlayerId(name);
      }
      ResultSet first = null;
      ResultSet second = null;
      if(id != -1) {
         try {
            first = query("UPDATE " + Table.JAILED.getName() + " SET time='-1',jailer='',date='',reason='' WHERE id='" + id + "';");
         } catch(SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to update player time for " + name, e);
         } finally {
            cleanup(first, null);
         }
         // Delete inventory table entries of their id
         try {
            second = query("DELETE FROM " + Table.INVENTORY.getName() + " WHERE id='" + id + "';");
         } catch(SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete inventory entires for " + name, e);
         } finally {
            cleanup(second, null);
         }
      } else {
         plugin.getLogger().warning("Could not reset player '" + name + "'");
      }
   }

   public void setField(Field field, String playername, String entry, int i, double d) {
      RootConfig config = plugin.getModuleForClass(RootConfig.class);
      JailLogic logic = plugin.getModuleForClass(JailLogic.class);
      int id = getPlayerId(playername);
      if(id == -1) {
         // Unknown player?
         logic.addPlayerToDatabase(playername);
         id = getPlayerId(playername);
      }
      PreparedStatement statement = null;
      ResultSet rs = null;
      if(id != -1) {
         try {
            statement = prepare("UPDATE " + field.getTable().getName() + " SET " + field.getColumnName() + "=? WHERE id='" + id + "';");
            boolean execute = false;
            switch(field.getType()) {

            case STRING: {
               if(entry != null) {
                  statement.setString(1, entry);
                  execute = true;
               } else {
                  plugin.getLogger().warning("String cannot be null for field: " + field.name());
               }
               break;
            }
            case INT: {
               statement.setInt(1, i);
               execute = true;
               break;
            }
            case DOUBLE: {
               statement.setDouble(1, d);
               execute = true;
               break;
            }
            default: {
               if(config.debugUnhandled) {
                  plugin.getLogger().warning("Unhandled setField for field " + field);
               }
               break;
            }
            }
            if(execute) {
               rs = query(statement);
            }
         } catch(SQLException e) {
            plugin.getLogger().warning("SQL Exception on setting field " + field.name() + " for '" + playername + "'");
            e.printStackTrace();
         } finally {
            cleanup(rs, statement);
         }
      } else {
         plugin.getLogger().warning("Could not set field " + field.name() + " for player '" + playername + "' to: " + entry);
      }
   }

   public String getStringField(Field field, String playername) {
      RootConfig config = plugin.getModuleForClass(RootConfig.class);
      JailLogic logic = plugin.getModuleForClass(JailLogic.class);
      String out = "";
      int id = getPlayerId(playername);
      if(id == -1) {
         // Unknown player?
         logic.addPlayerToDatabase(playername);
         id = getPlayerId(playername);
      }
      if(id != -1) {
         switch(field.getType()) {
         case STRING: {
            ResultSet query = null;
            try {
               query = query("SELECT * FROM " + field.getTable().getName() + " WHERE id ='" + id + "'");
               if(query.next()) {
                  out = query.getString(field.getColumnName());
                  if(query.wasNull()) {
                     out = "";
                  }
               }
            } catch(SQLException e) {
               plugin.getLogger().warning("SQL Exception on grabbing field " + field.name() + " for player '" + playername + "'");
               e.printStackTrace();
            } finally {
               cleanup(query, null);
            }
            break;
         }
         default: {
            if(config.debugUnhandled) {
               plugin.getLogger().warning("Unhandled getStringField for field " + field);
            }
            break;
         }
         }
      } else {
         plugin.getLogger().warning("Could not get field " + field.name() + " for player '" + playername + "'");
      }
      return out;
   }

   public int getIntField(Field field, String playername) {
      RootConfig config = plugin.getModuleForClass(RootConfig.class);
      JailLogic logic = plugin.getModuleForClass(JailLogic.class);
      int out = -1;
      int id = getPlayerId(playername);
      if(id == -1) {
         // Unknown player?
         logic.addPlayerToDatabase(playername);
         id = getPlayerId(playername);
      }
      if(id != -1) {
         switch(field.getType()) {
         case INT: {
            ResultSet query = null;
            try {
               query = query("SELECT * FROM " + field.getTable().getName() + " WHERE id ='" + id + "'");
               if(query.next()) {
                  out = query.getInt(field.getColumnName());
                  if(query.wasNull()) {
                     out = -1;
                  }
               }
            } catch(SQLException e) {
               plugin.getLogger().warning("SQL Exception on grabbing field " + field.name() + " for player '" + playername + "'");
               e.printStackTrace();
            } finally {
               cleanup(query, null);
            }
            break;
         }
         default: {
            if(config.debugUnhandled) {
               plugin.getLogger().warning("Unhandled getIntField for field " + field);
            }
            break;
         }
         }
      } else {
         plugin.getLogger().warning("Could not get field " + field.name() + " for player '" + playername + "'");
      }
      return out;
   }

   public double getDoubleField(Field field, String playername) {
      RootConfig config = plugin.getModuleForClass(RootConfig.class);
      JailLogic logic = plugin.getModuleForClass(JailLogic.class);
      double out = -1;
      int id = getPlayerId(playername);
      if(id == -1) {
         // Unknown player?
         logic.addPlayerToDatabase(playername);
         id = getPlayerId(playername);
      }
      if(id != -1) {
         switch(field.getType()) {
         case DOUBLE: {
            ResultSet query = null;
            try {
               query = query("SELECT * FROM " + field.getTable().getName() + " WHERE id ='" + id + "'");
               if(query.next()) {
                  out = query.getDouble(field.getColumnName());
                  if(query.wasNull()) {
                     out = -1;
                  }
               }
            } catch(SQLException e) {
               plugin.getLogger().warning("SQL Exception on grabbing field " + field.name() + " for player '" + playername + "'");
               e.printStackTrace();
            } finally {
               cleanup(query, null);
            }
            break;
         }
         default: {
            if(config.debugUnhandled) {
               plugin.getLogger().warning("Unhandled getDoubleField for field " + field);
            }
            break;
         }
         }
      } else {
         plugin.getLogger().warning("Could not get field " + field.name() + " for player '" + playername + "'");
      }
      return out;
   }

   public boolean setPlayerItems(String playername, Map<Integer, ItemStack> items) {
      JailLogic logic = plugin.getModuleForClass(JailLogic.class);
      boolean valid = false;
      int id = getPlayerId(playername);
      if(id == -1) {
         // Unknown player?
         logic.addPlayerToDatabase(playername);
         id = getPlayerId(playername);
      }
      if(id != -1) {
         ResultSet rs = null;
         PreparedStatement statement = null;
         ResultSet second = null;
         try {
            // Remove old items
            rs = query("DELETE FROM " + Table.INVENTORY.getName() + " WHERE id='" + id + "';");
            cleanup(rs, null);
            // Add in items
            for(Map.Entry<Integer, ItemStack> item : items.entrySet()) {
               statement = prepare("INSERT INTO " + Table.INVENTORY.getName() + " (id,slot,itemid,amount,durability,enchantments) VALUES(?,?,?,?,?,?)");
               statement.setInt(1, id);
               statement.setInt(2, item.getKey().intValue());
               statement.setInt(3, item.getValue().getTypeId());
               statement.setInt(4, item.getValue().getAmount());
               statement.setString(5, "" + item.getValue().getDurability());
               if(!item.getValue().getEnchantments().isEmpty()) {
                  StringBuilder sb = new StringBuilder();
                  for(Map.Entry<Enchantment, Integer> e : item.getValue().getEnchantments().entrySet()) {
                     sb.append(e.getKey().getId() + "v" + e.getValue().intValue() + "i");
                  }
                  // Remove trailing comma
                  sb.deleteCharAt(sb.length() - 1);
                  statement.setString(6, sb.toString());
               } else {
                  statement.setString(6, "");
               }
               second = query(statement);
               cleanup(second, statement);
            }
            valid = true;
         } catch(SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "SQL Exception on setting inventory for '" + playername + "'", e);
            valid = false;
         }
         return valid;
      } else {
         plugin.getLogger().log(Level.SEVERE, "Could not set items for player '" + playername + "'");
      }
      return valid;
   }

   public Map<Integer, ItemStack> getPlayerItems(String playername) {
      JailLogic logic = plugin.getModuleForClass(JailLogic.class);
      Map<Integer, ItemStack> items = new HashMap<Integer, ItemStack>();
      int id = getPlayerId(playername);
      if(id == -1) {
         // Unknown player?
         logic.addPlayerToDatabase(playername);
         id = getPlayerId(playername);
      }
      if(id != -1) {
         ResultSet query = null;
         try {
            query = query("SELECT * FROM " + Table.INVENTORY.getName() + " WHERE id='" + id + "';");
            if(query.next()) {
               do {
                  int itemid = query.getInt(Field.INV_ITEM.getColumnName());
                  int amount = query.getInt(Field.INV_AMOUNT.getColumnName());
                  short dur = query.getShort(Field.INV_DURABILITY.getColumnName());
                  ItemStack add = new ItemStack(itemid, amount, dur);
                  String enchantments = query.getString(Field.INV_ENCHANT.getColumnName());
                  if(add != null) {
                     if(!query.wasNull()) {
                        if(enchantments.contains("i") || enchantments.contains("v")) {
                           try {
                              String[] cut = enchantments.split("i");
                              for(int s = 0; s < cut.length; s++) {
                                 String[] cutter = cut[s].split("v");
                                 EnchantmentWrapper e = new EnchantmentWrapper(Integer.parseInt(cutter[0]));
                                 add.addUnsafeEnchantment(e.getEnchantment(), Integer.parseInt(cutter[1]));
                              }
                           } catch(ArrayIndexOutOfBoundsException a) {
                              // something went wrong
                           }
                        }
                     }
                     items.put(new Integer(query.getInt(Field.INV_SLOT.getColumnName())), add);
                  }
               } while(query.next());
            }
         } catch(SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "SQL Exception on getting inventory for '" + playername + "'", e);
         } finally {
            cleanup(query, null);
         }
      }
      return items;
   }

   public boolean setItem(String playername, int slot, ItemStack item) {
      return setItem(playername, slot, item, item.getAmount());
   }

   public boolean setItem(String playername, int slot, ItemStack item, int amount) {
      JailLogic logic = plugin.getModuleForClass(JailLogic.class);
      boolean valid = false;
      int id = getPlayerId(playername);
      if(id == -1) {
         // Unknown player?
         logic.addPlayerToDatabase(playername);
         id = getPlayerId(playername);
      }
      if(id != -1) {
         ResultSet rs = null;
         PreparedStatement statement = null;
         try {
            statement = prepare("INSERT INTO " + Table.INVENTORY.getName() + " (id,slot,itemid,amount,durability,enchantments) VALUES(?,?,?,?,?,?)");
            statement.setInt(1, id);
            statement.setInt(2, slot);
            statement.setInt(3, item.getTypeId());
            statement.setInt(4, amount);
            statement.setString(5, "" + item.getDurability());
            if(!item.getEnchantments().isEmpty()) {
               // TODO fix this to be ordered
               StringBuilder sb = new StringBuilder();
               for(Map.Entry<Enchantment, Integer> e : item.getEnchantments().entrySet()) {
                  sb.append(e.getKey().getId() + "v" + e.getValue().intValue() + "i");
               }
               // Remove trailing comma
               sb.deleteCharAt(sb.length() - 1);
               statement.setString(6, sb.toString());
            } else {
               statement.setString(6, "");
            }
            rs = query(statement);
            valid = true;
         } catch(SQLException e) {
            plugin.getLogger().warning("SQL Exception on setting inventory for '" + playername + "'");
            e.printStackTrace();
         } finally {
            cleanup(rs, statement);
         }
      }
      return valid;
   }

   public void removeItem(String playername, int slot) {
      JailLogic logic = plugin.getModuleForClass(JailLogic.class);
      int id = getPlayerId(playername);
      if(id == -1) {
         // Unknown player?
         logic.addPlayerToDatabase(playername);
         id = getPlayerId(playername);
      }
      if(id != -1) {
         ResultSet query = null;
         try {
            query = query("DELETE FROM " + Table.INVENTORY.getName() + " WHERE id='" + id + "' AND slot='" + slot + "';");
         } catch(SQLException e) {

         } finally {
            cleanup(query, null);
         }
      }
   }

}
