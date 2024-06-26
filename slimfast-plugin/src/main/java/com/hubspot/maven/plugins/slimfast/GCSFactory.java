package com.hubspot.maven.plugins.slimfast;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

public class GCSFactory {
    public static Storage create() {
        Storage storage = StorageOptions.getDefaultInstance().getService();
        return storage;
    }
}
