package com.resume.analyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.analyzer.entity.ResumeAnalysis;
import com.resume.analyzer.repository.ResumeAnalysisRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class GeminiService {

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Autowired
    private ResumeAnalysisRepository repository;

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=";

    public ResumeAnalysis analyzeResume(Long id) throws Exception {
        ResumeAnalysis resume = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Resume not found with ID: " + id));

        String rawText = resume.getRawText();
        String jdRawText = resume.getJdRawText() != null ? resume.getJdRawText() : "Not specified. Assume general software engineering role.";

        String prompt = "You are a senior technical recruiter and professional ATS parser. "
                + "Compare the user's Resume with the Job Description (JD) provided. "
                + "Perform a comprehensive ATS evaluation. Output your entire response as a single, valid JSON object containing exactly the following keys:\n"
                + "- overallScore (number, 0-100)\n"
                + "- atsCompatibilityScore (number, 0-100)\n"
                + "- jobMatchScore (number, 0-100)\n"
                + "- formatScore (number, 0-100)\n"
                + "- formatFeedback (object with keys: sectionHeadings (number 0-100), readability (number 0-100), atsfrequentFormatting (number 0-100), fontConsistency (number 0-100), bulletUsage (number 0-100), whiteSpacing (number 0-100), fileStructure (number 0-100))\n"
                + "- grammarScore (number, 0-100)\n"
                + "- grammarFeedback (object with keys: grammarMistakesCount (number), spellingMistakesCount (number), sentenceClarity (number 0-100), repeatedWordsCount (number), professionalLanguageScore (number 0-100))\n"
                + "- skillsMatchScore (number, 0-100)\n"
                + "- skillsFeedback (object with keys: matchingSkills (array of strings), missingSkills (array of strings), extraSkills (array of strings), skillMatchPercentage (number 0-100))\n"
                + "- keywordScore (number, 0-100)\n"
                + "- keywordFeedback (object with keys: keywordsFound (array of strings), missingKeywords (array of strings), suggestedKeywords (array of strings))\n"
                + "- experienceScore (number, 0-100)\n"
                + "- experienceFeedbackDetails (object with keys: requiredYears (number), relevantYears (number), missingAreas (array of strings))\n"
                + "- educationScore (number, 0-100)\n"
                + "- projectsScore (number, 0-100)\n"
                + "- projectsFeedbackDetails (object with keys: relevanceScore (number 0-100), missingProjectExperience (array of strings), suggestedProjectImprovements (array of strings))\n"
                + "- achievementsScore (number, 0-100)\n"
                + "- strengthScore (number, 0-100)\n"
                + "- weaknessScore (number, 0-100)\n"
                + "- readabilityScore (number, 0-100)\n"
                + "- interviewReadinessScore (number, 0-100)\n"
                + "- strengths (array of strings detailing resume strengths)\n"
                + "- weaknesses (array of strings detailing resume weaknesses)\n"
                + "- missingTechnicalSkills (array of strings)\n"
                + "- missingSoftSkills (array of strings)\n"
                + "- missingAtsKeywords (array of strings)\n"
                + "- grammarMistakesList (array of objects with keys: mistake, correction, reason)\n"
                + "- spellingMistakesList (array of objects with keys: mistake, correction)\n"
                + "- weakBulletPointsList (array of objects with keys: current, suggested, reason)\n"
                + "- sectionImprovements (object with keys: summary, skills, experience, projects, education, certifications. Each is an object with: suggestion (string), reason (string))\n"
                + "- topTenImprovements (array of strings, exactly 10 actionable recommendations)\n"
                + "- estimatedScoreAfterImprovements (number, 0-100)\n"
                + "- hiringRecommendation (string, one of: 'Excellent Match', 'Strong Match', 'Moderate Match', 'Weak Match', 'Not Recommended')\n"
                + "- overallSummary (string summarizing the review)\n\n"
                + "RESUME TEXT:\n" + rawText + "\n\n"
                + "JOB DESCRIPTION TEXT:\n" + jdRawText;

        String analysisJson = "";
        int atsScore = 0;
        String overallSummary = "";

        if (apiKey == null || apiKey.trim().isEmpty()) {
            // Mock response if API key is not configured
            analysisJson = getMockAnalysisJson(resume);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode innerNode = mapper.readTree(analysisJson);
            atsScore = innerNode.path("overallScore").asInt(75);
            overallSummary = innerNode.path("overallSummary").asText("Resume analyzed.");
        } else {
            try {
                RestTemplate restTemplate = new RestTemplate();
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> requestBody = new HashMap<>();

                Map<String, Object> textKey = new HashMap<>();
                textKey.put("text", prompt);

                Map<String, Object> contentNode = new HashMap<>();
                contentNode.put("parts", Collections.singletonList(textKey));

                requestBody.put("contents", Collections.singletonList(contentNode));

                Map<String, Object> genConfig = new HashMap<>();
                genConfig.put("responseMimeType", "application/json");
                requestBody.put("generationConfig", genConfig);

                String requestJson = mapper.writeValueAsString(requestBody);
                HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

                ResponseEntity<String> responseEntity = restTemplate.postForEntity(
                        GEMINI_API_URL + apiKey,
                        entity,
                        String.class
                );

                String responseBody = responseEntity.getBody();
                JsonNode root = mapper.readTree(responseBody);
                String resultText = root.path("candidates")
                        .path(0)
                        .path("content")
                        .path("parts")
                        .path(0)
                        .path("text")
                        .asText();

                // Validate if it is valid JSON
                JsonNode innerNode = mapper.readTree(resultText);
                atsScore = innerNode.path("overallScore").asInt(70);
                overallSummary = innerNode.path("overallSummary").asText("Resume analyzed successfully.");
                analysisJson = resultText;
            } catch (Exception e) {
                // If API fails (e.g. rate limit, invalid key), provide a user-friendly error fallback
                analysisJson = getErrorAnalysisJson(e.getMessage());
                atsScore = 50;
                overallSummary = "Error during analysis: " + e.getMessage() + ". Please verify your API key.";
            }
        }

        resume.setAtsScore(atsScore);
        resume.setAiSummary(overallSummary);
        resume.setAnalysisJson(analysisJson);

        return repository.save(resume);
    }

    private String getMockAnalysisJson(ResumeAnalysis resume) {
        String rawText = resume.getRawText();
        String jdRawText = resume.getJdRawText();
        if (jdRawText == null || jdRawText.trim().isEmpty()) {
            jdRawText = "Java, React, SQL, AWS, Docker, Git";
        }
        
        String resumeLower = rawText.toLowerCase();
        String jdLower = jdRawText.toLowerCase();

        // 1. Term Scanning
        String[] possibleSkills = {"java", "springboot", "spring", "react", "javascript", "docker", "kubernetes", "aws", "gcp", "mysql", "postgres", "sql", "git", "github", "agile", "microservices", "unit testing", "junit", "mockito", "html", "css", "typescript", "node", "python", "devops", "ci/cd"};
        String[] displaySkills = {"Java", "Spring Boot", "Spring Framework", "React", "JavaScript", "Docker", "Kubernetes", "AWS", "Google Cloud Platform (GCP)", "MySQL", "PostgreSQL", "SQL Datasets", "Git Version Control", "GitHub Integrations", "Agile Methodologies", "Microservices Architecture", "Unit Testing", "JUnit Framework", "Mockito Testing", "HTML5 Layouts", "CSS3 styling", "TypeScript", "Node.js", "Python programming", "DevOps workflows", "CI/CD pipelines"};

        java.util.List<String> matchingSkills = new java.util.ArrayList<>();
        java.util.List<String> missingSkills = new java.util.ArrayList<>();
        java.util.List<String> extraSkills = new java.util.ArrayList<>();
        java.util.List<String> keywordsFound = new java.util.ArrayList<>();
        java.util.List<String> missingKeywords = new java.util.ArrayList<>();

        // Check if tech skills in JD are present in the Resume
        for (int i = 0; i < possibleSkills.length; i++) {
            String skill = possibleSkills[i];
            String display = displaySkills[i];
            boolean inJd = jdLower.contains(skill);
            boolean inResume = resumeLower.contains(skill);

            if (inJd && inResume) {
                matchingSkills.add(display);
                keywordsFound.add(display);
            } else if (inJd && !inResume) {
                missingSkills.add(display);
                missingKeywords.add(display);
            } else if (!inJd && inResume) {
                extraSkills.add(display);
            }
        }

        if (matchingSkills.isEmpty()) {
            matchingSkills.add("Communications");
            matchingSkills.add("Problem Solving");
        }
        if (missingSkills.isEmpty()) {
            missingSkills.add("Kubernetes Orchestration");
            missingSkills.add("CI/CD Build Pipelines");
        }

        // 2. Score Calculation
        int totalRequirements = matchingSkills.size() + missingSkills.size();
        int skillMatchPct = (int) (((double) matchingSkills.size() / totalRequirements) * 100);
        
        int overallScore = (int) (50 + (skillMatchPct * 0.45));
        if (overallScore > 98) overallScore = 98;
        if (overallScore < 30) overallScore = 30;

        int atsScore = (int) (overallScore * 0.98);
        int jobMatchScore = skillMatchPct;
        int formatScore = 85;
        int grammarScore = 90;
        int keywordScore = atsScore;
        int experienceScore = 75;
        int educationScore = 90;
        int projectsScore = 80;
        int achievementsScore = 70;
        int strengthScore = 85;
        int weaknessScore = 40;
        int readabilityScore = 88;
        int interviewScore = 80;

        // Hiring Recommendation
        String hiringRecommendation = "Moderate Match";
        if (overallScore >= 85) hiringRecommendation = "Excellent Match";
        else if (overallScore >= 70) hiringRecommendation = "Strong Match";
        else if (overallScore >= 50) hiringRecommendation = "Moderate Match";
        else if (overallScore >= 40) hiringRecommendation = "Weak Match";
        else hiringRecommendation = "Not Recommended";

        // Structured JSON building
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("overallScore", overallScore);
        map.put("atsCompatibilityScore", atsScore);
        map.put("jobMatchScore", jobMatchScore);
        map.put("formatScore", formatScore);

        Map<String, Object> formatFeedback = new java.util.LinkedHashMap<>();
        formatFeedback.put("sectionHeadings", 90);
        formatFeedback.put("readability", 85);
        formatFeedback.put("atsfrequentFormatting", 95);
        formatFeedback.put("fontConsistency", 100);
        formatFeedback.put("bulletUsage", 80);
        formatFeedback.put("whiteSpacing", 85);
        formatFeedback.put("fileStructure", 90);
        map.put("formatFeedback", formatFeedback);

        map.put("grammarScore", grammarScore);
        Map<String, Object> grammarFeedback = new java.util.LinkedHashMap<>();
        grammarFeedback.put("grammarMistakesCount", 1);
        grammarFeedback.put("spellingMistakesCount", 1);
        grammarFeedback.put("sentenceClarity", 88);
        grammarFeedback.put("repeatedWordsCount", 0);
        grammarFeedback.put("professionalLanguageScore", 92);
        map.put("grammarFeedback", grammarFeedback);

        map.put("skillsMatchScore", skillMatchPct);
        Map<String, Object> skillsFeedback = new java.util.LinkedHashMap<>();
        skillsFeedback.put("matchingSkills", matchingSkills);
        skillsFeedback.put("missingSkills", missingSkills);
        skillsFeedback.put("extraSkills", extraSkills);
        skillsFeedback.put("skillMatchPercentage", skillMatchPct);
        map.put("skillsFeedback", skillsFeedback);

        map.put("keywordScore", keywordScore);
        Map<String, Object> keywordFeedback = new java.util.LinkedHashMap<>();
        keywordFeedback.put("keywordsFound", keywordsFound);
        keywordFeedback.put("missingKeywords", missingKeywords);
        keywordFeedback.put("suggestedKeywords", missingSkills);
        map.put("keywordFeedback", keywordFeedback);

        map.put("experienceScore", experienceScore);
        Map<String, Object> experienceDetails = new java.util.LinkedHashMap<>();
        experienceDetails.put("requiredYears", 4);
        experienceDetails.put("relevantYears", 3);
        experienceDetails.put("missingAreas", java.util.Collections.singletonList("Production DevOps containerization"));
        map.put("experienceFeedbackDetails", experienceDetails);

        map.put("educationScore", educationScore);
        map.put("projectsScore", projectsScore);
        Map<String, Object> projectsDetails = new java.util.LinkedHashMap<>();
        projectsDetails.put("relevanceScore", 85);
        projectsDetails.put("missingProjectExperience", java.util.Collections.singletonList("Enterprise scale microservice interactions"));
        projectsDetails.put("suggestedProjectImprovements", java.util.Arrays.asList("Detail Spring Boot load balancing parameters", "Provide database query caching specs"));
        map.put("projectsFeedbackDetails", projectsDetails);

        map.put("achievementsScore", achievementsScore);
        map.put("strengthScore", strengthScore);
        map.put("weaknessScore", weaknessScore);
        map.put("readabilityScore", readabilityScore);
        map.put("interviewReadinessScore", interviewScore);

        map.put("strengths", java.util.Arrays.asList(
            "Clear chronological experience listings.",
            "Strong programming fundamentals stated in technical stack overview.",
            "Clean layout compliant with modern single-column scanner guidelines."
        ));
        map.put("weaknesses", java.util.Arrays.asList(
            "Lacks quantitative support values (e.g. percentages or traffic improvements).",
            "Missing reference to cloud deployment or CI/CD frameworks."
        ));
        map.put("missingTechnicalSkills", missingSkills);
        map.put("missingSoftSkills", java.util.Arrays.asList("Cross-functional Teamwork", "Technical Leadership"));
        map.put("missingAtsKeywords", missingKeywords);

        // Mistakes list
        Map<String, String> gm1 = new java.util.LinkedHashMap<>();
        gm1.put("mistake", "Wrote a web app that are utilized by many.");
        gm1.put("correction", "Developed a web application utilized by multiple departments.");
        gm1.put("reason", "Checks subject-verb agreement issues.");
        map.put("grammarMistakesList", java.util.Collections.singletonList(gm1));

        Map<String, String> sm1 = new java.util.LinkedHashMap<>();
        sm1.put("mistake", "multithredding");
        sm1.put("correction", "multithreading");
        map.put("spellingMistakesList", java.util.Collections.singletonList(sm1));

        // Rewritten bullet points
        Map<String, String> bp1 = new java.util.LinkedHashMap<>();
        bp1.put("current", "Developed a web application.");
        bp1.put("suggested", "Orchestrated a secure web application using React and Spring Boot that reduced response times by 30%.");
        bp1.put("reason", "Recruiters prefer quantified achievements and ATS systems prioritize action verbs.");
        map.put("weakBulletPointsList", java.util.Collections.singletonList(bp1));

        // Section Improvements
        Map<String, Object> sectionImps = new java.util.LinkedHashMap<>();
        sectionImps.put("summary", Map.of("suggestion", "Begin with a strong value proposition summarizing core engineering credentials.", "reason", "Recruiters check the summary section first to gauge match criteria."));
        sectionImps.put("skills", Map.of("suggestion", "Classify tools into subcategories (e.g., Languages, Frameworks, Cloud).", "reason", "Grouping tools improves readability for human recruiters."));
        sectionImps.put("experience", Map.of("suggestion", "Start each bullet with a power action verb.", "reason", "Strong action verbs communicate accountability and leadership potential."));
        sectionImps.put("projects", Map.of("suggestion", "Link actual github repository links and add metrics.", "reason", "Adding reference links displays credibility and project depth."));
        sectionImps.put("education", Map.of("suggestion", "State graduation honors or core projects.", "reason", "Elevates candidate value when professional experience is young."));
        sectionImps.put("certifications", Map.of("suggestion", "Add professional certifications (e.g., AWS Developer, Java Associate).", "reason", "Validates technical proficiency via standardized testing metrics."));
        map.put("sectionImprovements", sectionImps);

        // Top 10 improvements
        map.put("topTenImprovements", java.util.Arrays.asList(
            "Add " + (missingSkills.isEmpty() ? "Docker" : missingSkills.get(0)) + " keywords directly into your skills block.",
            "Introduce metrics and percentage values to quantify achievements.",
            "Rewrite older work descriptions using 'Implemented' or 'Architected' instead of 'worked on'.",
            "Eliminate spelling errors like 'multithredding' to ensure professional styling.",
            "List project repositories using live HTTP link text.",
            "Reorganize skills block using sub-categories for quick scanning.",
            "Align typography fonts to use a single standard font family throughout.",
            "Verify all previous work descriptions are formulated in the past tense.",
            "Include cloud deployment listings (AWS EC2, Docker containerization).",
            "Keep the entire CV length strictly within 1 or 2 complete pages."
        ));

        map.put("estimatedScoreAfterImprovements", Math.min(overallScore + 12, 99));
        map.put("hiringRecommendation", hiringRecommendation);
        map.put("overallSummary", "The resume has a " + skillMatchPct + "% match with the Job Description of " + (resume.getJdFileName() != null ? resume.getJdFileName() : "the job") + ". Missing keywords like " + (missingKeywords.isEmpty() ? "general stacks" : missingKeywords.get(0)) + " are capping your score. (MOCK COMPARED - configure GEMINI_API_KEY in application.properties for real LLM scanning)");

        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(map);
        } catch (Exception e) {
            return "{\n  \"overallScore\": " + overallScore + ",\n  \"overallSummary\": \"Fallback parsing successful.\"\n}";
        }
    }

    private String getErrorAnalysisJson(String errorMsg) {
        return "{\n" +
                "  \"overallScore\": 50,\n" +
                "  \"atsCompatibilityScore\": 50,\n" +
                "  \"jobMatchScore\": 50,\n" +
                "  \"formatScore\": 50,\n" +
                "  \"grammarScore\": 50,\n" +
                "  \"skillsMatchScore\": 50,\n" +
                "  \"keywordScore\": 50,\n" +
                "  \"experienceScore\": 50,\n" +
                "  \"educationScore\": 50,\n" +
                "  \"projectsScore\": 50,\n" +
                "  \"achievementsScore\": 50,\n" +
                "  \"strengthScore\": 50,\n" +
                "  \"weaknessScore\": 50,\n" +
                "  \"readabilityScore\": 50,\n" +
                "  \"interviewReadinessScore\": 50,\n" +
                "  \"overallSummary\": \"Analysis halted. Error: " + errorMsg + "\",\n" +
                "  \"strengths\": [\"Text extraction succeeded.\"],\n" +
                "  \"weaknesses\": [\"Gemini AI connection failed. Check your API key.\"],\n" +
                "  \"missingTechnicalSkills\": [],\n" +
                "  \"missingSoftSkills\": [],\n" +
                "  \"missingAtsKeywords\": [],\n" +
                "  \"grammarMistakesList\": [],\n" +
                "  \"spellingMistakesList\": [],\n" +
                "  \"weakBulletPointsList\": [],\n" +
                "  \"topTenImprovements\": [\n" +
                "    \"Verify GEMINI_API_KEY value in resource configuration.\",\n" +
                "    \"Check server log details.\",\n" +
                "    \"Retry short simple PDFs.\",\n" +
                "    \"Retry plain text documents.\",\n" +
                "    \"Review network proxy configurations.\",\n" +
                "    \"Attempt in dark mode toggle.\",\n" +
                "    \"Refresh the browser sessions.\",\n" +
                "    \"Try a different browser agent.\",\n" +
                "    \"Ensure database is active.\",\n" +
                "    \"Verify file structure parsing compatibility.\"\n" +
                "  ],\n" +
                "  \"estimatedScoreAfterImprovements\": 65,\n" +
                "  \"hiringRecommendation\": \"Moderate Match\"\n" +
                "}";
    }
}
