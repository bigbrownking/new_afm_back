package org.nab.new_afm_back.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.core.io.ResourceLoader;
import org.springframework.beans.factory.annotation.Autowired;

@Component
@Slf4j
public class CaseAccessTracker {

    private static final Deque<String> GLOBAL_ACCESSED_CASES = new ConcurrentLinkedDeque<>();

    private static final String GLOBAL_SESSION_ID = "GLOBAL_SHARED_SESSION";

    private String filePath;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Autowired
    private ResourceLoader resourceLoader;

    @PostConstruct
    public void init() {
        try {
            String classPath = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
            if (classPath.endsWith("/classes/")) {
                this.filePath = classPath + "accessed_cases.json";
            } else {
                String jarDir = new File(classPath).getParent();
                this.filePath = jarDir + File.separator + "accessed_cases.json";
            }
        } catch (Exception e) {
            this.filePath = System.getProperty("user.dir") + File.separator + "accessed_cases.json";
            log.warn("Using fallback file path: {}", this.filePath);
        }

        loadFromFile();
        scheduler.scheduleAtFixedRate(this::saveToFile, 30, 30, TimeUnit.SECONDS);
        log.info("CaseAccessTracker initialized with file: {}", filePath);
    }

    @PreDestroy
    public void cleanup() {
        saveToFile();
        scheduler.shutdown();
        log.info("CaseAccessTracker cleanup completed");
    }

    public void addCaseNumber(String caseNumber) {
        log.info("Adding case number {} (Session ID: {})", caseNumber, GLOBAL_SESSION_ID);

        GLOBAL_ACCESSED_CASES.remove(caseNumber);

        GLOBAL_ACCESSED_CASES.addFirst(caseNumber);

        log.debug("Global session now has {} accessed cases", GLOBAL_ACCESSED_CASES.size());
        saveToFile();
    }

    public List<String> getAccessedCaseNumbers() {
        int size = GLOBAL_ACCESSED_CASES.size();
        log.info("Retrieved {} case numbers (Session ID: {})", size, GLOBAL_SESSION_ID);

        return new ArrayList<>(GLOBAL_ACCESSED_CASES);
    }

    public void clearAccessedCases() {
        log.info("Clearing all accessed cases (Session ID: {})", GLOBAL_SESSION_ID);
        GLOBAL_ACCESSED_CASES.clear();
        saveToFile();
    }

    public int getAccessedCasesCount() {
        return GLOBAL_ACCESSED_CASES.size();
    }

    public boolean containsCaseNumber(String caseNumber) {
        return GLOBAL_ACCESSED_CASES.contains(caseNumber);
    }

    public boolean removeCaseNumber(String caseNumber) {
        boolean removed = GLOBAL_ACCESSED_CASES.remove(caseNumber);
        if (removed) {
            log.info("Removed case number {} from global session", caseNumber);
            saveToFile();
        }
        return removed;
    }

    public String getLastAccessedCaseNumber() {
        return GLOBAL_ACCESSED_CASES.peekFirst();
    }

    public List<String> getLastAccessedCaseNumbers(int count) {
        return GLOBAL_ACCESSED_CASES.stream()
                .limit(count)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    private synchronized void saveToFile() {
        try {
            File file = new File(filePath);
            file.getParentFile().mkdirs();

            List<String> dataToSave = new ArrayList<>(GLOBAL_ACCESSED_CASES);
            objectMapper.writeValue(file, dataToSave);

            log.debug("Saved {} case numbers to file: {}", dataToSave.size(), filePath);
        } catch (IOException e) {
            log.error("Error saving accessed cases to file {}: {}", filePath, e.getMessage());
        }
    }

    private synchronized void loadFromFile() {
        try {
            File file = new File(filePath);
            if (file.exists() && file.length() > 0) {
                List<String> loadedData = objectMapper.readValue(file, new TypeReference<List<String>>() {});
                GLOBAL_ACCESSED_CASES.clear();
                GLOBAL_ACCESSED_CASES.addAll(loadedData);
                log.info("Loaded {} case numbers from file: {}", loadedData.size(), filePath);
            } else {
                log.info("No existing data file found, starting with empty case list");
            }
        } catch (IOException e) {
            log.error("Error loading accessed cases from file {}: {}", filePath, e.getMessage());
            log.info("Starting with empty case list");
        }
    }

    public void forceSave() {
        saveToFile();
        log.info("Force save completed");
    }

    public void forceReload() {
        loadFromFile();
        log.info("Force reload completed, now have {} cases", GLOBAL_ACCESSED_CASES.size());
    }
}