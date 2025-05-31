package org.nab.new_afm_back.service.impl;

import lombok.RequiredArgsConstructor;
import org.nab.new_afm_back.model.Case;
import org.nab.new_afm_back.repository.CaseRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.nab.new_afm_back.service.ICaseService;
import org.springframework.data.domain.Pageable;



import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CaseService implements ICaseService {
    private final CaseRepository caseRepository;

    public Case getCaseByNumber(String number){
        Optional<Case> caseOpt = caseRepository.getCaseByNumber(number);
        return caseOpt.orElseThrow(() ->
                new RuntimeException("Case not found with number: " + number));
    }

    public List<Case> getRecentCases(int count) {
        return caseRepository.findRecentCases(PageRequest.of(0, count));
    }
    public long getCaseCount() {
        return caseRepository.count();
    }

}
