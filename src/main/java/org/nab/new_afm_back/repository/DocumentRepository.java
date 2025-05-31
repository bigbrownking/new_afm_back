package org.nab.new_afm_back.repository;

import org.nab.new_afm_back.model.Document;
import org.nab.new_afm_back.model.Case;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByCaseObj(Case caseObj);
}
