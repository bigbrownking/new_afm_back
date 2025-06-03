package org.nab.new_afm_back.service.impl;

import lombok.RequiredArgsConstructor;
import org.nab.new_afm_back.model.Case;
import org.nab.new_afm_back.repository.CaseRepository;
import org.nab.new_afm_back.util.CaseAccessTracker;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
    public Page<Case> getRecentCases(int page, int size) {
        LocalDate twoDaysAgo = LocalDate.now().minusDays(2);
        Pageable pageable = PageRequest.of(page, size);
        return caseRepository.findRecentCases(twoDaysAgo, pageable);
    }

    public Page<Case> getRecentCasesReq(int page, int size) {
        Set<String> numbers = accessTracker.getAccessedCaseNumbers();
        List<Case> unorderedCases = caseRepository.findAllByNumberIn(numbers);

        Map<String, Case> caseMap = unorderedCases.stream()
                .collect(Collectors.toMap(Case::getNumber, c -> c));

        List<Case> orderedCases = numbers.stream()
                .map(caseMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        int start = page * size;
        int end = Math.min(start + size, orderedCases.size());
        List<Case> pageContent = start > end ? List.of() : orderedCases.subList(start, end);

        return new PageImpl<>(pageContent, PageRequest.of(page, size), orderedCases.size());
    }
    public int getCaseCount(String number) {
        Case found = caseRepository.getCaseByNumber(number)
                .orElseThrow(() -> new RuntimeException("Case not found with number: " + number));

        return found.getAdds().size();
    }

}
