package com.mitsugaru.karmicjail.inventory;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentWrapper;

/**
 * Wrapper class so that Enchantments can be ordered / comparable
 * 
 * @author Tokume
 * 
 */
public class ComparableEnchantment extends EnchantmentWrapper implements Comparable<ComparableEnchantment> {
    /**
     * Constructor
     * 
     * @param Enchantment
     *            object to wrap
     */
    public ComparableEnchantment(Enchantment enchantment) {
        super(enchantment.getId());
    }
    
    /**
     * Compares the enchantment ids to give order and allow for sorting.
     */
    @Override
    public int compareTo(ComparableEnchantment o) {
        return getId() - o.getId();
    }
}
