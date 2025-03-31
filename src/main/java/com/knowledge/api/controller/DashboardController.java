package com.knowledge.api.controller;

import com.knowledge.api.model.ApiResponse;
import com.knowledge.api.model.UserDetails;
import com.knowledge.api.service.DashboardService;
import com.knowledge.api.service.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final JwtService jwtService;

    // Manually define the constructor
    public DashboardController(DashboardService dashboardService, JwtService jwtService) {
        this.dashboardService = dashboardService;
        this.jwtService = jwtService;
    }

    @GetMapping("/dataMl")
    public ResponseEntity<?> getDataMl(
            @RequestParam("from") int from,
            @RequestParam("to") int to,
            @RequestParam("speciality") int speciality,
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

        List<Map<String, Object>> result = dashboardService.getDataMl(type, userDetails.getUserMasterId(),
                userDetails.getClientIds(), from, to, speciality, convert, subtype);

        // System.out.println("result: " + result);

        return new ResponseEntity<>(
                new ApiResponse(result, "Success"),
                HttpStatus.OK);
    }
}