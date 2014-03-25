package com.mitsugaru.karmicjail.storage;

import com.mitsugaru.karmicjail.KarmicJail;
import com.mitsugaru.karmicjail.storage.models.MySQLStorage;
import com.mitsugaru.karmicjail.storage.models.SQLiteStorage;
import com.mitsugaru.karmicjail.storage.models.YAMLStorage;

public class StorageFactory {
    
    /**
     * Plugin reference.
     */
    private final KarmicJail plugin;
    
    /**
     * Constructor.
     * @param plugin - Plugin reference.
     */
    public StorageFactory(KarmicJail plugin) {
        this.plugin = plugin;
    }

    /**
     * Build a Storage object for the specified type.
     * @param type - Storage type of source.
     * @return 
     */
    public Storage buildStorage(StorageType type) {
        Storage storage = null;
        switch(type) {
        case MYSQL: {
            storage = new MySQLStorage(plugin);
            break;
        }
        case SQLITE: {
            storage = new SQLiteStorage(plugin);
            break;
        }
        case YAML: {
            storage = new YAMLStorage(plugin);
            break;
        }
        default:
            break;
        }
        return storage;
    }
}
