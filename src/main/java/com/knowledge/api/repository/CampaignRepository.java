package com.knowledge.api.repository;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

import com.knowledge.api.exception.CustomException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;

import com.knowledge.api.utils.CommonUtils;

@Repository
public class CampaignRepository {

    private final JdbcTemplate jdbcTemplate;

    public CampaignRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Autowired
    private CommonUtils commonUtils;

    @Async
    public CompletableFuture<Map<String, Object>> buildCampaignJSON(int campaignId) {

        try {

            String creativeSql = "SELECT " +
                "CONCAT(cmc.creative_width, 'x', cmc.creative_height) as creative_dimention, " +
                "cmc.creative_id as CrId, " +
                "cmc.file as CrUrl, " +
                "cmc.type as Type, " +
                "cmc.video_duration as Dur, "+
                "cmc.internal_type_id AS ActionID, " +
                "CASE " +
                "   WHEN cmc.CTA = 'external_url' THEN 'ext' " +
                "   WHEN cmc.CTA = 'internal_url' THEN 'int' " +
                "   ELSE 'con' " +
                "END AS CTA_Type, " +
                "CASE " +
                "   WHEN cmc.CTA = 'internal_url' THEN 'base' " +
                "   WHEN cmc.CTA = 'external_url' OR cmc.CTA = 'pdf_open' THEN 'pop' " +
                "   WHEN cmc.CTA = 'file_download' THEN 'download' " +
                "   ELSE NULL " +
                "END AS Behave, " +
                "CASE " +
                "   WHEN cmc.CTA = 'external_url' THEN cmc.external_URL " +
                "   WHEN cmc.CTA = 'internal_url' THEN cmc.internal_type " +
                "   WHEN cmc.CTA = 'file_download' OR cmc.CTA = 'pdf_open' THEN cmc.CTAupload_file " +
                "   ELSE NULL " +
                "END AS ActionPath, " +
                "cmc.CTA_button AS Title, " +
                "cmc.CTA_text AS ButtonText " +
                "FROM clirbanner_master_creative cmc " +
                "LEFT JOIN clirbanner_campaign_to_creative cTc ON cTc.creative_id = cmc.creative_id " +
                "WHERE cTc.campaign_id = ? " +
                "GROUP BY creative_dimention " +
                "ORDER BY cmc.creative_id ASC";

            List<Map<String, Object>> creativeResult = jdbcTemplate.queryForList(creativeSql, campaignId);

            Map<String, Object> keyByCreativeDimention = new HashMap<>();
            Map<String, Object> CTAData = new HashMap<>();

            if (!creativeResult.isEmpty()) {
                Map<String, Object> firstRow = creativeResult.get(0);

                if (firstRow.get("CTA_Type") != null) {
                    CTAData.put("Type", firstRow.get("CTA_Type"));
                }
                if (firstRow.get("Behave") != null) {
                    CTAData.put("Behave", firstRow.get("Behave"));
                }
                if (firstRow.get("ActionPath") != null) {
                    CTAData.put("ActionPath", firstRow.get("ActionPath"));
                }
                if (firstRow.get("Title") != null) {
                    CTAData.put("Title", firstRow.get("Title"));
                }
                if (firstRow.get("ButtonText") != null) {
                    CTAData.put("ButtonText", firstRow.get("ButtonText"));
                }
                if (firstRow.get("ActionID") != null) {
                    CTAData.put("ActionID", firstRow.get("ActionID"));
                }

                for (Map<String, Object> row : creativeResult) {
                    String creativeDimension = (String) row.get("creative_dimention");
                    Map<String , Object> creativeDataSizeWise = new HashMap<>();

                    creativeDataSizeWise.put("CrId", row.get("CrId"));
                    creativeDataSizeWise.put("CrUrl", commonUtils.changeImgSrc(row.get("CrUrl")));
                    creativeDataSizeWise.put("Dur", row.get("Dur"));

                    keyByCreativeDimention.put("Type", row.get("Type"));
                    keyByCreativeDimention.put(creativeDimension , creativeDataSizeWise); 
                }

                keyByCreativeDimention.put("CampaignId", campaignId);
                keyByCreativeDimention.put("CTA", CTAData);
            }

            return CompletableFuture.completedFuture(keyByCreativeDimention);
            
        } catch (EmptyResultDataAccessException e) {
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            throw new CustomException( 500 , "Something went wrong" , e);
        }
        
    }


