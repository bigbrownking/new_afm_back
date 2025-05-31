package org.nab.new_afm_back.service;

import org.nab.new_afm_back.model.Document;
import java.util.List;

public interface IDocumentService {
    List<Document> getDocumentsByCaseId(Long caseId);
    Document saveDocument(Document document);
}
