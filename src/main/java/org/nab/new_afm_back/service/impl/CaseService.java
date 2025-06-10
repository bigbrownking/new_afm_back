package org.nab.new_afm_back.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
        LocalDate twoDaysAgo = LocalDate.now().minusDays(5);
        Pageable pageable = PageRequest.of(page, size);
        return caseRepository.findRecentCases(twoDaysAgo, pageable);
    }

    public Page<Case> getRecentCasesReq(int page, int size) {
        log.info("Fetching recent cases - Page: {}, Size: {}", page, size);

        List<String> numbers = accessTracker.getAccessedCaseNumbers();
        log.debug("Retrieved {} accessed case numbers: {}", numbers.size(), numbers);

        List<Case> unorderedCases = caseRepository.findAllByNumberIn(numbers);
        log.info("Found {} total cases matching the accessed numbers", unorderedCases.size());

        Map<String, Case> caseMap = unorderedCases.stream()
                .collect(Collectors.toMap(Case::getNumber, c -> c));
        log.trace("Created case map with {} entries", caseMap.size());

        List<Case> orderedCases = numbers.stream()
                .map(caseMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        log.debug("Ordered list contains {} valid cases (filtered from {} total)",
                orderedCases.size(), unorderedCases.size());

        int start = page * size;
        int end = Math.min(start + size, orderedCases.size());

        if (start > orderedCases.size()) {
            log.warn("Requested page {} exceeds available data (total cases: {})",
                    page, orderedCases.size());
            return new PageImpl<>(List.of(), PageRequest.of(page, size), orderedCases.size());
        }

        List<Case> pageContent = orderedCases.subList(start, end);
        log.info("Returning page {} with {} cases (range: {}-{})",
                page, pageContent.size(), start, end);

        return new PageImpl<>(pageContent, PageRequest.of(page, size), orderedCases.size());
    }
    public int getCaseCount(String number) {
        Case found = caseRepository.getCaseByNumber(number)
                .orElseThrow(() -> new RuntimeException("Case not found with number: " + number));

        return found.getCaseFiles().size();
    }

}
