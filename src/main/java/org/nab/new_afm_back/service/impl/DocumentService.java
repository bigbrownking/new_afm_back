package org.nab.new_afm_back.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.nab.new_afm_back.dto.response.Document;
import org.nab.new_afm_back.dto.response.*;
import org.nab.new_afm_back.service.IDocumentService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
            if (!resource.exists()) {
                throw new RuntimeException("documents.json not found in classpath");
            }

            documentsData = objectMapper.readTree(resource.getInputStream());
            log.info("Documents data loaded successfully");
        } catch (IOException e) {
            log.severe("Failed to load documents.json: " + e.getMessage());
            throw new RuntimeException("Could not load documents configuration", e);
        }
    }

    @Override
    public Document getDocument(String number) {
        return new Document();
    }

    private List<Predicate> parsePredicates(JsonNode documentNode) {
        List<Predicate> predicateList = new ArrayList<>();

        JsonNode predicatesNode = documentNode.get("predicates");
        if (predicatesNode == null || !predicatesNode.isArray()) {
            log.warning("No valid predicates array found for document: " + documentNode.get("header").asText());
            return predicateList;
        }

        for (JsonNode predicateNode : predicatesNode) {
            Predicate predicate = new Predicate();
            predicate.setLabel(predicateNode.get("label").asText());
            predicate.setSubLabel(predicateNode.get("subLabel").asText());
            predicate.setText(predicateNode.get("text").asText());
            predicateList.add(predicate);
        }

        return predicateList;
    }
    private List<Risk> parseRisks(JsonNode documentNode) {
        List<Risk> riskList = new ArrayList<>();

        JsonNode risksNode = documentNode.get("risks");
        if (risksNode == null || !risksNode.isArray()) {
            return riskList;
        }

        risksNode.forEach(riskNode -> {
            Risk risk = new Risk();
            risk.setLabel(riskNode.get("label").asText());
            risk.setSubLabel(riskNode.get("subLabel").asText());
            risk.setText(riskNode.get("text").asText());
            riskList.add(risk);
        });

        return riskList;
    }

    public String getTextOfPredicate(String number, String predicateKey) {
        Document doc = getDocument(number);
        if (doc == null || doc.getPredicates() == null) return null;

        return doc.getPredicates().stream()
                .filter(p -> p.getLabel().equals(predicateKey))
                .map(Predicate::getText)
                .findFirst()
                .orElse(null);
    }

    public String getTextOfRisk(String number, String riskKey) {
        Document doc = getDocument(number);
        if (doc == null || doc.getRisks() == null) return null;

        return doc.getRisks().stream()
                .filter(r -> r.getLabel().equals(riskKey))
                .map(Risk::getText)
                .findFirst()
                .orElse(null);
    }
}