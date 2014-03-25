package com.mitsugaru.karmicjail.storage.models;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.logging.Level;

import lib.PatPeter.SQLibrary.SQLite;

import org.bukkit.Location;

import com.mitsugaru.karmicjail.KarmicJail;
import com.mitsugaru.karmicjail.database.Table;
import com.mitsugaru.karmicjail.jail.Jail;
import com.mitsugaru.karmicjail.jail.JailStatus;

public class SQLiteStorage extends DatabaseStorage {

    private final SQLite sqlite;

    public SQLiteStorage(KarmicJail plugin) {
        super(plugin);
        sqlite = new SQLite(plugin.getLogger(), "KJ", plugin.getDataFolder()
                .getAbsolutePath(), "jail");
    }

    @Override
    public String getDate(String playerName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean setDate(String playerName, String date) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getJailer(String playerName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean setJailer(String playerName, String jailer) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public double getJailTime(String playerName) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean setJailTime(String playerName, double time) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public JailStatus getStatus(String playerName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean setStatus(String playerName, JailStatus status) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Location getLastLocation(String playerName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean setLastLocation(String playerName, Location location) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getReason(String playerName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean setReason(String playerName, String reason) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isMuted(String playerName) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean setMuted(String playerName, boolean mute) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Jail getJail(String jail) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean addJail(String jail, Location location) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean removeJail(String jail) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Jail getJailForPlayer(String playerName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<String> getGroups(String playerName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean setGroups(String playerName, Collection<String> groups) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void close() {
        sqlite.close();
    }

    @Override
    public boolean checkConnection() {
        return sqlite.isOpen();
    }

    @Override
    boolean checkTables() {
        boolean valid = true;
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
                plugin.getLogger().log(Level.SEVERE,
                        "SQLException on creating jailed table.", e);
                valid = false;
            }
        }
        // Check for history table
        if(!sqlite.isTable(Table.HISTORY.getName())) {
            plugin.getLogger().info(KarmicJail.TAG + " Created history table");
            // history table
            try {
                sqlite.query("CREATE TABLE "
                        + Table.HISTORY.getName()
                        + " (row INTEGER PRIMARY KEY, id INTEGER NOT NULL, history TEXT NOT NULL);");
            } catch(SQLException e) {
                plugin.getLogger().log(Level.SEVERE,
                        "SQLException on creating history table.", e);
                valid = false;
            }
        }
        // Check inventory table
        if(!sqlite.isTable(Table.INVENTORY.getName())) {
            plugin.getLogger()
                    .info(KarmicJail.TAG + " Created inventory table");
            // inventory table
            try {
                sqlite.query("CREATE TABLE "
                        + Table.INVENTORY.getName()
                        + " (row INTEGER PRIMARY KEY, id INTEGER NOT NULL, slot INTEGER NOT NULL, itemid INTEGER NOT NULL, amount INTEGER NOT NULL, durability TEXT NOT NULL, enchantments TEXT);");
            } catch(SQLException e) {
                plugin.getLogger().log(Level.SEVERE,
                        "SQLException on creating inventory table.", e);
                valid = false;
            }
        }
        return valid;
    }

    public ResultSet query(String query) throws SQLException {
        try {
            return sqlite.query(query);
        } catch(SQLException e) {
            // Try and reopen
            sqlite.open();
            return sqlite.query(query);
        }
    }

    public ResultSet query(PreparedStatement statement) throws SQLException {
        try {
            return sqlite.query(statement);
        } catch(SQLException e) {
            sqlite.open();
            return sqlite.query(statement);
        }
    }

    public PreparedStatement prepare(String statement) throws SQLException {
        try {
            return sqlite.prepare(statement);
        } catch(SQLException e) {
            sqlite.open();
            return sqlite.prepare(statement);
        }
    }

}
