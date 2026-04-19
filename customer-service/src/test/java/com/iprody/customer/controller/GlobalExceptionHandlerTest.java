package com.iprody.customer.controller;

import com.iprody.common.ResultCode;
import com.iprody.common.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.reactive.resource.NoResourceFoundException;

import java.net.URI;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler tests")
class GlobalExceptionHandlerTest {
    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("HandleTypeMismatch - 400 BAD_REQUEST. With parameter name and expected type")
    void handleTypeMismatch_returnsCorrectResponse() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("customerId");
        when(ex.getRequiredType()).thenReturn((Class) UUID.class);

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleTypeMismatch(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assert response.getBody() != null;
        assertThat(response.getBody().getCode()).isEqualTo(ResultCode.BAD_REQUEST.name());
        assertThat(response.getBody().getMessage()).contains("customerId", "UUID");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("HandleBadRequest -  400 BAD_REQUEST. For NoResourceFoundException")
    void handleBadRequest_forNoResourceFound_returns400() {
        NoResourceFoundException ex = new NoResourceFoundException(URI.create("/api/v1/unknown"), "GET /api/v1/unknown");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleBadRequest(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assert response.getBody() != null;
        assertThat(response.getBody().getCode()).isEqualTo(ResultCode.BAD_REQUEST.name());
        assertThat(response.getBody().getMessage()).isEqualTo(ResultCode.BAD_REQUEST.getDefaultMessage());
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("ResourceAccessException - 424 FAILED_DEPENDENCY.When external service is unavailable")
    void handleExternalServiceUnavailable_returns424() {
        ResourceAccessException ex = new ResourceAccessException(
                "Connection refused: http://external-service/api");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleExternalServiceUnavailable(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FAILED_DEPENDENCY);
        assertThat(response.getBody()).isNotNull();
        assert response.getBody() != null;
        assertThat(response.getBody().getCode()).isEqualTo(ResultCode.EXTERNAL_SERVICE_UNAVAILABLE.name());
        assertThat(response.getBody().getMessage()).isEqualTo(ResultCode.EXTERNAL_SERVICE_UNAVAILABLE.getDefaultMessage());
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("HandleUnexpected - 500 INTERNAL_SERVER_ERROR. With generic error message")
    void handleUnexpected_returnsCorrectResponse() {
        Exception ex = new RuntimeException("Something went wrong");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleUnexpected(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assert response.getBody() != null;
        assertThat(response.getBody().getCode()).isEqualTo(ResultCode.INTERNAL_SERVER_ERROR.name());
        assertThat(response.getBody().getMessage()).isEqualTo(ResultCode.INTERNAL_SERVER_ERROR.getDefaultMessage());
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("HandleAppException - 404 NOT_FOUND. Status from ResultCode and custom message")
    void handleAppException_returnsCorrectResponse() {
        UUID resourceId = UUID.randomUUID();
        AppException ex = new AppException(ResultCode.NOT_FOUND, resourceId);

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleAppException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assert response.getBody() != null;
        assertThat(response.getBody().getCode()).isEqualTo(ResultCode.NOT_FOUND.name());
        assertThat(response.getBody().getResourceId()).isEqualTo(resourceId);
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("MethodArgumentNotValidException - 400 BAD_REQUEST. Should handle single field error correctly")
    void handleValidationExceptions_singleError_formatsCorrectly() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "requestBody");
        bindingResult.addError(new FieldError("requestBody", "fullName", "must not be blank"));
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleValidationExceptions(ex);

        assert response.getBody() != null;
        assertThat(response.getBody().getMessage()).contains("fullName", "must not be blank");
    }
}