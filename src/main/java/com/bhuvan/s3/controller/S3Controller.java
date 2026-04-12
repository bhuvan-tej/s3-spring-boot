package com.bhuvan.s3.controller;

import com.bhuvan.s3.model.S3Response;
import com.bhuvan.s3.service.S3Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST Controller for S3 file operations.
 *
 * Endpoints:
 * POST   /api/s3/upload/{folder}                     Upload a file to Pictures/{folder}/
 * GET    /api/s3/files?folder={folder}               List all files under Pictures/{folder}
 * GET    /api/s3/{folder}/files/{fileName}           Download / view a file (Pre-signed URL)
 * POST   /{folder}/files/{fileName}/archive          Soft-delete: move to Archive/
 * DELETE /{folder}/files/{fileName}                  Hard-delete: Permanently deletes the file
 */
@Slf4j
@RestController
@RequestMapping("/api/s3")
@RequiredArgsConstructor
@Tag(name = "S3 File Operations", description = "Upload, list, download, archive and delete files in Amazon S3")
public class S3Controller {

    private final S3Service s3Service;


    @Operation(summary = "Upload a file", description = "Uploads a file to Pictures/{folder}/ inside the configured S3 bucket " +
            "Or creates under Pictures/ and stores the file")
    @PostMapping(value = "/upload/{folder}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "File uploaded successfully",
                    content = @Content(schema = @Schema(implementation = S3Response.ApiResponse.class))),
            @ApiResponse(responseCode = "400", description = "Blank folder or empty file", content = @Content)
    })
    public ResponseEntity<S3Response.ApiResponse> uploadFile(@PathVariable String folder,
                                                             @RequestParam("file") MultipartFile file) {

        log.info("Upload request — folder: {}, file: {}, size: {} bytes", folder, file.getOriginalFilename(), file.getSize());
        S3Response.ApiResponse response = s3Service.uploadFile(folder, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);

    }

    @Operation(summary = "Lists all files", description = "Lists all files across every sub-folder under Pictures/")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Files fetched successfully",
                    content = @Content(schema = @Schema(implementation = S3Response.ListFilesResponse.class))),
            @ApiResponse(responseCode = "404", description = "File not found", content = @Content)
    })
    @GetMapping(value = "/files", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<S3Response.ListFilesResponse> listAllFiles(@RequestParam(value = "folder", required = false)  String folder) {

        log.info("List all files");
        S3Response.ListFilesResponse response = s3Service.listAllFiles(folder);
        return ResponseEntity.ok(response);

    }

    @Operation(summary = "Get a pre-signed download URL", description = "Returns a temporary URL (valid 15 minutes) for direct download")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pre-signed URL generated",
                    content = @Content(schema = @Schema(implementation = S3Response.DownloadResponse.class))),
            @ApiResponse(responseCode = "404", description = "File not found", content = @Content)
    })
    @GetMapping(value = "/{folder}/files/{fileName}")
    public ResponseEntity<S3Response.DownloadResponse> getDownloadUrl(@PathVariable String folder,
                                                                      @PathVariable String fileName) {

        log.info("Download URL request – folder: {}, file: {}", folder, fileName);
        S3Response.DownloadResponse response = s3Service.getPresignedUrl(folder, fileName);
        return ResponseEntity.ok(response);

    }

    @Operation(summary = "Archive a file (soft delete)",
            description = "Moves the file to Archive/{folder}_{fileName}_{timestamp} and deletes the original")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "File archived",
                    content = @Content(schema = @Schema(implementation = S3Response.ArchiveResponse.class))),
            @ApiResponse(responseCode = "404", description = "File not found",  content = @Content)
    })
    @PostMapping(value = "/{folder}/files/{fileName}/archive")
    public ResponseEntity<S3Response.ArchiveResponse> archiveFile(@PathVariable String folder,
                                                                  @PathVariable String fileName) {

        log.info("Archive request – folder: {}, file: {}", folder, fileName);
        S3Response.ArchiveResponse response = s3Service.archiveFile(folder, fileName);
        return ResponseEntity.ok(response);

    }

    @Operation(summary = "Permanently delete a file", description = "Irreversible. Prefer /archive for recoverable deletes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "File deleted",
                    content = @Content(schema = @Schema(implementation = S3Response.ApiResponse.class))),
            @ApiResponse(responseCode = "404", description = "File not found", content = @Content)
    })
    @DeleteMapping("/{folder}/files/{fileName}")
    public ResponseEntity<S3Response.ApiResponse> deleteFile(@PathVariable String folder,
                                                             @PathVariable String fileName) {

        log.info("Hard delete – folder: {}, file: {}", folder, fileName);
        S3Response.ApiResponse response = s3Service.deleteFile(folder, fileName);
        return ResponseEntity.ok(response);

    }

}
