package org.nab.new_afm_back.controller;

import lombok.RequiredArgsConstructor;
import org.nab.new_afm_back.dto.response.Document;
import org.nab.new_afm_back.service.impl.DocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/documentation")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping("/{number}")
    public ResponseEntity<Document> getDocument(@PathVariable String number) {
        try {
            Document document = documentService.getDocument(number);

            if (document == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(document);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{number}/predicate/{predicate}")
    public ResponseEntity<String> getPredicateText(@PathVariable String number,
                                                   @PathVariable String predicate) {
        try {
            String text = documentService.getTextOfPredicate(number, predicate);

            if (text == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(text);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}