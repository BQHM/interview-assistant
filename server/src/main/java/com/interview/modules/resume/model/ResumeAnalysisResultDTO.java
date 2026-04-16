package com.interview.modules.resume.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResumeAnalysisResultDTO {
    private Integer overallScore;

    private Integer contentScore;
    private Integer structureScore;
    private Integer skillMatchScore;

    private Integer expressionScore;
    private Integer projectScore;
    private String summary;

    private String strengthsJson;
    private String suggestionsJson;
}
