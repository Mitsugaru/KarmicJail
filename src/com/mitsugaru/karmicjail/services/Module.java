package com.mitsugaru.karmicjail.services;

public interface Module {

    /**
     * Called when the module has been registered to the API.
     */
    public abstract void starting();

    /**
     * Called when the module has been removed from the API.
     */
    public abstract void closing();
    
}
