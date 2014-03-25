package com.mitsugaru.karmicjail.modules;

import com.mitsugaru.karmicjail.KarmicJail;
import com.mitsugaru.karmicjail.interfaces.IStorageModule;
import com.mitsugaru.karmicjail.services.AbstractModule;
import com.mitsugaru.karmicjail.storage.Storage;
import com.mitsugaru.karmicjail.storage.StorageFactory;

/**
 * Module handling storage source reference.
 */
public class StorageModule extends AbstractModule implements IStorageModule {
    
    /**
     * Storage source reference.
     */
    private Storage storage;

    /**
     * Constructor.
     * @param plugin - Plugin reference.
     */
    public StorageModule(KarmicJail plugin) {
        super(plugin);
    }
    
    @Override
    public Storage getStorage() {
        return storage;
    }

    @Override
    public void starting() {
        StorageFactory factory = new StorageFactory(plugin);
        //TODO set storage based on type.
    }

    @Override
    public void closing() {
        storage.close();
    }

}
