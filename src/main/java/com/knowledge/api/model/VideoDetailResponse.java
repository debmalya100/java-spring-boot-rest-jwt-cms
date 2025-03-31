package com.knowledge.api.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;


@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoDetailResponse {

    @JsonProperty("type_id")
    private Integer type_id;

    @JsonProperty("video_archive_question")
    private String video_archive_question;

    @JsonProperty("video_archive_answer")
    private String video_archive_answer;

    @JsonProperty("video_archive_citation")
    private String video_archive_citation;

    @JsonProperty("video_archive_transcription")
    private String video_archive_transcription;

    @JsonProperty("env")
    private String env;

    @JsonProperty("is_share")
    private Integer is_share;

    @JsonProperty("is_like")
    private Boolean is_like;

    @JsonProperty("is_comment")
    private Boolean is_comment;

    @JsonProperty("video_archive_question_raw")
    private String video_archive_question_raw;

    @JsonProperty("video_archive_answer_raw")
    private String video_archive_answer_raw;

    @JsonProperty("video_archive_file_img")
    private String video_archive_file_img;

    @JsonProperty("video_archive_file_img_thumbnail")
    private String video_archive_file_img_thumbnail;

    @JsonProperty("start_like")
    private Integer start_like;

    @JsonProperty("comment_status")
    private Integer comment_status;


    @JsonProperty("publication_date")
    private List<String> publication_date;

    @JsonProperty("client_name")
    private String client_name;

    @JsonProperty("client_logo")
    private String client_logo;

    @JsonProperty("privacy_status")
    private Integer privacy_status;

    @JsonProperty("type")
    private String con_type;

    @JsonProperty("vendor")
    private String vendor;

    @JsonProperty("src")
    private String src;

    @JsonProperty("deeplink")
    private String deeplink;

    @JsonProperty("gl_deeplink")
    private String gl_deeplink;

    @JsonProperty("video_archive_tags")
    private String video_archive_tags;

    @JsonProperty("session_doctor_id")
    private Integer session_doctor_id;

    @JsonProperty("play_time")
    private Double play_time;

    @JsonProperty("sponsor")
    private String sponsor;

    @JsonProperty("sponsor_logo")
    private String sponsor_logo;

    @JsonProperty("specialityIds")
    private String specialityIds;

  
}