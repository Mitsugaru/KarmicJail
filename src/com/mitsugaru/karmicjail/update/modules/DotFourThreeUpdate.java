package com.mitsugaru.karmicjail.update.modules;

import com.mitsugaru.karmicjail.KarmicJail;
import com.mitsugaru.karmicjail.config.RootConfig;
import com.mitsugaru.karmicjail.services.UpdateSubmodule;
import com.mitsugaru.karmicjail.services.version.Version;

/**
 * Handles the 0.43 update.
 * 
 * @author Mitsugaru
 */
public class DotFourThreeUpdate extends UpdateSubmodule {

    /**
     * Constructor.
     * 
     * @param plugin
     *            - Plugin instance.
     */
    public DotFourThreeUpdate(KarmicJail plugin) {
        super(plugin);
        targetVersion = new Version("0.43");
        targetVersion.setIgnorePatch(true);
    }

    @Override
    public void update() {
        RootConfig config = plugin.getModuleForClass(RootConfig.class);
        // Update config
        plugin.getLogger().info("Updating config");
        config.jailGroup = plugin.getConfig().getString("jailgroup", "Jailed");
        plugin.getConfig().set("group.jail.group", config.jailGroup);
        plugin.getConfig().set("jailgroup", null);
        config.removeGroups = plugin.getConfig().getBoolean("removegroups",
                true);
        plugin.getConfig().set("group.removeOnJail", config.removeGroups);
        plugin.getConfig().set("removegroups", null);
    }

}
