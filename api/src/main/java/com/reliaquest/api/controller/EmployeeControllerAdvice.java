package com.reliaquest.api.controller;

import com.reliaquest.api.exception.EmployeeNotFoundException;
import com.reliaquest.api.exception.RateLimitExceededException;
import com.reliaquest.api.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class EmployeeControllerAdvice {

    @ExceptionHandler
    protected ResponseEntity<?> handleException(Throwable ex) {
        log.error("Error handling web request.", ex);
        return ResponseEntity.internalServerError().body(Response.error(ex.getMessage()));
    }

    @ExceptionHandler(RateLimitExceededException.class)
    protected ResponseEntity<?> handleRateLimitExceeded(RateLimitExceededException ex) {
        log.error("Error handling web request.", ex);
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Response.error(ex.getMessage()));
    }

    @ExceptionHandler(EmployeeNotFoundException.class)
    protected ResponseEntity<?> handleEmployeeNotFound(EmployeeNotFoundException ex) {
        log.error("Error handling web request.", ex);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Response.error(ex.getMessage()));
    }
}
