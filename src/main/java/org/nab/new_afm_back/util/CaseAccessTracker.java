package org.nab.new_afm_back.util;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.util.LinkedHashSet;
import java.util.Set;

@Component
@SessionScope
public class CaseAccessTracker {
    private final Set<String> accessedCaseNumbers = new LinkedHashSet<>();

    public void addCaseNumber(String number) {
        accessedCaseNumbers.add(number);
    }

    public Set<String> getAccessedCaseNumbers() {
        return accessedCaseNumbers;
    }

    public void clear() {
        accessedCaseNumbers.clear();
    }
}

