package com.example.ododok.exception;

public class CsvProcessingException extends RuntimeException {

    private final String errorCode;
    private final String field;

    public CsvProcessingException(String message) {
        super(message);
        this.errorCode = null;
        this.field = null;
    }

    public CsvProcessingException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.field = null;
    }

    public CsvProcessingException(String message, String errorCode, String field) {
        super(message);
        this.errorCode = errorCode;
        this.field = field;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getField() {
        return field;
    }
}