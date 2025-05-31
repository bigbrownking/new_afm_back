package org.nab.new_afm_back.controller;

import org.nab.new_afm_back.model.Case;
import org.nab.new_afm_back.service.impl.CaseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/case")
public class CaseController {
    private final CaseService caseService;

    public CaseController(CaseService caseService) {
        this.caseService = caseService;
    }

    // Получить последние N кейсов (например, 10)
    @GetMapping("/recent")
    public List<Case> getRecentCases(@RequestParam(defaultValue = "10") int count) {
        return caseService.getRecentCases(count);
    }
    @GetMapping("/count")
    public long getCaseCount() {
        return caseService.getCaseCount();
    }

}
