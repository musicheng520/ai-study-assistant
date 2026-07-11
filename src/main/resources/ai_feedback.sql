CREATE TABLE ai_feedback (
                             id BIGINT PRIMARY KEY AUTO_INCREMENT,

                             user_id BIGINT NOT NULL,
                             course_id BIGINT NOT NULL,

                             target_type VARCHAR(50) NOT NULL,
                             target_id BIGINT NOT NULL,

                             rating VARCHAR(30) NOT NULL,
                             comment TEXT NULL,

                             created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                             CONSTRAINT chk_ai_feedback_target_type
                                 CHECK (
                                     target_type IN (
                                                     'ANSWER',
                                                     'SUMMARY',
                                                     'QUIZ',
                                                     'FLASHCARD',
                                                     'ASSIGNMENT_ANALYSIS',
                                                     'RUBRIC_ANALYSIS',
                                                     'REVISION_PACK'
                                         )
                                     ),

                             CONSTRAINT chk_ai_feedback_rating
                                 CHECK (
                                     rating IN (
                                                'HELPFUL',
                                                'NOT_HELPFUL',
                                                'INACCURATE'
                                         )
                                     ),

                             CONSTRAINT fk_ai_feedback_user
                                 FOREIGN KEY (user_id)
                                     REFERENCES users(id)
                                     ON DELETE CASCADE,

                             CONSTRAINT fk_ai_feedback_course
                                 FOREIGN KEY (course_id)
                                     REFERENCES courses(id)
                                     ON DELETE CASCADE,

                             INDEX idx_ai_feedback_user_created (
                                 user_id,
                                 created_at
                                 ),

                             INDEX idx_ai_feedback_course_created (
                                 course_id,
                                 created_at
                                 ),

                             INDEX idx_ai_feedback_target (
                                 target_type,
                                 target_id
                                 )
);

CREATE TABLE ai_request_logs (
                                 id BIGINT PRIMARY KEY AUTO_INCREMENT,

                                 user_id BIGINT NOT NULL,
                                 course_id BIGINT NULL,

                                 workflow_type VARCHAR(60) NOT NULL,
                                 model_name VARCHAR(100) NOT NULL DEFAULT 'unknown',

                                 prompt_tokens INT NOT NULL DEFAULT 0,
                                 completion_tokens INT NOT NULL DEFAULT 0,
                                 total_tokens INT NOT NULL DEFAULT 0,

                                 latency_ms BIGINT NOT NULL DEFAULT 0,

                                 cache_hit TINYINT(1) NOT NULL DEFAULT 0,
                                 retrieved_chunk_count INT NOT NULL DEFAULT 0,

                                 error_type VARCHAR(100) NULL,
                                 error_message TEXT NULL,

                                 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                 CONSTRAINT fk_ai_request_logs_user
                                     FOREIGN KEY (user_id)
                                         REFERENCES users(id)
                                         ON DELETE CASCADE,

                                 CONSTRAINT fk_ai_request_logs_course
                                     FOREIGN KEY (course_id)
                                         REFERENCES courses(id)
                                         ON DELETE SET NULL,

                                 INDEX idx_ai_request_logs_created_at (
                                     created_at
                                     ),

                                 INDEX idx_ai_request_logs_workflow_created (
                                     workflow_type,
                                     created_at
                                     ),

                                 INDEX idx_ai_request_logs_user_created (
                                     user_id,
                                     created_at
                                     ),

                                 INDEX idx_ai_request_logs_cache_created (
                                     cache_hit,
                                     created_at
                                     ),

                                 INDEX idx_ai_request_logs_error_created (
                                     error_type,
                                     created_at
                                     )
);