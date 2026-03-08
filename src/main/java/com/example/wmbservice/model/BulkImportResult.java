package com.example.wmbservice.model;

import java.util.List;
import java.util.Map;

/**
 * Result object for bulk import summary.
 */
public class BulkImportResult {
    private final int insertedCount;
    private final int duplicateCount;
    private final List<Map<String, Object>> errors;

    public BulkImportResult(int insertedCount, int duplicateCount, List<Map<String, Object>> errors) {
        this.insertedCount = insertedCount;
        this.duplicateCount = duplicateCount;
        this.errors = errors;
    }

    public int getInsertedCount() { return insertedCount; }
    public int getDuplicateCount() { return duplicateCount; }
    public List<Map<String, Object>> getErrors() { return errors; }
}