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