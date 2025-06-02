package org.nab.new_afm_back.repository;

import org.nab.new_afm_back.model.CaseFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CaseFileRepository extends JpaRepository<CaseFile, Long> {

    List<CaseFile> findByCaseEntityId(Long caseId);

    List<CaseFile> findByCaseEntityNumber(String caseNumber);

    Optional<CaseFile> findByFileNameAndCaseEntityNumber(String fileName, String caseNumber);

    @Query("SELECT cf FROM CaseFile cf WHERE cf.caseEntity.number = :caseNumber ORDER BY cf.uploadedAt DESC")
    List<CaseFile> findByCaseNumberOrderByUploadedAtDesc(@Param("caseNumber") String caseNumber);

    void deleteByCaseEntityNumber(String caseNumber);

    void deleteByFileNameAndCaseEntityNumber(String fileName, String caseNumber);
}