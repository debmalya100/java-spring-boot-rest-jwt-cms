package com.knowledge.api.service;

import com.knowledge.api.repository.ChannelRepository;
import com.knowledge.api.repository.KnowledgeRepository;
import com.knowledge.api.repository.CampaignRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class KnowledgeService {

    private final KnowledgeRepository knowledgeRepository;
    private final ChannelRepository channelRepository;
    private final CampaignRepository campaignRepository;

    private final RedisTemplate<String, Object> redisTemplate;

    public Map<String, Object> getDetailFeed(String typeId,
            Integer userMasterId, String clientIds) {

        if (!StringUtils.hasText(typeId)) {
            return Collections.emptyMap();
        }

        return getCompendiumDetail(typeId, userMasterId);

    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getCompendiumDetail(String typeId, Integer userMasterId) {
        String cacheName = "java_comp_detail_" + typeId;

        // Get user details
        Integer userType = knowledgeRepository.getUserType(userMasterId);
        String env = knowledgeRepository.getUserEnv(userMasterId);

        System.out.println("user env: " + env);

        // Check user package
        Map<String, Object> packageDetails = knowledgeRepository.checkUserPackage(userMasterId, "comp", env);
        // System.out.println("packageDetails: " + packageDetails);
        boolean isLocked = packageDetails != null && (boolean) packageDetails.get("content_access") == false;
        // Check
        // if
        // content_access
        // is
        // false

        if (userType == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> result;

        if (userType == 5) {
            // Internal user - no caching
            result = knowledgeRepository.getCompendiumDetails(typeId, userMasterId, userType, env);
        } else {
            // System.err.println("Cache miss for er " + cacheName);
            // Try cache first
            result = (Map<String, Object>) redisTemplate.opsForValue().get(cacheName);
            if (result == null) {
                System.err.println("Cache for ************ " + cacheName);
                result = knowledgeRepository.getCompendiumDetails(typeId, userMasterId, userType, env);
                if (result != null && !result.isEmpty()) { // Check if result is not empty
                    redisTemplate.opsForValue().set(cacheName, result, 1, TimeUnit.HOURS);
                }
            }
        }

        if (result != null && !result.isEmpty()) { // Check if result is not empty
            // Enhance result with additional data
            enhanceCompendiumResult(result, typeId, userMasterId, isLocked, env, packageDetails);
        }

        return result != null ? result : Collections.emptyMap();
    }

    public void enhanceCompendiumResult(Map<String, Object> result, String typeId,
            Integer userMasterId, boolean isLocked, String env, Map<String, Object> packageDetails) {
        try {
            // Fetch compendium speciality asynchronously
            CompletableFuture<List<Map<String, Object>>> compendiumSpecialityFuture = knowledgeRepository
                    .getCompendiumSpeciality(typeId);

            CompletableFuture<Map<String, Object>> channelFuture = channelRepository
                    .getChannelData(typeId, userMasterId, "comp");

            // Fetch other data asynchronously (ratings, comment count, vault status)
            CompletableFuture<Map<String, Object>> ratingsFuture = knowledgeRepository.getContentRatings(typeId,
                    userMasterId);
            CompletableFuture<Integer> commentCountFuture = knowledgeRepository.getCommentCount(typeId);
            CompletableFuture<Optional<Integer>> vaultStatusFuture = knowledgeRepository.getVaultStatus(typeId,
                    userMasterId);

            // Fetch campaign data asynchronously
            CompletableFuture<Map<String, Object>> campaignFuture = campaignRepository
                    .buildCampaignJSON(Integer.parseInt(typeId));

            // Wait for all futures to complete
            CompletableFuture.allOf(compendiumSpecialityFuture, ratingsFuture, commentCountFuture, vaultStatusFuture,
                    channelFuture,
                    campaignFuture)
                    .join();

            // Get results from futures
            List<Map<String, Object>> compendiumSpecialities = compendiumSpecialityFuture.get();
            Map<String, Object> ratings = ratingsFuture.get();
            int commentCount = commentCountFuture.get();
            Optional<Integer> vaultStatus = vaultStatusFuture.get();
            Map<String, Object> compendiumChannel = channelFuture.get();
            Map<String, Object> campaignData = campaignFuture.get();

            String disclamer = "All scientific content on the platform is provided for general medical " +
                    "education purposes meant for registered medical practitioners only. " +
                    "The content is not meant to substitute for the independent medical " +
                    "judgment of a physician relative to diagnostic and treatment options " +
                    "of a specific patient's medical condition. In no event will CLIRNET " +
                    "be liable for any decision made or action taken in reliance upon the " +
                    "information provided through this content.";

            // Add results to the result map
            result.putAll(ratings);
            result.put("comment_count", commentCount);
            result.put("channel", compendiumChannel);
            result.put("specialities_ids_and_names", compendiumSpecialities); // result
            result.put("vault", vaultStatus.orElse(0));
            result.put("campaign_data", campaignData); // Add campaign data to the result
            // Handle locked content
            result.put("disclaimer", disclamer);
            result.put("is_locked", packageDetails);

        } catch (InterruptedException | ExecutionException e) {
            // Handle exceptions
            e.printStackTrace();
        }
    }

    private CompletableFuture<List<Map<String, Object>>> fetchSurveysAsync(Integer userMasterId, String typeId) {
        return CompletableFuture.supplyAsync(() -> {
            // Fetch completed and incomplete surveys
            List<Map<String, Object>> completedSurveys = knowledgeRepository.getCompletedSurveys(userMasterId).join();
            List<Map<String, Object>> incompleteSurveys = knowledgeRepository.getIncompleteSurveys(userMasterId).join();

            // Merge survey IDs with null checks
            Set<String> excludedSurveyIds = new HashSet<>();
            completedSurveys.forEach(survey -> {
                Object surveyId = survey.get("survey_id");
                if (surveyId != null) {
                    excludedSurveyIds.add(surveyId.toString());
                }
            });
            incompleteSurveys.forEach(survey -> {
                Object surveyId = survey.get("survey_id");
                if (surveyId != null) {
                    excludedSurveyIds.add(surveyId.toString());
                }
            });

            String excludedSurveyIdsStr = String.join(",", excludedSurveyIds);

            // Fetch poll surveys
            List<Map<String, Object>> pollSurveys = knowledgeRepository
                    .getPollSurveys(typeId, userMasterId, excludedSurveyIdsStr).join();

            // Return empty list if no surveys are found
            if (pollSurveys == null || pollSurveys.isEmpty()) {
                return new ArrayList<>();
            }

            // Process and return poll surveys
            return processPollSurveys(pollSurveys);
        });
    }

    private List<Map<String, Object>> processPollSurveys(List<Map<String, Object>> pollSurveys) {
        List<Map<String, Object>> processedPollSurveys = new ArrayList<>();

        System.out.println("Poll Surveys: " + pollSurveys);

        // Return empty list if pollSurveys is null or empty
        if (pollSurveys == null || pollSurveys.isEmpty()) {
            return processedPollSurveys;
        }

        // Process each survey
        for (Map<String, Object> survey : pollSurveys) {
            Map<String, Object> processedSurvey = new HashMap<>();

            // Handle null values for all fields
            processedSurvey.put("survey_id", survey.getOrDefault("survey_id", ""));
            processedSurvey.put("category", survey.getOrDefault("category", ""));
            processedSurvey.put("point", survey.getOrDefault("survey_points", 0));
            processedSurvey.put("json_data", survey.getOrDefault("data", ""));
            processedSurvey.put("survey_title", survey.getOrDefault("survey_title", ""));
            processedSurvey.put("deeplink", survey.getOrDefault("deeplink", ""));

            // Handle null survey_description
            String surveyDescription = (String) survey.get("survey_description");
            if (surveyDescription != null) {
                processedSurvey.put("survey_description",
                        surveyDescription.substring(0, Math.min(150, surveyDescription.length())));
            } else {
                processedSurvey.put("survey_description", ""); // Default value for null description
            }

            processedSurvey.put("image", survey.getOrDefault("image", ""));
            processedSurvey.put("specialities_name", survey.getOrDefault("specialities_name", ""));
            processedSurvey.put("sponsor_name", survey.getOrDefault("sponsor", ""));
            processedSurvey.put("sponsor_logo", survey.getOrDefault("sponsor_logo", ""));
            processedSurvey.put("publishing_date", survey.getOrDefault("publishing_date", ""));

            processedPollSurveys.add(processedSurvey);
        }

        return processedPollSurveys;
    }
}