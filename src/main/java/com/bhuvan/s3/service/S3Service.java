package com.bhuvan.s3.service;

import com.bhuvan.s3.model.S3Response;
import com.bhuvan.s3.exception.S3Exception;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service layer for all Amazon S3 operations.
 *
 * S3 folder structure this service manages:
 *   {bucketName}/
 *   │── Pictures/                 ← base folder  (aws.s3.base-folder)
 *   │   ├── Beaches/
 *   │   │   └── IMG_1.JPG
 *   │   └── Waterfalls/
 *   │       ├── IMG_2.JPG
 *   │       └── IMG_3.JPG
 *   └── Archive/              ← archive folder (aws.s3.archive-folder)
 *       └── Waterfalls_IMG_4.JPG_20260409T191501
 *
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private static final DateTimeFormatter ARCHIVE_TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss").withZone(ZoneOffset.UTC);

    private final S3Client s3Client;

    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.base-folder}")
    private String baseFolder;

    @Value("${aws.s3.archive-folder}")
    private String archiveFolder;

    private static final int PRESIGN_DURATION_MINUTES = 15;

    /**
     * Uploads a {@link MultipartFile} to {@code Pictures/{folder}/{fileName}}.
     *
     * The S3 key is constructed as: {@code {baseFolder}/{folder}/{originalFileName}}
     *
     * @param folder    target subfolder name (e.g., "Beaches" or "Waterfalls")
     * @param file      multipart file received from the HTTP request
     * @return          the full S3 object key where the file was stored
     */
    public S3Response.ApiResponse uploadFile(String folder, MultipartFile file) {

        if (folder == null || folder.isBlank()) {
            throw new S3Exception.InvalidRequestException("Folder name must not be blank.");
        }
        if (file == null || file.isEmpty()) {
            throw new S3Exception.InvalidRequestException("Uploaded file must not be empty.");
        }

        // Build the full S3 key:  Pictures/Beaches/IMG_4.JPG
        String s3Key = buildKey(folder, file.getOriginalFilename());

        // Check folder existence via prefix
        ListObjectsV2Response response = s3Client.listObjectsV2(
                ListObjectsV2Request.builder()
                        .bucket(bucketName)
                        .prefix(buildFolderPrefix(folder))
                        .maxKeys(1)
                        .build()
        );

        boolean folderExists = response.hasContents() && !response.contents().isEmpty();

        if (folderExists) {
            log.info("Folder '{}' already exists. Adding file to existing folder.", folder);
        } else {
            log.info("Folder '{}' does not exist. Creating and adding file.", folder);
        }

        log.info("Uploading file to S3 — bucket: {}, key: {}, size: {} bytes", bucketName, s3Key, file.getSize());

        try {
            // Build the PutObjectRequest with content-type metadata
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(file.getContentType())   // preserves MIME type (image/jpeg etc.)
                    .contentLength(file.getSize())
                    .build();

            // Stream the file bytes directly to S3 — no local disk write needed
            s3Client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            return S3Response.ApiResponse.builder()
                    .success(true)
                    .message("File uploaded successfully.")
                    .key(s3Key)
                    .build();
        } catch (IOException e) {
            throw new S3Exception.FileUploadException("Failed to read uploaded file: " + e.getMessage(), e);
        }
    }

    /**
     * Lists all objects under {@code Pictures/<folder>/}.
     *
     * Returns only actual objects so that the
     * response only contains files the user uploaded.
     *
     * @param folder sub-folder to list (e.g. "Beaches"). Pass {@code ""} or {@code null}
     *               to list everything under Pictures/.
     * @return {@link S3Response.ListFilesResponse} with file metadata
     */
    public S3Response.ListFilesResponse listAllFiles(String folder) {

        // Build prefix – if folder is blank/null, list all of Pictures/
        String prefix = (folder == null || folder.isBlank())
                ? baseFolder : buildFolderPrefix(folder);

        assertObjectExists(prefix);

        log.info("Listing objects with prefix: {}", prefix);
        ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(prefix)
                .build();

        ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);

        // Map SDK S3Object → our FileMetadata DTO, skipping virtual folder markers
        List<S3Response.FileMetadata> files = listResponse.contents().stream()
                // Skip the "folder" marker objects (keys that end with /)
                .filter(obj -> !obj.key().endsWith("/"))
                .map(obj -> S3Response.FileMetadata.builder()
                        .key(obj.key().substring(prefix.length()))
                        // Strip prefix to get just the file name
                        .fileName(obj.key().substring(obj.key().lastIndexOf('/') + 1))
                        .size(obj.size())
                        .lastModified(obj.lastModified().toString())
                        .storageClass(obj.storageClassAsString())
                        .build())
                .collect(Collectors.toList());

        log.info("Found {} file(s) under prefix: {}", files.size(), prefix);

        return S3Response.ListFilesResponse.builder()
                .folder(folder != null ? folder : "all")
                .prefix(prefix)
                .files(files)
                .count(files.size())
                .build();
    }

    /**
     * Generates a pre-signed GET URL for {@code Pictures/<folder>/<fileName>}.
     *
     * The URL is valid for {@value #PRESIGN_DURATION_MINUTES} minutes and does NOT
     * require the caller to have AWS credentials – anyone with the URL can download
     * the file within the validity window.
     *
     * @param folder   sub-folder (e.g. "Waterfalls")
     * @param fileName file name (e.g. "IMG_4694.JPG")
     * @return {@link S3Response.DownloadResponse} containing the pre-signed URL
     * @throws S3Exception.FileNotFoundException if the object does not exist in S3
     */
    public S3Response.DownloadResponse getPresignedUrl(String folder, String fileName) {

        String key = buildFolderPrefix(folder) + fileName;
        log.info("Generating pre-signed URL for key: {}", key);

        // Verify the object actually exists before generating the URL
        assertObjectExists(key);

        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(PRESIGN_DURATION_MINUTES))
                .getObjectRequest(getRequest)
                .build();

        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
        String url = presigned.url().toString();

        log.info("Pre-signed URL generated for: {} (expires in {} min)", key, PRESIGN_DURATION_MINUTES);

        return S3Response.DownloadResponse.builder()
                .fileName(fileName)
                .key(key)
                .presignedUrl(url)
                .expiresInMinutes(PRESIGN_DURATION_MINUTES)
                .build();
    }

    /**
     * Verifies that an S3 object exists by issuing a lightweight HeadObject request.
     *
     * @param key full S3 object key
     * @throws S3Exception.FileNotFoundException if HeadObject returns 404
     */
    private void assertObjectExists(String key) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build());
        } catch (NoSuchKeyException e) {
            throw new S3Exception.FileNotFoundException("File not found in S3: " + key);
        }
    }

    /**
     * Soft-deletes a file by copying it to {@code Archive/} and removing the original.
     *
     * Archive key format: {@code Archive/{folder}_{fileName}_{timestamp}}
     * >Example: {@code Archive/Beaches_solids.jpg_20260409T230624}
     *
     * Flow:
     *   Assert source exists (HeadObject)
     *   CopyObject → Archive/
     *   DeleteObject → original key
     *
     * @param folder   sub-folder of the source file (e.g. "Beaches")
     * @param fileName file to archive (e.g. "solids.jpg")
     * @return {@link S3Response.ArchiveResponse}
     */
    public S3Response.ArchiveResponse archiveFile(String folder, String fileName) {

        String sourceKey  = buildFolderPrefix(folder) + fileName;
        String archiveKey = buildArchiveKey(folder, fileName);

        log.info("Archiving: {} → {}", sourceKey, archiveKey);

        assertObjectExists(sourceKey);

        try {
            // Step 1 – Copy to Archive/
            s3Client.copyObject(CopyObjectRequest.builder()
                    .sourceBucket(bucketName)
                    .sourceKey(sourceKey)
                    .destinationBucket(bucketName)
                    .destinationKey(archiveKey)
                    .build());

            log.info("Copied {} → {}", sourceKey, archiveKey);

            // Step 2 – Delete original
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(sourceKey)
                    .build());

            log.info("Deleted original: {}", sourceKey);

            return S3Response.ArchiveResponse.builder()
                    .originalKey(sourceKey)
                    .archivedKey(archiveKey)
                    .message("File archived successfully. To permanently delete, remove from Archive folder.")
                    .build();

        } catch (SdkClientException e) {
            log.error("Client/network error: {}", e.getMessage());
            throw new S3Exception.FileUploadException("Network error while archiving", e);
        }
    }

    /**
     * Permanently deletes an object. Irreversible — prefer {@link #archiveFile} when possible.
     *
     * @param folder   sub-folder (e.g. "Beaches")
     * @param fileName file name to delete
     * @return {@link S3Response.ApiResponse}
     */
    public S3Response.ApiResponse deleteFile(String folder, String fileName) {

        String key = buildFolderPrefix(folder) + fileName;
        log.info("Hard-deleting: {}", key);

        assertObjectExists(key);

        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build());

        log.info("Permanently deleted: {}", key);

        return S3Response.ApiResponse.builder()
                .success(true)
                .message("File permanently deleted.")
                .key(key)
                .build();
    }

    //HELPERS

    public String buildFolderPrefix(String folder) {
        // Normalize: strip leading/trailing slashes from the caller-supplied folder name
        String clean = folder.replaceAll("^/+|/+$", "");
        return baseFolder + clean + "/";
    }

    // Builds a full S3 object key for a file in a category subfolder.
    private String buildKey(String folder, String fileName) {
        return baseFolder + folder + "/" + fileName;
    }

    // Builds the flat archive key for a given folder + fileName
    private String buildArchiveKey(String folder, String fileName) {
        String timestamp = ARCHIVE_TIMESTAMP_FMT.format(Instant.now());
        // e.g. Archive/Beaches_solids.jpg_20260409T230624
        return archiveFolder + folder + "_" + fileName + "_" + timestamp;
    }

}