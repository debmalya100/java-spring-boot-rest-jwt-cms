package com.knowledge.api.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;

import com.fasterxml.jackson.core.type.TypeReference; // Add this import
import com.fasterxml.jackson.databind.ObjectMapper; // Add this import
import com.knowledge.api.utils.CommonUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Repository
@RequiredArgsConstructor
public class KnowledgeRepository {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    private CommonUtils commonUtils;

    public Integer getUserType(Integer userMasterId) {
        String sql = "SELECT master_user_type_id FROM user_master WHERE user_master_id = ?";
        return jdbcTemplate.queryForObject(sql, Integer.class, userMasterId);
    }

    public String getUserEnv(Integer userMasterId) {
        String sql = "SELECT etc.env_id " +
                "FROM user_master um " +
                "LEFT JOIN env_to_country etc ON etc.country_id = um.country_code " +
                "WHERE um.user_master_id = ?";

        System.out.println("SQL: " + sql);
        return jdbcTemplate.queryForObject(sql, String.class, userMasterId);
    }

    public Map<String, Object> checkUserPackage(Integer userMasterId, String module, String env) {

        // System.out.println("USer env----------" + env);
        // Step 1: Fetch user package details
        String sql = "SELECT " +
                "um.status, " +
                "um.country_code, " +
                "up.payment_package_id " +
                "FROM user_master um " +
                "LEFT JOIN payment_user_to_package up ON up.user_master_id = um.user_master_id " +
                "WHERE um.user_master_id = ?";

        Map<String, Object> result = new HashMap<>();

        try {

            // System.out.println("SQL: " + sql);
            Map<String, Object> userPackage = jdbcTemplate.queryForObject(sql,
                    (rs, rowNum) -> {
                        Map<String, Object> row = new HashMap<>();
                        row.put("status", rs.getInt("status"));
                        row.put("country_code", rs.getString("country_code"));
                        row.put("payment_package_id", rs.getObject("payment_package_id"));
                        return row;
                    },
                    userMasterId);

            // Step 2: Check if the user has a package
            if (userPackage != null && userPackage.get("payment_package_id") != null) {
                Integer paymentPackageId = (Integer) userPackage.get("payment_package_id");

                // Step 3: Fetch package settings
                String packageSettingsSql = "SELECT package_setting FROM payment_package WHERE id = ?";
                String packageSettingsJson = jdbcTemplate.queryForObject(packageSettingsSql, String.class,
                        paymentPackageId);

                // Step 4: Parse package settings JSON
                if (packageSettingsJson != null) {
                    ObjectMapper objectMapper = new ObjectMapper();
                    Map<String, Object> packageSettings = objectMapper.readValue(packageSettingsJson,
                            new TypeReference<Map<String, Object>>() {
                            });

                    // Step 5: Check module access
                    List<Map<String, Object>> menu = (List<Map<String, Object>>) packageSettings.get("menu");
                    for (Map<String, Object> menuItem : menu) {
                        if ("explore_more".equals(menuItem.get("module_name"))) {
                            List<Map<String, Object>> subMenu = (List<Map<String, Object>>) menuItem.get("sub_menu");
                            for (Map<String, Object> subMenuItem : subMenu) {
                                if (module.equals(subMenuItem.get("module_name"))) {
                                    boolean isPremium = "1".equals(subMenuItem.get("premium").toString());
                                    result.put("display_icon", subMenuItem.get("displayIcon"));
                                    result.put("content_access", !isPremium);
                                    return result;
                                }
                            }
                        } else if (module.equals(menuItem.get("module_name"))) {
                            boolean isPremium = "1".equals(menuItem.get("premium").toString());
                            result.put("display_icon", menuItem.get("displayIcon"));
                            result.put("content_access", !isPremium);
                            return result;
                        }
                    }
                }
            }

            // Default values if no package or module is found
            result.put("currency", ("1".equals(env)) ? "INR" : "USD");
            result.put("display_icon", false);
            result.put("content_access", true);
        } catch (Exception e) {
            // Handle exceptions (e.g., JSON parsing errors, database errors)
            e.printStackTrace();
            result.put("display_icon", false);
            result.put("content_access", true);
        }

        return result;
    }

