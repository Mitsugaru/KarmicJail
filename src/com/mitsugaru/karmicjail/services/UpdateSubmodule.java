package com.mitsugaru.karmicjail.services;

import com.mitsugaru.karmicjail.KarmicJail;
import com.mitsugaru.karmicjail.services.version.Version;

/**
 * Represents an update that requires changes to data structures or
 * configuration.
 * 
 * @author Mitsugaru
 */
public abstract class UpdateSubmodule implements Comparable<UpdateSubmodule> {

    /**
     * Plugin reference
     */
    protected final KarmicJail plugin;

    /**
     * The target version for the module.
     */
    protected Version targetVersion;

    /**
     * Constructor.
     * 
     * @param plugin
     *            - Plugin reference
     */
    public UpdateSubmodule(final KarmicJail plugin) {
        this.plugin = plugin;
    }

    /**
     * Get the target version this module applies to.
     * 
     * @return Version for update module.
     */
    public Version getTargetVersion() {
        return targetVersion;
    }

    /**
     * Whether we should apply the update changes based on the current version
     * versus the target version.
     * 
     * @param current
     *            - Current version.
     * @return True if the current version is lower than the target version.
     */
    public boolean shouldApplyUpdate(final Version current) {
        return current.compareTo(targetVersion) < 0;
    }

    @Override
    public int compareTo(UpdateSubmodule o) {
        return this.targetVersion.compareTo(o.getTargetVersion());
    }

    /**
     * Logic of this update.
     */
    public abstract void update();

}
