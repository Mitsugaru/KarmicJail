package com.mitsugaru.karmicjail.interfaces;

import com.mitsugaru.karmicjail.services.Module;

/**
 * Represents an update module.
 */
public interface IUpdateModule extends Module {

    /**
     * Check version and handle all incremental updates if necessary.
     */
    void doUpdates();
}
