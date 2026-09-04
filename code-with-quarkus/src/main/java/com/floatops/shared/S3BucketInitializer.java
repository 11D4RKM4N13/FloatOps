package com.floatops.shared;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;

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
            // Ver nota en WorkOrderService.adjuntarFoto: sobrecarga con
            // Consumer<Builder> en vez de encadenar .build(), por el
            // covariant return ambiguo entre S3Request.Builder y
            // CopyableBuilder que ECJ resuelve mal.
            s3Client.createBucket(b -> b.bucket(PHOTOS_BUCKET));
            LOG.info("Bucket '" + PHOTOS_BUCKET + "' creado");
        } catch (BucketAlreadyOwnedByYouException e) {
            LOG.info("Bucket '" + PHOTOS_BUCKET + "' ya existia");
        }
    }
}