# Module 55: API Contract Summary

This document summarizes the backend HTTP contract that the frontend can use directly.

Source of truth: Spring MVC controllers and DTOs under `src/main/java/com/msc/springai`.


## Day 6 Contract Freeze Corrections

The following points are fixed for frontend implementation:

- User roles are `USER` and `ADMIN`. Do not use `STUDENT` in frontend guards.
- Quiz generation uses singular paths: `/api/courses/{courseId}/quiz/generate` and `/api/documents/{documentId}/quiz/generate`.
- `draftKey` is an opaque backend string. The frontend must store it and send it back to the matching save endpoint, but must not parse or construct it.
- Feedback supports `ANSWER` in addition to generated learning resources.
- Rate fields such as `successRate` and `cacheHitRate` are percentages in the range `0-100`, not ratios in the range `0-1`.
- Notes CRUD is not part of the Day 6 active backend contract. `noteCount` may remain `0` until the V2 Notes module is implemented.

## Base Rules

- Base URL in local development: `http://localhost:8080`
- Content type for JSON requests: `application/json`
- File upload content type: `multipart/form-data`
- Time format: Java `LocalDateTime` serialized by Jackson, expected as ISO-like strings such as `2026-07-11T19:10:00`.
- Date format: Java `LocalDate`, expected as `YYYY-MM-DD`.
- Auth:
  - Public: `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/health`, `/api/dev/**`
  - All other production APIs require `Authorization: Bearer <token>`.
- Deletion responses:
  - Some endpoints return `204 No Content`.
  - Some endpoints return JSON like `{ "deleted": true, "quizId": 1 }`.

## Error Contract

All application errors are JSON objects shaped like:

```json
{
  "timestamp": "2026-07-11T19:10:00",
  "status": 400,
  "error": "Bad Request",
  "code": "INVALID_REQUEST",
  "message": "Human readable message"
}
```

Common status mapping:

| Status | Typical codes |
| --- | --- |
| `400` | `INVALID_REQUEST`, `VALIDATION_FAILED`, `AI_OUTPUT_INVALID`, `INVALID_AI_JSON`, `INVALID_DRAFT_KEY`, `INVALID_DRAFT_SCOPE`, `INVALID_DRAFT_TYPE`, `INVALID_DRAFT_VALUE`, `DOCUMENT_NOT_READY`, `NO_READY_DOCUMENTS`, `INVALID_FEEDBACK_TARGET_TYPE` |
| `401` | `UNAUTHORIZED` |
| `403` | `FORBIDDEN`, `COURSE_ACCESS_DENIED`, `DOCUMENT_ACCESS_DENIED`, `FORBIDDEN_DRAFT` |
| `404` | `COURSE_NOT_FOUND`, `DOCUMENT_NOT_FOUND`, `SUMMARY_NOT_FOUND`, `QUIZ_NOT_FOUND`, `FLASHCARD_NOT_FOUND`, `DRAFT_NOT_FOUND`, `FEEDBACK_TARGET_NOT_FOUND` |
| `502` | `AI_GENERATION_FAILED` |
| `500` | `INTERNAL_SERVER_ERROR`, Redis/serialization/hash failures |

## Shared Enums And Strings

| Name | Values |
| --- | --- |
| Difficulty | `EASY`, `MEDIUM`, `HARD` |
| Document status | `PROCESSING`, `READY`, `FAILED` in normal flows |
| Study task status | `TODO`, `DONE` |
| Study task source type | `ASSIGNMENT`, `RUBRIC`, `MANUAL`, `REVISION` |
| User role | `USER`, `ADMIN` |
| Workflow status | `RUNNING`, `SUCCESS`, `FAILED` |
| AI request workflow type | `RAG_QA`, `SUMMARY`, `QUIZ`, `FLASHCARD`, `SHORT_ANSWER_GRADING`, `ASSIGNMENT_ANALYSIS`, `RUBRIC_ANALYSIS`, `CHECKLIST_GENERATION`, `REVISION_PACK`, `COORDINATOR` |
| Workflow run type / intent | `RAG_QA`, `SUMMARY`, `QUIZ`, `FLASHCARD`, `ASSIGNMENT_ANALYSIS`, `RUBRIC_ANALYSIS`, `REVISION_PLAN`, `CHECKLIST`, `UNKNOWN` |
| Feedback target type | `ANSWER`, `SUMMARY`, `QUIZ`, `FLASHCARD`, `ASSIGNMENT_ANALYSIS`, `RUBRIC_ANALYSIS`, `REVISION_PACK` |
| Feedback rating | `HELPFUL`, `NOT_HELPFUL`, `INACCURATE` |

## Endpoint Index

