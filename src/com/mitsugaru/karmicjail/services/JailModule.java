package com.mitsugaru.karmicjail.services;

import com.mitsugaru.karmicjail.KarmicJail;

/**
 * Represents a module used for the jail plugin.
 */
public abstract class JailModule {

   /**
    * Plugin reference.
    */
   protected KarmicJail plugin;

   /**
    * Constructor.
    * 
    * @param plugin
    *           - Plugin hook.
    */
   public JailModule(KarmicJail plugin) {
      this.plugin = plugin;
   }

   /**
    * Called when the module has been registered to the API.
    */
   public abstract void starting();

   /**
    * Called when the module has been removed from the API.
    */
   public abstract void closing();

}
