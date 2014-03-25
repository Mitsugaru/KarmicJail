package com.mitsugaru.karmicjail.modules;

import java.util.HashMap;
import java.util.Map;

import com.mitsugaru.karmicjail.KarmicJail;
import com.mitsugaru.karmicjail.jail.Jail;
import com.mitsugaru.karmicjail.services.AbstractModule;
import com.mitsugaru.karmicjail.services.Service;

@Service
public class JailModule extends AbstractModule {
    
    private final Map<String, Jail> jailCache = new HashMap<String, Jail>();

    public JailModule(KarmicJail plugin) {
        super(plugin);
    }

    @Override
    public void starting() {
    }

    @Override
    public void closing() {
    }

    
}