| Area | Method | Path | Request | Response |
| --- | --- | --- | --- | --- |
| Health | `GET` | `/api/health` | - | `{status, app}` |
| Auth | `POST` | `/api/auth/register` | `RegisterRequest` | `AuthResponse` |
| Auth | `POST` | `/api/auth/login` | `LoginRequest` | `AuthResponse` |
| Auth | `GET` | `/api/auth/me` | - | `CurrentUserResponse` |
| Courses | `POST` | `/api/courses` | `CourseCreateRequest` | `CourseResponse` |
| Courses | `GET` | `/api/courses` | - | `CourseResponse[]` |
| Courses | `GET` | `/api/courses/{courseId}` | - | `CourseResponse` |
| Courses | `PUT` | `/api/courses/{courseId}` | `CourseUpdateRequest` | `CourseResponse` |
| Courses | `DELETE` | `/api/courses/{courseId}` | - | `204` |
| Courses | `GET` | `/api/courses/{courseId}/dashboard` | - | `CourseDashboardResponse` |
| Courses | `GET` | `/api/courses/{courseId}/overview` | - | `CourseOverviewResponse` |
| Documents | `POST` | `/api/courses/{courseId}/documents` | multipart `file`, `documentType?` | `DocumentResponse` |
| Documents | `GET` | `/api/courses/{courseId}/documents` | - | `DocumentResponse[]` |
| Documents | `GET` | `/api/documents/{documentId}` | - | `DocumentResponse` |
| Documents | `GET` | `/api/documents/{documentId}/status` | - | `DocumentStatusResponse` |
| Documents | `DELETE` | `/api/documents/{documentId}` | - | `204` |
| Documents | `POST` | `/api/documents/{documentId}/retry` | - | `DocumentResponse` |
| RAG Chat | `POST` | `/api/courses/{courseId}/chat/ask` | `RagAskRequest` | `RagAskResponse` |
| RAG Source | `GET` | `/api/courses/{courseId}/sources/chunks/{chunkId}` | - | `SourceChunkResponse` |
| Summary | `POST` | `/api/courses/{courseId}/summary/generate` | `SummaryGenerateRequest?` | `SummaryGenerateResponse` |
| Summary | `POST` | `/api/documents/{documentId}/summary/generate` | `SummaryGenerateRequest?` | `SummaryGenerateResponse` |
| Summary | `POST` | `/api/summary/save` | `SaveDraftRequest` | `SummarySaveResponse` |
| Summary | `GET` | `/api/courses/{courseId}/summaries` | - | `SavedSummaryResponse[]` |
| Summary | `DELETE` | `/api/summary/{summaryId}` | - | `{deleted, summaryId}` |
| Quiz | `POST` | `/api/courses/{courseId}/quiz/generate` | `QuizGenerateRequest?` | `QuizGenerateResponse` |
| Quiz | `POST` | `/api/documents/{documentId}/quiz/generate` | `QuizGenerateRequest?` | `QuizGenerateResponse` |
| Quiz | `POST` | `/api/quiz/save` | `SaveDraftRequest` | `QuizSaveResponse` |
| Quiz | `GET` | `/api/courses/{courseId}/quizzes` | - | `SavedQuizResponse[]` |
| Quiz | `GET` | `/api/quizzes/{quizId}` | - | `QuizDetailResponse` |
| Quiz | `DELETE` | `/api/quizzes/{quizId}` | - | `{deleted, quizId}` |
| Quiz | `POST` | `/api/quiz/{quizId}/submit` | `SubmitQuizRequest` | `QuizSubmitResponse` |
| Quiz | `GET` | `/api/quiz/{quizId}/attempts` | - | `QuizAttemptResponse[]` |
| Flashcards | `POST` | `/api/courses/{courseId}/flashcards/generate` | `FlashcardGenerateRequest?` | `FlashcardGenerateResponse` |
| Flashcards | `POST` | `/api/documents/{documentId}/flashcards/generate` | `FlashcardGenerateRequest?` | `FlashcardGenerateResponse` |
| Flashcards | `POST` | `/api/courses/{courseId}/flashcards/generate-from-wrong-topics` | `WrongTopicFlashcardGenerateRequest?` | `WeakTopicFlashcardGenerateResponse` |
| Flashcards | `POST` | `/api/flashcards/save` | `SaveDraftRequest` | `FlashcardSaveResponse` |
| Flashcards | `GET` | `/api/courses/{courseId}/flashcards` | - | `SavedFlashcardResponse[]` |
| Flashcards | `DELETE` | `/api/flashcards/{flashcardId}` | - | `{deleted, flashcardId}` |
| Progress | `GET` | `/api/progress/overview` | - | `UserProgressOverviewResponse` |
| Progress | `GET` | `/api/courses/{courseId}/progress` | - | `CourseProgressResponse` |
| Progress | `GET` | `/api/courses/{courseId}/progress/wrong-topics` | - | `CourseWeakTopicsResponse` |
| Progress | `GET` | `/api/courses/{courseId}/progress/recommendations` | - | `CourseReviewRecommendationsResponse` |
| Progress | `GET` | `/api/courses/{courseId}/activity` | - | `CourseActivityResponse` |
| Progress | `GET` | `/api/progress/streak` | - | `StudyStreakResponse` |
| Wrong Answers | `GET` | `/api/courses/{courseId}/wrong-answers` | query `resolved?`, `topic?` | `CourseWrongAnswersResponse` |
| Wrong Answers | `PATCH` | `/api/wrong-answers/{wrongAnswerId}/resolved` | - | `WrongAnswerResolvedResponse` |
| Wrong Answers | `DELETE` | `/api/wrong-answers/{wrongAnswerId}` | - | `{deleted, wrongAnswerId}` |
| History | `GET` | `/api/learning-history/recent` | query `limit?`, `offset?` | `LearningHistoryListResponse` |
| History | `GET` | `/api/courses/{courseId}/learning-history` | query `limit?`, `offset?` | `LearningHistoryListResponse` |
| History | `GET` | `/api/courses/{courseId}/learning-history/summary` | - | `LearningHistorySummaryResponse` |
| Study Tasks | `POST` | `/api/courses/{courseId}/tasks` | `CreateStudyTaskRequest` | `StudyTaskResponse` |
| Study Tasks | `POST` | `/api/courses/{courseId}/tasks/generate` | `GenerateStudyTasksRequest?` | `GenerateStudyTasksResponse` |
| Study Tasks | `GET` | `/api/courses/{courseId}/tasks` | - | `StudyTaskResponse[]` |
| Study Tasks | `PUT` | `/api/tasks/{taskId}` | `UpdateStudyTaskRequest` | `StudyTaskResponse` |
| Study Tasks | `PATCH` | `/api/tasks/{taskId}/complete` | - | `StudyTaskResponse` |
| Study Tasks | `DELETE` | `/api/tasks/{taskId}` | - | empty body |
| Assignment | `POST` | `/api/documents/{documentId}/assignment/analyze` | `AssignmentAnalyzeRequest?` | `AssignmentAnalysisResponse` |
| Assignment | `GET` | `/api/courses/{courseId}/assignment-analyses` | - | `AssignmentAnalysisResponse[]` |
| Rubric | `POST` | `/api/documents/{documentId}/rubric/analyze` | `RubricAnalyzeRequest?` | `RubricAnalysisResponse` |
| Rubric | `GET` | `/api/courses/{courseId}/rubric-analyses` | - | `RubricAnalysisResponse[]` |
| Revision | `POST` | `/api/courses/{courseId}/revision-pack/generate` | `GenerateRevisionPackRequest?` | `RevisionPackResponse` |
| Revision | `GET` | `/api/courses/{courseId}/revision-packs` | - | `RevisionPackResponse[]` |
| Revision | `GET` | `/api/revision-packs/{packId}` | - | `RevisionPackResponse` |
| Workflow | `POST` | `/api/workflows/run` | `WorkflowRunRequest` | `WorkflowRunResponse` |
| Workflow | `GET` | `/api/workflows/{workflowRunId}/status` | - | `AiWorkflowRun` |
| Workflow | `GET` | `/api/workflows/{workflowRunId}/steps` | - | `AiWorkflowStep[]` |
| Feedback | `POST` | `/api/feedback` | `AiFeedbackCreateRequest` | `AiFeedbackResponse` |
| Feedback | `GET` | `/api/feedback/my` | - | `AiFeedbackResponse[]` |
| Admin | `GET` | `/api/admin/metrics/ai` | query `days?` | `AiMetricsResponse` |
| Admin | `GET` | `/api/admin/metrics/cache` | query `days?` | `CacheMetricsResponse` |
| Admin | `GET` | `/api/admin/logs/ai-requests` | query `workflowType?`, `onlyFailures?`, `limit?`, `offset?` | `AiRequestLogResponse[]` |
| Admin | `GET` | `/api/admin/workflows` | query `status?`, `workflowType?`, `limit?`, `offset?` | `WorkflowRunLogResponse[]` |

