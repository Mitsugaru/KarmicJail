package com.mitsugaru.karmicjail.interfaces;

import com.mitsugaru.karmicjail.services.Module;

/**
 * Interface for managing jailed player groups.
 */
public interface IGroupModule extends Module {

    /**
     * Remove the groups for a given player. This should store removed groups
     * into the player via Storage. Optionally, it should add the jailed group
     * if there is one.
     * 
     * @return True if successful, else false.
     */
    boolean removeGroups(final String playerName);

    /**
     * Should remove the jailed group if there is one and restore the previous
     * groups for the previously jailed player.
     * 
     * @return True if successful, else false.
     */
    boolean restoreGroups(final String playerName);
}
