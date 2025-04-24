package com.nazarov.footballmanager.exception;

import com.fasterxml.jackson.databind.JsonMappingException.Reference;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorDetails> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
    ErrorDetails errorDetails = new ErrorDetails(Instant.now(), HttpStatus.NOT_FOUND.value(), "Not Found", ex.getMessage(), request.getDescription(false));
    log.warn("Resource not found: {}", ex.getMessage());
    return new ResponseEntity<>(errorDetails, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<ErrorDetails> handleBadRequestException(BadRequestException ex, WebRequest request) {
    ErrorDetails errorDetails = new ErrorDetails(Instant.now(), HttpStatus.BAD_REQUEST.value(), "Bad Request", ex.getMessage(), request.getDescription(false));
    log.warn("Bad request: {}", ex.getMessage());
    return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorDetails> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex, WebRequest request) {
    String specificMessage = "Invalid request body format or type mismatch.";
    if (ex.getCause() instanceof InvalidFormatException ife) {
      String fieldName = ife.getPath().stream()
          .map(Reference::getFieldName)
          .collect(Collectors.joining("."));
      specificMessage = String.format("Invalid format for field '%s'. Expected format compatible with %s.",
          fieldName, ife.getTargetType().getSimpleName());
    } else if (ex.getMessage() != null) {
      specificMessage = "Invalid JSON request: " + ex.getMessage().split(";")[0];
    }

    ErrorDetails errorDetails = new ErrorDetails(Instant.now(), HttpStatus.BAD_REQUEST.value(), "Bad Request", specificMessage, request.getDescription(false));
    log.warn("HTTP message not readable: {}", ex.getMessage());
    return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Object> handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult().getAllErrors().forEach((error) -> {
      String fieldName = ((FieldError) error).getField();
      String errorMessage = error.getDefaultMessage();
      errors.put(fieldName, errorMessage);
    });
    ValidationErrorDetails errorDetails = new ValidationErrorDetails(Instant.now(), HttpStatus.BAD_REQUEST.value(), "Validation Failed", errors, request.getDescription(false));
    log.warn("Validation failed: {}", errors);
    return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
  }

  // Security exceptions
  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ErrorDetails> handleAuthenticationException(AuthenticationException ex, WebRequest request) {
    ErrorDetails errorDetails = new ErrorDetails(Instant.now(), HttpStatus.UNAUTHORIZED.value(), "Unauthorized", ex.getMessage(), request.getDescription(false));
    log.warn("Authentication failed: {}", ex.getMessage());
    return new ResponseEntity<>(errorDetails, HttpStatus.UNAUTHORIZED);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorDetails> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
    ErrorDetails errorDetails = new ErrorDetails(Instant.now(), HttpStatus.FORBIDDEN.value(), "Forbidden", ex.getMessage(), request.getDescription(false));
    log.warn("Access denied: {}", ex.getMessage());
    return new ResponseEntity<>(errorDetails, HttpStatus.FORBIDDEN);
  }


  // Global fallback exception handler
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorDetails> handleGlobalException(Exception ex, WebRequest request) {
    ErrorDetails errorDetails = new ErrorDetails(Instant.now(), HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error", ex.getMessage(), request.getDescription(false));
    log.error("An unexpected error occurred: ", ex);
    return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);
  }


  private record ErrorDetails(Instant timestamp, int status, String error, String message, String path) {}
  private record ValidationErrorDetails(Instant timestamp, int status, String error, Map<String, String> validationErrors, String path) {}

}