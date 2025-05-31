package org.nab.new_afm_back.repository;

import org.nab.new_afm_back.model.Case;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CaseRepository extends JpaRepository<Case, Long> {
    Optional<Case> getCaseByNumber(String number);
}