    public Map<String, Object> getCompendiumDetails(String typeId, Integer userMasterId, Integer userType, String env) {

        String sql = "SELECT "
                + "cm.comp_qa_id as type_id, "
                + "cm.comp_qa_question, "
                + "cm.comp_qa_answer, "
                + "cm.comp_qa_citation, "
                + "cm.comp_qa_answer_raw, "
                + "cm.comp_qa_question_raw, "
                + "cm.comp_qa_file_img, "
                + "cm.added_on, "
                + "cm.env, "
                + "cm.deeplink, "
                + "cm.gl_deeplink, "
                + "cm.comp_qa_tags, "
                + "cm.privacy_status, "
                + "cm.publication_date, "
                + "cm.is_like, "
                + "cm.is_comment, "
                + "cln.client_name, "
                + "cln.client_logo, "
                + "cm.status, "
                + "cm.type, "
                + "cm.vendor, "
                + "cm.comment_status, "
                + "cm.src, "
                + "cm.start_like, "
                + "cTenv.price,"
                + "uTpyCont.status as user_contnet_payment_status,"

                + "GROUP_CONCAT(DISTINCT clintspon.client_name) as sponsor, "
                + "GROUP_CONCAT(DISTINCT clintspon.client_logo) as sponsor_logo "
                + "FROM knwlg_compendium_V1 AS cm "
                + "LEFT JOIN compendium_to_sponsor AS cmTspon ON cmTspon.comp_qa_id = cm.comp_qa_id "
                + "LEFT JOIN client_master AS clintspon ON clintspon.client_master_id = cmTspon.sponsor_id "
                + "LEFT JOIN client_master AS cln ON cln.client_master_id = cm.client_id "

                + "LEFT JOIN content_to_env as cTenv ON cTenv.type_id = cm.comp_qa_id and  cTenv.type = 1 "
                + "LEFT JOIN payment_user_to_content as uTpyCont ON uTpyCont.type_id = cm.comp_qa_id and uTpyCont.type = 1 and uTpyCont.user_master_id = ? "

                + "WHERE cm.status IN (3, 5) "
                + "AND cm.comp_qa_id = ? "
                + "AND cm.privacy_status IN (0, 1, 2)";

        // System.out.println("Query: " + sql);

        List<Map<String, Object>> results = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("type_id", rs.getString("type_id"));
                    result.put("con_type", rs.getString("type"));
                    result.put("type", "comp");
                    result.put("user_content_payment", rs.getString("user_contnet_payment_status"));
                    result.put("price", rs.getString("price"));
                    // result.put("comp_qa_question", rs.getString("comp_qa_question"));
                    result.put("answer_htm", rs.getString("comp_qa_answer"));
                    result.put("comp_qa_citation", rs.getString("comp_qa_citation"));
                    // result.put("comp_qa_answer_raw", rs.getString("comp_qa_answer_raw"));
                    result.put("question", rs.getString("comp_qa_question_raw"));
                    result.put("comp_qa_file_img", rs.getString("comp_qa_file_img"));
                    result.put("added_on", rs.getTimestamp("added_on"));
                    result.put("env", rs.getString("env"));
                    result.put("deeplink", rs.getString("deeplink"));
                    result.put("gl_deeplink", rs.getString("gl_deeplink"));
                    // result.put("comp_qa_tags", rs.getString("comp_qa_tags"));
                    result.put("privacy_status", rs.getString("privacy_status"));
                    result.put("publication_date", rs.getDate("publication_date"));
                    result.put("is_like", rs.getBoolean("is_like"));
                    result.put("is_comment", rs.getBoolean("is_comment"));
                    result.put("client_name", rs.getString("client_name"));
                    result.put("client_logo", rs.getString("client_logo"));
                    // result.put("status", rs.getString("status"));

                    result.put("vendor", rs.getString("vendor"));
                    result.put("comment_status", rs.getString("comment_status"));
                    result.put("src", rs.getString("src"));
                    result.put("start_like", rs.getString("start_like"));
                    // result.put("specialities_name", rs.getString("specialities_name"));
                    // result.put("specialities_ids_and_names",
                    // rs.getString("specialities_ids_and_names"));
                    result.put("sponsor", rs.getString("sponsor"));
                    result.put("sponsor_logo", rs.getString("sponsor_logo"));
                    // result.put("comp_qa_speciality_id", rs.getString("comp_qa_speciality_id"));
                    // result.put("averageRating", rs.getString("averageRating"));
                    // result.put("myrating", rs.getString("myrating"));
                    // result.put("count_comment", rs.getString("count_comment"));
                    // result.put("vault", rs.getString("vault"));
                    return result;
                },
                userMasterId,
                typeId);

        // StringBuilder sql = new StringBuilder();
        // sql.append("SELECT cm.comp_qa_id as type_id, ")
        // .append("cm.comp_qa_question, ")
        // .append("cm.comp_qa_answer, ")
        // .append("cm.comp_qa_citation, ")
        // .append("cm.comp_qa_answer_raw, ")
        // .append("cm.comp_qa_question_raw, ")
        // .append("cm.comp_qa_file_img, ")
        // .append("cm.added_on, ")
        // .append("cm.env, ")
        // .append("cm.deeplink, ")
        // .append("cm.gl_deeplink, ")
        // .append("cm.comp_qa_tags, ")
        // .append("cm.privacy_status, ")
        // .append("cm.publication_date, ")
        // .append("cm.is_like, ")
        // .append("cm.is_comment, ")
        // .append("cln.client_name, ")
        // .append("cln.client_logo ")
        // .append("FROM knwlg_compendium_V1 cm ")
        // .append("JOIN client_master cln ON cln.client_master_id = cm.client_id ")
        // .append("WHERE cm.comp_qa_id = ? ");

        // if (userType == 5) {
        // sql.append("AND cm.status IN (3,5) ");
        // } else {
        // sql.append("AND cm.status = 3 ")
        // .append("AND cm.publication_date <= CURDATE() ");
        // }

        // Uncomment and fix the env logic if needed
        /*
         * if (env != null) {
         * if (!env.equals("2")) {
         * sql.append("AND (cm.env = 2 OR cm.env = ?) ");
         * } else {
         * sql.append("AND cm.env = ? ");
         * }
         * }
         */

        // System.out.println("SQL Query: " + sql.toString());

        // Use query instead of queryForObject
        // List<Map<String, Object>> results = jdbcTemplate.query(sql.toString(),
        // (rs, rowNum) -> {
        // Map<String, Object> result = new HashMap<>();
        // result.put("type_id", rs.getString("type_id"));
        // result.put("comp_qa_question", rs.getString("comp_qa_question"));
        // result.put("comp_qa_answer", rs.getString("comp_qa_answer"));
        // result.put("comp_qa_citation", rs.getString("comp_qa_citation"));
        // result.put("comp_qa_answer_raw", rs.getString("comp_qa_answer_raw"));
        // result.put("comp_qa_question_raw", rs.getString("comp_qa_question_raw"));
        // result.put("comp_qa_file_img", rs.getString("comp_qa_file_img"));
        // result.put("added_on", rs.getTimestamp("added_on"));
        // result.put("env", rs.getString("env"));
        // result.put("deeplink", rs.getString("deeplink"));
        // result.put("gl_deeplink", rs.getString("gl_deeplink"));
        // result.put("comp_qa_tags", rs.getString("comp_qa_tags"));
        // result.put("privacy_status", rs.getString("privacy_status"));
        // result.put("publication_date", rs.getDate("publication_date"));
        // result.put("is_like", rs.getBoolean("is_like"));
        // result.put("is_comment", rs.getBoolean("is_comment"));
        // result.put("client_name", rs.getString("client_name"));
        // result.put("client_logo", rs.getString("client_logo"));
        // return result;
        // },
        // typeId); // Pass the typeId parameter

        // Check if the result is empty
        if (results.isEmpty()) {
            return Collections.emptyMap(); // Return an empty map if no results are found
        }

        // Return the first result
        return results.get(0);
    }

    @Async
    public CompletableFuture<List<Map<String, Object>>> getCompendiumSpeciality(String typeId) {
        String sql = "SELECT master_specialities_id, specialities_name " +
                "FROM master_specialities_V1 ms " +
                "JOIN compendium_to_specialities cst ON ms.master_specialities_id = cst.specialities_id " +
                "WHERE cst.comp_qa_id = ?";

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

    @Async
    public CompletableFuture<Map<String, Object>> getContentRatings(String typeId, Integer userMasterId) {
        String sql = "SELECT COUNT(rating) as avg_rating, " +
                "(SELECT rating FROM knwlg_rating WHERE post_id = ? AND post_type='comp' " +
                "AND user_master_id = ? AND rating != 0) as my_rating " +
                "FROM knwlg_rating WHERE post_id = ? AND post_type='comp' AND rating != 0";

        Map<String, Object> ratings = jdbcTemplate.queryForObject(sql,
                (rs, rowNum) -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("rating", rs.getInt("avg_rating"));
                    boolean hasMyRating = rs.getObject("my_rating") != null;
                    result.put("myRating", hasMyRating);
                    // result.put("myRating", rs.getObject("my_rating"));
                    return result;
                },
                typeId, userMasterId, typeId);
        return CompletableFuture.completedFuture(ratings);
    }

    @Async
    public CompletableFuture<Integer> getCommentCount(String typeId) {
        String sql = "SELECT COUNT(knwlg_comment_id) FROM knwlg_comment " +
                "WHERE type_id = ? AND type = 'comp' AND comment_approve_status = '1'";
        int commentCount = jdbcTemplate.queryForObject(sql, Integer.class, typeId);
        return CompletableFuture.completedFuture(commentCount);
    }

    @Async
    public CompletableFuture<Optional<Integer>> getVaultStatus(String typeId, Integer userMasterId) {
        String sql = "SELECT status FROM knwlg_vault " +
                "WHERE post_id = ? AND type_text = 'comp' AND user_id = ?";
        try {
            Integer status = jdbcTemplate.queryForObject(sql, Integer.class, typeId, userMasterId);
            return CompletableFuture.completedFuture(Optional.ofNullable(status));
        } catch (EmptyResultDataAccessException e) {
            // Handle the case where no result is found
            return CompletableFuture.completedFuture(Optional.empty());
        }
    }

    @Async
    public CompletableFuture<List<Map<String, Object>>> getCompletedSurveys(Integer userMasterId) {
        String sql = "SELECT survey_id FROM survey_user_answer sv WHERE sv.user_master_id = ? "
                + "AND MONTH(added_on) = MONTH(CURRENT_DATE()) "
                + "AND YEAR(added_on) = YEAR(CURRENT_DATE())";
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, userMasterId);
        return CompletableFuture.completedFuture(results);
    }

    @Async
    public CompletableFuture<List<Map<String, Object>>> getIncompleteSurveys(Integer userMasterId) {
        String sql = "SELECT survey_id FROM survey_user_incomplete_answer sv WHERE sv.status = 3 AND sv.user_master_id = ?";
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, userMasterId);
        return CompletableFuture.completedFuture(results);
    }

    @Async
    public CompletableFuture<List<Map<String, Object>>> getPollSurveys(String typeId, Integer userMasterId,
            String excludedSurveyIds) {
        String sql = "SELECT sv.survey_id,sv.category,sv.survey_title,sv.survey_description,sv.image,sv.publishing_date,sv.deeplink,sv.survey_points, svd.data, "
                + "GROUP_CONCAT(DISTINCT ms.specialities_name) as specialities_name, "
                + "GROUP_CONCAT(DISTINCT concat(ms.master_specialities_id, '#', ms.specialities_name)) as specialities_ids_and_names, "
                + "cln.client_name, cln.client_logo, "
                + "GROUP_CONCAT(DISTINCT clintspon.client_name) as sponsor, "
                + "GROUP_CONCAT(DISTINCT clintspon.client_logo) as sponsor_logo "
                + "FROM survey sv "
                + "LEFT JOIN survey_to_speciality as svts ON svts.survey_id = sv.survey_id "
                + "LEFT JOIN master_specialities_V1 as ms ON ms.master_specialities_id = svts.speciality_id "
                + "LEFT JOIN client_master as cln ON cln.client_master_id = sv.client_id "
                + "LEFT JOIN survey_to_sponsor as suvTspon ON suvTspon.survey_id = sv.survey_id "
                + "LEFT JOIN client_master as clintspon ON clintspon.client_master_id = suvTspon.sponsor_id "
                + "JOIN survey_detail as svd ON svd.survey_id = sv.survey_id "
                + "JOIN survey_to_medwiki as stm ON stm.survey_id = sv.survey_id "

                + "WHERE sv.status = 3 AND date(sv.publishing_date) <= CURDATE() "
                + "AND stm.medwiki_id = ? "
                + (excludedSurveyIds.isEmpty() ? "" : "AND sv.survey_id NOT IN (" + excludedSurveyIds + ")");

        System.out.println("SQL: " + sql);

        return CompletableFuture.supplyAsync(() -> {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, typeId);

            // Return null if the results list is empty or null
            if (results == null || results.isEmpty()) {
                return null;
            }

            return results;
        });

        // List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, typeId);
        // return CompletableFuture.completedFuture(results);
    }

    @Async
    public CompletableFuture<List<Map<String, Object>>> getCompSearchData(String compCommaSeparatedIds) {

        try {

            List<String> compIds = List.of(compCommaSeparatedIds.split(","));

            String sql = "SELECT " +
                    "cm.comp_qa_id as type_id, " +
                    " 'comp' as trending_type,  " +
                    "cm.comp_qa_question_raw as title, " +
                    "cm.comp_qa_file_img as image, " +
                    "cm.type  as con_type, " +
                    "cm.deeplink, " +
                    "GROUP_CONCAT(DISTINCT concat(ms.master_specialities_id, '#', ms.specialities_name)) as specialities_ids_and_names "
                    +

                    "FROM knwlg_compendium_V1 as cm " +
                    "JOIN compendium_to_specialities as cmTs ON cmTs.comp_qa_id = cm.comp_qa_id " +
                    "JOIN master_specialities_V1 as ms ON ms.master_specialities_id = cmTs.specialities_id " +

                    "WHERE cm.status=3 " +
                    "AND cm.privacy_status = 0 " +
                    "AND cm.comp_qa_id IN (:compIds) " +
                    "GROUP BY cm.comp_qa_id " +
                    "ORDER BY cm.comp_qa_id DESC";

            MapSqlParameterSource parameters = new MapSqlParameterSource()
                    .addValue("compIds", compIds);

            List<Map<String, Object>> result = namedParameterJdbcTemplate.queryForList(sql, parameters);

            for (Map<String, Object> row : result) {
                String specialities = (String) row.get("specialities_ids_and_names");
                List<Map<String, Object>> specialitiesList = commonUtils.parseSpecilityString(specialities);
                row.put("specialities_ids_and_names", specialitiesList);
            }

            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            return CompletableFuture.completedFuture(null);
        }

    }

}