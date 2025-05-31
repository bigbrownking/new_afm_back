package org.nab.new_afm_back.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nab.new_afm_back.dto.request.UploadCaseRequest;
import org.nab.new_afm_back.model.Article;
import org.nab.new_afm_back.model.Case;
import org.nab.new_afm_back.repository.ArticleRepository;
import org.nab.new_afm_back.repository.CaseRepository;
import org.nab.new_afm_back.service.IPdfService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfService implements IPdfService {

    private final CaseRepository caseRepository;
    private final ArticleRepository articleRepository;

    @Value("${file.upload.directory:./uploads}")
    private String uploadDirectory;

    private static final List<String> ALLOWED_EXTENSIONS = List.of("pdf", "doc", "docx", "txt");
    private static final long MAX_FILE_SIZE = 100 * 1024 * 1024;

    public Case uploadCaseWithPdf(UploadCaseRequest request, List<MultipartFile> additionalFiles) throws IOException {
        if (caseRepository.getCaseByNumber(request.getNumber()).isPresent()) {
            throw new IllegalArgumentException("Case number already exists: " + request.getNumber());
        }

        Article article = resolveArticle(request.getArticles());
        List<String> additionalFileNames = processAdditionalFiles(request, additionalFiles);

        Case newCase = Case.builder()
                .number(request.getNumber())
                .author(request.getAuthor())
                .investigator(request.getInvestigator())
                .policeman(request.getPoliceman())
                .object(request.getObject())
                .article(article)
                .uploadDate(LocalDate.now())
                .status(false)
                .info(0)
                .adds(additionalFileNames)
                .build();

        return caseRepository.save(newCase);
    }

    private Article resolveArticle(List<Integer> articleIds) {
        if (articleIds == null || articleIds.isEmpty()) return null;
        return articleRepository.findById(articleIds.get(0).longValue()).orElse(null);
    }

    private List<String> processAdditionalFiles(UploadCaseRequest request, List<MultipartFile> additionalFiles) throws IOException {
        List<String> fileNames = new ArrayList<>();
        if (additionalFiles == null || additionalFiles.isEmpty()) return fileNames;

        log.info("Processing {} additional files for case: {}", additionalFiles.size(), request.getNumber());

        for (int i = 0; i < additionalFiles.size(); i++) {
            MultipartFile file = additionalFiles.get(i);
            if (!file.isEmpty()) {
                try {
                    String fileName = file.getOriginalFilename();
                    validateAndSaveFile(file, fileName);
                    fileNames.add(fileName);
                    log.info("Additional file {} saved successfully", fileName);
                } catch (Exception e) {
                    log.error("Error saving additional file {}: {}", i, e.getMessage());
                }
            }
        }
        return fileNames;
    }

    private void validateAndSaveFile(MultipartFile file, String fileName) throws IOException {
        if (!validateUploadedFile(file)) {
            throw new IOException("Invalid file: " + file.getOriginalFilename());
        }
        savePdfToStorage(file, fileName);
    }

    @Override
    public byte[] extractPdfContent(MultipartFile pdfFile) throws IOException {
        return pdfFile.getBytes();
    }

    @Override
    public void savePdfToStorage(MultipartFile pdfFile, String fileName) throws IOException {
        Path uploadPath = Paths.get(uploadDirectory);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(pdfFile.getInputStream(), filePath);
        log.info("PDF file saved: {}", filePath);
    }

    @Override
    public boolean validateUploadedFile(MultipartFile file) {
        if (file == null || file.isEmpty()) return false;

        if (file.getSize() > MAX_FILE_SIZE) {
            log.warn("File size exceeds limit: {} bytes", file.getSize());
            return false;
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) return false;

        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            log.warn("Invalid file extension: {}", extension);
            return false;
        }

        // You can optionally check content types here if needed, but many browsers won't send accurate types for .doc/.txt
        return true;
    }

    private String generateAdditionalFileName(String caseNumber, String originalFilename, int index) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";
        return String.format("%s_additional_%d_%s%s", caseNumber, index, timestamp, extension);
    }

    public Case addAdditionalFilesToCase(String caseNumber, List<MultipartFile> additionalFiles) throws IOException {
        Optional<Case> optionalCase = caseRepository.getCaseByNumber(caseNumber);
        if (optionalCase.isEmpty()) {
            throw new IllegalArgumentException("Case not found: " + caseNumber);
        }

        Case existingCase = optionalCase.get();
        List<String> existingFileNames = existingCase.getAdds() != null ? new ArrayList<>(existingCase.getAdds()) : new ArrayList<>();

        if (additionalFiles != null && !additionalFiles.isEmpty()) {
            log.info("Processing {} additional files for case: {}", additionalFiles.size(), caseNumber);
            for (MultipartFile file : additionalFiles) {
                if (!file.isEmpty()) {
                    String fileName = file.getOriginalFilename();
                    try {
                        validateAndSaveFile(file, fileName);
                        existingFileNames.add(fileName);
                        log.info("Additional file {} saved successfully", fileName);
                    } catch (Exception e) {
                        log.error("Error saving additional file {}: {}", fileName, e.getMessage());
                    }
                }
            }
        }

        existingCase.setAdds(existingFileNames);
        return caseRepository.save(existingCase);
    }


    public void deleteAdditionalFileByName(String caseNumber, String fileName) {
        Optional<Case> optionalCase = caseRepository.getCaseByNumber(caseNumber);
        if (optionalCase.isEmpty()) {
            throw new IllegalArgumentException("Case not found: " + caseNumber);
        }

        Case existingCase = optionalCase.get();
        List<String> additionalFiles = existingCase.getAdds();

        if (additionalFiles == null || !additionalFiles.contains(fileName)) {
            throw new IllegalArgumentException("File '" + fileName + "' not found in case " + caseNumber);
        }

        Path filePath = Paths.get(uploadDirectory).resolve(fileName);
        try {
            Files.deleteIfExists(filePath);
            log.info("Deleted file from disk: {}", filePath);
        } catch (IOException e) {
            log.error("Failed to delete file: {}", filePath, e);
            throw new RuntimeException("Failed to delete file: " + fileName, e);
        }

        additionalFiles.remove(fileName);
        existingCase.setAdds(additionalFiles);
        caseRepository.save(existingCase);
        log.info("Removed file '{}' from case {}", fileName, caseNumber);
    }

}