## Auth

### `POST /api/auth/register`

Request:

```json
{
  "email": "student@example.com",
  "password": "password123",
  "displayName": "Student Name"
}
```

Validation:

- `email`: required, valid email
- `password`: required, minimum length 8
- `displayName`: required, max length 100

Response `AuthResponse`:

```json
{
  "token": "jwt-token",
  "userId": 1,
  "email": "student@example.com",
  "displayName": "Student Name",
  "role": "USER"
}
```

### `POST /api/auth/login`

Request:

```json
{
  "email": "student@example.com",
  "password": "password123"
}
```

Response: same as `AuthResponse`.

### `GET /api/auth/me`

Response:

```json
{
  "id": 1,
  "email": "student@example.com",
  "displayName": "Student Name",
  "role": "USER",
  "status": "ACTIVE"
}
```

## Courses

`CourseCreateRequest` and `CourseUpdateRequest`:

```json
{
  "name": "Machine Learning",
  "code": "CS501",
  "description": "Optional description",
  "color": "#2563eb"
}
```

Validation:

- `name`: required, max length 150
- `code`: optional, max length 50
- `color`: optional, max length 30

`CourseResponse`:

```json
{
  "id": 1,
  "userId": 1,
  "name": "Machine Learning",
  "code": "CS501",
  "description": "Optional description",
  "color": "#2563eb",
  "progressScore": 72.5,
  "createdAt": "2026-07-11T19:10:00",
  "updatedAt": "2026-07-11T19:10:00"
}
```

`CourseDashboardResponse`:

