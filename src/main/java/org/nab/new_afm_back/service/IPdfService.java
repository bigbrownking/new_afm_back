package org.nab.new_afm_back.service;

import org.nab.new_afm_back.dto.request.UploadCaseRequest;
import org.nab.new_afm_back.model.Case;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface IPdfService {
    Case uploadCaseWithPdf(UploadCaseRequest request, List<MultipartFile> additionalFiles) throws IOException;
    void deleteAdditionalFileByName(String caseNumber, String fileName);
    Case addAdditionalFilesToCase(String caseNumber, List<MultipartFile> additionalFiles) throws IOException;
    byte[] extractPdfContent(MultipartFile pdfFile) throws IOException;
    void savePdfToStorage(MultipartFile pdfFile, String fileName) throws IOException;
    boolean validateUploadedFile(MultipartFile pdfFile);
}
