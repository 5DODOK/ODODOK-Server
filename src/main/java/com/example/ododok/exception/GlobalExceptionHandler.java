package com.example.ododok.exception;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @Data
    public static class ErrorResponse {
        private int status;
        private String message;
        private String code;

        public ErrorResponse(int status, String message, String code) {
            this.status = status;
            this.message = message;
            this.code = code;
        }
    }

    @ExceptionHandler(CsvProcessingException.class)
    public ResponseEntity<ErrorResponse> handleCsvProcessingException(CsvProcessingException e) {
        HttpStatus status = determineHttpStatus(e.getErrorCode());
        ErrorResponse errorResponse = new ErrorResponse(
                status.value(),
                e.getMessage(),
                e.getErrorCode()
        );
        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e) {
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                "이 작업을 수행할 권한이 없습니다.",
                "ACCESS_DENIED"
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.PAYLOAD_TOO_LARGE.value(),
                "업로드 가능한 최대 크기를 초과했습니다.",
                "FILE_TOO_LARGE"
        );
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        log.error("Unexpected error occurred", e);
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "질문 일괄 등록 처리 중 오류가 발생했습니다.",
                "INTERNAL_SERVER_ERROR"
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    private HttpStatus determineHttpStatus(String errorCode) {
        if (errorCode == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }

        return switch (errorCode) {
            case "HEADER_MISMATCH", "INVALID_CSV_FORMAT", "EMPTY_FILE" -> HttpStatus.BAD_REQUEST;
            case "USER_NOT_FOUND" -> HttpStatus.UNAUTHORIZED;
            case "FILE_SIZE_EXCEEDED", "TOO_MANY_ROWS" -> HttpStatus.PAYLOAD_TOO_LARGE;
            case "INVALID_CONTENT_TYPE" -> HttpStatus.UNSUPPORTED_MEDIA_TYPE;
            case "FK_NOT_FOUND", "INVALID_DIFFICULTY_LABEL", "REQUIRED_FIELD_MISSING",
                 "FIELD_TOO_LONG", "INVALID_YEAR_FORMAT", "MUTUAL_EXCLUSION_VIOLATION",
                 "INVALID_ID_FORMAT" -> HttpStatus.UNPROCESSABLE_ENTITY;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}