package org.nab.new_afm_back.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nab.new_afm_back.dto.request.UploadCaseRequest;
import org.nab.new_afm_back.model.Case;
import org.nab.new_afm_back.service.impl.FileService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class FileControllerHandler {

    private final FileService fileService;
    private final ObjectMapper objectMapper;

    public ResponseEntity<?> handleUploadCase(String caseDataJson, List<MultipartFile> additionalFiles) {
        try {
            UploadCaseRequest request = objectMapper.readValue(caseDataJson, UploadCaseRequest.class);
            log.info("Parsed upload request for case number: {}, author: {}",
                    request.getNumber(), request.getAuthor());

            Case uploadedCase = fileService.uploadCaseWithFiles(request, additionalFiles);
            log.info("Case uploaded successfully with ID: {}", uploadedCase.getId());

            return ResponseEntity.status(HttpStatus.CREATED).body(uploadedCase);

        } catch (JsonProcessingException e) {
            log.error("Invalid JSON in caseData: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Invalid JSON in 'caseData': " + e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        } catch (IOException e) {
            log.error("IO error during processing: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing files: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during case upload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error: " + e.getMessage());
        }
    }

    public ResponseEntity<?> handleAddFiles(String caseNumber, List<MultipartFile> additionalFiles) {
        try {
            Case updatedCase = fileService.addAdditionalFilesToCase(caseNumber, additionalFiles);
            log.info("Successfully added files to case: {}", caseNumber);
            return ResponseEntity.ok(updatedCase);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for case {}: {}", caseNumber, e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        } catch (IOException e) {
            log.error("IO error for case {}: {}", caseNumber, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing files: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error for case: {}", caseNumber, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error: " + e.getMessage());
        }
    }

    public ResponseEntity<?> handleDeleteFile(String caseNumber, int fileId) {
        try {
            fileService.deleteAdditionalFileById(caseNumber, fileId);
            log.info("Successfully deleted file '{}' from case: {}", fileId, caseNumber);
            return ResponseEntity.ok("File deleted successfully");

        } catch (IllegalArgumentException e) {
            log.warn("File not found: case={}, file={}", caseNumber, fileId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error deleting file: case={}, file={}", caseNumber, fileId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error: " + e.getMessage());
        }
    }

    public ResponseEntity<?> handleDownloadDocument(String caseNumber, String format) {
        if (!isValidFormat(format)) {
            log.warn("Invalid format requested: {}", format);
            return ResponseEntity.badRequest().body("Invalid format. Supported formats: pdf, word");
        }

        try {
            DocumentDownloadInfo downloadInfo = prepareDownload(caseNumber, format);

            if (downloadInfo.resource() == null || !downloadInfo.resource().exists()) {
                log.warn("Document not found for case: {} in format: {}", caseNumber, format);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Document not found for case: " + caseNumber);
            }

            return buildDownloadResponse(downloadInfo);

        } catch (IllegalArgumentException e) {
            log.warn("Case not found: {}", caseNumber);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Error: " + e.getMessage());
        } catch (IOException e) {
            log.error("IO error for case: {}, format: {}", caseNumber, format, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error reading document: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error for case: {}, format: {}", caseNumber, format, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error: " + e.getMessage());
        }
    }

    private boolean isValidFormat(String format) {
        return format != null && (format.equalsIgnoreCase("pdf") || format.equalsIgnoreCase("word"));
    }

    private DocumentDownloadInfo prepareDownload(String caseNumber, String format) throws IOException {
        Resource resource;
        String mediaType;
        String fileExtension;

        switch (format.toLowerCase()) {
            case "pdf":
                resource = fileService.downloadPdfByCaseNumber(caseNumber);
                mediaType = MediaType.APPLICATION_PDF_VALUE;
                fileExtension = ".pdf";
                break;
            case "word":
                resource = fileService.downloadWordByCaseNumber(caseNumber);
                mediaType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                fileExtension = ".docx";
                break;
            default:
                throw new IllegalArgumentException("Unsupported format: " + format);
        }

        String filename = "documentation" + fileExtension;
        return new DocumentDownloadInfo(resource, mediaType, filename);
    }

    private ResponseEntity<?> buildDownloadResponse(DocumentDownloadInfo info) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + info.filename() + "\"");
        headers.add(HttpHeaders.CONTENT_TYPE, info.mediaType());

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(info.resource().contentLength())
                .body(info.resource());
    }

    private record DocumentDownloadInfo(Resource resource, String mediaType, String filename) { }
}