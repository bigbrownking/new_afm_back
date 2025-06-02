package org.nab.new_afm_back.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nab.new_afm_back.dto.request.UploadCaseRequest;
import org.nab.new_afm_back.model.Case;
import org.nab.new_afm_back.model.CaseFile;
import org.nab.new_afm_back.repository.CaseRepository;
import org.nab.new_afm_back.repository.CaseFileRepository;
import org.nab.new_afm_back.service.IPdfService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfService implements IPdfService {

    private final CaseRepository caseRepository;
    private final CaseFileRepository caseFileRepository;

    @Value("${file.upload.directory:./uploads}")
    private String uploadDirectory;

    private static final List<String> ALLOWED_EXTENSIONS = List.of("pdf", "doc", "docx", "txt");
    private static final long MAX_FILE_SIZE = 100 * 1024 * 1024;

    public Case uploadCaseWithPdf(UploadCaseRequest request, List<MultipartFile> additionalFiles) throws IOException {
        log.info("Starting case upload process for case number: {}", request.getNumber());

        if (caseRepository.getCaseByNumber(request.getNumber()).isPresent()) {
            log.warn("Case upload failed - case number already exists: {}", request.getNumber());
            throw new IllegalArgumentException("Case number already exists: " + request.getNumber());
        }

        log.debug("Resolving article for case: {}", request.getNumber());
        List<Integer> articles = request.getArticles();

        Case newCase = Case.builder()
                .number(request.getNumber())
                .author(request.getAuthor())
                .investigator(request.getInvestigator())
                .policeman(request.getPoliceman())
                .object(request.getObject())
                .articles(articles)
                .uploadDate(LocalDate.now())
                .build();

        log.debug("Saving new case to database: {}", request.getNumber());
        Case savedCase = caseRepository.save(newCase);

        log.debug("Processing additional files for case: {}", request.getNumber());
        List<String> additionalFileNames = processAdditionalFilesWithTracking(savedCase, additionalFiles, request.getAuthor());

        savedCase.setAdds(additionalFileNames);
        savedCase = caseRepository.save(savedCase);

        log.info("Case upload completed successfully: ID={}, Number={}, Files={}",
                savedCase.getId(), savedCase.getNumber(), additionalFileNames.size());

        return savedCase;
    }

    private List<String> processAdditionalFilesWithTracking(Case caseEntity, List<MultipartFile> additionalFiles, String uploadedBy) throws IOException {
        List<String> fileNames = new ArrayList<>();
        if (additionalFiles == null || additionalFiles.isEmpty()) {
            log.debug("No additional files to process for case: {}", caseEntity.getNumber());
            return fileNames;
        }

        log.info("Processing {} additional files for case: {}", additionalFiles.size(), caseEntity.getNumber());
        LocalDateTime uploadTime = LocalDateTime.now();

        for (int i = 0; i < additionalFiles.size(); i++) {
            MultipartFile file = additionalFiles.get(i);
            if (!file.isEmpty()) {
                try {
                    String fileName = file.getOriginalFilename();
                    log.debug("Processing file {}/{}: name={}, size={} bytes",
                            i + 1, additionalFiles.size(), fileName, file.getSize());

                    validateAndSaveFile(file, fileName);
                    fileNames.add(fileName);

                    CaseFile caseFile = CaseFile.builder()
                            .fileName(fileName)
                            .originalFileName(fileName)
                            .fileSize(file.getSize())
                            .fileType(getFileExtension(fileName))
                            .uploadedAt(uploadTime)
                            .uploadedBy(uploadedBy)
                            .caseEntity(caseEntity)
                            .build();

                    caseFileRepository.save(caseFile);

                    log.info("Additional file saved successfully with timestamp: {} (case: {}, uploaded at: {})",
                            fileName, caseEntity.getNumber(), uploadTime);
                } catch (Exception e) {
                    log.error("Error saving additional file {}/{} for case {}: {}",
                            i + 1, additionalFiles.size(), caseEntity.getNumber(), e.getMessage(), e);
                }
            } else {
                log.warn("Skipping empty file at index {} for case: {}", i, caseEntity.getNumber());
            }
        }

        log.info("Completed processing additional files for case {}: {}/{} files saved",
                caseEntity.getNumber(), fileNames.size(), additionalFiles.size());

        return fileNames;
    }

    private List<String> processAdditionalFiles(UploadCaseRequest request, List<MultipartFile> additionalFiles) throws IOException {
        List<String> fileNames = new ArrayList<>();
        if (additionalFiles == null || additionalFiles.isEmpty()) {
            log.debug("No additional files to process for case: {}", request.getNumber());
            return fileNames;
        }

        log.info("Processing {} additional files for case: {}", additionalFiles.size(), request.getNumber());

        for (int i = 0; i < additionalFiles.size(); i++) {
            MultipartFile file = additionalFiles.get(i);
            if (!file.isEmpty()) {
                try {
                    String fileName = file.getOriginalFilename();
                    log.debug("Processing file {}/{}: name={}, size={} bytes",
                            i + 1, additionalFiles.size(), fileName, file.getSize());

                    validateAndSaveFile(file, fileName);
                    fileNames.add(fileName);

                    log.info("Additional file saved successfully: {} (case: {})", fileName, request.getNumber());
                } catch (Exception e) {
                    log.error("Error saving additional file {}/{} for case {}: {}",
                            i + 1, additionalFiles.size(), request.getNumber(), e.getMessage(), e);
                }
            } else {
                log.warn("Skipping empty file at index {} for case: {}", i, request.getNumber());
            }
        }

        log.info("Completed processing additional files for case {}: {}/{} files saved",
                request.getNumber(), fileNames.size(), additionalFiles.size());

        return fileNames;
    }

    private void validateAndSaveFile(MultipartFile file, String fileName) throws IOException {
        log.debug("Validating file: {}", fileName);

        if (!validateUploadedFile(file)) {
            log.warn("File validation failed: {}", fileName);
            throw new IOException("Invalid file: " + file.getOriginalFilename());
        }

        log.debug("File validation passed, saving: {}", fileName);
        savePdfToStorage(file, fileName);
        log.debug("File saved successfully: {}", fileName);
    }

    @Override
    public byte[] extractPdfContent(MultipartFile pdfFile) throws IOException {
        log.debug("Extracting content from PDF file: {}", pdfFile.getOriginalFilename());
        byte[] content = pdfFile.getBytes();
        log.info("Extracted {} bytes from PDF: {}", content.length, pdfFile.getOriginalFilename());
        return content;
    }

    @Override
    public void savePdfToStorage(MultipartFile pdfFile, String fileName) throws IOException {
        log.debug("Saving file to storage: {} (size: {} bytes)", fileName, pdfFile.getSize());

        Path uploadPath = Paths.get(uploadDirectory);
        if (!Files.exists(uploadPath)) {
            log.info("Creating upload directory: {}", uploadPath);
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(fileName);
        log.debug("File will be saved to: {}", filePath);

        Files.copy(pdfFile.getInputStream(), filePath);
        log.info("File saved successfully: {} -> {}", fileName, filePath);
    }

    @Override
    public boolean validateUploadedFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.warn("File validation failed: file is null or empty");
            return false;
        }

        long fileSize = file.getSize();
        if (fileSize > MAX_FILE_SIZE) {
            log.warn("File validation failed: size {} bytes exceeds limit of {} bytes",
                    fileSize, MAX_FILE_SIZE);
            return false;
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            log.warn("File validation failed: filename is null");
            return false;
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            log.warn("File validation failed: extension '{}' not in allowed list: {}",
                    extension, ALLOWED_EXTENSIONS);
            return false;
        }

        log.debug("File validation passed: name={}, size={} bytes, extension={}",
                originalFilename, fileSize, extension);
        return true;
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
    public Case addAdditionalFilesToCase(String caseNumber, List<MultipartFile> additionalFiles) throws IOException {
        return addAdditionalFilesToCase(caseNumber, additionalFiles, null);
    }

    public Case addAdditionalFilesToCase(String caseNumber, List<MultipartFile> additionalFiles, String uploadedBy) throws IOException {
        log.info("Adding additional files to existing case: {}", caseNumber);

        Optional<Case> optionalCase = caseRepository.getCaseByNumber(caseNumber);
        if (optionalCase.isEmpty()) {
            log.warn("Case not found when trying to add files: {}", caseNumber);
            throw new IllegalArgumentException("Case not found: " + caseNumber);
        }

        Case existingCase = optionalCase.get();
        List<String> existingFileNames = existingCase.getAdds() != null ?
                new ArrayList<>(existingCase.getAdds()) : new ArrayList<>();

        log.info("Case found: {} (current additional files: {})",
                caseNumber, existingFileNames.size());

        if (additionalFiles != null && !additionalFiles.isEmpty()) {
            log.info("Processing {} new additional files for case: {}", additionalFiles.size(), caseNumber);
            LocalDateTime uploadTime = LocalDateTime.now();

            int successCount = 0;
            for (int i = 0; i < additionalFiles.size(); i++) {
                MultipartFile file = additionalFiles.get(i);
                if (!file.isEmpty()) {
                    String fileName = file.getOriginalFilename();
                    try {
                        log.debug("Processing additional file {}/{}: {}", i + 1, additionalFiles.size(), fileName);
                        validateAndSaveFile(file, fileName);
                        existingFileNames.add(fileName);

                        // Create CaseFile record with timestamp
                        CaseFile caseFile = CaseFile.builder()
                                .fileName(fileName)
                                .originalFileName(fileName)
                                .fileSize(file.getSize())
                                .fileType(getFileExtension(fileName))
                                .uploadedAt(uploadTime)
                                .uploadedBy(uploadedBy)
                                .caseEntity(existingCase)
                                .build();

                        caseFileRepository.save(caseFile);

                        successCount++;
                        log.info("Additional file added successfully with timestamp: {} (case: {}, uploaded at: {})",
                                fileName, caseNumber, uploadTime);
                    } catch (Exception e) {
                        log.error("Error saving additional file {} for case {}: {}",
                                fileName, caseNumber, e.getMessage(), e);
                    }
                } else {
                    log.warn("Skipping empty file at index {} for case: {}", i, caseNumber);
                }
            }

            log.info("Added {}/{} additional files to case: {}",
                    successCount, additionalFiles.size(), caseNumber);
        }

        existingCase.setAdds(existingFileNames);
        Case updatedCase = caseRepository.save(existingCase);

        log.info("Case updated successfully: {} (total additional files: {})",
                caseNumber, existingFileNames.size());

        return updatedCase;
    }

    public void deleteAdditionalFileByName(String caseNumber, String fileName) {
        log.info("Deleting additional file: {} from case: {}", fileName, caseNumber);

        Optional<Case> optionalCase = caseRepository.getCaseByNumber(caseNumber);
        if (optionalCase.isEmpty()) {
            log.warn("Case not found when trying to delete file: case={}, file={}", caseNumber, fileName);
            throw new IllegalArgumentException("Case not found: " + caseNumber);
        }

        Case existingCase = optionalCase.get();
        List<String> additionalFiles = existingCase.getAdds();

        if (additionalFiles == null || !additionalFiles.contains(fileName)) {
            log.warn("File not found in case: case={}, file={}, available files={}",
                    caseNumber, fileName, additionalFiles);
            throw new IllegalArgumentException("File '" + fileName + "' not found in case " + caseNumber);
        }

        // Delete physical file
        Path filePath = Paths.get(uploadDirectory).resolve(fileName);
        log.debug("Attempting to delete physical file: {}", filePath);

        try {
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.info("Physical file deleted successfully: {}", filePath);
            } else {
                log.warn("Physical file was not found on disk: {}", filePath);
            }
        } catch (IOException e) {
            log.error("Failed to delete physical file: {}", filePath, e);
            throw new RuntimeException("Failed to delete file: " + fileName, e);
        }

        // Remove from database (both from Case and CaseFile)
        additionalFiles.remove(fileName);
        existingCase.setAdds(additionalFiles);
        caseRepository.save(existingCase);

        // Remove CaseFile record
        caseFileRepository.deleteByFileNameAndCaseEntityNumber(fileName, caseNumber);

        log.info("File removed from case successfully: case={}, file={}, remaining files={}",
                caseNumber, fileName, additionalFiles.size());
    }

    public Resource downloadPdfByCaseNumber(String caseNumber) throws IOException {
        log.info("Processing PDF download request for case: {}", caseNumber);

        Case caseEntity = caseRepository.getCaseByNumber(caseNumber)
                .orElseThrow(() -> {
                    log.warn("Case not found for download: {}", caseNumber);
                    return new IllegalArgumentException("Case not found with number: " + caseNumber);
                });

        log.info("Case found for download: ID={}, Number={}", caseEntity.getId(), caseEntity.getNumber());

        String filename = determineFilename(caseNumber);
        log.debug("Determined filename for download: {} -> {}", caseNumber, filename);

        Path filePath = Paths.get(filename);
        log.debug("Looking for file at path: {}", filePath);

        if (!Files.exists(filePath)) {
            log.warn("PDF file not found on disk: case={}, expectedPath={}", caseNumber, filePath);
            throw new IllegalArgumentException("PDF file not found for case: " + caseNumber);
        }

        long fileSize = Files.size(filePath);
        log.info("PDF file found for download: case={}, file={}, size={} bytes",
                caseNumber, filename, fileSize);

        Resource resource = new UrlResource(filePath.toUri());
        log.debug("Created resource for download: {}", resource.getDescription());

        return resource;
    }

    private String determineFilename(String caseNumber) {
        String numericPart = caseNumber.replaceAll("[^0-9]", "");
        String filename;

        if (!numericPart.isEmpty()) {
            int caseNum = Integer.parseInt(numericPart);
            if (caseNum == 1) {
                filename = "documentation.pdf";
            } else if (caseNum == 2) {
                filename = "documentation1.pdf";
            } else {
                filename = "case_" + caseNumber + "_document.pdf";
            }
        } else {
            filename = "case_" + caseNumber + "_document.pdf";
        }

        log.debug("Filename determination: caseNumber={}, numericPart={}, result={}",
                caseNumber, numericPart, filename);
        return filename;
    }

    // New method to get files with upload timestamps
    public List<CaseFile> getFilesWithUploadTime(String caseNumber) {
        log.info("Retrieving files with upload timestamps for case: {}", caseNumber);
        return caseFileRepository.findByCaseNumberOrderByUploadedAtDesc(caseNumber);
    }

    // New method to get file upload history
    public List<CaseFile> getFileUploadHistory(String caseNumber) {
        return caseFileRepository.findByCaseEntityNumber(caseNumber);
    }


    public Resource downloadCaseFile(String number, Long fileId) throws IOException {
        Case caseEntity = caseRepository.getCaseByNumber(number)
                .orElseThrow(() -> new IllegalArgumentException("Case not found with ID: " + number));

        CaseFile caseFile = caseFileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found with ID: " + fileId));

        if (!caseFile.getCaseEntity().getId().equals(caseEntity.getId())) {
            throw new IllegalArgumentException("File does not belong to the given case.");
        }

        Path filePath = Paths.get(uploadDirectory, caseFile.getFileName());

        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("File not found on disk: " + filePath);
        }

        return new UrlResource(filePath.toUri());
    }
}