package com.knowledge.api.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<?> handleMissingParameter(MissingServletRequestParameterException ex) {
        String parameterName = ex.getParameterName();
        String errorMessage = "Bad Request: The query parameter '" + parameterName + "' is required (case-sensitive).";
        Map<String,Object> rerturnMap = new HashMap<>();
        rerturnMap.put("error", errorMessage);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(rerturnMap);
    }
}