    public CompletableFuture<List<Integer>> getExcludeCampaigns(String contentType) {

        try {   

            String sql = """
                            SELECT
                                exl.ids
                            FROM 
                                clirbanner_exclude_list exl
                            WHERE
                                exl.type = ?
                        """;

            String excludeList = jdbcTemplate.queryForObject(sql, String.class , contentType);

            String[] excludeCamoaignIds = excludeList.split(",");
            List<Integer> excludeCampaignIdList = new ArrayList<>();
            for (String id : excludeCamoaignIds) {
                excludeCampaignIdList.add(Integer.parseInt(id));
            }
            return CompletableFuture.completedFuture(excludeCampaignIdList);

        } catch(EmptyResultDataAccessException e) {
            return  CompletableFuture.completedFuture(null);    
        } catch (Exception e) {
            throw new CustomException( 500 , "Something went wrong" , e);
        }
    }


    public List<Integer> getAContentAllCampiagns(String userGroup , Integer userType , Integer contentId , String contentType) {

        try {
             
            if( !userGroup.trim().isEmpty()) {
               
                String Sql = """
                        SELECT
                            nb.campaign_id
                        FROM
                            clirbanner_master_campaign nb

                        LEFT JOIN clirbanner_campaign_to_speciality nbtsp on nb.campaign_id = nbtsp.campaign_id
                        LEFT JOIN clirbanner_campaign_to_user_type nbtutyp on nb.campaign_id = nbtutyp.campaign_id

                        LEFT JOIN clirbanner_campaign_to_content nbtc on nbtc.campaign_id = nb.campaign_id
                        LEFT JOIN campaing_to_group cmTgr on ( cmTgr.campaign_id = nb.campaign_id )

                    
                        WHERE  
                            nb.status = 3
                            and nbtc.type_id = ?
                            and nbtc.type = ?
                            and nbtutyp.user_type_id = ?
                            AND (cmTgr.group_id in (?) or cmTgr.campaign_id is null) 
                        """; 

                List<Integer> campaignIds = jdbcTemplate.queryForList(Sql, Integer.class ,contentId , contentType , userType , userGroup );
                return campaignIds;
            } else {

                String Sql = """
                            SELECT
                                nb.campaign_id
                            FROM
                                clirbanner_master_campaign nb

                            LEFT JOIN clirbanner_campaign_to_speciality nbtsp on nb.campaign_id = nbtsp.campaign_id
                            LEFT JOIN clirbanner_campaign_to_user_type nbtutyp on nb.campaign_id = nbtutyp.campaign_id

                            LEFT JOIN clirbanner_campaign_to_content nbtc on nbtc.campaign_id = nb.campaign_id
                            LEFT JOIN campaing_to_group cmTgr on ( cmTgr.campaign_id = nb.campaign_id )

                        
                            WHERE  
                                nb.status = 3
                                and nbtc.type_id = ?
                                and nbtc.type = ?
                                and nbtutyp.user_type_id = ?
                                AND (cmTgr.campaign_id is NULL)
                        """;

                List<Integer> campaignIds = jdbcTemplate.queryForList(Sql, Integer.class ,contentId , contentType , userType);
                return campaignIds;
            }
        
            
        } catch (EmptyResultDataAccessException e) {
            return null;
        } catch(Exception e) {
            throw new CustomException( 500 , "Something went wrong" , e);
        }
        
    }
}