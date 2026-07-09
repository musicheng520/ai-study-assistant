-- =========================================================
-- Day 3 Module 21
-- Summary / Quiz / Flashcard Learning Content Tables
-- =========================================================

-- 说明：
-- 1. documents 是数据库表名
-- 2. Java 里后续仍然可以叫 CourseDocument，避免和 Spring AI Document 冲突
-- 3. document_id = NULL 表示 course-level 内容
-- 4. document_id != NULL 表示 document-level 内容


-- =========================================================
-- 1. summaries
-- =========================================================

CREATE TABLE IF NOT EXISTS summaries (
                                         id BIGINT AUTO_INCREMENT PRIMARY KEY,

                                         user_id BIGINT NOT NULL,
                                         course_id BIGINT NOT NULL,
                                         document_id BIGINT NULL,

                                         title VARCHAR(255) NOT NULL,
    summary MEDIUMTEXT NOT NULL,
    key_concepts_json JSON NULL,
    definitions_json JSON NULL,
    revision_notes MEDIUMTEXT NOT NULL,

    source_scope VARCHAR(30) NOT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_summaries_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE,

    CONSTRAINT fk_summaries_course
    FOREIGN KEY (course_id) REFERENCES courses(id)
    ON DELETE CASCADE,

    CONSTRAINT fk_summaries_document
    FOREIGN KEY (document_id) REFERENCES documents(id)
    ON DELETE SET NULL,

    CONSTRAINT chk_summaries_source_scope
    CHECK (source_scope IN ('COURSE', 'DOCUMENT'))
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_summaries_user_course
    ON summaries(user_id, course_id);

CREATE INDEX idx_summaries_course_scope
    ON summaries(course_id, source_scope);

CREATE INDEX idx_summaries_document
    ON summaries(document_id);

CREATE INDEX idx_summaries_created_at
    ON summaries(created_at);


-- =========================================================
-- 2. quizzes
-- =========================================================

CREATE TABLE IF NOT EXISTS quizzes (
                                       id BIGINT AUTO_INCREMENT PRIMARY KEY,

                                       user_id BIGINT NOT NULL,
                                       course_id BIGINT NOT NULL,
                                       document_id BIGINT NULL,

                                       title VARCHAR(255) NOT NULL,
    difficulty VARCHAR(30) NOT NULL,
    source_scope VARCHAR(30) NOT NULL,
    question_count INT NOT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_quizzes_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE,

    CONSTRAINT fk_quizzes_course
    FOREIGN KEY (course_id) REFERENCES courses(id)
    ON DELETE CASCADE,

    CONSTRAINT fk_quizzes_document
    FOREIGN KEY (document_id) REFERENCES documents(id)
    ON DELETE SET NULL,

    CONSTRAINT chk_quizzes_difficulty
    CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD')),

    CONSTRAINT chk_quizzes_source_scope
    CHECK (source_scope IN ('COURSE', 'DOCUMENT')),

    CONSTRAINT chk_quizzes_question_count
    CHECK (question_count > 0)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_quizzes_user_course
    ON quizzes(user_id, course_id);

CREATE INDEX idx_quizzes_course_scope
    ON quizzes(course_id, source_scope);

CREATE INDEX idx_quizzes_document
    ON quizzes(document_id);

CREATE INDEX idx_quizzes_created_at
    ON quizzes(created_at);


-- =========================================================
-- 3. quiz_questions
-- =========================================================

CREATE TABLE IF NOT EXISTS quiz_questions (
                                              id BIGINT AUTO_INCREMENT PRIMARY KEY,

                                              quiz_id BIGINT NOT NULL,

                                              question_type VARCHAR(30) NOT NULL,
    question_text TEXT NOT NULL,
    options_json JSON NULL,
    correct_answer TEXT NOT NULL,
    explanation TEXT NOT NULL,
    difficulty VARCHAR(30) NOT NULL,
    topic VARCHAR(100) NOT NULL,
    source_chunk_id BIGINT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_quiz_questions_quiz
    FOREIGN KEY (quiz_id) REFERENCES quizzes(id)
    ON DELETE CASCADE,

    CONSTRAINT fk_quiz_questions_source_chunk
    FOREIGN KEY (source_chunk_id) REFERENCES document_chunks(id)
    ON DELETE SET NULL,

    CONSTRAINT chk_quiz_questions_type
    CHECK (question_type IN ('MCQ', 'SHORT_ANSWER')),

    CONSTRAINT chk_quiz_questions_difficulty
    CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD'))
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_quiz_questions_quiz
    ON quiz_questions(quiz_id);

CREATE INDEX idx_quiz_questions_type
    ON quiz_questions(question_type);

CREATE INDEX idx_quiz_questions_topic
    ON quiz_questions(topic);

CREATE INDEX idx_quiz_questions_source_chunk
    ON quiz_questions(source_chunk_id);

CREATE INDEX idx_quiz_questions_created_at
    ON quiz_questions(created_at);


-- =========================================================
-- 4. flashcards
-- =========================================================

CREATE TABLE IF NOT EXISTS flashcards (
                                          id BIGINT AUTO_INCREMENT PRIMARY KEY,

                                          user_id BIGINT NOT NULL,
                                          course_id BIGINT NOT NULL,
                                          document_id BIGINT NULL,

                                          front TEXT NOT NULL,
                                          back TEXT NOT NULL,
                                          topic VARCHAR(100) NOT NULL,
    difficulty VARCHAR(30) NOT NULL,
    source_type VARCHAR(50) NOT NULL,
    source_chunk_id BIGINT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_flashcards_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE,

    CONSTRAINT fk_flashcards_course
    FOREIGN KEY (course_id) REFERENCES courses(id)
    ON DELETE CASCADE,

    CONSTRAINT fk_flashcards_document
    FOREIGN KEY (document_id) REFERENCES documents(id)
    ON DELETE SET NULL,

    CONSTRAINT fk_flashcards_source_chunk
    FOREIGN KEY (source_chunk_id) REFERENCES document_chunks(id)
    ON DELETE SET NULL,

    CONSTRAINT chk_flashcards_difficulty
    CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD')),

    CONSTRAINT chk_flashcards_source_type
    CHECK (source_type IN ('COURSE', 'DOCUMENT'))
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_flashcards_user_course
    ON flashcards(user_id, course_id);

CREATE INDEX idx_flashcards_course_source_type
    ON flashcards(course_id, source_type);

CREATE INDEX idx_flashcards_document
    ON flashcards(document_id);

CREATE INDEX idx_flashcards_topic
    ON flashcards(topic);

CREATE INDEX idx_flashcards_source_chunk
    ON flashcards(source_chunk_id);

CREATE INDEX idx_flashcards_created_at
    ON flashcards(created_at);


-- =========================================================
-- 5. learning_history
-- 如果之前已经建过，这段不会重复创建
-- =========================================================

CREATE TABLE IF NOT EXISTS learning_history (
                                                id BIGINT AUTO_INCREMENT PRIMARY KEY,

                                                user_id BIGINT NOT NULL,
                                                course_id BIGINT NOT NULL,

                                                event_type VARCHAR(50) NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_id BIGINT NULL,
    topic VARCHAR(100) NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_learning_history_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE,

    CONSTRAINT fk_learning_history_course
    FOREIGN KEY (course_id) REFERENCES courses(id)
    ON DELETE CASCADE,

    CONSTRAINT chk_learning_history_event_type
    CHECK (event_type IN (
           'ASK',
           'SUMMARY',
           'QUIZ',
           'FLASHCARD',
           'NOTE',
           'REVIEW',
           'DOCUMENT_UPLOAD',
           'ASSIGNMENT_ANALYSIS',
           'RUBRIC_ANALYSIS',
           'REVISION_PACK'
                         )),

    CONSTRAINT chk_learning_history_target_type
    CHECK (target_type IN (
           'COURSE',
           'DOCUMENT',
           'SUMMARY',
           'QUIZ',
           'FLASHCARD',
           'TOPIC'
                          ))
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_learning_history_user_course
    ON learning_history(user_id, course_id);

CREATE INDEX idx_learning_history_event_type
    ON learning_history(event_type);

CREATE INDEX idx_learning_history_target
    ON learning_history(target_type, target_id);

CREATE INDEX idx_learning_history_created_at
    ON learning_history(created_at);


CREATE TABLE IF NOT EXISTS quiz_attempts (
                                             id BIGINT AUTO_INCREMENT PRIMARY KEY,

                                             user_id BIGINT NOT NULL,
                                             course_id BIGINT NOT NULL,
                                             quiz_id BIGINT NOT NULL,

                                             score DECIMAL(5,2) NOT NULL,
                                             total_questions INT NOT NULL,
                                             correct_count INT NOT NULL,

                                             started_at DATETIME NULL,
                                             submitted_at DATETIME NOT NULL,

                                             INDEX idx_quiz_attempts_user_id (user_id),
                                             INDEX idx_quiz_attempts_course_id (course_id),
                                             INDEX idx_quiz_attempts_quiz_id (quiz_id),
                                             INDEX idx_quiz_attempts_submitted_at (submitted_at)
);

CREATE TABLE IF NOT EXISTS quiz_attempt_answers (
                                                    id BIGINT AUTO_INCREMENT PRIMARY KEY,

                                                    attempt_id BIGINT NOT NULL,
                                                    question_id BIGINT NOT NULL,

                                                    user_answer TEXT,
                                                    is_correct BOOLEAN NOT NULL,

                                                    created_at DATETIME NOT NULL,

                                                    INDEX idx_quiz_attempt_answers_attempt_id (attempt_id),
                                                    INDEX idx_quiz_attempt_answers_question_id (question_id),
                                                    UNIQUE KEY uk_attempt_question (attempt_id, question_id)
);

CREATE TABLE IF NOT EXISTS wrong_answers (
                                             id BIGINT AUTO_INCREMENT PRIMARY KEY,

                                             user_id BIGINT NOT NULL,
                                             course_id BIGINT NOT NULL,
                                             quiz_id BIGINT NOT NULL,
                                             question_id BIGINT NOT NULL,

                                             topic VARCHAR(100) NOT NULL,
                                             user_answer TEXT,
                                             correct_answer TEXT,
                                             explanation TEXT,

                                             resolved BOOLEAN NOT NULL DEFAULT FALSE,

                                             created_at DATETIME NOT NULL,

                                             INDEX idx_wrong_answers_user_id (user_id),
                                             INDEX idx_wrong_answers_course_id (course_id),
                                             INDEX idx_wrong_answers_quiz_id (quiz_id),
                                             INDEX idx_wrong_answers_question_id (question_id),
                                             INDEX idx_wrong_answers_topic (topic),
                                             INDEX idx_wrong_answers_resolved (resolved),
                                             INDEX idx_wrong_answers_created_at (created_at)
);

CREATE TABLE IF NOT EXISTS study_streaks (
                                             id BIGINT AUTO_INCREMENT PRIMARY KEY,

                                             user_id BIGINT NOT NULL,
                                             current_streak INT NOT NULL DEFAULT 0,
                                             longest_streak INT NOT NULL DEFAULT 0,
                                             last_activity_date DATE NULL,

                                             updated_at DATETIME NOT NULL,

                                             UNIQUE KEY uk_study_streaks_user_id (user_id),
                                             INDEX idx_study_streaks_last_activity_date (last_activity_date)
);