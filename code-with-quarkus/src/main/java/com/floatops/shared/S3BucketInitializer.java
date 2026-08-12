package com.floatops.shared;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

// MinIO (a diferencia de AWS S3 real) no crea buckets automaticamente -
// hay que pedirselo explicitamente. Esto corre una vez al arrancar la app.
@ApplicationScoped
public class S3BucketInitializer {

    private static final Logger LOG = Logger.getLogger(S3BucketInitializer.class);
    public static final String PHOTOS_BUCKET = "floatops-photos";

    @Inject
    S3Client s3Client;

    void onStart(@Observes StartupEvent ev) {
        try {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(PHOTOS_BUCKET).build());
            LOG.info("Bucket '" + PHOTOS_BUCKET + "' creado");
        } catch (BucketAlreadyOwnedByYouException e) {
            LOG.info("Bucket '" + PHOTOS_BUCKET + "' ya existia");
        }
    }
}