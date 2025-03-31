package com.knowledge.api.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApiLogger {
    private static final Logger logger = LoggerFactory.getLogger(ApiLogger.class);
    private final ObjectMapper objectMapper;

    public void logApiCall(ApiLogData logData) {
        try {
            String logMessage = objectMapper.writeValueAsString(logData);
            logger.info("{}", logMessage);
        } catch (Exception e) {
            // Silent catch - no logging of errors
        }
    }
}