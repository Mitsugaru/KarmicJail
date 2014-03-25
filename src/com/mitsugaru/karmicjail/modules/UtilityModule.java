package com.mitsugaru.karmicjail.modules;

import com.mitsugaru.karmicjail.KarmicJail;
import com.mitsugaru.karmicjail.interfaces.IUtilityModule;
import com.mitsugaru.karmicjail.services.AbstractModule;

/**
 * Utility methods.
 */
public class UtilityModule extends AbstractModule implements IUtilityModule {
    
    /**
     * Constructor.
     * @param plugin - Plugin reference.
     */
    public UtilityModule(KarmicJail plugin) {
        super(plugin);
    }

    @Override
    public String prettifyMinutes(int minutes) {
        if(minutes < 1) {
            return "about less than a minute";
        }
        if(minutes == 1)
            return "about one minute";
        if(minutes < 60)
            return "about " + minutes + " minutes";
        if(minutes % 60 == 0) {
            if(minutes / 60 == 1)
                return "about one hour";
            else
                return "about " + (minutes / 60) + " hours";
        }
        int m = minutes % 60;
        int h = (minutes - m) / 60;
        return "about " + h + "h" + m + "m";
    }

    @Override
    public String expandName(String Name) {
        int m = 0;
        String Result = "";
        for(int n = 0; n < plugin.getServer().getOnlinePlayers().length; n++) {
            String str = plugin.getServer().getOnlinePlayers()[n].getName();
            if(str.matches("(?i).*" + Name + ".*")) {
                m++;
                Result = str;
                if(m == 2) {
                    return null;
                }
            }
            if(str.equalsIgnoreCase(Name))
                return str;
        }
        if(m == 1)
            return Result;
        if(m > 1) {
            return null;
        }
        return Name;
    }

    @Override
    public void starting() {
    }

    @Override
    public void closing() {
    }

}
