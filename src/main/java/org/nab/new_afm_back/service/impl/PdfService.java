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
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfService implements IPdfService {
    private final CaseRepository caseRepository;
    private final ArticleRepository articleRepository;

    @Value("${file.upload.directory:./uploads}")
    private String uploadDirectory;

    private static final List<String> ALLOWED_EXTENSIONS = List.of("pdf");
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
                    String fileName = generateAdditionalFileName(request.getNumber(), file.getOriginalFilename(), i);
                    validateAndSaveFile(file, fileName);
                    fileNames.add(fileName);
                    log.info("Additional file {} saved successfully", fileName);
                } catch (Exception e) {
                    log.error("Error saving additional file {}: {}", i, e.getMessage());
                    // Optionally: throw exception to abort upload
                }
            }
        }
        return fileNames;
    }

    private void validateAndSaveFile(MultipartFile file, String fileName) throws IOException {
        if (!validatePdfFile(file)) {
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
    public boolean validatePdfFile(MultipartFile pdfFile) {
        if (pdfFile == null || pdfFile.isEmpty()) return false;

        // File size check
        if (pdfFile.getSize() > MAX_FILE_SIZE) {
            log.warn("File size exceeds limit: {} bytes", pdfFile.getSize());
            return false;
        }

        // File extension check
        String originalFilename = pdfFile.getOriginalFilename();
        if (originalFilename == null) return false;
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            log.warn("Invalid file extension: {}", extension);
            return false;
        }

        // Content type check
        String contentType = pdfFile.getContentType();
        if (!"application/pdf".equals(contentType)) {
            log.warn("Invalid content type: {}", contentType);
            return false;
        }

        return true;
    }

    private String generateAdditionalFileName(String caseNumber, String originalFilename, int index) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";
        return String.format("%s_additional_%d_%s%s", caseNumber, index, timestamp, extension);
    }

    private String generateFileName(String caseNumber, String originalFilename) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        return String.format("%s_%s_%s%s", caseNumber, timestamp, uuid, extension);
    }
}
