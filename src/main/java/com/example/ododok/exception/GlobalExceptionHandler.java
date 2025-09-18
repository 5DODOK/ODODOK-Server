package com.example.ododok.exception;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        String message = "필수 필드가 누락되었거나 형식이 잘못되었습니다.";
        if (!errors.isEmpty()) {
            message = errors.values().iterator().next(); // 첫 번째 오류 메시지 사용
        }

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                message,
                "VALIDATION_FAILED"
        );
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        log.error("Unexpected error occurred", e);

        String message = "서버 내부 오류가 발생했습니다.";
        if (e.getMessage() != null && e.getMessage().contains("질문")) {
            message = "질문 처리 중 오류가 발생했습니다.";
        }

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                message,
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
            case "COMPANY_NOT_FOUND", "CATEGORY_NOT_FOUND", "QUESTION_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "DUPLICATE_QUESTION" -> HttpStatus.CONFLICT;
            case "FILE_SIZE_EXCEEDED", "TOO_MANY_ROWS" -> HttpStatus.PAYLOAD_TOO_LARGE;
            case "INVALID_CONTENT_TYPE" -> HttpStatus.UNSUPPORTED_MEDIA_TYPE;
            case "FK_NOT_FOUND", "INVALID_DIFFICULTY_LABEL", "REQUIRED_FIELD_MISSING",
                 "FIELD_TOO_LONG", "INVALID_YEAR_FORMAT", "MUTUAL_EXCLUSION_VIOLATION",
                 "INVALID_ID_FORMAT", "INVALID_DIFFICULTY" -> HttpStatus.UNPROCESSABLE_ENTITY;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}