```json
{
  "courseId": 1,
  "courseName": "Machine Learning",
  "documentCount": 3,
  "chatCount": 0,
  "quizCount": 0,
  "flashcardCount": 0,
  "message": "Dashboard API is ready. Chat, quiz and flashcard counts will be added later."
}
```

`CourseOverviewResponse`:

```json
{
  "courseId": 1,
  "courseName": "Machine Learning",
  "courseCode": "CS501",
  "courseColor": "#2563eb",
  "progressScore": 72.5,
  "stats": {
    "documentCount": 3,
    "readyDocumentCount": 2,
    "processingDocumentCount": 1,
    "failedDocumentCount": 0,
    "chatMessageCount": 18,
    "summaryCount": 4,
    "quizCount": 2,
    "quizAttemptCount": 5,
    "averageQuizScore": 81.5,
    "wrongAnswerCount": 7,
    "unresolvedWrongAnswerCount": 3,
    "flashcardCount": 20,
    "noteCount": 0,
    "taskCount": 5,
    "completedTaskCount": 2,
    "revisionPackCount": 1,
    "assignmentAnalysisCount": 1,
    "rubricAnalysisCount": 1
  },
  "weakTopics": [
    {
      "topic": "Gradient descent",
      "wrongCount": 3,
      "unresolvedCount": 2,
      "latestWrongAt": "2026-07-11T19:10:00"
    }
  ],
  "nextActions": [
    {
      "type": "REVIEW_WEAK_TOPIC",
      "title": "Review Gradient descent",
      "reason": "Recent unresolved mistakes",
      "priority": 1,
      "actionLabel": "Review",
      "targetPath": "/courses/1/progress"
    }
  ],
  "recentActivities": [
    {
      "id": 10,
      "eventType": "QUIZ",
      "targetType": "QUIZ",
      "targetId": 2,
      "topic": "Gradient descent",
      "title": "Quiz completed",
      "iconType": "quiz",
      "createdAt": "2026-07-11T19:10:00"
    }
  ],
  "generatedAt": "2026-07-11T19:10:00"
}
```

## Documents

### Upload

`POST /api/courses/{courseId}/documents`

Multipart fields:

- `file`: required file
- `documentType`: optional string, defaults to `OTHER`

Spring limit from config:

- max file size: `40MB`
- max request size: `40MB`

`DocumentResponse`:

```json
{
  "id": 1,
  "courseId": 1,
  "originalFileName": "lecture.pdf",
  "fileType": "pdf",
  "documentType": "LECTURE",
  "fileSize": 1048576,
  "status": "PROCESSING",
  "errorMessage": null,
  "totalPages": null,
  "chunkCount": 0,
  "createdAt": "2026-07-11T19:10:00",
  "updatedAt": "2026-07-11T19:10:00"
}
```

`DocumentStatusResponse`:

```json
{
  "documentId": 1,
  "status": "READY",
  "errorMessage": null,
  "chunkCount": 42
}
```

Frontend polling suggestion: after upload, poll `GET /api/documents/{documentId}/status` until `status` is `READY` or `FAILED`.

## RAG Chat And Sources

`RagAskRequest`:

```json
{
  "question": "Explain gradient descent",
  "sessionId": null,
  "topK": 5
}
```

Notes:

- `sessionId` is optional. If omitted or null, backend creates a new chat session.
- `topK` is optional; service default is intended around the retrieval default.

`RagAskResponse`:

```json
{
  "sessionId": 1,
  "userMessageId": 10,
  "assistantMessageId": 11,
  "answer": "Gradient descent is...",
  "noAnswer": false,
  "workflowType": "RAG_QA",
  "retrievedChunkCount": 5,
  "citations": [
    {
      "citationIndex": 1,
      "documentId": 1,
      "chunkId": 99,
      "fileName": "lecture.pdf",
      "pageNumber": 3,
      "sectionTitle": "Optimization",
      "snippet": "Gradient descent updates parameters...",
      "distance": 0.24
    }
  ]
}
```

`SourceChunkResponse`:

```json
{
  "chunkId": 99,
  "userId": 1,
  "courseId": 1,
  "documentId": 1,
  "chunkIndex": 4,
  "content": "Full chunk text",
  "contentHash": "hash",
  "pageNumber": 3,
  "sectionTitle": "Optimization",
  "tokenCount": 320,
  "vectorKey": "vector:chunk:99",
  "vectorStatus": "READY",
  "embeddingModel": "text-embedding-v4",
  "embeddingDimension": 2048,
  "fileName": "lecture.pdf",
  "fileType": "pdf",
  "documentType": "LECTURE",
  "createdAt": "2026-07-11T19:10:00"
}
```

## Generated Learning Content

Generation endpoints return a `draftKey`. Save endpoints persist generated content by sending that `draftKey`. Treat the `draftKey` as opaque: do not split it, infer scope from it, or build it on the frontend.

Common save request:

```json
{
  "draftKey": "cache:summary:draft:1:1:COURSE:abc123"
}
```

### Summary

`SummaryGenerateRequest`:

```json
{
  "topK": 3,
  "retrievalQuery": "Focus on exam definitions"
}
```

