package org.nab.new_afm_back.service;

import org.nab.new_afm_back.dto.request.UploadCaseRequest;
import org.nab.new_afm_back.model.Case;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface IFIleService {
    Case uploadCaseWithFiles(UploadCaseRequest request, List<MultipartFile> additionalFiles) throws IOException;
    void deleteAdditionalFileById(String caseNumber, int id);
    Case addAdditionalFilesToCase(String caseNumber, List<MultipartFile> additionalFiles) throws IOException;
    void saveFileToStorage(MultipartFile pdfFile, String fileName) throws IOException;
    boolean validateUploadedFile(MultipartFile pdfFile);
    Resource downloadWordByCaseNumber(String caseNumber) throws IOException;


}
