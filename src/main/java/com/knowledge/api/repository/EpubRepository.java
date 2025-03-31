package com.knowledge.api.repository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
// import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

import com.knowledge.api.exception.CustomException;
import com.knowledge.api.utils.CommonUtils;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class EpubRepository {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    private CommonUtils commonUtils;

    @Async
    public CompletableFuture<List<Map<String , Object>>> getEpubSearchData( String epubCommaSeparatedIds ) {
        
        try {

            List<String> epubIds = List.of(epubCommaSeparatedIds.split(","));
            
            String sql = "SELECT " +
                            "cm.epub_id as type_id," +
                            "cm.epub_title as title,"+
                            " 'epaper' as trending_type, "+
                            "cm.epub_img,"+
                            "cm.deeplink,"+
                            "GROUP_CONCAT(DISTINCT concat(ms.master_specialities_id, '#', ms.specialities_name) ) as specialities_ids_and_names "+

                        " FROM epub_master as cm "+
        
                        " JOIN epub_to_specialities as cmTs ON cmTs.epub_id = cm.epub_id "+
                        " JOIN master_specialities_V1 as ms ON ms.master_specialities_id = cmTs.specialities_id "+

                        " WHERE " +
                            " cm.status = 3 "+
                            " AND cm.privacy_status in(0,1,2) "+
                            " AND cm.publication_date <= CURDATE() " +
                            " AND cm.epub_id IN(:epubIds) "+
                            " GROUP BY cm.epub_id ";

            MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("epubIds", epubIds);

            List<Map<String, Object>> result = namedParameterJdbcTemplate.queryForList(sql, parameters);

            for( Map<String,Object> row : result) {
                String specialitiesIdsAndNames = (String) row.get("specialities_ids_and_names");
                List<Map<String, Object>> specilityObject = commonUtils.parseSpecilityString(specialitiesIdsAndNames);
                row.put("specialities_ids_and_names", specilityObject); 
            }

            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            return CompletableFuture.completedFuture(null);
        }

    }


    @Async
    public CompletableFuture<Map<String , Object>> getEpubDetailData( Integer Id , Integer userType, String userEnv ) {
        
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

            String sql = """
                            SELECT
                                cm.epub_id as type_id,
                                cm.epub_description as description,
                                cm.epub_title as title,
                                cm.is_converted,            
                                cm.epub_img,
                                cm.epub_img_thumbnail,
                                cm.epub_file,
                                cm.start_like,
                                cm.is_share,
                                cm.added_on,
                                cm.publication_date as publish_date,
                                cln.client_name,
                                cln.client_logo,
                                cTenv.price,
                                cm.deeplink,
                                cm.color,
                                cm.author,
                                GROUP_CONCAT(DISTINCT eTs.specialities_id) as specialityIds,
                                GROUP_CONCAT(DISTINCT clintspon.client_name) as sponsor,
                                GROUP_CONCAT(DISTINCT clintspon.client_logo) as sponsor_logo

                            FROM 
                                epub_master as cm

                            LEFT JOIN epub_to_specialities as eTs ON eTs.epub_id = cm.epub_id
                            LEFT JOIN client_master as cln ON cln.client_master_id = cm.client_id
                            LEFT JOIN epub_to_sponsor as cmTspon ON cmTspon.epub_id = cm.epub_id 
                            LEFT JOIN client_master as clintspon ON clintspon.client_master_id = cmTspon.sponsor_id 
                            LEFT JOIN content_to_env as cTenv ON cTenv.type_id = cm.epub_id and  cTenv.type = 6

                            WHERE 
                                cm.status = ?
                                """+envValue+"""
                                AND cm.privacy_status in(0,1)  
                                AND cm.publication_date <= CURDATE()  
                                AND cm.epub_id = ?
                    """; 

                    Map<String, Object> result = jdbcTemplate.queryForMap(sql, status , Id);

                    if (result == null || result.get("type_id") == null) {
                        throw new CustomException(HttpStatus.NON_AUTHORITATIVE_INFORMATION.value(), "No Data Found!");
                    }
                    
                    return CompletableFuture.completedFuture(result);

        } catch (EmptyResultDataAccessException e) {
           throw new CustomException( HttpStatus.NON_AUTHORITATIVE_INFORMATION.value() , "No Data Found!" , e);
        } catch (Exception e) {
            throw new CustomException( HttpStatus.INTERNAL_SERVER_ERROR.value() , "Something went wrong" , e);
        }
        
        
    }


    @Async
    public CompletableFuture<List<Map<String, Object>>> getEpubAuthorDetails( Integer Id) {
        try {

            String Sql = """
                        SELECT 
                            id as author_id,
                            author_name,
                            author_image,
                            author_description

                        FROM 
                           epub_to_author

                        WHERE
                            epub_id = ? 
                    """;

            // String d = CommonUtils.getRawQuery(Sql, Id);
            // System.out.println(d);

            List<Map<String , Object>> result = jdbcTemplate.queryForList(Sql,Id);
            
            return CompletableFuture.completedFuture(result);

        } catch (EmptyResultDataAccessException e) {
            return CompletableFuture.completedFuture(null);
        } catch(Exception e ) {
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR.value() , "Something Went Wrong");
        }
    }
}
