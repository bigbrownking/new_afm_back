package org.nab.new_afm_back.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nab.new_afm_back.dto.request.UploadCaseRequest;
import org.nab.new_afm_back.model.Case;
import org.nab.new_afm_back.service.impl.FileService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
@RestController
@RequestMapping("/pdf")
@RequiredArgsConstructor
@Tag(name = "PDF Management", description = "APIs for uploading and managing PDF documents")
@Slf4j
public class FileController {

    private final FileService fileService;

    @Operation(
            summary = "Upload case with PDF and additional files",
            description = "Upload a new case with a main PDF document and optional additional files"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Case uploaded successfully",
                    content = @Content(schema = @Schema(implementation = Case.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data or file validation failed",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error during file processing",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> uploadCaseWithFiles(
            @RequestPart("caseData") String caseDataJson,
            @RequestPart(value = "additionalFiles", required = false) List<MultipartFile> additionalFiles) {

        log.info("Starting case upload request. Additional files count: {}",
                additionalFiles != null ? additionalFiles.size() : 0);
        log.debug("Case data JSON: {}", caseDataJson);

        try {
            UploadCaseRequest request = new ObjectMapper().readValue(caseDataJson, UploadCaseRequest.class);
            log.info("Parsed upload request for case number: {}, author: {}",
                    request.getNumber(), request.getAuthor());

            Case uploadedCase = fileService.uploadCaseWithFiles(request, additionalFiles);

            log.info("Case uploaded successfully with ID: {}, number: {}",
                    uploadedCase.getId(), uploadedCase.getNumber());

            return ResponseEntity.status(HttpStatus.CREATED).body(uploadedCase);

        } catch (JsonProcessingException e) {
            log.error("Invalid JSON in caseData: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Invalid JSON in 'caseData': " + e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Validation error during case upload: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        } catch (IOException e) {
            log.error("IO error during PDF processing: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing PDF file: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during case upload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error: " + e.getMessage());
        }
    }

    @Operation(
            summary = "Add additional files to an existing case",
            description = "Attach one or more additional files to a case identified by its case number"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Files added successfully",
                    content = @Content(schema = @Schema(implementation = Case.class))),
            @ApiResponse(responseCode = "400", description = "Invalid case number or file validation failed",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error during file processing",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PostMapping(value = "/{caseNumber}/add-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> addAdditionalFilesToCase(
            @Parameter(description = "Case number to which files will be added", required = true)
            @PathVariable("caseNumber") String caseNumber,
            @Parameter(description = "List of additional files to upload", required = true)
            @RequestPart("additionalFiles") List<MultipartFile> additionalFiles) {

        log.info("Adding {} additional files to case: {}",
                additionalFiles != null ? additionalFiles.size() : 0, caseNumber);

        if (additionalFiles != null) {
            log.debug("File details: {}", additionalFiles.stream()
                    .map(f -> f.getOriginalFilename() + " (" + f.getSize() + " bytes)")
                    .toList());
        }

        try {
            Case updatedCase = fileService.addAdditionalFilesToCase(caseNumber, additionalFiles);

            log.info("Successfully added files to case: {}. Total additional files now: {}",
                    caseNumber, updatedCase.getCaseFiles() != null ? updatedCase.getCaseFiles().size() : 0);

            return ResponseEntity.ok(updatedCase);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for adding files to case {}: {}", caseNumber, e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        } catch (IOException e) {
            log.error("IO error while adding files to case {}: {}", caseNumber, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing files: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error while adding files to case: {}", caseNumber, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error: " + e.getMessage());
        }
    }

    @Operation(
            summary = "Delete an additional file from a case by file name",
            description = "Removes a specific additional file (not the main PDF) from a case identified by case number and file name"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File deleted successfully",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "404", description = "Case or file not found",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error during file deletion",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @DeleteMapping("/{caseNumber}/file/{fileId}")
    public ResponseEntity<?> deleteAdditionalFileByName(
            @Parameter(description = "Case number from which the file will be deleted", required = true)
            @PathVariable("caseNumber") String caseNumber,
            @Parameter(description = "Exact id of the file to delete", required = true)
            @PathVariable("fileId") int id) {

        log.info("Deleting file '{}' from case: {}", id, caseNumber);

        try {
            fileService.deleteAdditionalFileById(caseNumber, id);

            log.info("Successfully deleted file '{}' from case: {}", id, caseNumber);

            return ResponseEntity.ok("File '" + id + "' deleted successfully from case " + caseNumber);

        } catch (IllegalArgumentException e) {
            log.warn("File deletion failed - not found: case={}, file={}, error={}",
                    caseNumber, id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during file deletion: case={}, file={}",
                    caseNumber, id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error: " + e.getMessage());
        }
    }

    @Operation(
            summary = "Download document file by case number in specified format",
            description = "Download the document associated with a specific case number in PDF or Word format"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Document file downloaded successfully",
                    content = {
                            @Content(mediaType = "application/pdf"),
                            @Content(mediaType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                    }),
            @ApiResponse(responseCode = "400", description = "Invalid format specified",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "404", description = "Case not found or document file not available",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error during file retrieval",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @GetMapping("/{caseNumber}/download")
    public ResponseEntity<?> downloadDocumentByCaseNumber(
            @Parameter(description = "Case number to download document for", required = true)
            @PathVariable("caseNumber") String caseNumber,

            @Parameter(description = "Document format (pdf or word)", required = false,
                    schema = @Schema(allowableValues = {"pdf", "word"}, defaultValue = "pdf"))
            @RequestParam(value = "format", defaultValue = "pdf") String format) {

        log.info("Document download requested for case: {}, format: {}", caseNumber, format);

        if (!isValidFormat(format)) {
            log.warn("Invalid format requested: {}", format);
            return ResponseEntity.badRequest()
                    .body("Invalid format. Supported formats: pdf, word");
        }

        try {
            Resource documentResource;
            String mediaType;
            String fileExtension;

            switch (format.toLowerCase()) {
                case "pdf":
                    documentResource = fileService.downloadPdfByCaseNumber(caseNumber);
                    mediaType = MediaType.APPLICATION_PDF_VALUE;
                    fileExtension = ".pdf";
                    break;
                case "word":
                    documentResource = fileService.downloadWordByCaseNumber(caseNumber);
                    mediaType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                    fileExtension = ".docx";
                    break;
                default:
                    return ResponseEntity.badRequest()
                            .body("Unsupported format: " + format);
            }

            if (documentResource == null || !documentResource.exists()) {
                log.warn("Document file not found for case: {} in format: {}", caseNumber, format);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Document file not found for case: " + caseNumber + " in " + format + " format");
            }

            String filename = determineFilename(caseNumber, fileExtension);
            long fileSize = documentResource.contentLength();

            log.info("Serving document download: case={}, format={}, filename={}, size={} bytes",
                    caseNumber, format, filename, fileSize);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
            headers.add(HttpHeaders.CONTENT_TYPE, mediaType);

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(fileSize)
                    .body(documentResource);

        } catch (IllegalArgumentException e) {
            log.warn("Download failed - case not found: case={}, format={}, error={}",
                    caseNumber, format, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Error: " + e.getMessage());
        } catch (IOException e) {
            log.error("IO error reading document file for case: {}, format: {}", caseNumber, format, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error reading document file: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during document download for case: {}, format: {}",
                    caseNumber, format, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error: " + e.getMessage());
        }
    }

    private boolean isValidFormat(String format) {
        return format != null && (format.equalsIgnoreCase("pdf") || format.equalsIgnoreCase("word"));
    }

    private String determineFilename(String caseNumber, String extension) {

        return "documentation" + extension;
    }
}