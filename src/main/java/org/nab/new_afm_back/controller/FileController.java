package org.nab.new_afm_back.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nab.new_afm_back.model.Case;
import org.nab.new_afm_back.service.impl.FileService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/pdf")
@RequiredArgsConstructor
@Tag(name = "PDF Management", description = "APIs for uploading and managing PDF documents")
@Slf4j
public class FileController {

    private final FileControllerHandler handler;

    @Operation(summary = "Upload case with PDF and additional files")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Case uploaded successfully",
                    content = @Content(schema = @Schema(implementation = Case.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> uploadCaseWithFiles(
            @RequestPart("caseData") String caseDataJson,
            @RequestPart(value = "additionalFiles", required = false) List<MultipartFile> additionalFiles) {

        log.info("Starting case upload request. Additional files count: {}",
                additionalFiles != null ? additionalFiles.size() : 0);

        return handler.handleUploadCase(caseDataJson, additionalFiles);
    }

    @Operation(summary = "Add additional files to an existing case")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Files added successfully",
                    content = @Content(schema = @Schema(implementation = Case.class))),
            @ApiResponse(responseCode = "400", description = "Invalid case number or files"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping(value = "/{caseNumber}/add-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> addAdditionalFilesToCase(
            @Parameter(description = "Case number", required = true) @PathVariable String caseNumber,
            @Parameter(description = "Additional files", required = true) @RequestPart("additionalFiles") List<MultipartFile> additionalFiles) {

        log.info("Adding {} files to case: {}",
                additionalFiles != null ? additionalFiles.size() : 0, caseNumber);

        return handler.handleAddFiles(caseNumber, additionalFiles);
    }

    @Operation(summary = "Delete a file from a case")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Case or file not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/{caseNumber}/file/{fileId}")
    public ResponseEntity<?> deleteFile(
            @Parameter(description = "Case number", required = true) @PathVariable String caseNumber,
            @Parameter(description = "File ID", required = true) @PathVariable int fileId) {

        log.info("Deleting file '{}' from case: {}", fileId, caseNumber);

        return handler.handleDeleteFile(caseNumber, fileId);
    }

    @Operation(summary = "Download document by case number")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Document downloaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid format"),
            @ApiResponse(responseCode = "404", description = "Document not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{caseNumber}/download")
    public ResponseEntity<?> downloadDocument(
            @Parameter(description = "Case number to download document for", required = true)
            @PathVariable("caseNumber") String caseNumber,

            @Parameter(description = "Document format (pdf or word)", required = false,
                    schema = @Schema(allowableValues = {"pdf", "word"}, defaultValue = "pdf"))
            @RequestParam(value = "format", defaultValue = "pdf") String format) {

        log.info("Document download requested for case: {}, format: {}", caseNumber, format);

        return handler.handleDownloadDocument(caseNumber, format);
    }
}