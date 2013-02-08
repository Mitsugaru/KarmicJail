package com.mitsugaru.karmicjail.inventory;

import java.util.Map;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import com.mitsugaru.karmicjail.KarmicJail;
import com.mitsugaru.karmicjail.database.DBHandler;

/**
 * Displays a jailed user's inventory.
 * 
 * @author Tokume
 * 
 */
public class JailInventoryHolder implements InventoryHolder {
   private KarmicJail plugin;
   private Inventory inventory;
   private String target;

   public JailInventoryHolder(KarmicJail plugin, String target) {
      this.plugin = plugin;
      this.target = target;
   }

   public void setInventory(Inventory inventory) {
      DBHandler database = plugin.getModuleForClass(DBHandler.class);
      this.inventory = inventory;
      // Import inventory from database
      final Map<Integer, ItemStack> targetInv = database.getPlayerItems(target);
      for(Map.Entry<Integer, ItemStack> entry : targetInv.entrySet()) {
         inventory.setItem(entry.getKey().intValue(), entry.getValue());
      }
   }

   @Override
   public Inventory getInventory() {
      return inventory;
   }

   public String getTarget() {
      return target;
   }
}