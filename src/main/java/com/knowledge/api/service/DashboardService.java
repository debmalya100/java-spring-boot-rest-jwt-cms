package com.knowledge.api.service;

import com.knowledge.api.repository.DashboardRepository;
import com.knowledge.api.repository.CommonRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
@EnableCaching
public class DashboardService {

    private final DashboardRepository dashboardRepository;
    private final CommonRepository commonRepository;

    // @Cacheable(value = "dashboardData", key = "'dashboard_' + #type + '_' + #from
    // + '_' + #to + '_' + #speciality + '_' + #convert + '_' + #subtype")
    // + '_' + #to + '_' + #speciality + '_' + #convert + '_' + #subtype")
    public List<Map<String, Object>> getDataMl(String type, Integer userMasterId, String clientIds, int from, int to,
            int speciality, int convert, String subtype) {

        try {

            // Use the default values if subtype or convert are not provided
            if (subtype == null || subtype.isEmpty()) {
                subtype = ""; // Default value for subtype
            }
            if (convert == 0) {
                convert = 0; // Default value for convert
            }
            // Fetch compendium speciality asynchronously
            // Fetch user ML data
            String userMlData = dashboardRepository.getUserMlData(userMasterId);
            // System.out.println("userMlData: " + userMlData);
            // Get user details
            CompletableFuture<Integer> userTypeFuture = commonRepository.getUserType(userMasterId);
            CompletableFuture<String> envFuture = commonRepository.getUserEnv(userMasterId);

            try {
                CompletableFuture.allOf(userTypeFuture, envFuture).join();
            } catch (Exception e) {
                e.printStackTrace();
                return Collections.emptyList(); // Return an empty list or handle the error appropriately
            }

            Integer userType = userTypeFuture.get();
            String env = envFuture.get();

            System.out.println("env-------------------: " + env);

            // Fetch data based on type
            switch (type) {
                case "comp": // Compendium

                    System.out.println("type: " + type);
                    // Map<String, Object> results;
                    List<Map<String, Object>> compendiumData = dashboardRepository.getCompendiumData(userMasterId, from,
                            to,
                            speciality, userMlData, env, convert, subtype);
                    // Map<String, Object> result = new HashMap<>();
                    // result.put("readData", readData);
                    return compendiumData;
                case "read":

                    System.out.println("type: " + type);
                    // Map<String, Object> results;
                    List<Map<String, Object>> readData = dashboardRepository.getReadData(userMasterId, from, to,
                            speciality, userMlData, env, convert, subtype);
                    // Map<String, Object> result = new HashMap<>();
                    // result.put("readData", readData);
                    return readData;

                case "cme":

                    System.out.println("type: " + type);
                    // Map<String, Object> results;
                    List<Map<String, Object>> cmeData = dashboardRepository.getSessionData(userMasterId, clientIds,
                            from, to,
                            speciality, userMlData, env, convert, subtype);
                    // Map<String, Object> result = new HashMap<>();
                    // result.put("readData", readData);
                    return cmeData;

                case "opinions":

                    System.out.println("type: " + type);
                    // Map<String, Object> results;
                    List<Map<String, Object>> opinionsData = dashboardRepository.getOpinionsData(userMasterId, from, to,
                            speciality, userMlData, env);
                    // Map<String, Object> result = new HashMap<>();
                    // result.put("readData", readData);
                    return opinionsData;
                case "learn":

                    System.out.println("type: " + type);
                    // Map<String, Object> results;
                    List<Map<String, Object>> learnData = dashboardRepository.getLearnData(userMasterId, from, to,
                            speciality, userMlData, env);
                    // Map<String, Object> result = new HashMap<>();
                    // result.put("readData", readData);
                    return learnData;
                case "watch":

                    System.out.println("type: " + type);
                    // Map<String, Object> results;
                    List<Map<String, Object>> watchData = dashboardRepository.getWatchData(userMasterId, from, to,
                            speciality, userMlData, env);
                    // Map<String, Object> result = new HashMap<>();
                    // result.put("readData", readData);
                    return watchData;

                case "all":
                    System.out.println("type: " + type);
                    // Map<String, Object> results;
                    List<Map<String, Object>> allData = dashboardRepository.getAllData(userMasterId, clientIds, from,
                            to,
                            speciality, userMlData, env, convert, subtype);
                    // Map<String, Object> result = new HashMap<>();
                    // result.put("readData", readData);
                    return allData;

            }

        } catch (InterruptedException | ExecutionException e) {
            // Handle exceptions
            e.printStackTrace();
        }
        // Return an empty map or handle the error appropriately
        return Collections.emptyList();
    }

    @Scheduled(fixedRate = 3600000) // Clear cache every hour
    @CacheEvict(value = "dashboardData", allEntries = true)
    public void clearCache() {
        // Cache cleared automatically by annotation
    }
}