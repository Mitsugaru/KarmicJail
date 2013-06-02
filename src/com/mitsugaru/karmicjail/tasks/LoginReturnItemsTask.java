package com.mitsugaru.karmicjail.tasks;

import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.mitsugaru.karmicjail.KarmicJail;
import com.mitsugaru.karmicjail.config.RootConfig;
import com.mitsugaru.karmicjail.database.DBHandler;

public class LoginReturnItemsTask implements Runnable {

    private KarmicJail plugin;

    private Player player;

    public LoginReturnItemsTask(KarmicJail plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    @Override
    public void run() {
        DBHandler database = plugin.getModuleForClass(DBHandler.class);
        RootConfig config = plugin.getModuleForClass(RootConfig.class);
        // Return items if any
        if(config.returnInventory) {
            Map<Integer, ItemStack> items = database.getPlayerItems(player
                    .getName());
            for(Map.Entry<Integer, ItemStack> item : items.entrySet()) {
                try {
                    player.getInventory().setItem(item.getKey().intValue(),
                            item.getValue());
                } catch(ArrayIndexOutOfBoundsException e) {
                    // Ignore
                }
            }
        }
        database.resetPlayerInventory(player.getName());
    }

}
