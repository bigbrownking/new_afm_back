package org.nab.new_afm_back.service;

import org.nab.new_afm_back.dto.response.Document;

public interface IDocumentService {
    Document getDocument(String number);
    String getTextOfPredicate(String number, String predicate);

}
