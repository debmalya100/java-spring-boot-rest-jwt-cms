package com.knowledge.api.interceptor;

import com.knowledge.api.logging.ApiLogger;
import com.knowledge.api.logging.ApiLogData;
import com.knowledge.api.model.UserDetails;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ApiLoggingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ApiLoggingInterceptor.class);
    private final ApiLogger apiLogger;
    private static final String START_TIME_ATTRIBUTE = "startTime";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_ATTRIBUTE, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
            Object handler, Exception ex) throws Exception {

        try {
            if (shouldSkipLogging(request)) {
                return;
            }

            // Safely get startTime with fallback
            Long startTime = (Long) request.getAttribute(START_TIME_ATTRIBUTE);
            long executionTime = startTime != null ? System.currentTimeMillis() - startTime : 0;

            // Get authentication from security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            log.debug("Authentication object: {}", authentication);

            Integer userId = 0;
            if (authentication != null && authentication.getPrincipal() != null) {
                Object principal = authentication.getPrincipal();
                log.debug("Principal class: {}", principal.getClass().getName());

                if (principal instanceof UserDetails) {
                    UserDetails userDetails = (UserDetails) principal;
                    userId = userDetails.getUserMasterId();
                    log.debug("Extracted userId from UserDetails: {}", userId);
                }
            }

            ApiLogData logData = ApiLogData.builder()
                    .user_master_id(userId)
                    .version(request.getHeader("version"))
                    .api_called(request.getRequestURI())
                    .method(request.getMethod())
                    .requestParams(request.getQueryString())
                    .responseStatus(response.getStatus())
                    .executionTime(executionTime)
                    .ip_address(getClientIp(request))
                    .userAgent(request.getHeader("User-Agent"))
                    .timestamp(LocalDateTime.now())
                    .build();

            log.debug("Created ApiLogData: {}", logData);
            apiLogger.logApiCall(logData);

        } catch (Exception e) {
            log.error("Error in API logging: ", e);
            // Continue processing even if logging fails
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0];
        }
        return request.getRemoteAddr();
    }

    private boolean shouldSkipLogging(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.contains("/actuator") ||
                path.contains("/swagger") ||
                path.contains("/v3/api-docs");
    }
}