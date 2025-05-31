package org.nab.new_afm_back.service.impl;

import org.nab.new_afm_back.model.Document;
import org.nab.new_afm_back.model.Case;
import org.nab.new_afm_back.repository.DocumentRepository;
import org.nab.new_afm_back.repository.CaseRepository;
import org.nab.new_afm_back.service.IDocumentService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class DocumentService implements IDocumentService {

    private final DocumentRepository documentRepository;
    private final CaseRepository caseRepository;

    public DocumentService(DocumentRepository documentRepository, CaseRepository caseRepository) {
        this.documentRepository = documentRepository;
        this.caseRepository = caseRepository;
    }

    @Override
    public List<Document> getDocumentsByCaseId(Long caseId) {
        Case caseObj = caseRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found"));
        return documentRepository.findByCaseObj(caseObj);
    }

    @Override
    public Document saveDocument(Document document) {
        return documentRepository.save(document);
    }
}
