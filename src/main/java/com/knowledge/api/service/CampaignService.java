package com.knowledge.api.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.knowledge.api.exception.CustomException;
import com.knowledge.api.repository.CampaignRepository;
import com.knowledge.api.repository.CommonRepository;


@Service
public class CampaignService {
    

    @Autowired
    private CommonRepository commonRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Async
    public CompletableFuture<Map<String , Object>> getContentCampaign(int userMasterId , int contentId , String contentType) {
        
        try {
            
            Map<String , Object> returnData = new HashMap<>();

            returnData.put("banner_display" , false);
            returnData.put("creative_data" , null);

            CompletableFuture<String> userGroupFuture = commonRepository.getUserGroup(userMasterId);
            CompletableFuture<Integer> userTypeFuture = commonRepository.getUserType(userMasterId);
            // CompletableFuture<String> userSpecialityFuture = commonRepository.getUserInterestSpecilaity(userMasterId);
            CompletableFuture<List<Integer>> excludeCampaignFuture = campaignRepository.getExcludeCampaigns(contentType);

            CompletableFuture.allOf(userGroupFuture , userTypeFuture , excludeCampaignFuture).join();


            String userGroup = userGroupFuture.get();
            Integer userType = userTypeFuture.get();
            List<Integer> excludeCampaigns = excludeCampaignFuture.get();

            if(excludeCampaigns.contains(contentId)) {
                return CompletableFuture.completedFuture(returnData);
            }

            List<Integer> campaignIds = campaignRepository.getAContentAllCampiagns(userGroup , userType , contentId , contentType); 


            List<CompletableFuture<Map<String, Object>>> futureList = new ArrayList<>();
            if(campaignIds.size() > 0) {

                for (Integer campaignId : campaignIds) {

                    /**
                     * not waiting for the result to complete (non-blocking within the loop)
                     */
                    CompletableFuture<Map<String , Object>> campaignFuture = campaignRepository.buildCampaignJSON(campaignId)
                        .thenApply(campaign -> {
                            return campaign;
                        }).exceptionally(ex -> {
                            throw new CustomException( 500 , "Something went wrong" , ex);
                        });

                    futureList.add(campaignFuture);
                }

                CompletableFuture<Void> allOf = CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0]));

                return allOf.thenApply( data -> {
                    // This block runs after all the futures have completed
                    List<Map<String, Object>> allCampaigns = new ArrayList<>();
                    for (CompletableFuture<Map<String, Object>> future : futureList) {
                        Map<String, Object> campaign = future.join();  
                        if (campaign != null) {
                            allCampaigns.add(campaign);
                        }
                    }

                    
                    returnData.put("banner_display", true);
                    returnData.put("creative_data", allCampaigns);
                    return returnData;
                });

            }
            
            return CompletableFuture.completedFuture(returnData);

        } catch(EmptyResultDataAccessException e) {
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            // e.printStackTrace();
            throw new CustomException( 500 , "Something went wrong" , e);
        }
        
        
    }
}
