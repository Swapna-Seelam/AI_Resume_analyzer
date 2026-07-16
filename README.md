# AI Resume Analyzer & ATS Job Match System

An AI-powered web application that analyzes resumes against a Job Description (JD) to provide ATS compatibility scores, skill gap analysis, keyword recommendations, grammar feedback, and personalized resume improvement suggestions.

## Features

- Upload Resume (PDF/DOCX)
- Upload Job Description (PDF/DOCX/Text)
- AI-powered Resume vs JD comparison
- ATS Compatibility Score
- Overall Resume Score
- Job Match Score
- Skill Match Analysis
- Missing Skills Detection
- Keyword Extraction & Recommendations
- Grammar & Spelling Analysis
- Resume Format Evaluation
- Experience & Education Matching
- Project Relevance Analysis
- Personalized Resume Improvement Suggestions
- Interview Readiness Score
- Download Analysis Report
- Analysis History Dashboard

## Tech Stack

### Frontend
- React.js
- Material UI
- Axios

### Backend
- Spring Boot
- Spring Data JPA
- REST APIs

### Database
- MySQL

### AI Integration
- Google Gemini API

### Document Processing
- Apache PDFBox
- Apache POI

## Project Architecture

```
Frontend (React)
        │
        ▼
Spring Boot REST API
        │
        ▼
 Gemini API
        │
        ▼
Resume Analysis Engine
        │
        ▼
      MySQL
```

## Workflow

1. Upload the Job Description.
2. Upload the Resume.
3. Extract text from both documents.
4. Compare Resume with JD using Gemini AI.
5. Generate ATS scores and detailed analysis.
6. Display personalized recommendations.
7. Download the complete report.

## AI Analysis Includes

- ATS Compatibility Score
- Overall Resume Score
- Job Match Score
- Resume Format Score
- Grammar & Spelling Score
- Skills Match
- Missing Skills
- Missing Keywords
- Experience Match
- Education Match
- Project Analysis
- Strengths
- Weaknesses
- Resume Improvement Suggestions
- Interview Readiness Score
- Hiring Recommendation

## Installation

### Clone Repository

```bash
git clone https://github.com/Swapna-Seelam/AI_Resume_analyzer.git
```

### Backend

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

## Environment Variables

Create a `.env` file and add:

```env
GEMINI_API_KEY=YOUR_GEMINI_API_KEY
```

Update the database configuration in:

```
backend/src/main/resources/application.properties
```

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/resume_analyzer
spring.datasource.username=root
spring.datasource.password=your_password
```

## Future Enhancements

- Multi-JD comparison
- Resume optimization with one-click rewrite
- Cover Letter Generator
- LinkedIn Profile Analyzer
- Interview Question Generator
- Recruiter Dashboard
- Authentication & User Profiles

## Screenshots

_Add screenshots of the Home Page, Upload Page, Dashboard, and Analysis Report here._

## Author

**Swapna Seelam**

GitHub: https://github.com/Swapna-Seelam

## License

This project is licensed under the MIT License.