Defaults and bounds:

- `topK`: default `3`, min `1`, max `8`
- `retrievalQuery`: optional

`SummaryGenerateResponse`:

```json
{
  "draftKey": "cache:summary:draft:1:1:COURSE:abc123",
  "title": "Optimization Summary",
  "summary": "Main summary text",
  "keyConcepts": [
    {
      "name": "Gradient descent",
      "explanation": "An iterative optimization method"
    }
  ],
  "definitions": [
    {
      "term": "Learning rate",
      "definition": "Step size for each update"
    }
  ],
  "revisionNotes": "Revise convergence assumptions",
  "sourceScope": "COURSE"
}
```

`SummarySaveResponse`:

```json
{
  "summaryId": 1
}
```

`SavedSummaryResponse`:

```json
{
  "id": 1,
  "userId": 1,
  "courseId": 1,
  "documentId": null,
  "title": "Optimization Summary",
  "summary": "Main summary text",
  "keyConceptsJson": "[...]",
  "definitionsJson": "[...]",
  "revisionNotes": "Revise convergence assumptions",
  "sourceScope": "COURSE",
  "createdAt": "2026-07-11T19:10:00"
}
```

### Quiz

`QuizGenerateRequest`:

```json
{
  "topK": 3,
  "retrievalQuery": "Focus on supervised learning",
  "mcqCount": 2,
  "shortAnswerCount": 1,
  "difficulty": "MEDIUM"
}
```

Defaults and bounds:

- `topK`: default `1`, min `1`, max `5`
- `mcqCount`: default `2`, min `0`, max `5`
- `shortAnswerCount`: default `1`, min `0`, max `5`
- If both question counts are `0`, backend resets to `2` MCQ and `1` short answer.
- `difficulty`: normalized to uppercase; invalid values become `MEDIUM`.

`QuizGenerateResponse`:

```json
{
  "draftKey": "cache:quiz:draft:1:1:COURSE:abc123",
  "title": "Optimization Quiz",
  "difficulty": "MEDIUM",
  "sourceScope": "COURSE",
  "questionCount": 3,
  "questions": [
    {
      "questionType": "MCQ",
      "questionText": "What does the learning rate control?",
      "options": ["Step size", "Dataset size", "Model count"],
      "correctAnswer": "Step size",
      "explanation": "The learning rate controls update magnitude.",
      "difficulty": "MEDIUM",
      "topic": "Optimization",
      "sourceChunkId": 99
    }
  ]
}
```

`QuizSaveResponse`:

```json
{
  "quizId": 1
}
```

`SavedQuizResponse`:

```json
{
  "id": 1,
  "userId": 1,
  "courseId": 1,
  "documentId": null,
  "title": "Optimization Quiz",
  "difficulty": "MEDIUM",
  "sourceScope": "COURSE",
  "questionCount": 3,
  "createdAt": "2026-07-11T19:10:00"
}
```

`QuizDetailResponse` includes `SavedQuizResponse` fields plus:

```json
{
  "questions": [
    {
      "id": 1,
      "quizId": 1,
      "questionType": "MCQ",
      "questionText": "What does the learning rate control?",
      "optionsJson": "[\"Step size\",\"Dataset size\",\"Model count\"]",
      "correctAnswer": "Step size",
      "explanation": "The learning rate controls update magnitude.",
      "difficulty": "MEDIUM",
      "topic": "Optimization",
      "sourceChunkId": 99,
      "createdAt": "2026-07-11T19:10:00"
    }
  ]
}
```

`SubmitQuizRequest`:

```json
{
  "answers": [
    {
      "questionId": 1,
      "answer": "Step size"
    }
  ]
}
```

`QuizSubmitResponse`:

```json
{
  "attemptId": 1,
  "quizId": 1,
  "score": 100.0,
  "totalQuestions": 1,
  "correctCount": 1,
  "wrongAnswers": []
}
```

`QuizAttemptResponse`:

```json
{
  "attemptId": 1,
  "quizId": 1,
  "score": 100.0,
  "totalQuestions": 1,
  "correctCount": 1,
  "startedAt": "2026-07-11T19:10:00",
  "submittedAt": "2026-07-11T19:12:00"
}
```

### Flashcards

`FlashcardGenerateRequest`:

```json
{
  "topK": 3,
  "retrievalQuery": "Key formulas",
  "count": 4,
  "difficulty": "MEDIUM"
}
```

Defaults and bounds:

- `topK`: default `1`, min `1`, max `5`
- `count`: default `4`, min `1`, max `8`
- `difficulty`: normalized to uppercase; invalid values become `MEDIUM`.

`WrongTopicFlashcardGenerateRequest`:

```json
{
  "topicLimit": 3,
  "cardsPerTopic": 3,
  "difficulty": "MEDIUM",
  "topK": 9
}
```

Defaults and bounds:

- `topicLimit`: default `3`, min `1`, max `5`
- `cardsPerTopic`: default `3`, min `1`, max `5`
- `topK`: default `topicLimit * 3`, min `1`, max `10`

`FlashcardGenerateResponse`:

