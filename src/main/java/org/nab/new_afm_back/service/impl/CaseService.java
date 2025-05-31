package org.nab.new_afm_back.service.impl;

import lombok.RequiredArgsConstructor;
import org.nab.new_afm_back.model.Case;
import org.nab.new_afm_back.repository.CaseRepository;
import org.nab.new_afm_back.util.CaseAccessTracker;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.nab.new_afm_back.service.ICaseService;
import org.springframework.data.domain.Pageable;


import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CaseService implements ICaseService {
    private final CaseRepository caseRepository;
    private final CaseAccessTracker accessTracker;

    public Case getCaseByNumber(String number){
        Case found = caseRepository.getCaseByNumber(number)
                .orElseThrow(() -> new RuntimeException("Case not found with number: " + number));
        accessTracker.addCaseNumber(number);
        return found;
    }
    public List<Case> getRecentCases() {
        LocalDate twoDaysAgo = LocalDate.now().minusDays(2);
        return caseRepository.findRecentCases(twoDaysAgo);
    }

    public List<Case> getRecentCasesReq() {
        Set<String> numbers = accessTracker.getAccessedCaseNumbers();
        List<Case> unorderedCases = caseRepository.findAllByNumberIn(numbers);

        Map<String, Case> caseMap = unorderedCases.stream()
                .collect(Collectors.toMap(Case::getNumber, c -> c));

        return numbers.stream()
                .map(caseMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    public int getCaseCount(String number) {
        Case found = caseRepository.getCaseByNumber(number)
                .orElseThrow(() -> new RuntimeException("Case not found with number: " + number));

        return found.getAdds().size();
    }

}
