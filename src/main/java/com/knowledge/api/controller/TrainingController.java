package com.knowledge.api.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.knowledge.api.exception.CustomException;
import com.knowledge.api.model.UserDetails;
import com.knowledge.api.service.EpubService;
import com.knowledge.api.service.JwtService;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import com.knowledge.api.service.TrainingService;



@RestController
@RequestMapping("/api/v1/training")
public class TrainingController {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private TrainingService trainingService;


    @GetMapping("/detail")
    public ResponseEntity<?> detail(@RequestParam(required=true , name="id") Integer Id,
                @RequestHeader("Authorization") String token
    ) {

        try {
            
            UserDetails userDetails = null;
            try {
                userDetails = jwtService.extractUserDetails(token);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
            }

            Map<String, Object> detail = trainingService.getTraingDetails(userDetails.getUserMasterId() , userDetails.getClientIds() , Id);
            
            Map<String , Object> response = new LinkedHashMap<>();
            response.put("status" , HttpStatus.OK.value());
            response.put("message" , "Success");
            response.put("data" , detail);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String,Object> response = new HashMap<>();
            Throwable rootCause = e.getCause();
            if (rootCause instanceof CustomException) {
                CustomException customException = (CustomException) rootCause;
                response.put("status" , customException.getStatusCode());
                response.put("message" , customException.getErrorMessage());
                return ResponseEntity.status(customException.getStatusCode()).body(response);
            }

            response.put("status" , HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.put("message" , "Oops! Something went wrong");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
}
