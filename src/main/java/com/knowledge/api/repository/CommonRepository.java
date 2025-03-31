package com.knowledge.api.repository;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.api.exception.CustomException;

@Repository
public class CommonRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Async
    public CompletableFuture<Integer> getUserType(Integer userMasterId) {
        String sql = "SELECT master_user_type_id FROM user_master WHERE user_master_id = ?";
        // System.out.println("SQL: getUserType" + sql);
        Integer userType = jdbcTemplate.queryForObject(sql, Integer.class, userMasterId);
        return CompletableFuture.completedFuture(userType);
    }

    @Async
    public CompletableFuture<String> getUserEnv(Integer userMasterId) {
        String sql = "SELECT etc.env_id " +
                "FROM user_master um " +
                "LEFT JOIN env_to_country etc ON etc.country_id = um.country_code " +
                "WHERE um.user_master_id = ?";
        // System.out.println("SQL: getUserEnv" + sql);
        String env = jdbcTemplate.queryForObject(sql, String.class, userMasterId);
        return CompletableFuture.completedFuture(env);
    }

    @Async
    public CompletableFuture<Map<String, Object>> checkUserPackage(Integer userMasterId, String module, String env) {

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
                                    return CompletableFuture.completedFuture(result);
                                }
                            }
                        } else if (module.equals(menuItem.get("module_name"))) {
                            boolean isPremium = "1".equals(menuItem.get("premium").toString());
                            result.put("display_icon", menuItem.get("displayIcon"));
                            result.put("content_access", !isPremium);
                            return CompletableFuture.completedFuture(result); // Return result;
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
            // e.printStackTrace();
            result.put("display_icon", false);
            result.put("content_access", true);
        }

        return CompletableFuture.completedFuture(result);
    }

    @Async
    public CompletableFuture<Map<String, Object>> getContentRatings(Integer typeId, Integer userMasterId,
            String contentType) {
        String sql = "  SELECT " +
                "   COUNT(rating) as avg_rating, " +
                "   (SELECT rating FROM knwlg_rating WHERE post_id = ? AND post_type= ? AND user_master_id = ? AND rating != 0) as my_rating "
                +

                " FROM knwlg_rating " +

                " WHERE post_id = ? " +
                " AND post_type= ? " +
                " AND rating != 0";

        Map<String, Object> ratings = jdbcTemplate.queryForObject(sql,
                (rs, rowNum) -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("rating", rs.getInt("avg_rating"));
                    result.put("myRating", rs.getObject("my_rating"));
                    return result;
                },
                typeId, contentType, userMasterId, typeId, contentType);
        return CompletableFuture.completedFuture(ratings);
    }

    @Async
    public CompletableFuture<Integer> getCommentCount(Integer typeId, String contentType) {
        String sql = "SELECT COUNT(knwlg_comment_id) FROM knwlg_comment " +
                "WHERE type_id = ? AND type = ? AND comment_approve_status = '1'";
        int commentCount = jdbcTemplate.queryForObject(sql, Integer.class, typeId, contentType);
        return CompletableFuture.completedFuture(commentCount);
    }

    @Async
    public CompletableFuture<Optional<Integer>> getVaultStatus(Integer typeId, Integer userMasterId,
            String contentType) {
        String sql = "SELECT status FROM knwlg_vault " +
                "WHERE post_id = ? AND type_text = ? AND user_id = ?";
        try {
            Integer status = jdbcTemplate.queryForObject(sql, Integer.class, typeId, contentType, userMasterId);
            return CompletableFuture.completedFuture(Optional.ofNullable(status));
        } catch (EmptyResultDataAccessException e) {
            // Handle the case where no result is found
            return CompletableFuture.completedFuture(Optional.empty());
        }
    }

    @Async
    public CompletableFuture<List<Map<String, Object>>> getSpecilityEntities(String commaSeparatedSpecilityString) {

        try {

            if (commaSeparatedSpecilityString.isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }

            List<String> specilityIds = Arrays.asList(commaSeparatedSpecilityString.split(","));
            String specilities = String.join(",", Collections.nCopies(specilityIds.size(), "?"));

            String Sql = " SELECT " +
                    " ms.master_specialities_id as id , " +
                    " ms.specialities_name as name " +

                    " FROM master_specialities_V1 as ms " +
                    " WHERE ms.master_specialities_id IN (" + specilities + ")";

            List<Map<String, Object>> specilityEntities = jdbcTemplate.queryForList(Sql, specilityIds.toArray());

            return CompletableFuture.completedFuture(specilityEntities);

        } catch (EmptyResultDataAccessException e) {
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            throw new CustomException(500, "Something went wrong", e);
        }
    }

    @Async
    public CompletableFuture<List<Map<String, Object>>> getSessionDoctorEntities(String commaseparetedDoctorIds) {

        try {

            if (commaseparetedDoctorIds.isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }

            String sql = """
                        SELECT
                            ksd.sessions_doctors_id,
                            ksd.doctor_name,
                            ksd.profile_image,
                            ksd.profile,
                            ksd.subtitle,
                            ksd.display_speciality AS DepartmentName

                        FROM
                            knwlg_sessions_doctors ksd

                        LEFT JOIN master_doctor_specialization ms ON FIND_IN_SET(ms.master_doc_spec_id, ksd.speciality) > 0

                        WHERE
                            ksd.sessions_doctors_id = ?

                        GROUP BY
                            ksd.sessions_doctors_id
                    """;

            List<Map<String, Object>> sessionDoctorEntities = jdbcTemplate.queryForList(sql, commaseparetedDoctorIds);
            return CompletableFuture.completedFuture(sessionDoctorEntities);

        } catch (EmptyResultDataAccessException e) {
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            throw new CustomException(500, "Something went wrong", e);
        }
    }

    @Async
    public CompletableFuture<Map<String, Object>> getUserContentPaymentStatus(Integer contentId, Integer contentType,
            Integer userMasterId) {
        try {

            if (contentId == 0 && contentType == null && contentType == 0 && userMasterId == 0) {
                return CompletableFuture.completedFuture(null);
            }

            String sql = "SELECT CASE " +

                    "   WHEN puc.status IS NULL THEN " +
                    "       (SELECT payment_status FROM order_master " +
                    "           WHERE user_master_id = ? AND type_id = ? AND type = ? " +
                    "           ORDER BY id DESC LIMIT 1) " +
                    "   ELSE puc.status END AS status " +

                    "  FROM payment_user_to_content puc " +

                    "  WHERE puc.status = 3 " +
                    "  AND puc.type_id = ? " +
                    "  AND puc.type = ? " +
                    "  AND puc.user_master_id = ?";

            Map<String, Object> status = jdbcTemplate.queryForMap(sql, userMasterId, contentId, contentType, contentId,
                    contentType, userMasterId);

            return CompletableFuture.completedFuture(status);

        } catch (EmptyResultDataAccessException e) {
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            throw new CustomException(500, "Something went wrong", e);
        }
    }

    @Async
    public CompletableFuture<String> getUserGroup(Integer userMasterId) {

        try {

            String sql = "SELECT GROUP_CONCAT(group_id) as group_ids FROM user_to_usergroup WHERE user_master_id = ?";
            String groups = jdbcTemplate.queryForObject(sql, new Object[] { userMasterId }, String.class);
            return CompletableFuture.completedFuture(groups);

        } catch (EmptyResultDataAccessException e) {
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            throw new CustomException(500, "Something went wrong", e);
        }

    }

    public CompletableFuture<String> getUserInterestSpecilaity(Integer userMasterId) {
        try {
            String sql = "SELECT GROUP_CONCAT(specialities_id) as specialities_id FROM user_to_interest WHERE user_master_id = ?";
            String interestSpecilaity = jdbcTemplate.queryForObject(sql, String.class, userMasterId);
            return CompletableFuture.completedFuture(interestSpecilaity);
        } catch (EmptyResultDataAccessException e) {
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            throw new CustomException(500, "Something went wrong", e);
        }
    }

}
