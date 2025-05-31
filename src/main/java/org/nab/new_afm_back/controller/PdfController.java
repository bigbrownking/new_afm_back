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

        try {
            UploadCaseRequest request = new ObjectMapper().readValue(caseDataJson, UploadCaseRequest.class);
            Case uploadedCase = pdfService.uploadCaseWithPdf(request, additionalFiles);
            return ResponseEntity.status(HttpStatus.CREATED).body(uploadedCase);
        } catch (JsonProcessingException e) {
            return ResponseEntity.badRequest().body("Invalid JSON in 'caseData': " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing PDF file: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during file upload", e);
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
        try {
            Case updatedCase = pdfService.addAdditionalFilesToCase(caseNumber, additionalFiles);
            return ResponseEntity.ok(updatedCase);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing files: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during file upload", e);
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

        try {
            pdfService.deleteAdditionalFileByName(caseNumber, fileName);
            return ResponseEntity.ok("File '" + fileName + "' deleted successfully from case " + caseNumber);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during file deletion", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error: " + e.getMessage());
        }
    }


}
