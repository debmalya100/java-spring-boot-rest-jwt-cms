package com.knowledge.api.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;
import lombok.RequiredArgsConstructor;
import com.knowledge.api.utils.CommonUtils;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.Objects;
import java.util.Arrays;
import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class DashboardRepository {

    private final JdbcTemplate jdbcTemplate;

    private final CommonRepository commonRepository;

    @Autowired
    private CommonUtils commonUtils;

    public String getUserMlData(Integer userMasterId) {
        String sql = "select re_ml from user_master where user_master_id = ?";
        return jdbcTemplate.queryForObject(sql, String.class, userMasterId);
    }

    public List<Map<String, Object>> getCompendiumData(Integer userMasterId, int from, int to, int speciality,
            String userMlData,
            String env, int convert, String subtype) {

        try {
            CompletableFuture<List<Map<String, Object>>> getcompFuture = getcompMl(userMasterId, from, to,
                    speciality, userMlData, subtype, env, convert);
            // System.out.println("getReadData: " + userMlData);
            List<Map<String, Object>> compendium = getcompFuture.get();

            // Return the merged data
            return compendium;

        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList(); // Return an empty map or handle the error appropriately
        }

    }

    public List<Map<String, Object>> getReadData(Integer userMasterId, int from, int to, int speciality,
            String userMlData,
            String env, int convert, String subtype) {

        try {

            System.out.println("env+++++++++++++++++++++++++++++++++++++++++: " + env);
            CompletableFuture<List<Map<String, Object>>> getcompFuture = getcompMl(userMasterId, from, to,
                    speciality, userMlData, subtype, env, convert);

            CompletableFuture<List<Map<String, Object>>> getepubFuture = getepubMl(userMasterId, from, to,
                    speciality, userMlData, env, convert);

            // System.out.println("getReadData: " + userMlData);

            try {
                CompletableFuture.allOf(getcompFuture, getepubFuture).join();
            } catch (Exception e) {
                e.printStackTrace();
                return Collections.emptyList(); // Return an empty list or handle the error appropriately
            }

            List<Map<String, Object>> compendium = getcompFuture.get();
            List<Map<String, Object>> epub = getepubFuture.get();
            // System.out.println("compendium: " + compendium);
            // Merge the two lists
            List<Map<String, Object>> mergedData = new ArrayList<>();
            if (compendium != null) {
                mergedData.addAll(compendium);
            }
            if (epub != null) {
                mergedData.addAll(epub);
            }

            // Return the merged data
            return mergedData;

        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList(); // Return an empty map or handle the error appropriately
        }

    }

    public List<Map<String, Object>> getSessionData(Integer userMasterId, String clientIds, int from, int to,
            int speciality,
            String userMlData,
            String env, int convert, String subtype) {
        try {
            CompletableFuture<List<Map<String, Object>>> getSessionFuture = getSessionMl(userMasterId, from, to,
                    clientIds,
                    speciality, userMlData, subtype, env);

            try {
                CompletableFuture.allOf(getSessionFuture).join();
            } catch (Exception e) {
                e.printStackTrace();
                return Collections.emptyList(); // Return an empty list or handle the error appropriately
            }

            List<Map<String, Object>> session = getSessionFuture.get();

            // Return the merged data
            return session;

        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList(); // Return an empty map or handle the error appropriately
        }
    }

    public List<Map<String, Object>> getWatchData(Integer userMasterId, int from, int to, int speciality,
            String userMlData,
            String env) {
        try {
            CompletableFuture<List<Map<String, Object>>> getVidFuture = getvidMl(userMasterId, from, to,
                    speciality, userMlData, env);

            try {
                CompletableFuture.allOf(getVidFuture).join();
            } catch (Exception e) {
                e.printStackTrace();
                return Collections.emptyList(); // Return an empty list or handle the error appropriately
            }

            List<Map<String, Object>> video = getVidFuture.get();

            // Return the merged data
            return video;

        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList(); // Return an empty map or handle the error appropriately
        }
    }

    public List<Map<String, Object>> getOpinionsData(Integer userMasterId, int from, int to, int speciality,
            String userMlData,
            String env) {
        try {
            CompletableFuture<List<Map<String, Object>>> getSpqFuture = getSpqMl(userMasterId, from, to,
                    speciality, userMlData, env);

            try {
                CompletableFuture.allOf(getSpqFuture).join();
            } catch (Exception e) {
                e.printStackTrace();
                return Collections.emptyList(); // Return an empty list or handle the error appropriately
            }

            List<Map<String, Object>> spq = getSpqFuture.get();

            // Return the merged data
            return spq;

        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList(); // Return an empty map or handle the error appropriately
        }
    }

    public List<Map<String, Object>> getLearnData(Integer userMasterId, int from, int to, int speciality,
            String userMlData,
            String env) {

        try {

            CompletableFuture<List<Map<String, Object>>> getTrainingFuture = getTrainingMl(userMasterId, from, to,
                    speciality, userMlData, env);

            List<Map<String, Object>> traingList = getTrainingFuture.get();

            // Return the merged data
            return traingList;

        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList(); // Return an empty map or handle the error appropriately
        }
    }

    public List<Map<String, Object>> getAllData(Integer userMasterId, String clientIds, int from, int to,
            int speciality,
            String userMlData,
            String env, int convert, String subtype) {

        try {

            CompletableFuture<List<Map<String, Object>>> getcompFuture = getcompMl(userMasterId, from, to,
                    speciality, userMlData, subtype, env, convert);
            CompletableFuture<List<Map<String, Object>>> getepubFuture = getepubMl(userMasterId, from, to,
                    speciality, userMlData, env, convert);

            CompletableFuture<List<Map<String, Object>>> getSessionFuture = getSessionMl(userMasterId, from, to,
                    clientIds, speciality, userMlData, subtype, env);

            CompletableFuture<List<Map<String, Object>>> getSpqFuture = getSpqMl(userMasterId, from, to,
                    speciality, userMlData, env);

            CompletableFuture<List<Map<String, Object>>> getVidFuture = getvidMl(userMasterId, from, to,
                    speciality, userMlData, env);

            CompletableFuture<List<Map<String, Object>>> getTrainingFuture = getTrainingMl(userMasterId, from, to,
                    speciality, userMlData, env);

            try {
                CompletableFuture.allOf(getcompFuture, getepubFuture, getSessionFuture, getSpqFuture, getVidFuture,
                        getTrainingFuture)
                        .join();
            } catch (Exception e) {
                e.printStackTrace();
                return Collections.emptyList(); // Return an empty list or handle the error appropriately
            }

            List<Map<String, Object>> compendium = getcompFuture.get();
            List<Map<String, Object>> epub = getepubFuture.get();
            List<Map<String, Object>> session = getSessionFuture.get();
            List<Map<String, Object>> spq = getSpqFuture.get();
            List<Map<String, Object>> video = getVidFuture.get();
            List<Map<String, Object>> training = getTrainingFuture.get();

            // System.out.println("compendium:********************** " + compendium);
            // System.out.println("epub:********************** " + epub);
            // System.out.println("session:********************** " + session);
            // System.out.println("spq:********************** " + spq);
            // System.out.println("video:********************** " + video);
            // Merge the two lists
            List<Map<String, Object>> mergedData = new ArrayList<>();
            if (compendium != null) {
                mergedData.addAll(compendium);
            }
            if (epub != null) {
                mergedData.addAll(epub);
            }
            if (session != null) {
                mergedData.addAll(session);
            }
            if (spq != null) {
                mergedData.addAll(spq);
            }
            if (video != null) {
                mergedData.addAll(video);
            }

            if (training != null) {
                mergedData.addAll(training);
            }
            // System.out.println("mergedData:********************** " + mergedData);

            // Return the merged data
            return mergedData;

        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList(); // Return an empty map or handle the error appropriately
        }

    }

    @Async
    public CompletableFuture<List<Map<String, Object>>> getcompMl(
            Integer userMasterId, int limitFrom, int limitTo, int speciality, String userMlData,
            String subtype, String env, int convert) {

        CompletableFuture<Map<String, Object>> packageDetailsFuture = commonRepository.checkUserPackage(
                userMasterId, "comp", env);

        System.out.println("envvvvvvvvvv*************************: " + env);

        try {
            CompletableFuture.allOf(packageDetailsFuture).join();
        } catch (Exception e) {
            e.printStackTrace();
            return CompletableFuture.completedFuture(Collections.emptyList()); // Handle errors
        }

        Map<String, Object> packageDetails;
        try {
            packageDetails = packageDetailsFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return CompletableFuture.completedFuture(Collections.emptyList()); // Handle errors
        }

        // System.out.println("packageDetails: " + packageDetails);

        // Build SQL query
        String querySmart = (userMlData != null && !userMlData.isEmpty() && !userMlData.equals("0"))
                ? "AND cm.comp_qa_id IN (" + userMlData + ")"
                : (speciality == 0 ? "" : "AND cmTs.specialities_id IN (" + speciality + ")");

        String envStatus = (env != null)
                ? (env.equals("2") ? "AND cTenv.env = " + env : "AND (cTenv.env = 2 OR cTenv.env = " + env + ")")
                : "";

        String featuredStatus = (subtype != null)
                ? (env.equals("2") ? "AND cTenv.env = " + env : "AND (cTenv.env = 2 OR cTenv.env = " + env + ")")
                : "";

        String limit = "LIMIT " + limitFrom + ", " + limitTo;

        String sql = "SELECT " +
                "cm.comp_qa_id as type_id, " +
                "cm.is_share, " +
                "cm.comp_qa_question, " +
                "cm.template_id, " +
                "cm.comp_qa_answer, " +
                "cm.comp_qa_answer_raw as description, " +
                "cm.comp_qa_question_raw as title, " +
                "cm.comp_qa_file_img, " +
                "cm.comp_qa_file_img_thumbnail, " +
                "cm.added_on, " +
                "cm.publication_date as publish_date, " +
                "cln.client_name, " +
                "cln.client_logo, " +
                "cm.color, " +
                "cm.type, " +
                "cm.vendor, " +
                "cm.src, " +
                "cm.deeplink, " +
                "cm.gl_deeplink, " +
                "cm.start_like, " +
                "cm.display_in_dashboard, " +
                "cSummary.comment_count, " +
                "cSummary.rating_count, " +
                "cSummary.view_count, " +
                "cTenv.price, " +
                "uTpyCont.status as user_content_payment_status, " +
                "GROUP_CONCAT(DISTINCT ms.specialities_name) as specialities_name, " +
                "GROUP_CONCAT(DISTINCT CONCAT(ms.master_specialities_id, '#', ms.specialities_name)) as specialities_ids_and_names, "
                +
                "GROUP_CONCAT(DISTINCT clintspon.client_name) as sponsorCM, " +
                "GROUP_CONCAT(DISTINCT clintspon.client_master_id) as sponsor_ids, " +
                "GROUP_CONCAT(DISTINCT clintspon.client_logo) as sponsor_logoCM, " +
                "cm.comp_qa_speciality_id " +
                "FROM knwlg_compendium_V1 as cm " +
                "JOIN compendium_to_specialities as cmTs ON cmTs.comp_qa_id = cm.comp_qa_id " +
                "JOIN master_specialities_V1 as ms ON ms.master_specialities_id = cmTs.specialities_id " +
                "JOIN client_master as cln ON cln.client_master_id = cm.client_id " +
                "LEFT JOIN compendium_to_sponsor as cmTspon ON cmTspon.comp_qa_id = cm.comp_qa_id " +
                "LEFT JOIN client_master as clintspon ON clintspon.client_master_id = cmTspon.sponsor_id " +
                "LEFT JOIN content_summary as cSummary ON cSummary.type_id = cm.comp_qa_id AND cSummary.type = 1 " +
                "LEFT JOIN content_to_env as cTenv ON cTenv.type_id = cm.comp_qa_id AND cTenv.type = 1 " +
                "LEFT JOIN payment_user_to_content as uTpyCont ON uTpyCont.type_id = cm.comp_qa_id AND uTpyCont.type = 1 AND uTpyCont.user_master_id = ? "
                +
                "WHERE cm.status = 3 " +
                "AND cm.publication_date <= CURDATE() " +
                querySmart + " " +
                "AND cm.privacy_status = 0 " +
                envStatus + " " +
                "GROUP BY cm.comp_qa_id, cTenv.price, uTpyCont.status, cSummary.comment_count, cSummary.rating_count, cSummary.view_count "
                +
                "ORDER BY cm.comp_qa_id DESC " + limit;

        // System.out.println("sql: " + sql);

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, userMasterId);

        // Process the results using a loop
        List<Map<String, Object>> processedResults = new ArrayList<>();
        int i = 1;
        for (Map<String, Object> rs : results) {

            List<Map<String, Object>> specilityArray = commonUtils
                    .parseSpecilityString(rs.get("specialities_ids_and_names").toString());

            String answeString = commonUtils.truncateString(rs.get("description").toString(), 100);

            Map<String, Object> map = new HashMap<>();
            map.put("type_id", rs.get("type_id"));
            map.put("type", "comp");
            map.put("con_type", "comp");
            map.put("user_content_payment",
                    rs.get("user_content_payment_status") != null ? rs.get("user_content_payment_status").toString()
                            : null);
            map.put("price", rs.get("price") != null ? rs.get("price").toString() : null);
            map.put("answer", answeString);
            map.put("question", rs.get("title"));
            map.put("comp_qa_file_img", rs.get("comp_qa_file_img"));

            map.put("comment_count", rs.get("comment_count"));
            map.put("rating", rs.get("rating"));
            map.put("view_count", rs.get("comment_count"));
            map.put("is_featured", rs.get("display_in_dashboard"));

            map.put("color", rs.get("color"));

            map.put("specialities", rs.get("specialities_name"));
            map.put("specialities_ids_and_names", specilityArray);

            // map.put("env", env);

            LocalDateTime startDateTime = (LocalDateTime) rs.get("publish_date");
            String formattedDateTime = commonUtils.formatDateTime(startDateTime);

            map.put("date", formattedDateTime);
            map.put("deeplink", rs.get("deeplink"));
            map.put("gl_deeplink", rs.get("gl_deeplink"));
            map.put("client_name", rs.get("client_name"));
            map.put("client_logo", rs.get("client_logo"));
            map.put("sponsor", rs.get("sponsorCM"));
            map.put("sponsor_logo", rs.get("sponsor_logoCM"));
            map.put("is_locked", packageDetails);
            processedResults.add(map);
            i++;
        }

        return CompletableFuture.completedFuture(processedResults);
    }

    @Async
    public CompletableFuture<List<Map<String, Object>>> getvidMl(
            Integer userMasterId, int limitFrom, int limitTo, int speciality, String userMlData, String env) {

        CompletableFuture<Map<String, Object>> packageDetailsFuture = commonRepository.checkUserPackage(
                userMasterId, "video_archived", env);

        try {
            CompletableFuture.allOf(packageDetailsFuture).join();
        } catch (Exception e) {
            e.printStackTrace();
            return CompletableFuture.completedFuture(Collections.emptyList()); // Handle errors
        }

        Map<String, Object> packageDetails;
        try {
            packageDetails = packageDetailsFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return CompletableFuture.completedFuture(Collections.emptyList()); // Handle errors
        }

        // System.out.println("packageDetails: " + packageDetails);

        // Build SQL query
        String querySmart = (userMlData != null && !userMlData.isEmpty() && !userMlData.equals("0"))
                ? "AND cm.comp_qa_id IN (" + userMlData + ")"
                : (speciality == 0 ? "" : "AND cmTs.specialities_id IN (" + speciality + ")");

        String envStatus = (env != null)
                ? (env.equals("2") ? "AND cTenv.env = " + env : "AND (cTenv.env = 2 OR cTenv.env = " + env + ")")
                : "";

        String limit = "LIMIT " + limitFrom + ", " + limitTo;

        String sql = "SELECT " +
                "cm.video_archive_id as type_id, " +
                "cm.video_archive_question, " +
                "cm.video_archive_answer, " +
                "cm.video_archive_question_raw as title, " +
                "cm.video_archive_answer_raw as description, " +
                "cm.video_archive_file_img, " +
                "cm.video_archive_file_img_thumbnail, " +
                "cm.is_share, " +
                "cm.deeplink, " +
                "cm.start_like, " +
                "cm.added_on, " +
                "cm.publication_date, " +
                "cln.client_name, " +
                "cln.client_logo, " +
                "cm.duration, " +
                "cm.type, " +
                "cm.vendor, " +
                "cm.src, " +
                "ks.session_doctor_id, " +
                "msct.category_name, " +
                "msct.category_logo, " +

                "cTenv.price, " +
                "uTpyCont.status as user_contnet_payment_status, " +
                "cSummary.comment_count, " +
                "cSummary.rating_count, " +
                "cSummary.view_count, " +
                "GROUP_CONCAT(DISTINCT clintspon.client_name) as sponsor, " +
                "GROUP_CONCAT(DISTINCT clintspon.client_master_id) as sponsor_ids, " +
                "GROUP_CONCAT(DISTINCT clintspon.client_logo) as sponsor_logo, " +
                "GROUP_CONCAT(DISTINCT ms.specialities_name) as specialities_name, " +
                "GROUP_CONCAT(DISTINCT concat(ms.master_specialities_id, '#', ms.specialities_name) ) as specialities_ids_and_names, "
                +
                "cm.video_archive_speciality_id " +
                "FROM knwlg_video_archive as cm " +
                "JOIN video_archive_to_specialities as cmTs ON cmTs.video_archive_id = cm.video_archive_id " +
                "JOIN master_specialities_V1 as ms ON ms.master_specialities_id = cmTs.specialities_id " +
                "JOIN client_master as cln ON cln.client_master_id = cm.client_id " +
                "LEFT JOIN video_archive_to_sponsor as cmTspon ON cmTspon.video_archive_id = cm.video_archive_id " +
                "LEFT JOIN client_master as clintspon ON clintspon.client_master_id = cmTspon.sponsor_id " +
                "LEFT JOIN content_summary as cSummary ON cSummary.type_id = cm.video_archive_id AND cSummary.type = 3 "
                +
                "LEFT JOIN knwlg_sessions_V1 as ks ON ks.session_id = cm.video_archive_session_id " +
                "LEFT JOIN master_session_category as msct ON msct.mastersession_category_id = ks.category_id " +
                "LEFT JOIN content_to_env as cTenv ON cTenv.type_id = cm.video_archive_id AND cTenv.type = 3 " +
                "LEFT JOIN payment_user_to_content as uTpyCont ON uTpyCont.type_id = cm.video_archive_id AND uTpyCont.type = 3 AND uTpyCont.user_master_id = ? "
                +
                "WHERE cm.status = 3 " +
                "AND cm.publication_date <= CURDATE() " +
                querySmart + " " +
                "AND cm.privacy_status = 0 " +
                envStatus + " " +
                "GROUP BY cm.video_archive_id, cTenv.price, uTpyCont.status, cSummary.comment_count, cSummary.rating_count, cSummary.view_count "
                +
                "ORDER BY cm.video_archive_id DESC " + limit;

        // System.out.println("sql: " + sql);

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, userMasterId);

        // Process the results using a loop
        List<Map<String, Object>> processedResults = new ArrayList<>();
        int i = 1;
        for (Map<String, Object> rs : results) {

            List<Map<String, Object>> specilityArray = new CommonUtils()
                    .parseSpecilityString(rs.get("specialities_ids_and_names").toString());

            String description = rs.get("description").toString();
            String cleanDescription = CommonUtils.removeHtmlTags(description); // Remove HTML tags
            String truncatedDescription = new CommonUtils().truncateString(cleanDescription, 100);

            Map<String, Object> map = new HashMap<>();
            map.put("type_id", rs.get("type_id"));
            map.put("type", "video_archive");
            map.put("con_type", "video_archive");
            map.put("user_content_payment",
                    rs.get("user_content_payment_status") != null ? rs.get("user_content_payment_status").toString()
                            : null);
            map.put("price", rs.get("price") != null ? rs.get("price").toString() : null);
            map.put("answer", truncatedDescription);
            map.put("question", rs.get("title"));
            map.put("image", rs.get("video_archive_file_img"));

            map.put("vendor", rs.get("vendor"));
            map.put("src", rs.get("src"));

            map.put("comment_count", rs.get("comment_count"));
            map.put("rating", rs.get("rating"));
            map.put("view_count", rs.get("comment_count"));
            map.put("is_featured", rs.get("display_in_dashboard"));

            map.put("color", rs.get("color"));

            map.put("specialities", rs.get("specialities_name"));
            map.put("specialities_ids_and_names", specilityArray);

            map.put("env", env);

            LocalDateTime publicationDate = (LocalDateTime) rs.get("publication_date");
            // Step 2: Format the LocalDateTime object into a string
            // String formattedDateTime = formatDateTime(startDateTime);
            // List<Integer> dateTimeArray = (List<Integer>) rs.get("start_datetime");
            CommonUtils commonUtils = new CommonUtils();
            String formattedDateTime = commonUtils.formatDateTime(publicationDate);

            map.put("data", formattedDateTime);
            // map.put("data", rs.get("publication_date").toString());
            map.put("deeplink", rs.get("deeplink"));
            map.put("gl_deeplink", rs.get("gl_deeplink"));
            map.put("client_name", rs.get("client_name"));
            map.put("client_logo", rs.get("client_logo"));
            map.put("sponsor", rs.get("sponsorCM"));
            map.put("sponsor_logo", rs.get("sponsor_logoCM"));
            map.put("is_locked", packageDetails);
            processedResults.add(map);
            i++;
        }

        return CompletableFuture.completedFuture(processedResults);
    }

    @Async
    public CompletableFuture<List<Map<String, Object>>> getepubMl(
            Integer userMasterId, int limitFrom, int limitTo, int speciality, String userMlData, String env,
            int convert) {

        CompletableFuture<Map<String, Object>> packageDetailsFuture = commonRepository.checkUserPackage(
                userMasterId, "epub", env);

        String mobileview;
        if (convert == 1) {
            mobileview = " and cm.is_converted = 1";
        } else {
            mobileview = "";
        }

        Map<String, Object> packageDetails;
        try {
            packageDetails = packageDetailsFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return CompletableFuture.completedFuture(Collections.emptyList()); // Handle errors
        }

        // Build SQL query
        String querySmart = (userMlData != null && !userMlData.isEmpty() && !userMlData.equals("0"))
                ? "AND cm.comp_qa_id IN (" + userMlData + ")"
                : (speciality == 0 ? "" : "AND cmTs.specialities_id IN (" + speciality + ")");

        String envStatus = (env != null)
                ? (env.equals("2") ? "AND cTenv.env = " + env : "AND (cTenv.env = 2 OR cTenv.env = " + env + ")")
                : "";

        String limit = "LIMIT " + limitFrom + ", " + limitTo;

        String sql = "SELECT " +
                "cm.epub_id as type_id, " +
                "cm.epub_description as description, " +
                "cm.epub_title as title, " +
                "cm.is_share, " +
                "cm.epub_img, " +
                "cm.epub_img_thumbnail, " +
                "cm.epub_file, " +
                "cm.author, " +
                "cm.added_on, " +
                "cm.start_like, " +
                "cm.publication_date as publish_date, " +
                "cln.client_name, " +
                "cln.client_logo, " +
                "cTenv.price, " +
                "uTpyCont.status as user_contnet_payment_status, " +
                "cSummary.comment_count, " +
                "cSummary.rating_count, " +
                "cSummary.view_count, " +
                "cm.deeplink, " +
                "cm.gl_deeplink, " +
                "cm.color, " +
                "GROUP_CONCAT(DISTINCT ms.specialities_name) as specialities_name, " +
                "GROUP_CONCAT(DISTINCT CONCAT(ms.master_specialities_id, '#', ms.specialities_name)) as specialities_ids_and_names, "
                +
                "GROUP_CONCAT(DISTINCT clintspon.client_name) as sponsor, " +
                "GROUP_CONCAT(DISTINCT clintspon.client_master_id) as sponsor_ids, " +
                "GROUP_CONCAT(DISTINCT clintspon.client_logo) as sponsor_logo " +
                "FROM epub_master as cm " +
                "LEFT JOIN epub_to_specialities as cmTs ON cmTs.epub_id = cm.epub_id " +
                "LEFT JOIN master_specialities_V1 as ms ON ms.master_specialities_id = cmTs.specialities_id " +
                "LEFT JOIN client_master as cln ON cln.client_master_id = cm.client_id " +
                "LEFT JOIN epub_to_sponsor as cmTspon ON cmTspon.epub_id = cm.epub_id " +
                "LEFT JOIN client_master as clintspon ON clintspon.client_master_id = cmTspon.sponsor_id " +
                "LEFT JOIN content_summary as cSummary ON cSummary.type_id = cm.epub_id and cSummary.type = 9 " +
                "LEFT JOIN content_to_env as cTenv ON cTenv.type_id = cm.epub_id and cTenv.type = 9 " +
                "LEFT JOIN payment_user_to_content as uTpyCont ON uTpyCont.type_id = cm.epub_id and uTpyCont.type = 9 and uTpyCont.user_master_id = ? "
                +
                "WHERE cm.status = 3 " +
                "AND cm.publication_date <= CURDATE() " +
                querySmart + " " +
                "AND cm.privacy_status = 0 " +
                envStatus + " " +
                "GROUP BY cm.epub_id, cTenv.price, uTpyCont.status, cSummary.comment_count, cSummary.rating_count, cSummary.view_count "
                +
                "ORDER BY cm.epub_id DESC " + limit;

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, userMasterId);

        // Process the results using a loop
        List<Map<String, Object>> processedResults = new ArrayList<>();
        int i = 1;
        for (Map<String, Object> rs : results) {

            List<Map<String, Object>> specilityArray = new CommonUtils()
                    .parseSpecilityString(rs.get("specialities_ids_and_names").toString());

            String answeString = new CommonUtils().truncateString(rs.get("description").toString(), 100);

            Map<String, Object> map = new HashMap<>();
            map.put("type_id", rs.get("type_id"));
            map.put("type", "epub");
            map.put("con_type", "epub");
            map.put("user_content_payment",
                    rs.get("user_content_payment_status") != null ? rs.get("user_content_payment_status").toString()
                            : null);
            map.put("price", rs.get("price") != null ? rs.get("price").toString() : null);
            map.put("answer", answeString);
            map.put("question", rs.get("title"));
            map.put("epub_file", rs.get("epub_file"));
            map.put("image", rs.get("epub_img_thumbnail"));

            map.put("comment_count", rs.get("comment_count"));
            map.put("rating", rs.get("rating"));
            map.put("view_count", rs.get("comment_count"));
            map.put("is_featured", rs.get("display_in_dashboard"));

            map.put("color", rs.get("color"));

            map.put("specialities", rs.get("specialities_name"));
            map.put("specialities_ids_and_names", specilityArray);

            map.put("env", env);
            map.put("data", rs.get("publish_date").toString());
            map.put("deeplink", rs.get("deeplink"));
            map.put("gl_deeplink", rs.get("gl_deeplink"));
            map.put("client_name", rs.get("client_name"));
            map.put("client_logo", rs.get("client_logo"));
            map.put("sponsor", rs.get("sponsorCM"));
            map.put("sponsor_logo", rs.get("sponsor_logoCM"));
            map.put("is_locked", packageDetails);
            processedResults.add(map);
            i++;
        }

        return CompletableFuture.completedFuture(processedResults);
    }

    @Async
    public CompletableFuture<List<Map<String, Object>>> getSpqMl(
            Integer userMasterId, int limitFrom, int limitTo, int speciality, String userMlData, String env) {

        // Check user package
        CompletableFuture<Map<String, Object>> packageDetailsFuture = commonRepository.checkUserPackage(
                userMasterId, "survey", env);

        try {
            CompletableFuture.allOf(packageDetailsFuture).join();
        } catch (Exception e) {
            e.printStackTrace();
            return CompletableFuture.completedFuture(Collections.emptyList()); // Handle errors
        }

        Map<String, Object> packageDetails;
        try {
            packageDetails = packageDetailsFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return CompletableFuture.completedFuture(Collections.emptyList()); // Handle errors
        }

        // System.out.println("packageDetails: " + packageDetails);

        // Build SQL query
        String querySmart = (userMlData != null && !userMlData.isEmpty() && !userMlData.equals("0"))
                ? "AND sv.survey_id IN (" + userMlData + ")"
                : (speciality == 0 ? "" : "AND svts.speciality_id IN (" + speciality + ")");

        String envStatus = (env != null)
                ? (env.equals("2") ? "AND cTenv.env = " + env : "AND (cTenv.env = 2 OR cTenv.env = " + env + ")")
                : "";

        String limit = "LIMIT " + limitFrom + ", " + limitTo;

        // Fetch completed and incomplete survey IDs
        String sqlCompl = "SELECT sv.survey_id FROM survey_user_answer sv WHERE sv.user_master_id = ?";
        List<Map<String, Object>> complResults = jdbcTemplate.queryForList(sqlCompl, userMasterId);

        // Handle null values in complResults
        List<String> complIDs = complResults.stream()
                .map(rs -> {
                    Object surveyId = rs.get("survey_id");
                    return surveyId != null ? surveyId.toString() : null; // Handle null values
                })
                .filter(Objects::nonNull) // Filter out null values
                .collect(Collectors.toList());

        String sqlInCompl = "SELECT sv.survey_id FROM survey_user_incomplete_answer sv WHERE sv.status = 3 AND sv.user_master_id = ?";
        List<Map<String, Object>> inComplResults = jdbcTemplate.queryForList(sqlInCompl, userMasterId);

        // Handle null values in inComplResults
        List<String> inComplIDs = inComplResults.stream()
                .map(rs -> {
                    Object surveyId = rs.get("survey_id");
                    return surveyId != null ? surveyId.toString() : null; // Handle null values
                })
                .filter(Objects::nonNull) // Filter out null values
                .collect(Collectors.toList());

        List<String> allIDs = new ArrayList<>();
        allIDs.addAll(complIDs);
        allIDs.addAll(inComplIDs);
        List<String> uniqueIDs = allIDs.stream().distinct().collect(Collectors.toList());

        String qryStr = uniqueIDs.isEmpty() ? "" : "AND sv.survey_id NOT IN (" + String.join(",", uniqueIDs) + ")";

        // Main SQL query
        String sql = "SELECT " +
                "sv.survey_id, " +
                "sv.survey_title, " +
                "sv.survey_description, " +
                "sv.image, " +
                "sv.survey_time, " +
                "sv.question_count, " +
                "sv.survey_points, " +
                "sv.category, " +
                "sv.is_share, " +
                "sv.publishing_date, " +
                "svd.data, " +
                "GROUP_CONCAT(DISTINCT ms.specialities_name) as specialities_name, " +
                "GROUP_CONCAT(DISTINCT CONCAT(ms.master_specialities_id, '#', ms.specialities_name)) as specialities_ids_and_names, "
                +
                "cln.client_name, " +
                "cln.client_logo, " +
                "cTenv.price, " +
                "uTpyCont.status as user_content_payment_status, " +
                "GROUP_CONCAT(DISTINCT clintspon.client_name) as sponsor, " +
                "GROUP_CONCAT(DISTINCT clintspon.client_master_id) as sponsor_ids, " +
                "GROUP_CONCAT(DISTINCT clintspon.client_logo) as sponsor_logo " +
                "FROM survey sv " +
                "LEFT JOIN survey_to_speciality as svts ON svts.survey_id = sv.survey_id " +
                "LEFT JOIN master_specialities_V1 as ms ON ms.master_specialities_id = svts.speciality_id " +
                "LEFT JOIN client_master as cln ON cln.client_master_id = sv.client_id " +
                "LEFT JOIN survey_to_sponsor as suvTspon ON suvTspon.survey_id = sv.survey_id " +
                "LEFT JOIN client_master as clintspon ON clintspon.client_master_id = suvTspon.sponsor_id " +
                "LEFT JOIN survey_detail as svd ON svd.survey_id = sv.survey_id " +
                "LEFT JOIN content_to_env as cTenv ON cTenv.type_id = sv.survey_id AND cTenv.type = 6 " +
                "LEFT JOIN payment_user_to_content as uTpyCont ON uTpyCont.type_id = sv.survey_id AND uTpyCont.type = 6 AND uTpyCont.user_master_id = ? "
                +
                "WHERE sv.status = 3 " +
                "AND sv.privacy_status = 0 " +
                qryStr + " " +
                querySmart + " " +
                envStatus + " " +
                "AND DATE(sv.publishing_date) <= CURDATE() " +
                "AND DATE(sv.publishing_date) BETWEEN CURDATE() - INTERVAL 1 YEAR AND CURDATE() " +
                "GROUP BY sv.survey_id, svd.data, cTenv.price, uTpyCont.status " +
                limit;
        // System.out.println("sql: " + sql);

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, userMasterId);

        // Process the results
        List<Map<String, Object>> processedResults = new ArrayList<>();
        for (Map<String, Object> rs : results) {
            Map<String, Object> map = new HashMap<>();
            map.put("survey_id", rs.get("survey_id"));
            map.put("type_id", rs.get("survey_id"));
            map.put("trending_type", "survey");
            map.put("is_locked", packageDetails);
            map.put("price", rs.get("price"));
            map.put("is_share", rs.get("is_share"));
            map.put("user_content_payment", rs.get("user_content_payment_status"));
            map.put("type", "survey");
            map.put("category", rs.get("category"));
            map.put("survey_time", rs.get("survey_time"));
            map.put("question_count", rs.get("question_count"));
            map.put("point", rs.get("survey_points"));
            map.put("json_data", rs.get("data"));
            map.put("survey_title", rs.get("survey_title"));
            map.put("deeplink", rs.get("deeplink"));
            map.put("survey_description", rs.get("survey_description"));
            map.put("image", rs.get("image"));
            map.put("specialities_name", rs.get("specialities_name"));

            // Handle null value for specialities_ids_and_names
            String specialitiesIdsAndNames = rs.get("specialities_ids_and_names") != null
                    ? rs.get("specialities_ids_and_names").toString()
                    : "";
            map.put("specialities_ids_and_names", new CommonUtils().parseSpecilityString(specialitiesIdsAndNames));

            map.put("client_name", rs.get("client_name"));
            map.put("client_logo", rs.get("client_logo"));
            map.put("sponsor_name", rs.get("sponsor"));
            map.put("sponsor_id", rs.get("sponsor_ids"));
            map.put("sponsor_logo", rs.get("sponsor_logo"));
            map.put("publishing_date", rs.get("publishing_date"));
            map.put("date", rs.get("publishing_date").toString());

            processedResults.add(map);
        }

        return CompletableFuture.completedFuture(processedResults);
    }

    @Async
    public CompletableFuture<List<Map<String, Object>>> getSessionMl(
            Integer userMasterId, int limitFrom, int limitTo, String userClientIds, int speciality, String userMlData,
            String subtype, String env) {

        // Check user package asynchronously
        CompletableFuture<Map<String, Object>> packageDetailsFuture = commonRepository.checkUserPackage(
                userMasterId, "session", env);

        // Fetch booked session IDs asynchronously
        CompletableFuture<List<String>> bookedSessionIdsFuture = getBookedSessionIds(userMasterId);

        try {
            // Wait for both asynchronous tasks to complete
            CompletableFuture.allOf(packageDetailsFuture, bookedSessionIdsFuture).join();
        } catch (Exception e) {
            e.printStackTrace();
            return CompletableFuture.completedFuture(Collections.emptyList()); // Handle errors
        }

        Map<String, Object> packageDetails;
        List<String> bookedSessionIds;
        try {
            packageDetails = packageDetailsFuture.get();
            bookedSessionIds = bookedSessionIdsFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return CompletableFuture.completedFuture(Collections.emptyList()); // Handle errors
        }

        // System.out.println("speciality:**************** " + speciality);
        // System.out.println("bookedSessionIds: " + bookedSessionIds);

        // Build SQL query based on subtype
        String querySmart = (userMlData != null && !userMlData.isEmpty() && !userMlData.equals("0"))
                ? "AND ks.session_id IN (" + userMlData + ")"
                : (speciality == 0 ? "" : "AND sTs.specialities_id IN (" + speciality + ")");

        String envStatus = (env != null)
                ? (env.equals("2") ? "AND cTenv.env = " + env : "AND (cTenv.env = 2 OR cTenv.env = " + env + ")")
                : "";

        String limit = "LIMIT " + limitFrom + ", " + limitTo;

        // Fetch child session IDs
        List<String> childSessionIDs = getAllChildSessionIDs();
        String childSessionStr = childSessionIDs.isEmpty() ? ""
                : "AND ks.session_id NOT IN (" + String.join(",", childSessionIDs) + ")";

        // Base SQL query (common for all cases)
        String baseSql = "SELECT " +
                "ks.session_id, " +
                "ks.start_datetime, " +
                "ks.is_multiday_session, " +
                "ks.session_status, " +
                "ks.session_description as description, " +
                "ks.session_topic, " +
                "ks.session_topic as title, " +
                "ks.gl_deeplink, " +
                "ks.deeplink, " +
                "ks.color, " +
                "cln.client_name, " +
                "cln.client_logo, " +
                "cTenv.price, " +
                "uTpyCont.status as user_content_payment_status, " +
                "GROUP_CONCAT(DISTINCT ms.specialities_name) as specialities_name, " +
                "GROUP_CONCAT(DISTINCT CONCAT(ms.master_specialities_id, '#', ms.specialities_name)) as specialities_ids_and_names, "
                +
                "GROUP_CONCAT(DISTINCT clintspon.client_name) as sponsor, " +
                "GROUP_CONCAT(DISTINCT clintspon.client_master_id) as sponsor_ids, " +
                "GROUP_CONCAT(DISTINCT clintspon.client_logo) as sponsor_logo, " +
                "msct.category_name, " +
                "msct.category_logo, " +
                "GROUP_CONCAT(DISTINCT sdoc.sessions_doctors_id SEPARATOR '----') as session_doctor_id, " +
                "GROUP_CONCAT(DISTINCT sdoc.doctor_name SEPARATOR '----') as doctor_name, " +
                "GROUP_CONCAT(DISTINCT sdoc.speciality SEPARATOR '----') as speciality, " +
                "stci.cover_image1, " +
                "stci.cover_image2, " +
                "stci.cover_image3, " +
                "stci.cover_image4, " +
                "stci.cover_image5, " +
                "GROUP_CONCAT(DISTINCT sdoc.profile SEPARATOR '----') as profile, " +
                "GROUP_CONCAT(DISTINCT sdoc.profile_image SEPARATOR '----') as profile_images " +
                "FROM knwlg_sessions_V1 as ks " +
                "LEFT JOIN session_to_specialities as sTs ON sTs.session_id = ks.session_id " +
                "LEFT JOIN master_specialities_V1 as ms ON ms.master_specialities_id = sTs.specialities_id " +
                "LEFT JOIN client_master as cln ON cln.client_master_id = ks.client_id " +
                "LEFT JOIN session_to_cover_image as stci ON stci.session_id = ks.session_id " +
                "LEFT JOIN session_to_sponsor as sTspon ON sTspon.session_id = ks.session_id " +
                "LEFT JOIN client_master as clintspon ON clintspon.client_master_id = sTspon.sponsor_id " +
                "LEFT JOIN master_session_category as msct ON msct.mastersession_category_id = ks.category_id " +
                "LEFT JOIN session_to_sessiondoctor as sTdoc ON sTdoc.session_id = ks.session_id " +
                "LEFT JOIN knwlg_sessions_doctors as sdoc ON sdoc.sessions_doctors_id = sTdoc.sessions_doctors_id " +
                "LEFT JOIN content_to_env as cTenv ON cTenv.type_id = ks.session_id AND cTenv.type = 2 " +
                "LEFT JOIN payment_user_to_content as uTpyCont ON uTpyCont.type_id = ks.session_id AND uTpyCont.type = 2 AND uTpyCont.user_master_id = ? "
                +
                "WHERE ks.status = 3 " +
                querySmart + " " +
                envStatus + " " +
                "AND ks.privacy_status = 0 ";

        // Add case-specific conditions
        String sql;
        switch (subtype) {
            case "normal":
                String sqlStr = bookedSessionIds.isEmpty() ? ""
                        : "AND ks.session_id NOT IN (" + String.join(",", bookedSessionIds) + ")";
                sql = baseSql +
                        "AND (ks.session_status = 1 OR ks.session_status = 2 OR ks.session_status = 4) " +
                        childSessionStr + " " +
                        "AND ks.end_datetime >= NOW() " +
                        "GROUP BY ks.session_id, cTenv.price, uTpyCont.status, stci.cover_image1, stci.cover_image2, stci.cover_image3, stci.cover_image4, stci.cover_image5 "
                        +
                        "ORDER BY ks.start_datetime ASC " + limit;
                break;

            case "reserved":
                String reservedSqlStr = bookedSessionIds.isEmpty() ? ""
                        : "AND ks.session_id IN (" + String.join(",", bookedSessionIds) + ")";
                sql = baseSql +
                        "AND ks.start_datetime >= NOW() " +
                        "AND ks.session_status IN (1, 2, 7) " +
                        reservedSqlStr + " " +
                        "GROUP BY ks.session_id, cTenv.price, uTpyCont.status, stci.cover_image1, stci.cover_image2, stci.cover_image3, stci.cover_image4, stci.cover_image5 "
                        +
                        "ORDER BY ks.start_datetime ASC " + limit;
                break;

            case "recorded":
                // Recorded sessions have a different base query
                sql = getRecordedSessionSql(userMasterId, querySmart, envStatus, limit);
                break;

            case "upcoming":
                sql = baseSql +
                        "AND (ks.session_status = 1 OR ks.session_status = 4) " +
                        "AND ks.end_datetime >= NOW() " +
                        "GROUP BY ks.session_id, cTenv.price, uTpyCont.status, stci.cover_image1, stci.cover_image2, stci.cover_image3, stci.cover_image4, stci.cover_image5 "
                        +
                        "ORDER BY ks.start_datetime ASC " + limit;
                break;

            default:
                sql = baseSql +
                        "AND (ks.session_status = 1 OR ks.session_status = 4) " +
                        "AND ks.end_datetime >= NOW() " +
                        "GROUP BY ks.session_id, cTenv.price, uTpyCont.status, stci.cover_image1, stci.cover_image2, stci.cover_image3, stci.cover_image4, stci.cover_image5 "
                        +
                        "ORDER BY ks.start_datetime ASC " + limit;
                // System.out.println("Server sql: ************************************* " +
                // sql);
                break;
        }

        System.out.println("sql session: " + sql);

        // Execute the query
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, userMasterId);

        System.out.println("results session ML: " + results);

        // Process the results
        List<Map<String, Object>> processedResults = processResults(results, packageDetails);

        return CompletableFuture.completedFuture(processedResults);
    }

    @Async
    public CompletableFuture<List<Map<String, Object>>> getTrainingMl(
            Integer userMasterId, int limitFrom, int limitTo, int speciality, String userMlData, String env) {

        System.out.println("userMasterId: " + userMasterId);

        CompletableFuture<Map<String, Object>> packageDetailsFuture = commonRepository.checkUserPackage(
                userMasterId, "training", env);

        // try {
        // CompletableFuture.allOf(packageDetailsFuture).join();
        // } catch (Exception e) {
        // e.printStackTrace();
        // return CompletableFuture.completedFuture(Collections.emptyList());
        // }

        Map<String, Object> packageDetails;
        try {
            packageDetails = packageDetailsFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        String querySmart = (userMlData != null && !userMlData.isEmpty() && !userMlData.equals("0"))
                ? "AND tm.id IN (" + userMlData + ")"
                : (speciality == 0 ? "" : "AND tts.specialities_id IN (" + speciality + ")");

        String envStatus = (env != null)
                ? (env.equals("2") ? "AND cTenv.env = " + env : "AND (cTenv.env = 2 OR cTenv.env = " + env + ")")
                : "";

        String limit = "LIMIT " + limitFrom + ", " + limitTo;

        String sql = "SELECT " +
                "tm.id, " +
                "tm.preview_image, " +
                "tm.published_date, " +
                "tm.gl_deeplink, " +
                "tm.deeplink, " +
                "tm.title, " +
                "tm.featured_video, " +
                "tm.color, " +
                "tm.duration, " +
                "tm.cert_template_id, " +
                "cTenv.price, " +
                "tm.is_share, " +
                "uTpyCont.status as user_content_payment_status, " +
                "GROUP_CONCAT(DISTINCT clintspon.client_name) as sponsor, " +
                "GROUP_CONCAT(DISTINCT Tdoc.session_doctor_id) as session_doctor_id, " +
                "GROUP_CONCAT(DISTINCT clintspon.client_logo) as sponsor_logo, " +
                "GROUP_CONCAT(DISTINCT clintspon.client_master_id) as sponsor_ids, " +
                "cSummary.comment_count, " +
                "cSummary.rating_count, " +
                "cSummary.view_count, " +
                "(SELECT COUNT(id) FROM training_module_content WHERE type='comp' AND status=3 AND training_id=tm.id) as total_medwiki, "
                +
                "(SELECT COUNT(id) FROM training_module_content WHERE type='session' AND status=3 AND training_id=tm.id) as total_session, "
                +
                "(SELECT COUNT(id) FROM training_module_content WHERE type='survey' AND status=3 AND training_id=tm.id) as total_survey, "
                +
                "(SELECT COUNT(id) FROM training_module_content WHERE type='live_video' AND status=3 AND training_id=tm.id) as total_live_training, "
                +
                "(SELECT COUNT(id) FROM training_module_content WHERE type='clinical_video' AND status=3 AND training_id=tm.id) as total_clinical_video, "
                +
                "(SELECT COUNT(id) FROM training_module WHERE training_id=tm.id AND status=3) as count_module " +
                "FROM training_master as tm " +
                "LEFT JOIN training_to_sponsor as ts ON tm.id = ts.training_id " +
                "LEFT JOIN client_master as clintspon ON ts.sponsor_id = clintspon.client_master_id " +
                "LEFT JOIN training_to_speciality tts ON tts.training_id = tm.id " +
                "LEFT JOIN training_to_session_doctor as Tdoc ON Tdoc.training_id = tm.id " +
                "LEFT JOIN content_summary as cSummary ON cSummary.type_id = tm.id AND cSummary.type = 4 " +
                "LEFT JOIN content_to_env as cTenv ON cTenv.type_id = tm.id AND cTenv.type = 4 " +
                "LEFT JOIN payment_user_to_content as uTpyCont ON uTpyCont.type_id = tm.id AND uTpyCont.type = 4 AND uTpyCont.user_master_id = ? "
                +
                "WHERE tm.status = 3 " +
                "AND DATE(tm.published_date) <= CURDATE() " +
                "AND tm.privacy_status = 0 " +
                querySmart + " " +
                envStatus + " " +
                "GROUP BY tm.id, cTenv.price, uTpyCont.status, cSummary.comment_count, cSummary.rating_count, cSummary.view_count "
                +
                "ORDER BY tm.published_date DESC, tm.display_in_dashboard DESC " + limit;

        // System.out.println("sql: " + sql);

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, userMasterId);

        List<Map<String, Object>> processedResults = new ArrayList<>();
        for (Map<String, Object> rs : results) {

            // Process content count
            Map<String, Object> contentCount = new HashMap<>();
            contentCount.put("comp", rs.get("total_medwiki"));
            contentCount.put("session", rs.get("total_session"));
            contentCount.put("survey", rs.get("total_survey"));
            contentCount.put("video_archive", rs.get("total_clinical_video"));
            contentCount.put("live_training", rs.get("total_live_training"));

            // Process sponsors
            List<Map<String, Object>> allSponsors = new ArrayList<>();
            if (rs.get("sponsor_logo") != null) {
                String[] sponsorLogos = rs.get("sponsor_logo").toString().split(",");
                String[] sponsorNames = rs.get("sponsor").toString().split(",");

                for (int i = 0; i < sponsorLogos.length; i++) {
                    Map<String, Object> sponsor = new HashMap<>();
                    sponsor.put("name", sponsorNames[i]);
                    sponsor.put("logo", sponsorLogos[i]);
                    allSponsors.add(sponsor);

                }
            }

            // Process session doctors
            // List<Map<String, Object>> sessionDoctors = new ArrayList<>();
            // if (rs.get("session_doctor_id") != null) {
            // String[] doctorIds = rs.get("session_doctor_id").toString().split(",");
            // for (String doctorId : doctorIds) {
            // sessionDoctors.addAll(parseSessionDoctors(doctorId));
            // }
            // }

            // Get module details
            CompletableFuture<List<Map<String, Object>>> moduleDetailsFuture = getModuleDetails((Integer) rs.get("id"));
            List<Map<String, Object>> moduleDetails = new ArrayList<>();
            try {
                moduleDetails = moduleDetailsFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }

            Map<String, Object> map = new HashMap<>();
            map.put("id", rs.get("id"));
            map.put("type", "training");
            map.put("is_locked", packageDetails);
            map.put("price", rs.get("price"));
            map.put("user_content_payment", rs.get("user_content_payment_status"));
            map.put("is_share", rs.get("is_share"));
            map.put("image", rs.get("preview_image"));
            map.put("title", rs.get("title"));
            map.put("featured_video", rs.get("featured_video"));
            map.put("color", rs.get("color") != null ? rs.get("color") : "#eb34e5");
            map.put("module_count", rs.get("count_module"));
            LocalDateTime startDateTime = (LocalDateTime) rs.get("published_date");
            String formattedDateTime = commonUtils.formatDateTime(startDateTime);

            map.put("date", formattedDateTime);
            // map.put("date", rs.get("published_date"));
            map.put("training_module_content", contentCount);
            map.put("preview_image", rs.get("preview_image"));
            map.put("sponsor_name", rs.get("sponsor"));
            map.put("sponsor_logo", rs.get("sponsor_logo"));
            map.put("all_sponsor", allSponsors);
            // map.put("session_doctor_entities", sessionDoctors);
            map.put("duration", rs.get("duration"));
            map.put("deeplink", env.equals("1") ? (rs.get("gl_deeplink") != null ? rs.get("gl_deeplink") : 0)
                    : (rs.get("deeplink") != null ? rs.get("deeplink") : 0));
            map.put("rating", rs.get("rating_count"));
            map.put("is_certificate", rs.get("cert_template_id") != null);
            // System.out.println("is_certificate: " + rs.get("cert_template_id"));
            map.put("module_contents", moduleDetails);
            // map.put("is_certificate", rs.get("cert_template_id") != null ? (Integer)
            // rs.get("cert_template_id") : 0);

            processedResults.add(map);
        }

        return CompletableFuture.completedFuture(processedResults);
    }

    @Async
    private CompletableFuture<List<Map<String, Object>>> getModuleDetails(Integer trainingId) {
        String sql = "SELECT " +
                "tmc.module_id, " +
                "tmc.type, " +
                "tmc.type_id, " +
                "ksv.session_topic, " +
                "ksv.cover_image, " +
                "kcv.comp_qa_topic, " +
                "kcv.comp_qa_file_img, " +
                "s.survey_title, " +
                "s.image, " +
                "kva.video_archive_topic, " +
                "kva.video_archive_file_img " +
                "FROM training_module_content as tmc " +
                "LEFT JOIN knwlg_sessions_V1 as ksv ON (tmc.type='session' AND ksv.session_id=tmc.type_id) " +
                "LEFT JOIN knwlg_compendium_V1 as kcv ON (tmc.type='comp' AND kcv.comp_qa_id=tmc.type_id) " +
                "LEFT JOIN survey as s ON (tmc.type='survey' AND s.survey_id=tmc.type_id) " +
                "LEFT JOIN knwlg_video_archive as kva ON (tmc.type='clinical_video' AND kva.video_archive_id=tmc.type_id) "
                +
                "WHERE tmc.training_id = ? AND tmc.status = 3 " +
                "AND (" +
                "   (s.status=3 AND s.privacy_status = 0) OR " +
                "   (ksv.status = 3 AND ksv.privacy_status = 0) OR " +
                "   (kcv.status = 3 AND kcv.privacy_status = 0) OR " +
                "   (kva.status = 3 AND kva.privacy_status = 0) OR " +
                "   (s.status = 3 AND s.privacy_status != 1)" +
                ") " +
                "GROUP BY tmc.id " +
                "ORDER BY tmc.id ASC";

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, trainingId);
        List<Map<String, Object>> processedResults = new ArrayList<>();

        for (Map<String, Object> row : results) {
            Map<String, Object> response = new HashMap<>();
            String type = (String) row.get("type");
            response.put("type", type);
            response.put("type_id", row.get("type_id"));

            switch (type) {
                case "comp":
                    response.put("topic", row.get("comp_qa_topic"));
                    response.put("image", row.get("comp_qa_file_img"));
                    break;
                case "session":
                    response.put("topic", row.get("session_topic"));
                    response.put("image", row.get("cover_image"));
                    break;
                case "survey":
                    response.put("topic", row.get("survey_title"));
                    response.put("image", row.get("image"));
                    break;
                case "clinical_video":
                    response.put("topic", row.get("video_archive_topic"));
                    response.put("image", row.get("video_archive_file_img"));
                    break;
            }
            processedResults.add(response);
        }

        return CompletableFuture.completedFuture(processedResults);
    }

    @Async
    public CompletableFuture<List<String>> getBookedSessionIds(Integer userMasterId) {
        String sql = "SELECT ks.knwlg_sessions_id FROM knwlg_sessions_participant ks WHERE ks.participant_id = ?";
        return CompletableFuture.supplyAsync(() -> {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, userMasterId);
            return results.stream()
                    .map(rs -> rs.get("knwlg_sessions_id").toString())
                    .collect(Collectors.toList());
        });
    }

    // Helper method to get the SQL for recorded sessions
    private String getRecordedSessionSql(Integer userMasterId, String querySmart, String envStatus, String limit) {
        return "SELECT " +
                "cm.video_archive_id as type_id, " +
                "cm.src, " +
                "cm.video_archive_question, " +
                "cm.video_archive_answer, " +
                "cm.video_archive_question_raw, " +
                "cm.video_archive_answer_raw, " +
                "cm.video_archive_file_img, " +
                "cm.video_archive_file_img_thumbnail, " +
                "GROUP_CONCAT(DISTINCT ms.specialities_name) as specialities_name, " +
                "GROUP_CONCAT(DISTINCT CONCAT(ms.master_specialities_id, '#', ms.specialities_name)) as specialities_ids_and_names, "
                +
                "cm.video_archive_speciality_id, " +
                "cm.deeplink, " +
                "kvtd.play_time, " +
                "cm.duration, " +
                "cm.is_share, " +
                "cTenv.price, " +
                "uTpyCont.status as user_content_payment_status, " +
                "GROUP_CONCAT(DISTINCT clintspon.client_name) as sponsor, " +
                "GROUP_CONCAT(DISTINCT clintspon.client_logo) as sponsor_logo, " +
                "(SELECT COUNT(rt.rating) FROM knwlg_rating rt WHERE rt.post_id = cm.video_archive_id AND rt.post_type = 'video_archive' AND rt.rating != 0) as averageRating, "
                +
                "rtmy.rating as myrating, " +
                "kv.status as vault, " +
                "ks.session_doctor_id " +
                "FROM knwlg_video_archive as cm " +
                "LEFT JOIN video_archive_to_specialities as cmTs ON cmTs.video_archive_id = cm.video_archive_id " +
                "LEFT JOIN master_specialities_V1 as ms ON ms.master_specialities_id = cmTs.specialities_id " +
                "LEFT JOIN knwlg_video_tracking_data as kvtd ON kvtd.content_id = cm.video_archive_id AND kvtd.content_type = 'video_archive' AND kvtd.user_master_id = ? "
                +
                "LEFT JOIN video_archive_to_sponsor as cmTspon ON cmTspon.video_archive_id = cm.video_archive_id " +
                "LEFT JOIN client_master as clintspon ON clintspon.client_master_id = cmTspon.sponsor_id " +
                "LEFT JOIN knwlg_sessions_V1 as ks ON ks.session_id = cm.video_archive_session_id " +
                "LEFT JOIN master_session_category as msct ON msct.mastersession_category_id = ks.category_id " +
                "LEFT JOIN knwlg_vault as kv ON kv.post_id = cm.video_archive_id AND kv.type_text = 'video_archive' AND kv.user_id = ? "
                +
                "LEFT JOIN knwlg_rating as rtmy ON rtmy.post_id = cm.video_archive_id AND rtmy.post_type = 'video_archive' AND rtmy.rating != 0 AND rtmy.user_master_id = ? "
                +
                "LEFT JOIN knwlg_rating as rt ON rt.post_id = cm.video_archive_id AND rt.post_type = 'video_archive' " +
                "LEFT JOIN client_master as cln ON cln.client_master_id = cm.client_id " +
                "LEFT JOIN content_to_env as cTenv ON cTenv.type_id = cm.video_archive_id AND cTenv.type = 3 " +
                "LEFT JOIN payment_user_to_content as uTpyCont ON uTpyCont.type_id = cm.video_archive_id AND uTpyCont.type = 3 AND uTpyCont.user_master_id = ? "
                +
                "WHERE cm.status = 3 AND cm.privacy_status = 0 " +
                querySmart + " " +
                envStatus + " " +
                "GROUP BY cm.video_archive_id, kvtd.play_time, cTenv.price, uTpyCont.status, kv.status, rtmy.rating " +
                "ORDER BY cm.display_in_dashboard DESC, cm.publication_date DESC " + limit;
    }

    // Helper method to process results
    private List<Map<String, Object>> processResults(List<Map<String, Object>> results,
            Map<String, Object> packageDetails) {
        List<Map<String, Object>> processedResults = new ArrayList<>();
        for (Map<String, Object> rs : results) {
            Map<String, Object> map = new HashMap<>();

            map.put("session_id", rs.get("session_id"));
            map.put("type_id", rs.get("session_id"));
            map.put("trending_type", "session");
            map.put("is_locked", packageDetails);
            map.put("price", rs.get("price"));
            map.put("user_content_payment", rs.get("user_content_payment_status"));
            map.put("is_share", rs.get("is_share"));
            map.put("type", "session");

            LocalDateTime startDateTime = (LocalDateTime) rs.get("start_datetime");
            String formattedDateTime = commonUtils.formatDateTime(startDateTime);

            map.put("start_datetime", formattedDateTime);
            map.put("end_datetime", rs.get("end_datetime"));
            map.put("session_topic", rs.get("title"));
            map.put("description", rs.get("description"));
            map.put("specialities_name", rs.get("specialities_name"));
            map.put("specialities_ids_and_names",
                    commonUtils.parseSpecilityString(rs.get("specialities_ids_and_names").toString()));
            map.put("sponsor_name", rs.get("sponsor"));
            map.put("sponsor_logo", rs.get("sponsor_logo"));
            map.put("deeplink", rs.get("deeplink"));
            map.put("cover_image", rs.get("cover_image1"));
            map.put("session_doctor_id", rs.get("session_doctor_id"));
            map.put("session_doctor_entities", parseSessionDoctors(rs.get("session_doctor_id").toString()));

            processedResults.add(map);
        }
        return processedResults;
    }

    // Helper method to fetch child session IDs
    public List<String> getAllChildSessionIDs() {
        Map<String, Object> allChildSession = getAllChildSession();
        return (List<String>) allChildSession.get("sessions");
    }

    public Map<String, Object> getAllChildSession() {
        // SQL query to fetch child session data
        String sql = "SELECT kstc.multidaysession_id, " +
                "GROUP_CONCAT(DISTINCT kstc.childsession_id SEPARATOR ',') as childsession_id, " +
                "GROUP_CONCAT(DISTINCT sts.sessions_doctors_id SEPARATOR ',') as session_doctor_id " +
                "FROM knwlg_session_to_child as kstc " +
                "LEFT JOIN session_to_sessiondoctor as sts ON sts.session_id = kstc.childsession_id " +
                "GROUP BY kstc.multidaysession_id";

        // Execute the query
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);

        // Process the results
        Map<String, Object> response = new HashMap<>();
        List<String> allChildSessionIds = new ArrayList<>();
        Map<String, Integer> sessionCount = new HashMap<>();
        Map<String, Integer> doctorCount = new HashMap<>();
        Map<String, String> sessionDoctors = new HashMap<>();

        for (Map<String, Object> row : results) {
            String multiDaySessionId = row.get("multidaysession_id").toString();
            String childSessionIds = row.get("childsession_id").toString();
            String sessionDoctorIds = row.get("session_doctor_id").toString();

            // Split child session IDs and session doctor IDs
            List<String> childIds = Arrays.asList(childSessionIds.split(","));
            List<String> doctorIds = Arrays.asList(sessionDoctorIds.split(","));

            // Add all child session IDs to the main list
            allChildSessionIds.addAll(childIds);

            // Store the count of child sessions and session doctors for each multi-day
            // session
            sessionCount.put(multiDaySessionId, childIds.size());
            doctorCount.put(multiDaySessionId, doctorIds.size());
            sessionDoctors.put(multiDaySessionId, sessionDoctorIds);
        }

        // Build the response
        response.put("doctorcount", doctorCount);
        response.put("sessioncount", sessionCount);
        response.put("sessions", allChildSessionIds);
        response.put("session_doctors", sessionDoctors);

        return response;
    }

    // Helper method to parse session doctors
    private List<Map<String, Object>> parseSessionDoctors(String sessionDoctorIds) {
        // Fetch and parse session doctor details
        String[] ids = sessionDoctorIds.split("----");

        // System.out.println("ids: " + Arrays.toString(ids));
        List<Map<String, Object>> doctors = new ArrayList<>();
        for (String id : ids) {
            String sql = "SELECT sessions_doctors_id,doctor_name,profile_image,display_speciality FROM knwlg_sessions_doctors WHERE sessions_doctors_id = ?";
            Map<String, Object> doctor = jdbcTemplate.queryForMap(sql, id);
            doctors.add(doctor);
        }
        return doctors;
    }

}