```json
{
  "draftKey": "cache:flashcard:draft:1:1:COURSE:abc123",
  "title": "Optimization Flashcards",
  "sourceScope": "COURSE",
  "count": 4,
  "difficulty": "MEDIUM",
  "cards": [
    {
      "front": "What is learning rate?",
      "back": "The step size used during parameter updates.",
      "topic": "Optimization",
      "difficulty": "MEDIUM",
      "sourceChunkId": 99
    }
  ]
}
```

`WeakTopicFlashcardGenerateResponse`:

```json
{
  "draftKey": "cache:flashcard:draft:1:1:COURSE:abc123",
  "sourceType": "WRONG_TOPICS",
  "topics": ["Optimization"],
  "cards": [
    {
      "front": "What is learning rate?",
      "back": "The step size used during parameter updates.",
      "topic": "Optimization",
      "difficulty": "MEDIUM",
      "sourceChunkId": 99
    }
  ]
}
```

`FlashcardSaveResponse`:

```json
{
  "savedCount": 4,
  "flashcardIds": [1, 2, 3, 4]
}
```

`SavedFlashcardResponse`:

```json
{
  "id": 1,
  "userId": 1,
  "courseId": 1,
  "documentId": null,
  "front": "What is learning rate?",
  "back": "The step size used during parameter updates.",
  "topic": "Optimization",
  "difficulty": "MEDIUM",
  "sourceType": "COURSE",
  "sourceChunkId": 99,
  "createdAt": "2026-07-11T19:10:00"
}
```

## Progress, Wrong Answers, And History

`UserProgressOverviewResponse`:

```json
{
  "courseCount": 3,
  "documentCount": 10,
  "readyDocumentCount": 8,
  "questionAskedCount": 25,
  "summaryCount": 6,
  "quizCount": 4,
  "flashcardCount": 30,
  "averageQuizScore": 78.4,
  "currentStreak": 5,
  "longestStreak": 12,
  "recentActivity": [
    {
      "eventType": "QUIZ",
      "targetType": "QUIZ",
      "targetId": 1,
      "topic": "Optimization",
      "createdAt": "2026-07-11T19:10:00"
    }
  ]
}
```

`CourseProgressResponse`:

```json
{
  "courseId": 1,
  "documentCount": 3,
  "readyDocumentCount": 2,
  "chatMessageCount": 18,
  "summaryCount": 4,
  "quizCount": 2,
  "quizAttemptCount": 5,
  "averageQuizScore": 81.5,
  "wrongAnswerCount": 7,
  "unresolvedWrongAnswerCount": 3,
  "flashcardCount": 20,
  "noteCount": 0,
  "progressScore": 72.5,
  "weakTopics": [],
  "recommendedNextReview": "Optimization",
  "recentActivity": []
}
```

`CourseWeakTopicsResponse`:

```json
{
  "courseId": 1,
  "topicCount": 1,
  "weakTopics": [
    {
      "topic": "Optimization",
      "wrongCount": 3,
      "resolvedCount": 1,
      "unresolvedCount": 2,
      "lastWrongAt": "2026-07-11T19:10:00",
      "relatedQuizCount": 2
    }
  ]
}
```

`CourseReviewRecommendationsResponse`:

```json
{
  "courseId": 1,
  "count": 1,
  "recommendations": [
    {
      "type": "WEAK_TOPIC",
      "topic": "Optimization",
      "quizId": 1,
      "documentId": 1,
      "reason": "Repeated wrong answers",
      "priority": 1,
      "action": "REVIEW"
    }
  ]
}
```

`CourseActivityResponse`:

```json
{
  "courseId": 1,
  "count": 1,
  "activities": [
    {
      "eventType": "SUMMARY",
      "targetType": "SUMMARY",
      "targetId": 1,
      "topic": "Optimization",
      "createdAt": "2026-07-11T19:10:00"
    }
  ]
}
```

`StudyStreakResponse`:

```json
{
  "currentStreak": 5,
  "longestStreak": 12,
  "lastActivityDate": "2026-07-11"
}
```

`CourseWrongAnswersResponse`:

```json
{
  "courseId": 1,
  "resolved": false,
  "topic": "Optimization",
  "total": 1,
  "topicGroups": [
    {
      "topic": "Optimization",
      "count": 3,
      "unresolvedCount": 2,
      "resolvedCount": 1
    }
  ],
  "items": [
    {
      "wrongAnswerId": 1,
      "quizId": 1,
      "questionId": 1,
      "topic": "Optimization",
      "userAnswer": "Dataset size",
      "correctAnswer": "Step size",
      "explanation": "Learning rate controls update magnitude.",
      "resolved": false,
      "createdAt": "2026-07-11T19:10:00"
    }
  ]
}
```

`WrongAnswerResolvedResponse`:

```json
{
  "wrongAnswerId": 1,
  "resolved": true,
  "topic": "Optimization"
}
```

`LearningHistoryListResponse`:

