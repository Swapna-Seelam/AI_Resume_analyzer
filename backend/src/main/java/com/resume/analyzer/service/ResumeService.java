package com.resume.analyzer.service;

import com.resume.analyzer.entity.ResumeAnalysis;
import com.resume.analyzer.repository.ResumeAnalysisRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;

@Service
public class ResumeService {

    @Autowired
    private ResumeAnalysisRepository repository;

    public ResumeAnalysis parseAndInitResumeAndJd(MultipartFile resumeFile, MultipartFile jdFile, String jdText) throws IOException {
        String resumeFileName = resumeFile.getOriginalFilename();
        if (resumeFileName == null) {
            throw new IllegalArgumentException("Invalid resume file name");
        }

        String resumeText = "";

        if (resumeFileName.toLowerCase().endsWith(".pdf")) {
            resumeText = parsePdf(resumeFile.getInputStream());
        } else if (resumeFileName.toLowerCase().endsWith(".docx")) {
            resumeText = parseDocx(resumeFile.getInputStream());
        } else {
            throw new IllegalArgumentException("Unsupported resume file type. Only PDF and DOCX files are allowed.");
        }

        if (resumeText.trim().isEmpty()) {
            throw new IllegalArgumentException("The uploaded resume file does not contain any readable text.");
        }

        String jdFileName = null;
        String jdRawText = null;

        if (jdFile != null && !jdFile.isEmpty()) {
            jdFileName = jdFile.getOriginalFilename();
            if (jdFileName != null) {
                if (jdFileName.toLowerCase().endsWith(".pdf")) {
                    jdRawText = parsePdf(jdFile.getInputStream());
                } else if (jdFileName.toLowerCase().endsWith(".docx")) {
                    jdRawText = parseDocx(jdFile.getInputStream());
                } else if (jdFileName.toLowerCase().endsWith(".txt")) {
                    jdRawText = new String(jdFile.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
                } else {
                    throw new IllegalArgumentException("Unsupported Job Description file type. Allowed: PDF, DOCX, TXT.");
                }
            }
        } else if (jdText != null && !jdText.trim().isEmpty()) {
            jdFileName = "Pasted Job Description";
            jdRawText = jdText.trim();
        }

        if (jdRawText == null || jdRawText.trim().isEmpty()) {
            throw new IllegalArgumentException("A valid Job Description file or pasted text is required.");
        }

        ResumeAnalysis analysis = new ResumeAnalysis();
        analysis.setFileName(resumeFileName);
        analysis.setUploadDate(LocalDateTime.now());
        analysis.setRawText(resumeText);
        analysis.setJdFileName(jdFileName);
        analysis.setJdRawText(jdRawText);

        return repository.save(analysis);
    }

    private String parsePdf(InputStream inputStream) throws IOException {
        try (PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String parseDocx(InputStream inputStream) throws IOException {
        try (XWPFDocument document = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }
}
