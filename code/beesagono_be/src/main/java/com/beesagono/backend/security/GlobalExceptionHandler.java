package com.beesagono.backend.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global REST controller advice providing centralized exception handling,
 * request validation error formatting, and standardized HTTP error responses
 * across the application.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles missing required HTTP request parameters.
     *
     * @param ex the missing servlet request parameter exception
     * @return a {@link ResponseEntity} with a 400 Bad Request status and error
     *         details
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParams(MissingServletRequestParameterException ex) {
        logger.warn("Parametro obbligatorio mancante: {}", ex.getParameterName());
        return buildResponse(HttpStatus.BAD_REQUEST, "Il parametro '" + ex.getParameterName() + "' è obbligatorio.");
    }

    /**
     * Handles validation errors triggered by {@code @Valid} annotations on request
     * bodies.
     *
     * @param ex the method argument validation exception
     * @return a {@link ResponseEntity} with a 400 Bad Request status containing
     *         field-level validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        logger.warn("Errore di validazione DTO intercettato: {} errori", ex.getBindingResult().getErrorCount());

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.put(error.getField(), error.getDefaultMessage()));

        Map<String, Object> body = createBaseBody(HttpStatus.BAD_REQUEST);
        body.put("message", "Errore di validazione dei dati inviati.");
        body.put("errors", fieldErrors);

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles constraint violation exceptions for request parameters and path
     * variables.
     *
     * @param ex the constraint violation exception
     * @return a {@link ResponseEntity} with a 400 Bad Request status and error
     *         details
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        logger.warn("Violazione dei vincoli sui parametri: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Handles malformed or unreadable JSON request bodies.
     *
     * @param ex the HTTP message not readable exception
     * @return a {@link ResponseEntity} with a 400 Bad Request status
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        logger.warn("Body della richiesta malformato o illeggibile: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "Il corpo della richiesta JSON non è valido o è malformato.");
    }

    /**
     * Handles parameter type mismatch exceptions in endpoint path variables or
     * parameters.
     *
     * @param ex the method argument type mismatch exception
     * @return a {@link ResponseEntity} with a 400 Bad Request status and
     *         descriptive message
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = String.format("Il parametro '%s' deve essere di tipo %s",
                ex.getName(), ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "valido");
        logger.warn("Tipo di parametro errato: {}", message);
        return buildResponse(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * Handles requests made with unsupported HTTP methods.
     *
     * @param ex the HTTP request method not supported exception
     * @return a {@link ResponseEntity} with a 405 Method Not Allowed status
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        logger.warn("Metodo HTTP non supportato: {}", ex.getMethod());
        return buildResponse(HttpStatus.METHOD_NOT_ALLOWED,
                "Metodo HTTP " + ex.getMethod() + " non supportato per questo endpoint.");
    }

    /**
     * Handles authentication failures caused by invalid user credentials.
     *
     * @param ex the bad credentials exception thrown during authentication
     * @return a {@link ResponseEntity} with a 401 Unauthorized status and error
     *         message
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentialsException(BadCredentialsException ex) {
        logger.warn("Autenticazione fallita: credenziali non valide - {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", ex.getMessage()));
    }

    /**
     * Handles custom status exceptions explicitly thrown using
     * {@link ResponseStatusException}.
     *
     * @param ex the response status exception
     * @return a {@link ResponseEntity} matching the status code and reason from the
     *         exception
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException ex) {
        logger.warn("Eccezione di stato HTTP intercettata [{}]: {}", ex.getStatusCode(), ex.getReason());

        Map<String, Object> body = createBaseBody(HttpStatus.valueOf(ex.getStatusCode().value()));
        body.put("message", ex.getReason());

        return new ResponseEntity<>(body, ex.getStatusCode());
    }

    /**
     * Fallback handler for all unexpected or unhandled server exceptions.
     *
     * @param ex the unhandled exception
     * @return a {@link ResponseEntity} with a 500 Internal Server Error status
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        logger.error("Errore interno del server non gestito: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Si è verificato un errore interno al server.");
    }

    /**
     * Creates a standardized base map for error response bodies.
     *
     * @param status the HTTP status to attach
     * @return a map containing timestamp, status code, and reason phrase
     */
    private Map<String, Object> createBaseBody(HttpStatus status) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        return body;
    }

    /**
     * Builds a standardized error response entity with a custom error message.
     *
     * @param status  the HTTP status code
     * @param message the descriptive error message
     * @return a configured {@link ResponseEntity}
     */
    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
        Map<String, Object> body = createBaseBody(status);
        body.put("message", message);
        return new ResponseEntity<>(body, status);
    }
}