```json
{
  "courseId": 1,
  "limit": 20,
  "offset": 0,
  "count": 1,
  "activities": [
    {
      "id": 1,
      "courseId": 1,
      "eventType": "QUIZ",
      "targetType": "QUIZ",
      "targetId": 1,
      "topic": "Optimization",
      "title": "Quiz completed",
      "description": "Score: 100%",
      "iconType": "quiz",
      "createdAt": "2026-07-11T19:10:00"
    }
  ]
}
```

`LearningHistorySummaryResponse`:

```json
{
  "courseId": 1,
  "totalActivities": 20,
  "latestActivityAt": "2026-07-11T19:10:00",
  "eventTypeCounts": [
    {
      "eventType": "QUIZ",
      "count": 4
    }
  ]
}
```

## Study Tasks

`CreateStudyTaskRequest`:

```json
{
  "documentId": 1,
  "title": "Review chapter 3",
  "description": "Focus on optimization",
  "dueDate": "2026-07-15T09:00:00"
}
```

`GenerateStudyTasksRequest`:

```json
{
  "includeAssignment": true,
  "includeRubric": true,
  "skipExisting": true,
  "dueDate": "2026-07-15T09:00:00",
  "maxTasks": 20
}
```

Defaults and bounds:

- `maxTasks`: default `20`, max `50`

`GenerateStudyTasksResponse`:

```json
{
  "courseId": 1,
  "createdCount": 2,
  "message": "Created 2 study tasks",
  "tasks": []
}
```

`UpdateStudyTaskRequest`:

```json
{
  "title": "Review chapter 4",
  "description": "Updated description",
  "dueDate": "2026-07-16T09:00:00"
}
```

`StudyTaskResponse`:

```json
{
  "id": 1,
  "courseId": 1,
  "documentId": 1,
  "title": "Review chapter 3",
  "description": "Focus on optimization",
  "status": "TODO",
  "dueDate": "2026-07-15T09:00:00",
  "sourceType": "MANUAL",
  "createdAt": "2026-07-11T19:10:00",
  "updatedAt": "2026-07-11T19:10:00"
}
```

## Assignment, Rubric, And Revision Pack

`AssignmentAnalyzeRequest` and `RubricAnalyzeRequest`:

```json
{
  "force": false
}
```

`AssignmentAnalysisResponse`:

```json
{
  "id": 1,
  "courseId": 1,
  "documentId": 1,
  "requirements": ["Write a report"],
  "deliverables": ["PDF submission"],
  "deadline": "2026-07-20T23:59:00",
  "checklist": ["Define problem", "Discuss method"],
  "highScoreTips": "Use evidence from lectures.",
  "suggestedStructure": ["Introduction", "Method", "Evaluation"],
  "riskWarnings": ["Missing citations"],
  "createdAt": "2026-07-11T19:10:00"
}
```

`RubricAnalysisResponse`:

```json
{
  "id": 1,
  "courseId": 1,
  "documentId": 1,
  "criteria": [
    {
      "name": "Analysis",
      "weight": "40%",
      "description": "Depth and correctness"
    }
  ],
  "excellentBand": ["Clear argument", "Strong evidence"],
  "commonMistakes": "Superficial discussion",
  "highScoreStrategy": "Tie each claim to evidence.",
  "createdAt": "2026-07-11T19:10:00"
}
```

`GenerateRevisionPackRequest`:

```json
{
  "maxWeakTopics": 5,
  "maxRelatedChunks": 3
}
```

Defaults and bounds:

- `maxWeakTopics`: default `5`, max `10`
- `maxRelatedChunks`: default `3`, max `5`

`RevisionPackResponse`:

```json
{
  "id": 1,
  "courseId": 1,
  "title": "Week 7 Revision Pack",
  "summary": "Focus on optimization and evaluation.",
  "weakTopics": [
    {
      "topic": "Optimization",
      "reason": "Repeated quiz mistakes"
    }
  ],
  "reviewOrder": ["Optimization", "Evaluation"],
  "recommendedActions": ["Redo quiz 1"],
  "relatedDocuments": [
    {
      "documentId": 1,
      "fileName": "lecture.pdf"
    }
  ],
  "studyTasks": ["Review lecture.pdf page 3"],
  "suggestedFlashcards": ["Learning rate definition"],
  "generatedQuizId": 2,
  "createdAt": "2026-07-11T19:10:00"
}
```

## Workflow

`WorkflowRunRequest`:

```json
{
  "courseId": 1,
  "documentId": 1,
  "message": "Generate a revision plan"
}
```

`WorkflowRunResponse`:

```json
{
  "workflowRunId": 1,
  "intent": "REVISION_PLAN",
  "status": "SUCCESS",
  "message": "Workflow completed",
  "result": {}
}
```

`AiWorkflowRun`:

```json
{
  "id": 1,
  "userId": 1,
  "courseId": 1,
  "workflowType": "REVISION_PLAN",
  "status": "SUCCESS",
  "inputJson": "{}",
  "outputJson": "{}",
  "errorMessage": null,
  "startedAt": "2026-07-11T19:10:00",
  "completedAt": "2026-07-11T19:10:20"
}
```

`AiWorkflowStep`:

