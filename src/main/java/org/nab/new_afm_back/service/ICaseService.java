package org.nab.new_afm_back.service;

import org.nab.new_afm_back.model.Case;

public interface ICaseService {
    Case getCaseByNumber(String number);
}
