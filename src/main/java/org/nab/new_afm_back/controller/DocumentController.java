package org.nab.new_afm_back.controller;

import org.nab.new_afm_back.model.Document;
import org.nab.new_afm_back.service.IDocumentService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/documentation")
public class DocumentController {
    private final IDocumentService documentService;

    public DocumentController(IDocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    public List<Document> getDocumentsByCase(@RequestParam Long caseId) {
        return documentService.getDocumentsByCaseId(caseId);
    }
}
