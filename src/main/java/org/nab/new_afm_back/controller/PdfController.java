package org.nab.new_afm_back.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    @Operation(summary = "Upload case with PDF and additional files",
            description = "Upload a new case with a main PDF document and optional additional files")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Case uploaded successfully", content = @Content(schema = @Schema(implementation = Case.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data or file validation failed", content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error during file processing", content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadCaseWithPdf(
            @Parameter(description = "Case metadata in JSON format", required = true)
            @RequestPart("caseData") @Valid UploadCaseRequest request,

            @Parameter(description = "Upload one or more additional files", content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE))
            @RequestPart(value = "additionalFiles") List<MultipartFile> additionalFiles) {

        try {
            Case uploadedCase = pdfService.uploadCaseWithPdf(request, additionalFiles);
            return ResponseEntity.status(HttpStatus.CREATED).body(uploadedCase);
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
}