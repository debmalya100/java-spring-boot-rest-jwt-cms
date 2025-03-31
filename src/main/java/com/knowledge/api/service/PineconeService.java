package com.knowledge.api.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;




@Service
public class PineconeService {
    
    @Value("${pinecone.api.key}")
    private String pineconeApiKey;

    @Value("${pinecone.api.url}")
    private String pineconeUrl;

    public Map<String , Object> getSimiliarData(double[] embedding , Optional<Integer> top_k , Optional<List<String>> filter , Optional<List<String>> speciality) {

        int actualTop_k = top_k.orElse(100); 

        // for filter
        Map<String, Object> filterMap = Map.of(
            "$in", filter.orElseGet(() -> Arrays.asList("ebook", "video", "medwiki"))
        );

        Map<String, Object> filterBody = new HashMap<>();
        filterBody.put("content", filterMap);


        // for specility 
        if (speciality.isPresent() && !speciality.get().isEmpty()) {
            filterBody.put("speciality", Map.of("$in", speciality.get()));
        } 

       

        Map<String, Object> requestBody = Map.of(
            "vector", embedding,
            "top_k", actualTop_k,
            "include_metadata", true,
            "filter", filterBody
        );

        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Api-Key", pineconeApiKey); 
        headers.set("Content-Type", "application/json");  

        
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Map> response = restTemplate.exchange(pineconeUrl, HttpMethod.POST, requestEntity, Map.class);


        // getting the response
        Map<String, Object> responseBody = response.getBody();

        // System.out.println("Pinecone Response: " + responseBody);

        return responseBody;
    }

}
