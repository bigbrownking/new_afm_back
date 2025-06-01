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
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nab.new_afm_back.dto.request.UploadCaseRequest;
import org.nab.new_afm_back.model.Case;
import org.nab.new_afm_back.service.impl.PdfService;
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
public class PdfController {

    private final PdfService pdfService;

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
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadCaseWithPdf(
            @RequestPart("caseData") String caseDataJson,
            @RequestPart(value = "additionalFiles", required = false) List<MultipartFile> additionalFiles) {

        log.info("Starting case upload request. Additional files count: {}",
                additionalFiles != null ? additionalFiles.size() : 0);
        log.debug("Case data JSON: {}", caseDataJson);

        try {
            UploadCaseRequest request = new ObjectMapper().readValue(caseDataJson, UploadCaseRequest.class);
            log.info("Parsed upload request for case number: {}, author: {}",
                    request.getNumber(), request.getAuthor());

            Case uploadedCase = pdfService.uploadCaseWithPdf(request, additionalFiles);

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
            Case updatedCase = pdfService.addAdditionalFilesToCase(caseNumber, additionalFiles);

            log.info("Successfully added files to case: {}. Total additional files now: {}",
                    caseNumber, updatedCase.getAdds() != null ? updatedCase.getAdds().size() : 0);

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
    @DeleteMapping("/{caseNumber}/file/{fileName}")
    public ResponseEntity<?> deleteAdditionalFileByName(
            @Parameter(description = "Case number from which the file will be deleted", required = true)
            @PathVariable("caseNumber") String caseNumber,
            @Parameter(description = "Exact name of the file to delete", required = true)
            @PathVariable("fileName") String fileName) {

        log.info("Deleting file '{}' from case: {}", fileName, caseNumber);

        try {
            pdfService.deleteAdditionalFileByName(caseNumber, fileName);

            log.info("Successfully deleted file '{}' from case: {}", fileName, caseNumber);

            return ResponseEntity.ok("File '" + fileName + "' deleted successfully from case " + caseNumber);

        } catch (IllegalArgumentException e) {
            log.warn("File deletion failed - not found: case={}, file={}, error={}",
                    caseNumber, fileName, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during file deletion: case={}, file={}",
                    caseNumber, fileName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error: " + e.getMessage());
        }
    }

    @Operation(
            summary = "Download PDF file by case number",
            description = "Download the main PDF document associated with a specific case number"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "PDF file downloaded successfully",
                    content = @Content(mediaType = "application/pdf")),
            @ApiResponse(responseCode = "404", description = "Case not found or PDF file not available",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error during file retrieval",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @GetMapping("/{caseNumber}/download")
    public ResponseEntity<?> downloadPdfByCaseNumber(
            @Parameter(description = "Case number to download PDF for", required = true)
            @PathVariable("caseNumber") String caseNumber) {

        log.info("PDF download requested for case: {}", caseNumber);

        try {
            Resource pdfResource = pdfService.downloadPdfByCaseNumber(caseNumber);

            if (pdfResource == null || !pdfResource.exists()) {
                log.warn("PDF file not found for case: {}", caseNumber);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("PDF file not found for case: " + caseNumber);
            }

            String filename = determineFilename(caseNumber);
            long fileSize = pdfResource.contentLength();

            log.info("Serving PDF download: case={}, filename={}, size={} bytes",
                    caseNumber, filename, fileSize);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE);

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(fileSize)
                    .body(pdfResource);

        } catch (IllegalArgumentException e) {
            log.warn("Download failed - case not found: case={}, error={}", caseNumber, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Error: " + e.getMessage());
        } catch (IOException e) {
            log.error("IO error reading PDF file for case: {}", caseNumber, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error reading PDF file: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during PDF download for case: {}", caseNumber, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error: " + e.getMessage());
        }
    }

    private String determineFilename(String caseNumber) {
        String numericPart = caseNumber.replaceAll("[^0-9]", "");
        String filename;

        if (!numericPart.isEmpty()) {
            int caseNum = Integer.parseInt(numericPart);
            if (caseNum == 1) {
                filename = "documentation.pdf";
            } else if (caseNum == 2) {
                filename = "documentation1.pdf";
            } else {
                filename = "case_" + caseNumber + "_document.pdf";
            }
        } else {
            filename = "case_" + caseNumber + "_document.pdf";
        }

        log.debug("Determined filename for case {}: {}", caseNumber, filename);
        return filename;
    }
}