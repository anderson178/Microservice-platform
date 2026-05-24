package com.iprody.inquiry.controller;

import com.iprody.common.ResultCode;
import com.iprody.common.exception.AppException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException ex) {
        return ResponseEntity
                .status(ex.getCode().getHttpStatus())
                .body(new ErrorResponse(
                        ex.getCode().name(),
                        StringUtils.isNoneBlank(ex.getMessage()) ? ex.getMessage() : ex.getCode().getDefaultMessage(),
                        ex.getResourceId(),
                        LocalDateTime.now()
                ));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse(
                        HttpStatus.BAD_REQUEST.name(),
                        "Invalid parameter: " + ex.getName() + " (expected: " + ex.getRequiredType().getSimpleName() + ")",
                        null,
                        LocalDateTime.now()
                ));
    }

    @ExceptionHandler({
            NoResourceFoundException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception e) {
        log.error("Unexpected error: {}", e.getMessage(), e);

        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse(
                        HttpStatus.BAD_REQUEST.name(),
                        ResultCode.BAD_REQUEST.getDefaultMessage(),
                        null,
                        LocalDateTime.now()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("Unexpected error: {}", e.getMessage(), e);

        return ResponseEntity
                .internalServerError()
                .body(new ErrorResponse(
                        HttpStatus.INTERNAL_SERVER_ERROR.name(),
                        ResultCode.INTERNAL_SERVER_ERROR.getDefaultMessage(),
                        null,
                        LocalDateTime.now()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse(
                        ResultCode.VALIDATION_ERROR.name(),
                        errors.toString(),
                        null,
                        LocalDateTime.now()
                ));
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ErrorResponse> handleExternalServiceUnavailable(ResourceAccessException ex) {
        log.error("External service unavailable: {}", ex.getMessage(), ex);

        return ResponseEntity
                .status(HttpStatus.FAILED_DEPENDENCY)
                .body(new ErrorResponse(
                        ResultCode.EXTERNAL_SERVICE_UNAVAILABLE.name(),
                        ResultCode.EXTERNAL_SERVICE_UNAVAILABLE.getDefaultMessage(),
                        null,
                        LocalDateTime.now()
                ));
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<Object> handleAuthorizationDeniedException(AuthorizationDeniedException ex) {
        Map<String, Object> body = Map.of(
                "code", HttpStatus.FORBIDDEN,
                "message", "Access Denied: " + ex.getMessage(),
                "timestamp", LocalDateTime.now()
        );

        return new ResponseEntity<>(body, HttpStatus.FORBIDDEN);
    }


    @Data
    @AllArgsConstructor
    public static class ErrorResponse {
        private String code;
        private String message;
        private UUID resourceId;
        private LocalDateTime timestamp;
    }
}
