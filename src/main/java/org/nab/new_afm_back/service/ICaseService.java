package org.nab.new_afm_back.service;

import org.nab.new_afm_back.model.Case;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ICaseService {
    Case getCaseByNumber(String number);
    Page<Case> getRecentCases(int page, int size);
    Page<Case> getRecentCasesReq(int page, int size);
    int getCaseCount(String number);
}
