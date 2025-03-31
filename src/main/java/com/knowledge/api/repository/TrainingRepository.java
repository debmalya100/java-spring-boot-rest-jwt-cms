package com.knowledge.api.repository;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

import com.knowledge.api.exception.CustomException;


@Repository
public class TrainingRepository {
    

    @Async
    public CompletableFuture<Map<String,Object>> getTrainingDetailData(Integer Id , Integer userMasterId , Integer userType , String userEnv) {

        try {

            String status = "3";
            if( userType == 5) {
                status = "3,5";
            }

            String envValue = null;
            if( !"1".equals(userEnv) ) {
                envValue = " AND (cTenv.env = 2 or cTenv.env = "+userEnv+")";
            } else {
                envValue = " AND cTenv.env = "+userEnv;
            }



            String sql  = """
                            
                    """;
            
            return CompletableFuture.completedFuture(null);
        } catch (EmptyResultDataAccessException e) {
            return CompletableFuture.completedFuture(null);
        } catch(Exception e) {
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Something Went Wrong");
        }
    }
}
