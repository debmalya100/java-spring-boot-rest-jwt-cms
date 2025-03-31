package com.knowledge.api.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OpenAiService {
    
    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.base}")
    private String apiBase;

    @Value("${openai.api.version}")
    private String apiVersion;

    @Value("${openai.api.engine}")
    private String engine;


    @Value("${openai.api.model}")
    private String model;

    public double[] getEmbedding(String text) throws Exception {    

        // String url = apiBase + "openai/deployments/" + engine + "/embeddings?api-version=" + apiVersion;
        String url = "https://api.openai.com/v1/embeddings?api-version=" + apiVersion;
    

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("Content-Type", "application/json");

        
        Map<String, Object> payload = new HashMap<>();
        payload.put("input", text);
        payload.put("model", model);

       
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);

        // Send request
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, Map.class);

        Map<String, Object> responseBody = response.getBody();
        
        if (responseBody != null && responseBody.containsKey("data")) {
            List<Map<String, Object>> data = (List<Map<String, Object>>) responseBody.get("data");
            if (!data.isEmpty()) {
                Map<String, Object> embeddingData = data.get(0);
                List<Double> embedding = (List<Double>) embeddingData.get("embedding");
                return embedding.stream().mapToDouble(Double::doubleValue).toArray();
            }
        }

        throw new RuntimeException("Failed to get embedding from OpenAI API");
    }
}
