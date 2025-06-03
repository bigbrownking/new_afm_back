package org.nab.new_afm_back.repository;

import org.nab.new_afm_back.model.Case;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface CaseRepository extends JpaRepository<Case, Long> {
    Optional<Case> getCaseByNumber(String number);
    @Query("SELECT c FROM Case c WHERE c.uploadDate >= :startDate ORDER BY c.uploadDate DESC")
    Page<Case> findRecentCases(@Param("startDate") LocalDate startDate, Pageable pageable);

    List<Case> findAllByNumberIn(Set<String> numbers);

}
