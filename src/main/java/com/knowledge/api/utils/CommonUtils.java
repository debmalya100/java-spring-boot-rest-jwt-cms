package com.knowledge.api.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class CommonUtils {

    public List<Map<String, Object>> parseSpecilityString(String specility) {

        try {
            specility = specility.trim();
            if (specility == null) {
                return null;
            }

            List<Map<String, Object>> result = new ArrayList<>();
            String[] pairs = specility.split(",");

            for (String signlePair : pairs) {
                String[] idSpecility = signlePair.split("#");
                if (idSpecility.length == 2) {
                    Integer id = Integer.parseInt(idSpecility[0]);
                    String specilityName = idSpecility[1];
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", id);
                    map.put("name", specilityName);
                    result.add(map);
                }
            }

            return result;

        } catch (Exception e) {
            return null;
        }

    }

    public List<Map<String, Object>> parsePineconeSpecility(Object specility) {
        try {
            if (specility == null) {
                return null;
            }

            List<String> specilityList = (List<String>) specility;

            List<Map<String, Object>> specialityObject = new ArrayList<>();

            for (String spec : specilityList) {
                specialityObject.add(
                        Map.of(
                                "name", spec));
            }
            // System.out.println("specility: " + specialityObject);

            return specialityObject;
        } catch (Exception e) {
            return null;
        }
    }

    public String formatDateTime(LocalDateTime dateTime) {
        // Define the desired format
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // Format the LocalDateTime object
        return dateTime.format(formatter);
    }

    // Method to remove HTML tags and entities
    public static String removeHtmlTags(String input) {
        if (input == null) {
            return "";
        }
        // Remove HTML tags using regex
        String noHtml = input.replaceAll("<[^>]*>", "");
        // Remove HTML entities (e.g., &nbsp;, &amp;, etc.)
        noHtml = noHtml.replaceAll("&[^;]+;", "");
        return noHtml;
    }

    public String truncateString(String input, int maxLength) {
        if (input.length() > maxLength) {
            return input.substring(0, maxLength) + "...";
        }
        return input;
    }


    public String getDisclaimer(String content) {
        return switch (content.toLowerCase()) {
            case "epub" -> "All scientific content on the platform is provided for general medical education purposes meant for registered medical practitioners only. The content is not meant to substitute for the independent medical judgment of a physician relative to diagnostic and treatment options of a specific patient's medical condition. In no event will CLIRNET be liable for any decision made or action taken in reliance upon the information provided through this content.";

            case "video" ->  "All scientific content on the platform is provided for general medical education purposes meant for registered medical practitioners only. The content is not meant to substitute for the independent medical judgment of a physician relative to diagnostic and treatment options of a specific patient\u2019s medical condition. In no event will CLIRNET be liable for any decision made or action taken in reliance upon the information provided through this content.";
            
            case "medwiki" -> "MedWiki";
            
            default -> "Default";
        };
    }

    public static String getRawQuery(String sql, Object... params) {
        String rawQuery = sql;
        for (Object param : params) {
            rawQuery = rawQuery.replaceFirst("\\?", param instanceof String ? "'" + param + "'" : param.toString());
        }
        return rawQuery;
    }

    public Object changeImgSrc(Object image) {
        if (image instanceof String) {
            // Handle single string input
            return replaceImageSource((String) image);
        } else if (image instanceof List) {
            // Handle list of strings input
            List<String> returnArray = new ArrayList<>();
            for (Object value : (List<?>) image) {
                if (value instanceof String && !((String) value).isEmpty()) {
                    returnArray.add(replaceImageSource((String) value));
                }
            }
            return returnArray;
        } else {
            // Handle other cases (optional)
            return null;
        }
    }

    private String replaceImageSource(String imageUrl) {
        if (imageUrl.contains("https://storage.googleapis.com/")) {
            return imageUrl.replace("https://storage.googleapis.com/", "https://img-cdn.clirnet.com/");
        } else {
            return imageUrl;
        }
    }
}
