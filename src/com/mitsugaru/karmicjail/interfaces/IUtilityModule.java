package com.mitsugaru.karmicjail.interfaces;

import com.mitsugaru.karmicjail.services.Module;

public interface IUtilityModule extends Module {
    
    /**
     * Makes the minutes readable
     * 
     * @param minutes
     * @return String of readable minutes
     */
    String prettifyMinutes(int minutes);
    
    /**
     * Attempts to look up full name based on who's on the server Given a
     * partial name
     * 
     * @author Frigid, edited by Raphfrk and petteyg359
     */
    String expandName(String Name);
}
