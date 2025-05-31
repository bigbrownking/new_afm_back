package org.nab.new_afm_back.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.nab.new_afm_back.model.Case;
import org.nab.new_afm_back.service.impl.CaseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/case")
@RequiredArgsConstructor
@Tag(name = "Case Management", description = "APIs for managing legal cases")
public class CaseController {
    private final CaseService caseService;

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
    public ResponseEntity<List<Case>> getRecentCases() {
        return ResponseEntity.ok(caseService.getRecentCases());
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
    public ResponseEntity<List<Case>> getRecentCasesReq() {
        return ResponseEntity.ok(caseService.getRecentCasesReq());
    }
}
