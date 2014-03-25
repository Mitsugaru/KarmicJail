package com.mitsugaru.karmicjail.storage.models;

import java.util.Collection;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import com.mitsugaru.karmicjail.KarmicJail;
import com.mitsugaru.karmicjail.jail.Jail;
import com.mitsugaru.karmicjail.jail.JailStatus;
import com.mitsugaru.karmicjail.storage.Storage;

public class YAMLStorage implements Storage {
    
    private final KarmicJail plugin;
    
    public YAMLStorage(KarmicJail plugin) {
        this.plugin = plugin;
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
    public Map<Integer, ItemStack> getItems(String playerName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean setItems(String playerName, Map<Integer, ItemStack> items) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean setItem(String playerName, int slot, ItemStack item,
            int amount) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean removeItem(String playerName, int slot) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean reset(String playerName) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean resetInventory(String playerName) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Collection<String> getHistory(String playerName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean addHistory(String playerName, String history) {
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
        // TODO Auto-generated method stub
        
    }

}
