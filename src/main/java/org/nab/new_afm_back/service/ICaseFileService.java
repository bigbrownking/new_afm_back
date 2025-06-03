package org.nab.new_afm_back.service;

import org.nab.new_afm_back.model.CaseFile;
import org.springframework.data.domain.Page;

public interface ICaseFileService {
    Page<CaseFile> getAllCaseFilesOfCase(String number, int page, int size);
}
