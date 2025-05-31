package org.nab.new_afm_back.service;

import org.nab.new_afm_back.model.Case;

import java.util.List;

public interface ICaseService {
    Case getCaseByNumber(String number);
    List<Case> getRecentCases();
    List<Case> getRecentCasesReq();
    int getCaseCount(String number);
}
