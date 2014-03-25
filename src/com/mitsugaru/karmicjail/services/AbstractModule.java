package com.mitsugaru.karmicjail.services;

import com.mitsugaru.karmicjail.KarmicJail;

/**
 * Represents a module used for the jail plugin.
 */
public abstract class AbstractModule implements Module {

   /**
    * Plugin reference.
    */
   protected final KarmicJail plugin;

   /**
    * Constructor.
    * 
    * @param plugin
    *           - Plugin hook.
    */
   public AbstractModule(KarmicJail plugin) {
      this.plugin = plugin;
   }

}
