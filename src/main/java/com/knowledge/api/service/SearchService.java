package com.knowledge.api.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.knowledge.api.repository.EpubRepository;
import com.knowledge.api.repository.KnowledgeRepository;
import com.knowledge.api.repository.VideoRepository;
import com.knowledge.api.utils.CommonUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SearchService {
    @Autowired
    private OpenAiService openAiService;

    @Autowired
    private PineconeService pineconeService;

   
    private final KnowledgeRepository knowledgeRepository;
    private final EpubRepository epubRepository;
    private final VideoRepository videoRepository;

    @Autowired
    private CommonUtils commonUtils;
    

    public List<Map<String , Object>> getRagData(String text , Integer limit , String filter , String speciality ) {
        try {

            List<String> filters = null;
            if( filter != null ) {
                filters = new ArrayList<>(List.of(filter.split(",")));
            }

            List<String> specialities = null;
            if( speciality != null ) {
                specialities = new ArrayList<>(List.of(speciality.split(",")));
            }


            double[] embedding = openAiService.getEmbedding(text);
            Map<String, Object> similiarData = pineconeService.getSimiliarData(embedding , Optional.of(limit) , Optional.ofNullable(filters) , Optional.ofNullable(specialities));
            
            List<Map<String, Object>> matches = (List<Map<String, Object>>) similiarData.get("matches");
            List<Map<String, Object>> testResult = new ArrayList<>();
            
            for (Map<String, Object> match : matches) {
                Map<String, Object> metadata = (Map<String, Object>) match.get("metadata");

                String content = (String) metadata.get("content");
                Integer contentId = (Integer) metadata.get("content_id");
                Object Speciality =  commonUtils.parsePineconeSpecility( metadata.get("speciality") );
                String title = (String) metadata.get("title");

                Map<String , Object> map = new HashMap<>();
                map.put("content", content);
                map.put("content_id", contentId);
                map.put("specialities", Speciality);
                map.put("title", title);
                
                testResult.add(map);
            }
            return testResult;
        } catch (Exception e) {
            return null;
        }
        
        
    }

    public List<Map<String , Object>> getSearchData(String text , Integer limit ) {
        try {


            double[] embedding = openAiService.getEmbedding(text);
            Map<String, Object> similiarData = pineconeService.getSimiliarData(embedding , Optional.of(limit) , Optional.empty() ,  Optional.empty());
            
            List<Map<String, Object>> matches = (List<Map<String, Object>>) similiarData.get("matches");
            
            Map<String, List<Integer>> contentGroup = new HashMap<>();

            
            for (Map<String, Object> match : matches) {
                Map<String, Object> metadata = (Map<String, Object>) match.get("metadata");

                String content = (String) metadata.get("content");
                
                Object contentIdObj = metadata.get("content_id");
                int contentId = 0;  
                if (contentIdObj instanceof String) {
                    try {
                        contentId = Integer.parseInt((String) contentIdObj);
                    } catch (NumberFormatException e) {
                        System.out.println("Error parsing content_id (String): " + e.getMessage());
                    }
                } else if (contentIdObj instanceof Integer) {
                    contentId = (Integer) contentIdObj; 
                } else {
                    System.out.println("Unexpected content_id type: " + contentIdObj.getClass());
                }

                contentGroup.computeIfAbsent(content, k -> new ArrayList<>()).add(contentId);
            }

          

            CompletableFuture<String> medwikiFuture = CompletableFuture.supplyAsync(() ->{
                if (contentGroup.containsKey("medwiki")) {
                    return contentGroup.get("medwiki").stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(", "));
                } 

                return null;
            });


            CompletableFuture <String> ebookFuture = CompletableFuture.supplyAsync(() ->{
                if (contentGroup.containsKey("ebook")) {
                    return contentGroup.get("ebook").stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(", "));
                } 
                return null;
            });


            CompletableFuture<String> videoFuture = CompletableFuture.supplyAsync(() ->{
                if (contentGroup.containsKey("video")) {
                    return contentGroup.get("video").stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(", "));
                } 
                return null;
            });


            CompletableFuture<Void> allFutures = CompletableFuture.allOf(medwikiFuture, ebookFuture, videoFuture);

            Map<String , String> futereResult = allFutures.thenApply((voidResult) -> {

                Map<String , String> idResults = new HashMap<>();
                try {
                    String CommaSeparatedMedwiki = medwikiFuture.get();
                    String CommaSeparatedEbook = ebookFuture.get();
                    String CommaSeparatedVideo = videoFuture.get();

                    idResults.put("medwiki", CommaSeparatedMedwiki);
                    idResults.put("epaper", CommaSeparatedEbook);
                    idResults.put("video", CommaSeparatedVideo);

                   
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return idResults;
            }).join();



            CompletableFuture<List<Map<String, Object>>> compFutureRes = 
                    futereResult.get("medwiki") != null
                        ? knowledgeRepository.getCompSearchData(futereResult.get("medwiki"))
                        : CompletableFuture.completedFuture(null);

            CompletableFuture<List<Map<String, Object>>> epubFutureRes =
                    futereResult.get("epaper") != null
                            ? epubRepository.getEpubSearchData(futereResult.get("epaper"))
                            : CompletableFuture.completedFuture(null);

            CompletableFuture<List<Map<String, Object>>> videobFutureRes =
                    futereResult.get("video") != null
                            ? videoRepository.getVideoSearchData(futereResult.get("video"))
                            : CompletableFuture.completedFuture(null);



            CompletableFuture.allOf(compFutureRes , epubFutureRes , videobFutureRes).join();

            List<Map<String, Object>> compData  = compFutureRes.get();
            List<Map<String, Object>> epubData  = epubFutureRes.get();
            List<Map<String, Object>> videoData = videobFutureRes.get();

            List<Map<String,Object>> combineData = new ArrayList<>();


            // removing null values if any from compData, epubData, videoData 
            // beacuse if any of these are null the combineData will be null
            Optional.ofNullable(compData).ifPresent(combineData::addAll);
            Optional.ofNullable(epubData).ifPresent(combineData::addAll);
            Optional.ofNullable(videoData).ifPresent(combineData::addAll);

            
            return combineData;

        } catch (Exception e) {
            return null;
        }
        
        
    }
}
