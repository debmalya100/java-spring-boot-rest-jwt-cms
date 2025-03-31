package com.knowledge.api.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.knowledge.api.service.SearchService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/api/search")
public class SearchController {
    

    @Autowired
    private SearchService serachService;

    @GetMapping("/rag")
    public ResponseEntity<Map<String, Object>> searchRag(
        @RequestParam(name="query") String text,
        @RequestParam Integer limit ,
        @RequestParam(required = false ) String filter ,
        @RequestParam(required = false ) String speciality 
        
        ) {

       Map<String, Object> result = new HashMap<>();
        try {
           
            List<Map<String , Object>> out = serachService.getRagData(text , limit , filter , speciality);

            result.put("status", "success");
            result.put("param", text);
            result.put("data", out);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }


    @GetMapping("/data")
    public ResponseEntity<Map<String, Object>> serach(@RequestParam String text, @RequestParam(required = true) Integer limit ) {

     
       Map<String, Object> result = new HashMap<>();
        try {
           
            List<Map<String , Object>> out = serachService.getSearchData(text , limit);

            result.put("status", "success");
            result.put("param", text);
            result.put("data", out);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }
    
}
