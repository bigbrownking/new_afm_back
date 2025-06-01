package org.nab.new_afm_back.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.nab.new_afm_back.dto.response.Document;
import org.nab.new_afm_back.service.IDocumentService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

@Service
@Log
@RequiredArgsConstructor
public class DocumentService implements IDocumentService {

    private final ObjectMapper objectMapper;
    private JsonNode documentsData;

    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource = new ClassPathResource("documents.json");
            documentsData = objectMapper.readTree(resource.getInputStream());
            log.info("Documents data loaded successfully");
        } catch (IOException e) {
            log.severe("Failed to load documents.json: " + e.getMessage());
            throw new RuntimeException("Could not load documents configuration", e);
        }
    }

    @Override
    public Document getDocument(String number) {
        JsonNode documentNode = documentsData.get(number);
        if (documentNode == null) {
            return null;
        }

        Document document = new Document();
        document.setHeader(documentNode.get("header").asText());

        document.setPredicates(new ArrayList<>());

        JsonNode predicatesNode = documentNode.get("predicates");
        if (predicatesNode != null && predicatesNode.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = predicatesNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                document.setPredicate(field.getKey(), field.getValue().asText());
            }
        }

        return document;
    }

    @Override
    public String getTextOfPredicate(String number, String predicate) {
        JsonNode documentNode = documentsData.get(number);
        if (documentNode == null) {
            log.warning("Document with number " + number + " not found");
            return null;
        }

        JsonNode predicatesNode = documentNode.get("predicates");
        if (predicatesNode == null) {
            log.warning("No predicates found for document " + number);
            return null;
        }

        JsonNode predicateNode = predicatesNode.get(predicate);
        if (predicateNode == null) {
            log.warning("Predicate '" + predicate + "' not found in document " + number);
            return null;
        }

        return predicateNode.asText();
    }
}