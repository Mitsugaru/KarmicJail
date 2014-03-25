package com.mitsugaru.karmicjail.interfaces;

import com.mitsugaru.karmicjail.services.Module;
import com.mitsugaru.karmicjail.storage.Storage;

/**
 * Interface for StorageModule.
 */
public interface IStorageModule extends Module {

    /**
     * Get the Storage model currently configured to be used.
     * @return Storage model object.
     */
    Storage getStorage();
}
