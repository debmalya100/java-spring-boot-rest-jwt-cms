package com.knowledge.api.service;


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.knowledge.api.exception.CustomException;
import com.knowledge.api.repository.ChannelRepository;
import com.knowledge.api.repository.CommonRepository;
import com.knowledge.api.repository.TrainingRepository;
import com.knowledge.api.utils.CommonUtils;

@Service
public class TrainingService {
    
    @Autowired
    private CommonRepository commonRepository;

    @Autowired
    private TrainingRepository trainingRepository;

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private ChannelRepository channelRepository;

    @Autowired
    private CommonUtils commonUtils;


    public Map<String , Object> getTraingDetails(Integer userMasterId , String clientIds , Integer Id) {
        try {

            CompletableFuture<Integer> userTypeFuture = commonRepository.getUserType(userMasterId); 
            CompletableFuture<String> userEnvFuture = commonRepository.getUserEnv(userMasterId); 
            

            /**
             * userType and userEnv
             */
            CompletableFuture.allOf(userTypeFuture, userEnvFuture).join();
            Integer userType = userTypeFuture.get();
            String userEnv = userEnvFuture.get();


            CompletableFuture<Map<String, Object>> tainingDetailFuture = trainingRepository.getTrainingDetailData(Id , userMasterId , userType , userEnv );


            CompletableFuture.allOf(tainingDetailFuture).join();

            Map<String, Object> tainingDetail = tainingDetailFuture.get();


            return new HashMap<>();
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


}
