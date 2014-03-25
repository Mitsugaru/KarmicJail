package com.mitsugaru.karmicjail.storage;

import java.util.Collection;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import com.mitsugaru.karmicjail.jail.Jail;
import com.mitsugaru.karmicjail.jail.JailStatus;

/**
 * Common interface for storage sources.
 */
public interface Storage {

    /**
     * Get the date for when a player is jailed.
     * 
     * @param playerName
     *            - Player name.
     * @return String-based date and time of jail.
     */
    String getDate(String playerName);

    /**
     * Set the jail date for a player.
     * 
     * @param playerName
     *            - Player name.
     * @param date
     *            - Jail date as a string.
     * @return True if successful, else false.
     */
    boolean setDate(String playerName, String date);

    /**
     * Get the player that jailed the given player.
     * 
     * @param playerName
     *            - Player name.
     * @return Name of player that issued the jail command.
     */
    String getJailer(String playerName);

    /**
     * Set the player that jailed the given player.
     * 
     * @param playerName
     *            - Player name.
     * @param jailer
     *            - Jailer name.
     * @return True if successful, else false.
     */
    boolean setJailer(String playerName, String jailer);

    /**
     * Get the auto-release jail time for a given player
     * 
     * @param playerName
     *            - Player name.
     * @return Time duration. If negative or 0, assume indefinite.
     */
    double getJailTime(String playerName);

    /**
     * Set the auto-release time for a given player.
     * 
     * @param playerName
     *            - Player name.
     * @param time
     *            - Duration of jail time.
     * @return True if successful, else false.
     */
    boolean setJailTime(String playerName, double time);

    /**
     * Get the jail status for a given player.
     * 
     * @param playerName
     *            - String player.
     * @return Jail status for player, defaults to freed if unknown.
     */
    JailStatus getStatus(String playerName);

    /**
     * Set a player's status.
     * 
     * @param playerName
     *            - Player name.
     * @param status
     *            - Jail status.
     * @return True if successful, else false.
     */
    boolean setStatus(String playerName, JailStatus status);

    /**
     * Get the last location of the given player.
     * 
     * @param playerName
     *            - Player name.
     * @return Location of where the player was last.
     */
    Location getLastLocation(String playerName);

    /**
     * Set a player's last location.
     * 
     * @param playerName
     *            - Player name
     * @param location
     *            - Location to save.
     * @return True if successful, else false.
     */
    boolean setLastLocation(String playerName, Location location);

    /**
     * Get the jail reason.
     * 
     * @param playerName
     *            - Player name.
     * @return Reason for player being in jail.
     */
    String getReason(String playerName);

    /**
     * Set a player's jail reason.
     * 
     * @param playerName
     *            - Player name
     * @param reason
     *            - Reason to set.
     * @return True if successful.
     */
    boolean setReason(String playerName, String reason);

    /**
     * Get whether a player is muted.
     * 
     * @param playerName
     *            - Player name.
     * @return If they are muted or not.
     */
    boolean isMuted(String playerName);

    /**
     * Set the mute flag for a player.
     * 
     * @param playerName
     *            - Player name.
     * @param mute
     *            - Whether to mute them or not.
     * @return True if successful, else false.
     */
    boolean setMuted(String playerName, boolean mute);

    /**
     * Get the items for a given jailed player.
     * 
     * @param playerName
     *            - Player name.
     * @return Map of items.
     */
    Map<Integer, ItemStack> getItems(String playerName);

    /**
     * Set the items for a given jailed player.
     * 
     * @param playerName
     *            - Player name.
     * @param items
     *            - Items to save.
     * @return True if successful, else false.
     */
    boolean setItems(String playerName, Map<Integer, ItemStack> items);

    /**
     * Set a specific item for a jailed player's inventory.
     * 
     * @param playerName
     *            - Player name.
     * @param slot
     *            - Slot to modify.
     * @param item
     *            - Item to set.
     * @param amount
     *            - Amount to set.
     * @return True if successful, else false.
     */
    boolean setItem(String playerName, int slot, ItemStack item, int amount);

    /**
     * Remove an item from a jailed player's inventory.
     * 
     * @param playerName
     *            - Player name.
     * @param slot
     *            - Slot to clear.
     * @return True if successful, else false.
     */
    boolean removeItem(String playerName, int slot);

    /**
     * Reset a player's jail status.
     * 
     * @param playerName
     *            - Player name.
     * @return True if successful, else false.
     */
    boolean reset(String playerName);

    /**
     * Reset the inventory records for a given jailed player.
     * 
     * @param playerName
     *            - Player name.
     * @return True if successful, else false.
     */
    boolean resetInventory(String playerName);

    /**
     * Get a collection of player history, in chronological order.
     * 
     * @param playerName
     *            - Player name.
     * @return Collection of history data.
     */
    Collection<String> getHistory(String playerName);

    /**
     * Add an entry to a player's historical record.
     * 
     * @param playerName
     *            - Player name.
     * @param history
     *            - Entry to add.
     * @return True if successfully added in storage, else false.
     */
    boolean addHistory(String playerName, String history);

    /**
     * Get the location for the given jail.
     * 
     * @param jail
     *            - Jail name.
     * @return Location of jail. If no jail exists, returns null.
     */
    Jail getJail(String jail);

    /**
     * Add a jail with the given location.
     * 
     * @param jail
     *            - Name of jail.
     * @param location
     *            - Location of jail.
     * @return True if added, else false.
     */
    boolean addJail(String jail, Location location);

    /**
     * Remove a jail.
     * 
     * @param jail
     *            - Jail name.
     * @return True if successful, else false.
     */
    boolean removeJail(String jail);

    /**
     * Get the jail associated with given jailed player.
     * 
     * @param playerName
     *            - Jailed player name.
     * @return Jail for the player. Returns null if player is not jailed. If no
     *         jail is found, attempts to use the default jail.
     */
    Jail getJailForPlayer(String playerName);

    /**
     * Get the stored groups for a given player.
     * 
     * @param playerName
     *            - Player name.
     * @return Collection of player's groups.
     */
    Collection<String> getGroups(String playerName);

    /**
     * Store the old groups for a given jail player.
     * 
     * @param playerName
     *            - Player name.
     * @param groups
     *            - Groups to save.
     * @return True if successful, else false.
     */
    boolean setGroups(String playerName, Collection<String> groups);

    /**
     * Close connection with source, if necessary.
     */
    void close();
}
