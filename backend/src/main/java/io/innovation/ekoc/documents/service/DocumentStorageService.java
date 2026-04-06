package io.innovation.ekoc.documents.service;

import io.innovation.ekoc.config.MinIOConfig;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentStorageService {

    private final MinioClient minioClient;
    private final MinIOConfig minIOConfig;

    public String store(MultipartFile file, UUID documentId) {
        ensureBucketExists();
        String storageKey = buildKey(documentId, file.getOriginalFilename());
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minIOConfig.getBucketName())
                    .object(storageKey)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            log.debug("Stored document {} at key {}", documentId, storageKey);
            return storageKey;
        } catch (Exception e) {
            throw new RuntimeException("Failed to store document in MinIO: " + e.getMessage(), e);
        }
    }

    public InputStream download(String storageKey) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(minIOConfig.getBucketName())
                    .object(storageKey)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to download document from MinIO: " + e.getMessage(), e);
        }
    }

    public void delete(String storageKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(minIOConfig.getBucketName())
                    .object(storageKey)
                    .build());
            log.debug("Deleted object at key {}", storageKey);
        } catch (Exception e) {
            log.warn("Failed to delete object {} from MinIO: {}", storageKey, e.getMessage());
        }
    }

    private void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(minIOConfig.getBucketName()).build());
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(minIOConfig.getBucketName()).build());
                log.info("Created MinIO bucket: {}", minIOConfig.getBucketName());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to ensure MinIO bucket exists: " + e.getMessage(), e);
        }
    }

    private String buildKey(UUID documentId, String originalFilename) {
        String safeName = originalFilename != null
                ? originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_")
                : "file";
        return "documents/" + documentId + "/" + safeName;
    }
}
