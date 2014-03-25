package com.mitsugaru.karmicjail.storage.models;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentWrapper;
import org.bukkit.inventory.ItemStack;

import com.mitsugaru.karmicjail.KarmicJail;
import com.mitsugaru.karmicjail.config.RootConfig;
import com.mitsugaru.karmicjail.database.Field;
import com.mitsugaru.karmicjail.database.Table;
import com.mitsugaru.karmicjail.jail.JailLogic;
import com.mitsugaru.karmicjail.storage.Storage;

/**
 * Database-based storage source.
 */
public abstract class DatabaseStorage implements Storage {

    /**
     * Plugin reference.
     */
    protected final KarmicJail plugin;

    /**
     * Constructor.
     * 
     * @param plugin
     *            - Plugin reference.
     */
    public DatabaseStorage(KarmicJail plugin) {
        this.plugin = plugin;
    }

    /**
     * Cleanup the given resources.
     * 
     * @param result
     *            - ResultSet to close.
     * @param statement
     *            - Statement to close.
     */
    protected void cleanup(ResultSet result, PreparedStatement statement) {
        if(result != null) {
            try {
                result.close();
            } catch(SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "SQLException on cleanup",
                        e);
            }
        }
        if(statement != null) {
            try {
                statement.close();
            } catch(SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "SQLException on cleanup",
                        e);
            }
        }
    }
    
    /**
     * Get the ID for a given player name.
     * 
     * @param playerName
     *            - Player name.
     * @return Identifier.
     */
    public int getPlayerId(String playername) {
        int id = -1;
        ResultSet query = null;
        try {
            // TODO make this a prepared statement
            query = query("SELECT * FROM " + Table.JAILED.getName()
                    + " WHERE playername='" + playername + "';");
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

    /**
     * Validate that the necessary tables are available. Create any that are
     * missing.
     * 
     * @return True if valid. Returns false if there was an issue creating
     *         tables.
     */
    abstract boolean checkTables();

    /**
     * Check the database connection.
     * 
     * @return True if we're still connected, else false.
     */
    abstract boolean checkConnection();
    
    abstract ResultSet query(String query) throws SQLException;

    abstract ResultSet query(PreparedStatement statement) throws SQLException;

    abstract PreparedStatement prepare(String statement) throws SQLException;
    
    @Override
    public Collection<String> getHistory(String name) {
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
                query = query("SELECT * FROM " + Table.HISTORY.getName()
                        + " WHERE id='" + id + "' ORDER BY row DESC;");
                if(query.next()) {
                    do {
                        list.add(query.getString(Field.HISTORY.getColumnName()));
                    } while(query.next());
                }
            } catch(SQLException e) {
                plugin.getLogger().warning(
                        "SQL Exception on grabbing player history:" + name);
                e.printStackTrace();
            } finally {
                cleanup(query, null);
            }
        }
        return list;
    }

    @Override
    public boolean addHistory(String name, String reason) {
        boolean valid = false;
        JailLogic logic = plugin.getModuleForClass(JailLogic.class);
        int id = getPlayerId(name);
        if(id == -1) {
            // Unknown player?
            logic.addPlayerToDatabase(name);
            id = getPlayerId(name);
        }
        if(id != -1) {
            try {
                final PreparedStatement statement = prepare("INSERT INTO "
                        + Table.HISTORY.getName() + " (id,"
                        + Field.HISTORY.getColumnName() + ") VALUES(?,?);");
                statement.setInt(1, id);
                statement.setString(2, reason);
                statement.executeUpdate();
                //TODO should ensure this can be cleaned up
                statement.close();
                valid = true;
            } catch(SQLException e) {
                plugin.getLogger().warning(
                        "SQL Exception on inserting player history:" + name);
                e.printStackTrace();
            }
        }
        return valid;
    }

    @Override
    public boolean reset(String name) {
        boolean valid = false;
        JailLogic logic = plugin.getModuleForClass(JailLogic.class);
        int id = getPlayerId(name);
        if(id == -1) {
            // Unknown player?
            logic.addPlayerToDatabase(name);
            id = getPlayerId(name);
        }
        ResultSet first = null;

        if(id != -1) {
            try {
                first = query("UPDATE "
                        + Table.JAILED.getName()
                        + " SET time='-1',jailer='',date='',reason='' WHERE id='"
                        + id + "';");
                valid = true;
            } catch(SQLException e) {
                plugin.getLogger().log(Level.SEVERE,
                        "Failed to update player time for " + name, e);
            } finally {
                cleanup(first, null);
            }

        } else {
            plugin.getLogger().warning("Could not reset player '" + name + "'");
        }
        return valid;
    }

    @Override
    public boolean resetInventory(String name) {
        boolean valid = false;
        JailLogic logic = plugin.getModuleForClass(JailLogic.class);
        int id = getPlayerId(name);
        if(id == -1) {
            // Unknown player?
            logic.addPlayerToDatabase(name);
            id = getPlayerId(name);
        }
        ResultSet second = null;
        // Delete inventory table entries of their id
        if(id != -1) {
            try {
                second = query("DELETE FROM " + Table.INVENTORY.getName()
                        + " WHERE id='" + id + "';");
                valid = true;
            } catch(SQLException e) {
                plugin.getLogger().log(Level.SEVERE,
                        "Failed to delete inventory entires for " + name, e);
            } finally {
                cleanup(second, null);
            }
        } else {
            plugin.getLogger().warning(
                    "Could not reset player inventory for '" + name + "'");
        }
        return valid;
    }

    protected void setField(Field field, String playername, String entry, int i,
            double d) {
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
                statement = prepare("UPDATE " + field.getTable().getName()
                        + " SET " + field.getColumnName() + "=? WHERE id='"
                        + id + "';");
                boolean execute = false;
                switch(field.getType()) {

                case STRING: {
                    if(entry != null) {
                        statement.setString(1, entry);
                        execute = true;
                    } else {
                        plugin.getLogger().warning(
                                "String cannot be null for field: "
                                        + field.name());
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
                        plugin.getLogger().warning(
                                "Unhandled setField for field " + field);
                    }
                    break;
                }
                }
                if(execute) {
                    rs = query(statement);
                }
            } catch(SQLException e) {
                plugin.getLogger().warning(
                        "SQL Exception on setting field " + field.name()
                                + " for '" + playername + "'");
                e.printStackTrace();
            } finally {
                cleanup(rs, statement);
            }
        } else {
            plugin.getLogger().warning(
                    "Could not set field " + field.name() + " for player '"
                            + playername + "' to: " + entry);
        }
    }

    protected String getStringField(Field field, String playername) {
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
                    query = query("SELECT * FROM " + field.getTable().getName()
                            + " WHERE id ='" + id + "'");
                    if(query.next()) {
                        out = query.getString(field.getColumnName());
                        if(query.wasNull()) {
                            out = "";
                        }
                    }
                } catch(SQLException e) {
                    plugin.getLogger().warning(
                            "SQL Exception on grabbing field " + field.name()
                                    + " for player '" + playername + "'");
                    e.printStackTrace();
                } finally {
                    cleanup(query, null);
                }
                break;
            }
            default: {
                if(config.debugUnhandled) {
                    plugin.getLogger().warning(
                            "Unhandled getStringField for field " + field);
                }
                break;
            }
            }
        } else {
            plugin.getLogger().warning(
                    "Could not get field " + field.name() + " for player '"
                            + playername + "'");
        }
        return out;
    }

    protected int getIntField(Field field, String playername) {
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
                    query = query("SELECT * FROM " + field.getTable().getName()
                            + " WHERE id ='" + id + "'");
                    if(query.next()) {
                        out = query.getInt(field.getColumnName());
                        if(query.wasNull()) {
                            out = -1;
                        }
                    }
                } catch(SQLException e) {
                    plugin.getLogger().warning(
                            "SQL Exception on grabbing field " + field.name()
                                    + " for player '" + playername + "'");
                    e.printStackTrace();
                } finally {
                    cleanup(query, null);
                }
                break;
            }
            default: {
                if(config.debugUnhandled) {
                    plugin.getLogger().warning(
                            "Unhandled getIntField for field " + field);
                }
                break;
            }
            }
        } else {
            plugin.getLogger().warning(
                    "Could not get field " + field.name() + " for player '"
                            + playername + "'");
        }
        return out;
    }

    protected double getDoubleField(Field field, String playername) {
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
                    query = query("SELECT * FROM " + field.getTable().getName()
                            + " WHERE id ='" + id + "'");
                    if(query.next()) {
                        out = query.getDouble(field.getColumnName());
                        if(query.wasNull()) {
                            out = -1;
                        }
                    }
                } catch(SQLException e) {
                    plugin.getLogger().warning(
                            "SQL Exception on grabbing field " + field.name()
                                    + " for player '" + playername + "'");
                    e.printStackTrace();
                } finally {
                    cleanup(query, null);
                }
                break;
            }
            default: {
                if(config.debugUnhandled) {
                    plugin.getLogger().warning(
                            "Unhandled getDoubleField for field " + field);
                }
                break;
            }
            }
        } else {
            plugin.getLogger().warning(
                    "Could not get field " + field.name() + " for player '"
                            + playername + "'");
        }
        return out;
    }

    @Override
    public boolean setItems(String playername,
            Map<Integer, ItemStack> items) {
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
                rs = query("DELETE FROM " + Table.INVENTORY.getName()
                        + " WHERE id='" + id + "';");
                cleanup(rs, null);
                // Add in items
                for(Map.Entry<Integer, ItemStack> item : items.entrySet()) {
                    statement = prepare("INSERT INTO "
                            + Table.INVENTORY.getName()
                            + " (id,slot,itemid,amount,durability,enchantments) VALUES(?,?,?,?,?,?)");
                    statement.setInt(1, id);
                    statement.setInt(2, item.getKey().intValue());
                    statement.setInt(3, item.getValue().getTypeId());
                    statement.setInt(4, item.getValue().getAmount());
                    statement
                            .setString(5, "" + item.getValue().getDurability());
                    if(!item.getValue().getEnchantments().isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        for(Map.Entry<Enchantment, Integer> e : item.getValue()
                                .getEnchantments().entrySet()) {
                            sb.append(e.getKey().getId() + "v"
                                    + e.getValue().intValue() + "i");
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
                plugin.getLogger().log(
                        Level.SEVERE,
                        "SQL Exception on setting inventory for '" + playername
                                + "'", e);
                valid = false;
            }
            return valid;
        } else {
            plugin.getLogger().log(Level.SEVERE,
                    "Could not set items for player '" + playername + "'");
        }
        return valid;
    }

    @Override
    public Map<Integer, ItemStack> getItems(String playername) {
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
                query = query("SELECT * FROM " + Table.INVENTORY.getName()
                        + " WHERE id='" + id + "';");
                if(query.next()) {
                    do {
                        int itemid = query.getInt(Field.INV_ITEM
                                .getColumnName());
                        int amount = query.getInt(Field.INV_AMOUNT
                                .getColumnName());
                        short dur = query.getShort(Field.INV_DURABILITY
                                .getColumnName());
                        ItemStack add = new ItemStack(itemid, amount, dur);
                        String enchantments = query.getString(Field.INV_ENCHANT
                                .getColumnName());
                        if(add != null) {
                            if(!query.wasNull()) {
                                if(enchantments.contains("i")
                                        || enchantments.contains("v")) {
                                    try {
                                        String[] cut = enchantments.split("i");
                                        for(int s = 0; s < cut.length; s++) {
                                            String[] cutter = cut[s].split("v");
                                            EnchantmentWrapper e = new EnchantmentWrapper(
                                                    Integer.parseInt(cutter[0]));
                                            add.addUnsafeEnchantment(
                                                    e.getEnchantment(),
                                                    Integer.parseInt(cutter[1]));
                                        }
                                    } catch(ArrayIndexOutOfBoundsException a) {
                                        // something went wrong
                                    }
                                }
                            }
                            items.put(
                                    new Integer(query.getInt(Field.INV_SLOT
                                            .getColumnName())), add);
                        }
                    } while(query.next());
                }
            } catch(SQLException e) {
                plugin.getLogger().log(
                        Level.SEVERE,
                        "SQL Exception on getting inventory for '" + playername
                                + "'", e);
            } finally {
                cleanup(query, null);
            }
        }
        return items;
    }

    @Override
    public boolean setItem(String playername, int slot, ItemStack item,
            int amount) {
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
                statement = prepare("INSERT INTO "
                        + Table.INVENTORY.getName()
                        + " (id,slot,itemid,amount,durability,enchantments) VALUES(?,?,?,?,?,?)");
                statement.setInt(1, id);
                statement.setInt(2, slot);
                statement.setInt(3, item.getTypeId());
                statement.setInt(4, amount);
                statement.setString(5, "" + item.getDurability());
                if(!item.getEnchantments().isEmpty()) {
                    // TODO fix this to be ordered
                    StringBuilder sb = new StringBuilder();
                    for(Map.Entry<Enchantment, Integer> e : item
                            .getEnchantments().entrySet()) {
                        sb.append(e.getKey().getId() + "v"
                                + e.getValue().intValue() + "i");
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
                plugin.getLogger().warning(
                        "SQL Exception on setting inventory for '" + playername
                                + "'");
                e.printStackTrace();
            } finally {
                cleanup(rs, statement);
            }
        }
        return valid;
    }

    @Override
    public boolean removeItem(String playername, int slot) {
        boolean valid = false;
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
                query = query("DELETE FROM " + Table.INVENTORY.getName()
                        + " WHERE id='" + id + "' AND slot='" + slot + "';");
                valid = true;
            } catch(SQLException e) {
                //TODO log exception
            } finally {
                cleanup(query, null);
            }
        }
        return valid;
    }

}
