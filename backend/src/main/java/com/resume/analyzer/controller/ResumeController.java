package com.resume.analyzer.controller;

import com.resume.analyzer.entity.ResumeAnalysis;
import com.resume.analyzer.repository.ResumeAnalysisRepository;
import com.resume.analyzer.service.GeminiService;
import com.resume.analyzer.service.ResumeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/resume")
@CrossOrigin(origins = "*")
public class ResumeController {

    @Autowired
    private ResumeService resumeService;

    @Autowired
    private GeminiService geminiService;

    @Autowired
    private ResumeAnalysisRepository repository;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadResume(
            @RequestParam("resume") MultipartFile resumeFile,
            @RequestParam(value = "jdFile", required = false) MultipartFile jdFile,
            @RequestParam(value = "jdText", required = false) String jdText
    ) {
        try {
            ResumeAnalysis analysis = resumeService.parseAndInitResumeAndJd(resumeFile, jdFile, jdText);
            Map<String, Object> response = new HashMap<>();
            response.put("id", analysis.getId());
            response.put("fileName", analysis.getFileName());
            response.put("uploadDate", analysis.getUploadDate());
            response.put("rawText", analysis.getRawText());
            response.put("jdFileName", analysis.getJdFileName());
            response.put("jdRawText", analysis.getJdRawText());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to parse files: " + e.getMessage()));
        }
    }

    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeResume(@RequestBody Map<String, Long> request) {
        try {
            Long id = request.get("id");
            if (id == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Resume ID parameter 'id' is required"));
            }
            ResumeAnalysis analysis = geminiService.analyzeResume(id);
            return ResponseEntity.ok(analysis);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Analysis failed: " + e.getMessage()));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<ResumeAnalysis>> getHistory() {
        List<ResumeAnalysis> history = repository.findAllByOrderByUploadDateDesc();
        return ResponseEntity.ok(history);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAnalysisById(@PathVariable("id") Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAnalysis(@PathVariable("id") Long id) {
        try {
            if (!repository.existsById(id)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Analysis record not found"));
            }
            repository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Analysis deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete: " + e.getMessage()));
        }
    }
}
