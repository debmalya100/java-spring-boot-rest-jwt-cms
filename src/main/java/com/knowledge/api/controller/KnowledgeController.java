package com.knowledge.api.controller;

import com.knowledge.api.model.ApiResponse;
import com.knowledge.api.model.UserDetails;
import com.knowledge.api.service.JwtService;
import com.knowledge.api.service.KnowledgeService;
import com.knowledge.api.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;
    private final DashboardService dashboardService;
    private final JwtService jwtService;

    @GetMapping("/feed/detail")
    public ResponseEntity<?> getFeedDetail(
            @RequestParam("type_id") String typeId,
            @RequestHeader("Authorization") String token) {

        // System.out.println("**************typeId************* " + typeId);
        // System.out.println("**************token************* " + token);

        UserDetails userDetails = null;
        try {
            userDetails = jwtService.extractUserDetails(token);
        } catch (Exception e) {
            System.err.println("Error extracting user details: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }

        // System.out.println("**************userDetails************* " + userDetails);
        // Rest of the method
        Map<String, Object> logData = new HashMap<>();
        logData.put("called_on", LocalDateTime.now());
        logData.put("process_start_time", LocalDateTime.now());
        logData.put("doctor_id", userDetails.getUserMasterId());

        Map<String, Object> result = knowledgeService.getDetailFeed(
                typeId, userDetails.getUserMasterId(),
                userDetails.getClientIds());

        HttpStatus status = (result.get("type_id") == null || result.get("type_id").toString().isEmpty())
                ? HttpStatus.NON_AUTHORITATIVE_INFORMATION
                : HttpStatus.OK;

        logData.put("response", status.value());
        logData.put("message", "Success");
        logData.put("response_on", LocalDateTime.now());

        return new ResponseEntity<>(
                new ApiResponse(result, "Success"),
                status);
    }

    @GetMapping("/feed/list")
    public ResponseEntity<?> getFeedlist(
            @RequestParam("from") int from,
            @RequestParam("to") int to,
            @RequestParam("spIds") int speciality,
            @RequestParam("type") String type,
            @RequestParam(value = "subtype", required = false, defaultValue = "") String subtype, // Optional
            @RequestParam(value = "convert", required = false, defaultValue = "0") int convert, // Optional
            @RequestHeader("Authorization") String token) {

        UserDetails userDetails = null;
        try {
            userDetails = jwtService.extractUserDetails(token);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }

        // System.out.println("**************userDetails************* " + userDetails);

        List<Map<String, Object>> result = dashboardService.getDataMl(type, userDetails.getUserMasterId(),
                userDetails.getClientIds(), from, to, speciality, convert, subtype);

        return new ResponseEntity<>(
                new ApiResponse(result, "Success"),
                HttpStatus.OK);
    }
}