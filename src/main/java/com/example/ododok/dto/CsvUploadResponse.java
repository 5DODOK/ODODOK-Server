package com.example.ododok.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CsvUploadResponse {

    private Summary summary;
    private List<ValidationError> errors;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private int totalRows;
        private int created;
        private int updated;
        private int skipped;
        private boolean dryRun;
        private String upsertKey;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationError {
        private int row;
        private String code;
        private String field;
        private String message;
    }
}