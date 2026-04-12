package com.bhuvan.s3.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Response DTOs used by the S3 REST API.
 */

public class S3Response {

    /**
     * Generic API response
     * Sample JSON:
     * {
     *   "success": true,
     *   "message": "File uploaded successfully",
     *   "key":     "Pictures/Beaches/IMG_4.HEIC"
     * }
     */
    @Data
    @Builder
    public static class ApiResponse {

        private boolean success;
        private String message;
        private String key;
    }

    /**
     * Fetch all files response
     * Sample JSON:
     * {
     *   "folder": "all",
     *   "prefix": "Pictures/",
     *   "files": [
     *     { "key": "Beaches/IMG_1.PNG", "size": 1048576, "lastModified": "2026-04-09T19:16:15Z" ... }
     *   ],
     *   "count": 2
     * }
     */
    @Data
    @Builder
    public static class ListFilesResponse {

        private String folder;
        private String prefix;
        private List<FileMetadata> files;
        private Integer count;

    }

    /**
     * Metadata for a single S3 object, returned inside {@link ListFilesResponse}.
     */
    @Data
    @Builder
    public static class FileMetadata {

        private String key;
        private String fileName;
        private long size;
        private String lastModified;
        private String storageClass;

    }

    /**
     * Returned by the download endpoint.
     * Sample JSON:
     * {
     *   "fileName":     "IMG_1.HEIC",
     *   "key":          "Pictures/Beaches/IMG_1392.HEIC",
     *   "presignedUrl": "https://s3.amazonaws.com/...",
     *   "expiresInMinutes": 15
     * }
     */
    @Data
    @Builder
    public static class DownloadResponse {

        private String fileName;
        private String key;
        private String presignedUrl;
        private int expiresInMinutes;

    }

    /**
     * Returned after a soft-delete (archive) operation.
     * Sample JSON:
     * {
     *   "originalKey"  : "Pictures/Waterfalls/IMG_2.JPG",
     *   "archivedKey"  : "Pictures/Archive/Waterfalls_IMG_2.JPG_20260409T191501",
     *   "message"      : "File archived successfully"
     * }
     */
    @Data
    @Builder
    public static class ArchiveResponse {

        private String originalKey;
        private String archivedKey;
        private String message;

    }

}