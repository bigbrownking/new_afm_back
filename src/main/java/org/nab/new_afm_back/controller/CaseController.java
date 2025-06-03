package org.nab.new_afm_back.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.nab.new_afm_back.model.Case;
import org.nab.new_afm_back.model.CaseFile;
import org.nab.new_afm_back.service.impl.CaseFileService;
import org.nab.new_afm_back.service.impl.CaseService;
import org.nab.new_afm_back.service.impl.PdfService;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/case")
@RequiredArgsConstructor
@Tag(name = "Case Management", description = "APIs for managing legal cases")
public class CaseController {
    private final CaseService caseService;
    private final PdfService pdfService;
    private final CaseFileService caseFileService;

    @Operation(
            summary = "Get case by number",
            description = "Retrieve a specific case by its unique case number"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Case found successfully",
                    content = @Content(schema = @Schema(implementation = Case.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Case not found",
                    content = @Content
            )
    })
    @GetMapping("/{number}")
    public ResponseEntity<Case> getCaseByPathNumber(@PathVariable String number) {
        Case _case = caseService.getCaseByNumber(number);
        return ResponseEntity.ok(_case);
    }

    @Operation(
            summary = "Get recent cases by upload",
            description = "Retrieve a list of recently uploaded cases"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Recent cases retrieved successfully",
                    content = @Content(schema = @Schema(implementation = Case.class))
            )
    })
    @GetMapping("/recent")
    public ResponseEntity<Page<Case>> getRecentCases(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(caseService.getRecentCases(page, size));
    }

    @Operation(
            summary = "Get recent cases by request",
            description = "Retrieve a list of recently searched cases"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Recent cases retrieved successfully",
                    content = @Content(schema = @Schema(implementation = Case.class))
            )
    })
    @GetMapping("/recentReq")
    public ResponseEntity<Page<Case>> getRecentCasesReq(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(caseService.getRecentCasesReq(page, size));
    }
    @GetMapping("/{number}/count")
    public ResponseEntity<Integer> getCaseCount(@PathVariable String number) {
        return ResponseEntity.ok(caseService.getCaseCount(number));
    }


    @Operation(
            summary = "Download a specific file associated with a case",
            description = "Download a file uploaded as part of a case, using case ID and file ID"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File downloaded successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE)),
            @ApiResponse(responseCode = "404", description = "File or case not found",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @GetMapping("/{number}/file/{fileId}/download")
    public ResponseEntity<?> downloadCaseFile(
            @Parameter(description = "Case number", required = true) @PathVariable String number,
            @Parameter(description = "File ID", required = true) @PathVariable Long fileId) {

        log.info("Downloading file ID " + fileId +  "from case ID " +  number);

        try {
            Resource fileResource = pdfService.downloadCaseFile(number, fileId);

            if (fileResource == null || !fileResource.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found.");
            }

            String contentDisposition = "attachment; filename=\"" + fileResource.getFilename() + "\"";

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                    .body(fileResource);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Error: " + e.getMessage());
        } catch (IOException e) {
            log.error("Error while reading file ID {}: {}", fileId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error reading file: " + e.getMessage());
        }
    }


    @GetMapping("/{number}/caseFiles")
    private ResponseEntity<Page<CaseFile>> getCaseFiles(@PathVariable String number,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "10") int size){
        return ResponseEntity.ok(caseFileService.getAllCaseFilesOfCase(number, page, size));
    }
}
