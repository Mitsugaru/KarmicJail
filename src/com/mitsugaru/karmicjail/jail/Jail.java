package com.mitsugaru.karmicjail.jail;

import org.bukkit.Location;

/**
 * Represents a Jail.
 */
public class Jail {

    /**
     * Jail name.
     */
    protected final String name;

    /**
     * Jail location.
     */
    protected Location location;

    boolean denyBlockBreak = true;

    boolean denyBlockPlace = true;

    boolean denyChat = false;

    boolean denyCommands = false;

    boolean denyInteract = true;

    boolean denyInventory = true;

    boolean denyItemCraft = true;

    boolean denyItemDrop = true;

    boolean denyItemEnchant = true;

    boolean denyItemPickup = true;

    boolean denyMove = false;

    private boolean isDefault = false;

    /**
     * Constructor.
     * 
     * @param name
     *            - Jail name.
     */
    public Jail(String name) {
        this.name = name;
    }

    /**
     * Get the jail name.
     * 
     * @return Jail name.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the jail location.
     * 
     * @return Location of jail point.
     */
    public Location getLocation() {
        return location;
    }

    /**
     * 
     * @param location
     */
    public void setLocation(Location location) {
        this.location = location;
    }

    /**
     * Check if this is the default jail to use.
     * 
     * @return True if this is the default jail, else false.
     */
    public boolean isDefault() {
        return isDefault;
    }

    /**
     * Set this jail to the default.
     * 
     * @param isDefault
     *            - Default flag.
     */
    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public boolean isDenyBlockBreak() {
        return denyBlockBreak;
    }

    public void setDenyBlockBreak(boolean denyBlockBreak) {
        this.denyBlockBreak = denyBlockBreak;
    }

    public boolean isDenyBlockPlace() {
        return denyBlockPlace;
    }

    public void setDenyBlockPlace(boolean denyBlockPlace) {
        this.denyBlockPlace = denyBlockPlace;
    }

    public boolean isDenyChat() {
        return denyChat;
    }

    public void setDenyChat(boolean denyChat) {
        this.denyChat = denyChat;
    }

    public boolean isDenyCommands() {
        return denyCommands;
    }

    public void setDenyCommands(boolean denyCommands) {
        this.denyCommands = denyCommands;
    }

    public boolean isDenyInteract() {
        return denyInteract;
    }

    public void setDenyInteract(boolean denyInteract) {
        this.denyInteract = denyInteract;
    }

    public boolean isDenyInventory() {
        return denyInventory;
    }

    public void setDenyInventory(boolean denyInventory) {
        this.denyInventory = denyInventory;
    }

    public boolean isDenyItemCraft() {
        return denyItemCraft;
    }

    public void setDenyItemCraft(boolean denyItemCraft) {
        this.denyItemCraft = denyItemCraft;
    }

    public boolean isDenyItemDrop() {
        return denyItemDrop;
    }

    public void setDenyItemDrop(boolean denyItemDrop) {
        this.denyItemDrop = denyItemDrop;
    }

    public boolean isDenyItemEnchant() {
        return denyItemEnchant;
    }

    public void setDenyItemEnchant(boolean denyItemEnchant) {
        this.denyItemEnchant = denyItemEnchant;
    }

    public boolean isDenyItemPickup() {
        return denyItemPickup;
    }

    public void setDenyItemPickup(boolean denyItemPickup) {
        this.denyItemPickup = denyItemPickup;
    }

    public boolean isDenyMove() {
        return denyMove;
    }

    public void setDenyMove(boolean denyMove) {
        this.denyMove = denyMove;
    }

}
