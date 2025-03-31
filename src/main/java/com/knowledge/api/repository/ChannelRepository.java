package com.knowledge.api.repository;

import lombok.RequiredArgsConstructor;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;
import org.springframework.dao.EmptyResultDataAccessException;
import com.knowledge.api.exception.CustomException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.springframework.http.HttpStatus;

import com.knowledge.api.exception.CustomException;

@Repository
@RequiredArgsConstructor
public class ChannelRepository {

    private final JdbcTemplate jdbcTemplate;

    @Async
    public CompletableFuture<Map<String, Object>> getChannelData(String typeId, Integer userMasterId, String type) {
        String sql = "";
        Map<String, Object> channel = new HashMap<>();

        try {
            switch (type) {
                case "session":
                    sql = "SELECT ctva.channel_master_id, cm.title, cm.follower_count, cm.logo_type, cm.logo, cm.privacy_status, cm.short_description, cm.deeplink, cm.is_followers, cm.is_activities, cm.is_share, cTus.status as followed_status "
                            +
                            "FROM channel_to_video_archive as ctva " +
                            "JOIN channel_master as cm ON cm.channel_master_id = ctva.channel_master_id " +
                            "LEFT JOIN channel_to_user as cTus ON cTus.channel_master_id = cm.channel_master_id AND cTus.user_master_id = ? "
                            +
                            "WHERE ctva.video_archive_id = ? " +
                            "LIMIT 1";

                    channel = jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                        Map<String, Object> result = new HashMap<>();
                        result.put("channel_id", rs.getString("channel_master_id"));
                        result.put("title", rs.getString("title"));
                        result.put("is_followers", rs.getBoolean("is_followers"));
                        result.put("is_activities", rs.getBoolean("is_activities"));
                        result.put("is_share", rs.getBoolean("is_share"));
                        result.put("logo", rs.getString("logo"));
                        result.put("logo_type", rs.getString("logo_type"));
                        result.put("description", rs.getString("short_description"));
                        result.put("privacy_status", rs.getString("privacy_status"));
                        result.put("followed_status", rs.getString("followed_status"));
                        result.put("deeplink", rs.getString("deeplink"));
                        result.put("follower_count", rs.getInt("follower_count")); // Ensure this field is retrieved
                        return result;
                    }, userMasterId, typeId);

                    if (channel != null) {
                        String channelId = (String) channel.get("channel_id");
                        channel.put("speciality", specialitiesByChannelId(channelId).join());

                        // Handle null follower_count
                        int followerCount = channel.get("follower_count") != null ? (int) channel.get("follower_count")
                                : 0;
                        int totalFollowers = totalFollowersCountByChannelId(channelId).join();
                        channel.put("follower_count", followerCount + totalFollowers);

                        channel.put("total_activity",
                                totalSessionActivityCountByChannelId(channelId, userMasterId).join());
                    }
                    break;

                case "comp":
                    sql = "SELECT ctc.channel_master_id, cm.title, cm.logo, cm.logo_type, cm.privacy_status, cm.follower_count, cm.short_description, cm.deeplink, cm.is_followers, cm.is_share, cm.is_activities, cTus.status as followed_status "
                            +
                            "FROM channel_to_compendium as ctc " +
                            "JOIN channel_master as cm ON cm.channel_master_id = ctc.channel_master_id " +
                            "LEFT JOIN channel_to_user as cTus ON cTus.channel_master_id = cm.channel_master_id AND cTus.user_master_id = ? "
                            +
                            "WHERE ctc.comp_qa_id = ? " +
                            "LIMIT 1";

                    channel = jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                        Map<String, Object> result = new HashMap<>();
                        result.put("channel_id", rs.getString("channel_master_id"));
                        result.put("title", rs.getString("title"));
                        result.put("is_followers", rs.getBoolean("is_followers"));
                        result.put("is_activities", rs.getBoolean("is_activities"));
                        result.put("is_share", rs.getBoolean("is_share"));
                        result.put("logo", rs.getString("logo"));
                        result.put("logo_type", rs.getString("logo_type"));
                        result.put("description", rs.getString("short_description"));
                        result.put("privacy_status", rs.getString("privacy_status"));
                        result.put("followed_status", rs.getString("followed_status"));
                        result.put("deeplink", rs.getString("deeplink"));
                        result.put("follower_count", rs.getInt("follower_count")); // Ensure this field is retrieved
                        return result;
                    }, userMasterId, typeId);

                    if (channel != null) {
                        String channelId = (String) channel.get("channel_id");
                        channel.put("speciality", specialitiesByChannelId(channelId).join());

                        // Handle null follower_count
                        int followerCount = channel.get("follower_count") != null ? (int) channel.get("follower_count")
                                : 0;
                        int totalFollowers = totalFollowersCountByChannelId(channelId).join();
                        channel.put("follower_count", followerCount + totalFollowers);

                        channel.put("total_activity", totalActivityCountByChannelId(channelId, userMasterId).join());
                    }
                    break;

                case "epub":

                    sql = """
                            SELECT
                                chm.channel_master_id as channel_id,
                                chm.title as channel_title,
                                chm.logo as channel_logo,
                                chm.logo_type,
                                chm.description as channel_desc,
                                chm.privacy_status as channel_privacy_stts,
                                chm.follower_count as channel_follower_count,
                                chm.deeplink as channel_deep_link,
                                ctus.status as followed_status,
                                cm.client_id as epub_client,
                                cm.added_on
                            
                            FROM 
                                epub_master as cm
                        
                            LEFT JOIN 
                                channel_master as chm ON chm.client_id = cm.client_id 
                                
                            LEFT JOIN 
                                channel_to_user as ctus ON ctus.channel_master_id = chm.channel_master_id
                            
                            WHERE
                                cm.status = 3
                                AND cm.epub_id = ?
                                    
                            GROUP BY 
                                chm.channel_master_id 
                            ORDER BY 
                                cm.added_on DESC  
                            LIMIT 1

                            """;

                    Map<String , Object> detail = jdbcTemplate.queryForMap(sql,typeId);

                    if( detail == null || detail.get("channel_id") == null ) {
                        channel = Collections.emptyMap(); 
                    } else {

                        CompletableFuture<Integer> activityCountFuture =  getAChannelTotalActivityCount(Integer.parseInt(detail.get("channel_id").toString()));
                        CompletableFuture<Integer> followerCountFuture = totalFollowersCountByChannelId(detail.get("channel_id").toString());

                        CompletableFuture.allOf(activityCountFuture ,followerCountFuture ).join();

                        Integer activityCount = activityCountFuture.get();
                        Integer followerCount = followerCountFuture.get();

                        Integer totalFollowerCount = followerCount + Integer.parseInt(detail.get("channel_follower_count").toString());
                        
                        channel.put("channel_id",detail.get("channel_id"));
                        channel.put("title",detail.get("channel_title"));
                        channel.put("logo",detail.get("channel_logo"));
                        channel.put("logo_type",detail.get("logo_type"));
                        channel.put("description",detail.get("channel_desc"));
                        channel.put("deeplink",detail.get("channel_deep_link"));
                        channel.put("followed_status",detail.get("followed_status"));
                        channel.put("epub_client",detail.get("epub_client"));
                        channel.put("total_activity", activityCount);
                        channel.put("follower_count", totalFollowerCount);

                    }

            }
        } catch (EmptyResultDataAccessException e) {
            // Handle case where no result is found
            channel = Collections.emptyMap();
        } catch (Exception e) {
            // Log the exception
            // e.printStackTrace();
            channel = Collections.emptyMap();
        }

        return CompletableFuture.completedFuture(channel);
    }

    @Async
    public CompletableFuture<List<Map<String, Object>>> specialitiesByChannelId(String channelId) {
        String sql = "SELECT ms.master_specialities_id, ms.specialities_name " +
                "FROM master_specialities_V1 as ms " +
                "LEFT JOIN channel_to_specialities as cs ON cs.specialities_id = ms.master_specialities_id " +
                "WHERE cs.channel_master_id = ?";

        List<Map<String, Object>> specialities = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> result = new HashMap<>();
            result.put("master_specialities_id", rs.getInt("master_specialities_id"));
            result.put("specialities_name", rs.getString("specialities_name"));
            return result;
        }, channelId);

        return CompletableFuture.completedFuture(specialities);
    }

    @Async
    public CompletableFuture<Integer> totalFollowersCountByChannelId(String channelId) {
        String sql = "SELECT count(DISTINCT(user_master_id)) as follower_count " +
                "FROM channel_to_user " +
                "WHERE status = 3 AND channel_master_id = ?";

        Integer followerCount = jdbcTemplate.queryForObject(sql, Integer.class, channelId);
        return CompletableFuture.completedFuture(followerCount != null ? followerCount : 0);
    }

    @Async
    public CompletableFuture<Integer> totalSessionActivityCountByChannelId(String channelId, Integer userMasterId) {
        String sql = "SELECT COUNT(*) as total_activity " +
                "FROM channel_to_session as a " +
                "WHERE a.channel_master_id = ? " +
                "GROUP BY a.channel_master_id";

        Integer totalActivity = jdbcTemplate.queryForObject(sql, Integer.class, channelId);
        return CompletableFuture.completedFuture(totalActivity != null ? totalActivity : 0);
    }

    @Async
    public CompletableFuture<Integer> totalActivityCountByChannelId(String channelId, Integer userMasterId) {
        String envStatus = getEnvStatus(userMasterId);

        String compendiumSql = "SELECT COUNT(*) as total_activity " +
                "FROM channel_to_compendium as a " +
                "INNER JOIN knwlg_compendium_V1 as cm ON cm.comp_qa_id = a.comp_qa_id " +
                "LEFT JOIN content_to_env as cTenv ON cTenv.type_id = cm.comp_qa_id AND cTenv.type = 1 " +
                "WHERE a.channel_master_id = ? AND cm.status = 3 AND cm.privacy_status IN (0,1) " + envStatus;

        Integer totalCompendiumActivity = jdbcTemplate.queryForObject(compendiumSql, Integer.class, channelId);

        String surveySql = "SELECT COUNT(*) as total_activity " +
                "FROM channel_to_survey as a " +
                "INNER JOIN survey as sv ON sv.survey_id = a.survey_id " +
                "INNER JOIN survey_detail as svd ON svd.survey_id = sv.survey_id " +
                "LEFT JOIN content_to_env as cTenv ON cTenv.type_id = sv.survey_id AND cTenv.type = 6 " +
                "WHERE a.channel_master_id = ? AND sv.status = 3 AND sv.privacy_status IN (0,1) " + envStatus;

        Integer totalSurveyActivity = jdbcTemplate.queryForObject(surveySql, Integer.class, channelId);

        String courseSql = "SELECT COUNT(*) as total_activity " +
                "FROM channel_to_course as a " +
                "INNER JOIN training_master as tm ON tm.id = a.course_id " +
                "WHERE a.channel_master_id = ? AND tm.privacy_status IN (0,1) AND tm.status = 3";

        Integer totalCourseActivity = jdbcTemplate.queryForObject(courseSql, Integer.class, channelId);

        String sessionSql = "SELECT COUNT(*) as total_activity " +
                "FROM channel_to_session as a " +
                "INNER JOIN knwlg_sessions_V1 as ks ON ks.session_id = a.session_id " +
                "LEFT JOIN content_to_env as cTenv ON cTenv.type_id = ks.session_id AND cTenv.type = 2 " +
                "WHERE a.channel_master_id = ? AND ks.status = 3 AND ks.privacy_status != 2 " + envStatus;

        Integer totalSessionActivity = jdbcTemplate.queryForObject(sessionSql, Integer.class, channelId);

        String archiveVideoSql = "SELECT COUNT(*) as total_activity " +
                "FROM channel_to_video_archive as a " +
                "INNER JOIN knwlg_video_archive as kva ON kva.video_archive_id = a.video_archive_id " +
                "LEFT JOIN content_to_env as cTenv ON cTenv.type_id = kva.video_archive_id AND cTenv.type = 3 " +
                "WHERE a.channel_master_id = ? AND kva.status = 3 AND kva.privacy_status != 2 " + envStatus;

        Integer totalArchiveVideoActivity = jdbcTemplate.queryForObject(archiveVideoSql, Integer.class, channelId);

        String postSql = "SELECT COUNT(*) as total_activity " +
                "FROM channel_to_post as a " +
                "WHERE a.channel_master_id = ? AND a.status = 3";

        Integer totalPostActivity = jdbcTemplate.queryForObject(postSql, Integer.class, channelId);

        String documentSql = "SELECT COUNT(*) as total_activity " +
                "FROM channel_to_document as a " +
                "WHERE a.channel_master_id = ? AND a.status = 3";

        Integer totalDocumentActivity = jdbcTemplate.queryForObject(documentSql, Integer.class, channelId);

        int totalActivity = (totalCompendiumActivity != null ? totalCompendiumActivity : 0) +
                (totalSurveyActivity != null ? totalSurveyActivity : 0) +
                (totalCourseActivity != null ? totalCourseActivity : 0) +
                (totalSessionActivity != null ? totalSessionActivity : 0) +
                (totalArchiveVideoActivity != null ? totalArchiveVideoActivity : 0) +
                (totalPostActivity != null ? totalPostActivity : 0) +
                (totalDocumentActivity != null ? totalDocumentActivity : 0);

        return CompletableFuture.completedFuture(totalActivity);
    }


    @Async
    public CompletableFuture<Integer> getAChannelTotalActivityCount( Integer channelMasterId) {

        try {
            String Sql = """
                        SELECT SUM(total_count) AS total_activity
                        FROM (
                            SELECT count(cm.comp_qa_id) AS total_count FROM knwlg_compendium_V1 as cm 
                            LEFT JOIN channel_to_compendium as cmTs ON cmTs.comp_qa_id = cm.comp_qa_id 
                            WHERE cmTs.channel_master_id = ? and cm.status=3 and cm.privacy_status in (0,1) and cm.publication_date <= CURDATE()

                            UNION ALL
                            SELECT count(sv.survey_id) AS total_count FROM survey sv 
                            LEFT JOIN channel_to_survey AS cts ON cts.survey_id = sv.survey_id 
                            WHERE cts.channel_master_id = ? AND sv.status = 3 and sv.privacy_status in (0,1) and sv.publishing_date <= CURDATE()

                            UNION ALL
                            SELECT count(cm.id) AS total_count FROM training_master as cm 
                            LEFT JOIN channel_to_course as ctc on ctc.course_id = cm.id  
                            WHERE ctc.channel_master_id = ? and cm.status=3 and cm.privacy_status != 2 and cm.published_date <= CURDATE()

                            UNION ALL
                            SELECT count(ks.session_id) AS total_count FROM knwlg_sessions_V1 as ks 
                            LEFT JOIN channel_to_session AS cts ON cts.session_id = ks.session_id  
                            WHERE cts.channel_master_id = ? and ks.status = 3 and ks.privacy_status != 2 and ks.session_status not in (3,5,6)

                            UNION ALL
                            SELECT count(cm.video_archive_id) AS total_count FROM knwlg_video_archive as cm 
                            LEFT JOIN channel_to_video_archive as ctva ON cm.video_archive_id = ctva.video_archive_id 
                            WHERE ctva.channel_master_id = ?  and cm.status=3 and cm.privacy_status != 2 and cm.publication_date <= CURDATE()

                            UNION ALL
                            SELECT count(cm.epub_id) as total_count FROM epub_master cm 
                            WHERE cm.client_id = (select client_id from channel_master where channel_master_id = ?) 
                            and status=3 and publication_date <= CURDATE() and is_converted = 1

                            UNION ALL
                            SELECT COUNT(id) AS total_count FROM channel_to_post 
                            WHERE channel_master_id = ? AND status = 3 AND post_date <= CURDATE()

                            UNION ALL
                            SELECT COUNT(id) AS total_count FROM channel_to_document 
                            WHERE channel_master_id = ? and status = 3 
                            
                        ) AS subquery

                    """;
            Object[] params = new Object[8];
            Arrays.fill(params, channelMasterId);
            Integer result =  jdbcTemplate.queryForObject(Sql, Integer.class, params);
            return CompletableFuture.completedFuture(result);

        } catch(EmptyResultDataAccessException e) {
            return CompletableFuture.completedFuture(0);
        } catch(Exception e) {
            throw new CustomException( HttpStatus.INTERNAL_SERVER_ERROR.value() , "Something went wrong" , e);
        }


    } 

    private String getEnvStatus(Integer userMasterId) {
        String env = getUserEnv(userMasterId);
        if (env != null) {
            if (!env.equals("2")) {
                return "AND (cTenv.env = 2 OR cTenv.env = " + env + ")";
            } else {
                return "AND cTenv.env = " + env;
            }
        } else {
            return "";
        }
    }

    private String getUserEnv(Integer userMasterId) {
        String sql = "SELECT etc.env_id " +
                "FROM user_master um " +
                "LEFT JOIN env_to_country etc ON etc.country_id = um.country_code " +
                "WHERE um.user_master_id = ?";
        return jdbcTemplate.queryForObject(sql, String.class, userMasterId);
    }
}