```json
{
  "id": 1,
  "workflowRunId": 1,
  "stepName": "Retrieve context",
  "status": "SUCCESS",
  "startedAt": "2026-07-11T19:10:00",
  "completedAt": "2026-07-11T19:10:02",
  "errorMessage": null
}
```

## Feedback

`AiFeedbackCreateRequest`:

```json
{
  "courseId": 1,
  "targetType": "ANSWER",
  "targetId": 11,
  "rating": "HELPFUL",
  "comment": "Useful explanation with clear citations"
}
```

Notes:

- `targetType` and `rating` are normalized to uppercase.
- Supported `targetType`: `ANSWER`, `SUMMARY`, `QUIZ`, `FLASHCARD`, `ASSIGNMENT_ANALYSIS`, `RUBRIC_ANALYSIS`, `REVISION_PACK`.
- For `ANSWER`, `targetId` is the assistant `chat_messages.id`.
- Backend verifies that the target exists and belongs to the current user/course.

`AiFeedbackResponse`:

```json
{
  "id": 1,
  "courseId": 1,
  "targetType": "ANSWER",
  "targetId": 11,
  "rating": "HELPFUL",
  "comment": "Useful explanation with clear citations",
  "createdAt": "2026-07-11T19:10:00"
}
```

## Admin Metrics


Notes:

- `successRate` and `cacheHitRate` are returned as percentages. Example: `95.0` means 95%.
- `totalTokens` may be `0` when the model provider does not return usage metadata or when the request is served from cache.

`AiMetricsResponse`:

```json
{
  "days": 7,
  "totalRequests": 100,
  "successCount": 95,
  "failedCount": 5,
  "successRate": 95.0,
  "totalTokens": 123456,
  "averageLatencyMs": 1200.5,
  "maxLatencyMs": 9000,
  "averageRetrievedChunkCount": 4.3,
  "workflowUsage": [
    {
      "workflowType": "SUMMARY",
      "requestCount": 20,
      "failedCount": 1,
      "totalTokens": 12000,
      "averageLatencyMs": 900.0
    }
  ],
  "recentFailures": [
    {
      "id": 1,
      "userId": 1,
      "courseId": 1,
      "workflowType": "QUIZ",
      "modelName": "deepseek-v4-pro",
      "errorType": "AI_GENERATION_FAILED",
      "errorMessage": "Generation failed",
      "createdAt": "2026-07-11T19:10:00"
    }
  ]
}
```

`CacheMetricsResponse`:

```json
{
  "days": 7,
  "ragRequestCount": 100,
  "cacheHitCount": 40,
  "cacheMissCount": 60,
  "cacheHitRate": 40.0,
  "averageCacheHitLatencyMs": 30.5,
  "averageCacheMissLatencyMs": 900.5,
  "averageRetrievedChunkCount": 4.3
}
```

`AiRequestLogResponse`:

```json
{
  "id": 1,
  "userId": 1,
  "courseId": 1,
  "workflowType": "RAG_QA",
  "modelName": "deepseek-v4-pro",
  "promptTokens": 1000,
  "completionTokens": 300,
  "totalTokens": 1300,
  "latencyMs": 1200,
  "cacheHit": false,
  "retrievedChunkCount": 5,
  "errorType": null,
  "errorMessage": null,
  "createdAt": "2026-07-11T19:10:00"
}
```

`WorkflowRunLogResponse`:

```json
{
  "id": 1,
  "userId": 1,
  "courseId": 1,
  "workflowType": "SUMMARY",
  "status": "SUCCESS",
  "errorMessage": null,
  "startedAt": "2026-07-11T19:10:00",
  "completedAt": "2026-07-11T19:10:20",
  "durationMs": 20000,
  "stepCount": 3
}
```


## V2 / Not Active In Day 6 Contract

The following items are planned or mentioned in the frontend design, but should not be implemented as Day 1 frontend dependencies unless matching backend endpoints are confirmed:

- Notes CRUD: `POST /api/courses/{courseId}/notes`, `POST /api/documents/{documentId}/notes`, `GET /api/courses/{courseId}/notes`, `PUT /api/notes/{noteId}`, `DELETE /api/notes/{noteId}`.
- `/api/version`: only use it if a real backend endpoint exists.
- Any exact workflow step timeline should only be shown when the backend returns real steps; do not fabricate parsing or AI steps on the frontend.

## Dev-Only Interfaces

`/api/dev/**` is public in `SecurityConfig`, but these endpoints should not be used as formal frontend product dependencies. They exist for local verification, seeding, parsing, chunking, Redis, vector search, retrieval tests, and draft-cache tests.

## Frontend Integration Checklist

1. Store `AuthResponse.token` after login/register and send it as `Authorization: Bearer <token>`.
2. For document upload, show `PROCESSING` immediately and poll document status.
3. For generated content, call `generate`, keep the returned opaque `draftKey`, then call the matching `save` endpoint when the user accepts it. Do not parse or construct `draftKey` on the frontend.
4. Parse saved quiz `optionsJson`, `keyConceptsJson`, and `definitionsJson` on the frontend if structured display is needed.
5. Treat AI and retrieval endpoints as potentially slow; show loading and retry states.
6. Use the shared error contract for toast/error banners.
