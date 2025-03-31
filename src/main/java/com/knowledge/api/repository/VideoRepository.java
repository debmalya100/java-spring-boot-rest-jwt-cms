package com.knowledge.api.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

import com.knowledge.api.exception.CustomException;
import com.knowledge.api.utils.CommonUtils;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class VideoRepository {
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private final JdbcTemplate jdbcTemplate;
    
    @Autowired
    private CommonUtils commonUtils;

    @Async
    public CompletableFuture<List<Map<String,Object>>> getVideoSearchData( String videoCommaSeparatedIds) {
        
        try {
            List<String> videoIds = List.of(videoCommaSeparatedIds.split(","));

            String sql = "  SELECT  "+
                        "      cm.video_archive_id as type_id,  "+
                        "      'video' as trending_type,  "+
                        "      cm.video_archive_question_raw as question,  "+
                        "      cm.video_archive_file_img,  "+
                        "      cm.deeplink,  "+
                        "      cm.duration,  "+
                        "      cm.type as con_type,  "+
                        "      cm.src,  "+
                        "      GROUP_CONCAT(DISTINCT concat(ms.master_specialities_id, '#', ms.specialities_name)) as specialities_ids_and_names  "+
                        "  FROM  "+
                        "      knwlg_video_archive as cm  "+
                        "      left JOIN video_archive_to_specialities as cmTs ON cmTs.video_archive_id = cm.video_archive_id  "+
                        "      left JOIN master_specialities_V1 as ms ON ms.master_specialities_id = cmTs.specialities_id  "+
                        "      LEFT JOIN knwlg_sessions_V1 as ks ON ks.session_id = cm.video_archive_session_id  "+
                        "  WHERE  "+
                        "      cm.status = 3 "+ 
                        "      and cm.privacy_status = 0  "+
                        "      and cm.video_archive_id in (:videoIds)  "+
                        "  GROUP BY  "+
                        "      cm.video_archive_id  "+
                        "  order by  "+
                        "      cm.publication_date DESC " ;

            MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("videoIds", videoIds);

            List<Map<String, Object>> result = namedParameterJdbcTemplate.queryForList(sql, parameters);

            for( Map<String, Object> row : result ) {
                String specialities = (String) row.get("specialities_ids_and_names");
                List<Map<String, Object>> specialitiesList = commonUtils.parseSpecilityString(specialities);
                row.put("specialities_ids_and_names", specialitiesList);
            }
            
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            return CompletableFuture.completedFuture(null);
        }
    }


  
    @Async
    public CompletableFuture<Map<String,Object>> getClinicalVideoDetail( Integer videoId , Integer userMasterId , String userEnv , Integer userType) {
        
        try {

            // for internal user they can see the draft content
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

            String sql = "SELECT " +
                        "   cm.video_archive_id as type_id, " +
                        "   cm.video_archive_citation, " +
                        "   cm.video_archive_transcription, " +
                        "   cm.env, " +
                        "   cm.is_share, " +
                        "   cm.is_like, " +
                        "   cm.is_comment, " +
                        "   cm.video_archive_question_raw, " +
                        "   cm.video_archive_answer_raw, " +
                        "   cm.video_archive_file_img, " +
                        "   cm.video_archive_file_img_thumbnail, " +
                        "   cm.start_like, " +
                        "   cm.comment_status, " +
                        "   cm.added_on, " +
                        "   cm.publication_date, " +
                        "   cln.client_name, " +
                        "   cln.client_logo, " +
                        "   cm.type, " +
                        "   cm.vendor, " +
                        "   cm.src, " +
                        "   cm.deeplink, " +
                        "   cm.gl_deeplink, " +
                        "   cm.video_archive_tags, " +
                        "   ks.session_doctor_id, " +
                        "   kvtd.play_time, " +
                        "   cTenv.price, " +
                        "   GROUP_CONCAT(DISTINCT clintspon.client_name) as sponsor, " +
                        "   GROUP_CONCAT(DISTINCT clintspon.client_logo) as sponsor_logo, " +
                        "   GROUP_CONCAT(cmTs.specialities_id) AS specialityIds " +


                        "FROM knwlg_video_archive as cm " +

                        "JOIN video_archive_to_specialities as cmTs ON cmTs.video_archive_id = cm.video_archive_id " +
                        "JOIN master_specialities_V1 as ms ON ms.master_specialities_id = cmTs.specialities_id " +
                        "LEFT JOIN knwlg_video_tracking_data as kvtd ON kvtd.content_id=cm.video_archive_id AND kvtd.content_type='video_archive' AND kvtd.user_master_id= ? " +
                        "LEFT JOIN video_archive_to_sponsor as cmTspon ON cmTspon.video_archive_id = cm.video_archive_id " +
                        "LEFT JOIN client_master as clintspon ON clintspon.client_master_id = cmTspon.sponsor_id " +
                        "LEFT JOIN knwlg_sessions_V1 as ks ON ks.session_id = cm.video_archive_session_id " +
                        "LEFT JOIN client_master as cln ON cln.client_master_id = cm.client_id " +
                        "LEFT JOIN content_to_env as cTenv ON cTenv.type_id = cm.video_archive_id and  cTenv.type = 3  "+


                        " WHERE cm.status IN( ? ) " +envValue+
                        "   AND cm.publication_date <= CURDATE() " +
                        "   AND cm.privacy_status IN (0,1) " +
                        "   AND cm.video_archive_id = ?";

            // String sqlRaw = getRawQuery(sql, userMasterId, status, videoId);
            // System.out.println("sqlRaw: " + sqlRaw);

            Map<String, Object> result = jdbcTemplate.queryForMap(sql , userMasterId , status, videoId);

            if (result == null || result.get("type_id") == null ) {
                throw new CustomException(203, "No Data Found!");
            }

            return CompletableFuture.completedFuture(result);

        } catch (EmptyResultDataAccessException e) {
            throw new CustomException(203, "No Data Found!" );
        } catch (DataAccessException e) {
            throw new CustomException(500, "Oops A databse Error Occured!");
        } catch (Exception e) {
            if(e instanceof CustomException) {
                System.out.println("it is a CustomException: " + e.getMessage());
                CustomException customException = (CustomException) e;
                int statusCode = customException.getStatusCode();
                String errorMessage = customException.getErrorMessage();
                throw new CustomException(statusCode, errorMessage, e);
            } else {
                throw new CustomException(203, e.getMessage() , e  );
            }
        }
    }

    @Async
    public CompletableFuture<List<Map<String, Object>>> getClinicalVideoSpeciality(String typeId) {
        String sql = "SELECT "+
                    "   master_specialities_id, specialities_name " +

                    " FROM master_specialities_V1 ms " +
                    " JOIN video_archive_to_specialities vas ON ms.master_specialities_id = vas.video_archive_id " +
                    " WHERE vas.video_archive_id = ?";

        List<Map<String, Object>> compendiumSpecialities = jdbcTemplate.query(sql,
                (rs, rowNum) -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("id", rs.getInt("master_specialities_id"));
                    result.put("name", rs.getString("specialities_name"));
                    return result;
                },
                typeId);

        return CompletableFuture.completedFuture(compendiumSpecialities);
    }


    
    public String getRawQuery(String sql, Object... params) {
        String rawQuery = sql;
        for (Object param : params) {
            rawQuery = rawQuery.replaceFirst("\\?", param instanceof String ? "'" + param + "'" : param.toString());
        }
        return rawQuery;
    }
}
