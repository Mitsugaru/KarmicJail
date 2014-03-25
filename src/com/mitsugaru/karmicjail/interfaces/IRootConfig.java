package com.mitsugaru.karmicjail.interfaces;

import com.mitsugaru.karmicjail.services.Module;
import com.mitsugaru.karmicjail.storage.StorageType;

/**
 * Module interface for managing the main configuration file.
 */
public interface IRootConfig extends Module {

    /**
     * Get the storage type to be used.
     * 
     * @return Storage type.
     */
    StorageType getStorageType();

    /**
     * Get the database host name.
     * 
     * @return Host name
     */
    String getHostName();

    /**
     * Get the database port number.
     * 
     * @return Port number.
     */
    int getPort();

    /**
     * Get the database to use.
     * 
     * @return Database
     */
    String getDatabase();

    /**
     * Get the database user name.
     * 
     * @return User name.
     */
    String getUsername();

    /**
     * Get the database password.
     * 
     * @return Password
     */
    String getPassword();

    /**
     * Whether we should broadcast that a player has been jailed.
     * 
     * @return
     */
    boolean isBroadcastJail();

    /**
     * Whether we should broadcast that a player has been unjailed.
     * 
     * @return
     */
    boolean isBroadcastUnjail();

    /**
     * Whether we should broadcast if a jailed player's reason has been changed.
     * 
     * @return
     */
    boolean isBroadcastReason();

    /**
     * Whether we should broadcast only to players that have a specified
     * permission or to everyone on the server.
     * 
     * @return
     */
    boolean isBroadcastIgnorePermission();

    /**
     * Whether we should broadcast if a jailed player joins the server.
     * 
     * @return
     */
    boolean isBroadcastOnJoin();

    /**
     * Whether we should remove player groups on jail.
     * 
     * @return
     */
    boolean isRemoveGroups();

    /**
     * Whether we should return player groups on unjail.
     * 
     * @return
     */
    boolean isReturnGroups();

    /**
     * Whether we should clear player inventory on jail.
     * 
     * @return
     */
    boolean isClearInventory();

    /**
     * Whether we should return player inventory on unjail.
     * 
     * @return
     */
    boolean isReturnInventory();

    /**
     * Whether moderators / administrators can modify saved jailed player
     * inventory.
     * 
     * @return
     */
    boolean isModifyInventory();

    /**
     * Whether or not a timed jail requires associated permission.
     * 
     * @return
     */
    boolean isTimePermRequired();

    /**
     * Whether to warp all players to the default jail on join. I do not
     * remember what this was used for... but it must have been some early
     * request.
     * 
     * @return
     */
    boolean isWarpAllOnJoin();

    /**
     * If we should log storage messages to console.
     * 
     * @return
     */
    boolean isDebugStorage();

    /**
     * If we should log event messages to console.
     * 
     * @return
     */
    boolean isDebugEvents();

    /**
     * If we should log group messages to console.
     * 
     * @return
     */
    boolean isDebugGroups();

    /**
     * If we should log command duration time messages to console.
     * 
     * @return
     */
    boolean isDebugTime();

    /**
     * If we should log unhandled messages to console.
     * 
     * @return
     */
    boolean isDebugUnhandled();

    /**
     * If we should log core jail logic messages to console.
     * 
     * @return
     */
    boolean isDebugLogic();
}
