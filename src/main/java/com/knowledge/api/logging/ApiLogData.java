package com.knowledge.api.logging;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiLogData {
    private Integer user_master_id;
    private String version;
    private String api_called;
    private String method;
    private String requestParams;
    private Integer responseStatus;
    private String responseBody;
    private Long executionTime;
    private String ip_address;
    private String userAgent;
    private LocalDateTime timestamp;
}