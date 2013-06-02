package com.mitsugaru.karmicjail.update;

import java.util.SortedSet;
import java.util.TreeSet;

import org.bukkit.configuration.ConfigurationSection;

import com.mitsugaru.karmicjail.KarmicJail;
import com.mitsugaru.karmicjail.services.Version;
import com.mitsugaru.karmicjail.update.modules.DotFourFourUpdate;
import com.mitsugaru.karmicjail.update.modules.DotFourThreeUpdate;
import com.mitsugaru.karmicjail.update.modules.DotFourUpdate;
import com.mitsugaru.karmicjail.update.modules.DotThreeUpdate;
import com.mitsugaru.karmicjail.update.modules.DotTwoUpdate;

/**
 * Handles changes for future updates from previous versions.
 * 
 * @author Mitsugaru
 */
public class UpdateManager {

    /**
     * Plugin instance.
     */
    private KarmicJail plugin;

    /**
     * Update modules.
     */
    private final SortedSet<UpdateModule> modules = new TreeSet<UpdateModule>();

    /**
     * Constructor.
     * 
     * @param plugin
     *            - Plugin instance.
     */
    public UpdateManager(KarmicJail plugin) {
        this.plugin = plugin;

        modules.add(new DotTwoUpdate(plugin));
        modules.add(new DotThreeUpdate(plugin));
        modules.add(new DotFourUpdate(plugin));
        modules.add(new DotFourThreeUpdate(plugin));
        modules.add(new DotFourFourUpdate(plugin));
    }

    /**
     * Check if updates are necessary
     */
    public void checkUpdate() {
        // Check if need to update
        ConfigurationSection config = plugin.getConfig();
        Version version = new Version(plugin.getDescription().getVersion());
        if(!version.validate()) {
            version.setIgnorePatch(true);
        }
        Version current = new Version(config.getString("version"));
        if(!current.validate()) {
            current.setIgnorePatch(true);
        }
        if(version.compareTo(current) > 0) {
            // Update to latest version
            plugin.getLogger().info(
                    "Updating to v"
                            + plugin.getDescription().getVersion()
                                    .replace("-SNAPSHOT", ""));
            update(current);
        }
    }

    /**
     * This method is called to make the appropriate changes, most likely only
     * necessary for database schema modification, for a proper update.
     */
    private void update(Version current) {
        // Iterate over modules.
        for(UpdateModule module : modules) {
            if(module.shouldApplyUpdate(current)) {
                module.update();
            }
        }

        // Update version number in config.yml
        plugin.getConfig().set("version", plugin.getDescription().getVersion());
        plugin.saveConfig();
    }
}
