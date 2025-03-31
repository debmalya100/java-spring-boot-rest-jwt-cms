package com.knowledge.api.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.api.exception.CustomException;
import com.knowledge.api.model.VideoDetailResponse;
import com.knowledge.api.repository.ChannelRepository;
import com.knowledge.api.repository.CommonRepository;
import com.knowledge.api.repository.VideoRepository;
import com.knowledge.api.utils.CommonUtils;

@Service
public class VideoService {

    @Autowired
    private CommonRepository commonRepository; 

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private ChannelRepository channelRepository;    

    @Autowired
    private CampaignService campaignService;      

    @Autowired
    private CommonUtils commonUtils;

    @Autowired
    private ObjectMapper objectMapper;
    public Map<String , Object> getVideoDetail(Integer userMasterId , String clientIds , Integer Id) {

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
            CompletableFuture<Map<String, Object>> userPackageFuture = userEnvFuture.thenCompose(env -> 
                commonRepository.checkUserPackage(userMasterId, "video_archived", env)
            );


            Map<String, Object> userPackage = userPackageFuture.get();


            /**
             * Async VideoDetail and base data  
             */
            CompletableFuture<Map<String , Object>> VideoDetailsFuture = videoRepository.getClinicalVideoDetail(Id , userMasterId , userEnv , userType);
            CompletableFuture<Map<String , Object>> ContentRatingFuture = commonRepository.getContentRatings(Id , userMasterId , "video_archive");
            CompletableFuture<Integer> ContentCommentCountFuture = commonRepository.getCommentCount(Id , "video_archive");
            CompletableFuture<Optional<Integer>> ContentVaultFuture = commonRepository.getVaultStatus(Id , userMasterId ,"video_archive");
            CompletableFuture<Map<String , Object>> ContentCampaignFuture = campaignService.getContentCampaign(userMasterId , Id , "video_archive");
        
            CompletableFuture.allOf(VideoDetailsFuture, ContentRatingFuture ,ContentCommentCountFuture , ContentVaultFuture , ContentCampaignFuture ).join();
            
            Map<String , Object> videoDetail = VideoDetailsFuture.get();
            Map<String , Object> ContentRating = ContentRatingFuture.get();
            Integer contentComment = ContentCommentCountFuture.get();
            Optional<Integer> contentVault = ContentVaultFuture.get();
            Map<String , Object> ContentCampaignData = ContentCampaignFuture.get();


            /**
             * building content campaign data
             */
            Boolean bannerDisplay = (Boolean) ContentCampaignData.get("banner_display");
            Object creativeData = ContentCampaignData.get("creative_data");

            /**
             * getting specility and the other data after getting query result
             */
            String specialityId = String.valueOf(videoDetail.get("specialityIds"));

            CompletableFuture<List<Map<String , Object>>> specilityEntitiesFuture = commonRepository.getSpecilityEntities(specialityId);
            CompletableFuture<Map<String , Object>> channelFuture = channelRepository.getChannelData(String.valueOf(Id), userMasterId, "session");
            CompletableFuture<List<Map<String , Object>>> sessionDoctorEntitiesFuture = commonRepository.getSessionDoctorEntities(String.valueOf(videoDetail.get("session_doctor_id")));
            CompletableFuture<Map<String , Object>> userPaymentStatusFuture = commonRepository.getUserContentPaymentStatus( Id , 3 , userMasterId );

            CompletableFuture.allOf(specilityEntitiesFuture, channelFuture , sessionDoctorEntitiesFuture , userPaymentStatusFuture).join();

            List<Map<String , Object>> specilityEntities = specilityEntitiesFuture.get();
            Map<String , Object> channel = channelFuture.get();
            List<Map<String , Object>> sessionDoctorEntities = sessionDoctorEntitiesFuture.get();
            Map<String , Object> userPaymentStatus = userPaymentStatusFuture.get();

           
            /**
             * VideoDetailResponse
             */

            Map<String , Object> response = new HashMap<>();

            response.put("type_id", videoDetail.get("type_id"));
            response.put("con_type", videoDetail.get("type"));
            response.put("is_share", videoDetail.get("is_share"));
            response.put("is_like", videoDetail.get("is_like"));
            response.put("is_commentable", videoDetail.get("is_comment"));
            response.put("is_locked", userPackage);
            response.put("user_content_payment", userPaymentStatus);
            response.put("src", videoDetail.get("src"));
            response.put("question", videoDetail.get("video_archive_question_raw"));
            response.put("answer", videoDetail.get("video_archive_answer_raw"));
            response.put("image", videoDetail.get("video_archive_file_img"));
            response.put("play_time", videoDetail.get("play_time"));
            response.put("specialities_ids_and_names", specilityEntities);
            response.put("channel", channel);
            response.put("comment_count", contentComment);
            response.put("rating", ContentRating.get("rating"));
            response.put("myrating", ContentRating.get("myRating"));
            response.put("vault", contentVault.orElse(0));
            response.put("deeplink", videoDetail.get("deeplink"));
            response.put("disclaimer", commonUtils.getDisclaimer("video"));
            response.put("session_doctor_entities", sessionDoctorEntities);
            response.put("sponsor_logo", videoDetail.get("sponsor_logo"));
            response.put("price", videoDetail.get("price") != null ? Integer.parseInt(videoDetail.get("price").toString()) : 0);
            response.put("display_banner", bannerDisplay);
            response.put("campaign_data", creativeData);
            return response;
            
        } catch (Exception e) {
            Throwable rootCause = e.getCause();
            if (rootCause instanceof CustomException) {
                CustomException customException = (CustomException) rootCause;
                throw customException; 
            } else {
                throw new RuntimeException("Failed to get data", rootCause);
            }
        }
       
    }


    private VideoDetailResponse mapToResponseModel(Map<String, Object> data) {
        return objectMapper.convertValue(data, VideoDetailResponse.class);
    }
}
