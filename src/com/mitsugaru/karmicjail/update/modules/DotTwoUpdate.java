package com.mitsugaru.karmicjail.update.modules;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

import com.mitsugaru.karmicjail.KarmicJail;
import com.mitsugaru.karmicjail.database.DBHandler;
import com.mitsugaru.karmicjail.services.UpdateSubmodule;
import com.mitsugaru.karmicjail.services.version.Version;

/**
 * Handles the 0.2 update.
 * 
 * @author Mitsugaru
 */
public class DotTwoUpdate extends UpdateSubmodule {

    /**
     * Constructor.
     * 
     * @param plugin
     *            - Plugin instance.
     */
    public DotTwoUpdate(KarmicJail plugin) {
        super(plugin);
        targetVersion = new Version("0.2");
        targetVersion.setIgnorePatch(true);
    }

    @Override
    public void update() {
        DBHandler database = plugin.getModuleForClass(DBHandler.class);
        // Add mute column
        String query = "ALTER TABLE jailed ADD muted INTEGER;";
        ResultSet rs = null;
        try {
            rs = database.query(query);
        } catch(SQLException e) {
            plugin.getLogger().log(Level.SEVERE,
                    "Failed to alter jailed table with muted column.", e);
        } finally {
            database.cleanup(rs, null);
        }
    }

}
