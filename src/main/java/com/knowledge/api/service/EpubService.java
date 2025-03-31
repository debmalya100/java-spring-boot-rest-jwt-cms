package com.knowledge.api.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

// import org.apache.commons.lang3.StringEscapeUtils;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.knowledge.api.exception.CustomException;
import com.knowledge.api.repository.ChannelRepository;
import com.knowledge.api.repository.CommonRepository;
import com.knowledge.api.repository.EpubRepository;
import com.knowledge.api.utils.CommonUtils;

@Service
public class EpubService {
    
    @Autowired
    private CommonRepository commonRepository;

    @Autowired
    private EpubRepository epubRepository;

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private ChannelRepository channelRepository;

    @Autowired
    private CommonUtils commonUtils;

    public Map<String, Object> getEpubDetail(Integer userMasterId , String clientIds , Integer id) {

        try {

            CompletableFuture<Integer> userTypeFuture = commonRepository.getUserType(userMasterId); 
            CompletableFuture<String> userEnvFuture = commonRepository.getUserEnv(userMasterId); 
            

            /**
             * userType and userEnv
             */
            CompletableFuture.allOf(userTypeFuture, userEnvFuture).join();
            Integer userType = userTypeFuture.get();
            String userEnv = userEnvFuture.get();


            /**
             * getting user package
             */
            // CompletableFuture<Map<String, Object>> userPackageFuture = userEnvFuture.thenCompose(env -> 
            //     commonRepository.checkUserPackage(userMasterId, "epub", env)
            // );

            // Map<String, Object> userPackage = userPackageFuture.get();


            /**
             * Async epubDetail and base data  
             */
            CompletableFuture<Map<String, Object>> epubDetailFuture = epubRepository.getEpubDetailData(id , userType , userEnv );
            CompletableFuture<Map<String , Object>> ContentRatingFuture = commonRepository.getContentRatings(id , userMasterId , "epub");
            CompletableFuture<Integer> ContentCommentCountFuture = commonRepository.getCommentCount(id , "epub");
            CompletableFuture<Optional<Integer>> ContentVaultFuture = commonRepository.getVaultStatus(id , userMasterId ,"epub");
            CompletableFuture<Map<String , Object>> ContentCampaignFuture = campaignService.getContentCampaign(userMasterId , id , "epub");
            CompletableFuture<Map<String, Object>> userPackageFuture = commonRepository.checkUserPackage(userMasterId, "epub", userEnv);
            CompletableFuture<Map<String , Object>> channelFuture = channelRepository.getChannelData(String.valueOf(id), userMasterId, "epub");
            CompletableFuture<List<Map<String,Object>>> authorDetailFuture = epubRepository.getEpubAuthorDetails(Integer.parseInt(String.valueOf(id).toString()));


            CompletableFuture.allOf(epubDetailFuture , ContentRatingFuture ,ContentCommentCountFuture, ContentVaultFuture , ContentCampaignFuture , userPackageFuture , authorDetailFuture).join();

            Map<String, Object> epubDetail = epubDetailFuture.get();
            Map<String , Object> ContentRating = ContentRatingFuture.get();
            Integer contentComment = ContentCommentCountFuture.get();
            Optional<Integer> contentVault = ContentVaultFuture.get();
            Map<String , Object> ContentCampaignData = ContentCampaignFuture.get();
            Map<String, Object> userPackage = userPackageFuture.get();
            Map<String, Object> channelData = channelFuture.get();
            List<Map<String,Object>> authorDetail = authorDetailFuture.get();

            /**
             * building content campaign data
             */
            Boolean bannerDisplay = (Boolean) ContentCampaignData.get("banner_display");
            Object creativeData = ContentCampaignData.get("creative_data");

            

            CompletableFuture<List<Map<String , Object>>> specilityEntitiesFuture = commonRepository.getSpecilityEntities(String.valueOf(epubDetail.get("specialityIds")));
            CompletableFuture<Map<String , Object>> userPaymentStatusFuture = commonRepository.getUserContentPaymentStatus( id , 6 , userMasterId );

            CompletableFuture.allOf(specilityEntitiesFuture , userPaymentStatusFuture).join();

            List<Map<String , Object>> specilityEntities = specilityEntitiesFuture.get();
            Map<String , Object> userPaymentStatus = userPaymentStatusFuture.get();



            /**
             * building response data
             */

            Map<String , Object> response = new HashMap<>();

            response.put("type_id" , epubDetail.get("type_id"));
            response.put("type" , "epub");
            response.put("title" , epubDetail.get("title"));
            response.put("description" , epubDetail.get("description"));
            response.put("epub_file" , epubDetail.get("epub_file"));
            response.put("image" , commonUtils.changeImgSrc(epubDetail.get("epub_img_thumbnail")));
            response.put("specialities_ids_and_names" , specilityEntities);
            response.put("is_locked" ,userPackage);
            response.put("price", epubDetail.get("price") != null ? Integer.parseInt(epubDetail.get("price").toString()) : 0);
            response.put("user_content_payment" , userPaymentStatus);
            response.put("is_share" , epubDetail.get("is_share"));
            response.put("sponsor_logo" , commonUtils.changeImgSrc(epubDetail.get("sponsor_logo")));
            response.put("rating" , ContentRating.get("rating"));
            response.put("myrating" , ContentRating.get("myRating"));
            response.put("vault" , contentVault);
            response.put("comment_count" , contentComment);
            response.put("deeplink" , epubDetail.get("deeplink"));
            response.put("is_converted" , epubDetail.get("is_converted"));
            response.put("channel" , channelData);
            response.put("author_entities" , authorDetail);
            response.put("disclaimer" , commonUtils.getDisclaimer("epub"));
            response.put("display_banner", bannerDisplay);
            response.put("campaign_data", creativeData);

           

            // response.put("contentRating" , ContentRating);
            // response.put("contentComment" , contentComment);
            // response.put("contentVault" , contentVault);
            // response.put("bannerDisplay" , bannerDisplay);
            // response.put("creativeData" , creativeData);
            // response.put("userPaymentStatus" , userPaymentStatus);
            // response.put("userPackage" , userPackage);


            // System.out.println("specilityEntities: " + specilityEntities);
          return response;  
        } catch (Exception e) {
            // e.printStackTrace();
            Throwable rootCause = e.getCause();
            if (rootCause instanceof CustomException) {
                CustomException customException = (CustomException) rootCause;
                throw customException; 
            } else {
                throw new RuntimeException("Failed to get data", rootCause);
            }
        }
        
    }
}
