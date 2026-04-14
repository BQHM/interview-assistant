package com.interview.modules.resume.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResumeAnalysisResultDTO {
    private String overallScore;
    private String contentScore;
    private String structureScore;
    private String skillMatchScore;

    private String expressionScore;
    private String projectScore;
    private String summary;

    private String strengthsJson;
    private String suggestionsJson;
}
