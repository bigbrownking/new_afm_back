package org.nab.new_afm_back.repository;

import org.nab.new_afm_back.model.CaseFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    Page<CaseFile> findByCaseEntityNumber(String caseNumber, Pageable pageable);

    Optional<CaseFile> findByFileNameAndCaseEntityNumber(String fileName, String caseNumber);

    @Query("SELECT cf FROM CaseFile cf WHERE cf.caseEntity.number = :caseNumber ORDER BY cf.uploadedAt DESC")
    List<CaseFile> findByCaseNumberOrderByUploadedAtDesc(@Param("caseNumber") String caseNumber);

    @Query("SELECT cf FROM CaseFile cf WHERE cf.caseEntity.number = :caseNumber")
    Page<CaseFile> findByCaseNumberWithPagination(@Param("caseNumber") String caseNumber, Pageable pageable);

    void deleteByCaseEntityNumber(String caseNumber);

    void deleteByFileNameAndCaseEntityNumber(String fileName, String caseNumber);
    void deleteByIdAndCaseEntityNumber(Long id, String caseEntity_number);
    boolean existsByFileName(String caseNumber);

}