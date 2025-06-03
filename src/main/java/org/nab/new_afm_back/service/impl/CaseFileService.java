package org.nab.new_afm_back.service.impl;

import lombok.RequiredArgsConstructor;
import org.nab.new_afm_back.model.CaseFile;
import org.nab.new_afm_back.repository.CaseFileRepository;
import org.nab.new_afm_back.service.ICaseFileService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CaseFileService implements ICaseFileService {
    private final CaseFileRepository caseFileRepository;
    @Override
    public Page<CaseFile> getAllCaseFilesOfCase(String number, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "uploadedAt"));
        return caseFileRepository.findByCaseEntityNumber(number, pageable);
    }
}
