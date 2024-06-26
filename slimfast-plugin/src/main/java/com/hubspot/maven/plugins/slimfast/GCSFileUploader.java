package com.hubspot.maven.plugins.slimfast;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

import java.io.IOException;
import java.nio.file.Path;

public class GCSFileUploader extends BaseFileUploader {
    private Storage gcs;
    private Log log;

    @Override
    protected void doInit(UploadConfiguration config, Log log) {
        this.gcs = GCSFactory.create();
        this.log = log;
    }

    @Override
    protected void doUpload(String bucket, String key, Path path) throws MojoFailureException, MojoExecutionException {
        if (keyExists(bucket, key)) {
            log.info("Key already exists " + key);
            return;
        }

        try {
            BlobId id = BlobId.of(bucket, key);
            BlobInfo blobInfo = BlobInfo.newBuilder(id).build();
            Storage.BlobWriteOption precondition = Storage.BlobWriteOption.doesNotExist();
            gcs.createFrom(blobInfo, path, precondition);
            log.info("Successfully uploaded key " + key);
        } catch (IOException e) {
            throw new MojoFailureException("Error uploading file " + path, e);
        }

    }

    @Override
    protected void doDestroy() throws MojoFailureException {
    }

    private boolean keyExists(String bucket, String key) throws MojoFailureException {
        BlobId id = BlobId.of(bucket, key);
        Blob blob = gcs.get(id);
        return blob != null && blob.exists();
    }
}
