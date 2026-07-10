CREATE TABLE IF NOT EXISTS assignment_analyses (
                                                   id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                                   user_id BIGINT NOT NULL,
                                                   course_id BIGINT NOT NULL,
                                                   document_id BIGINT NOT NULL,

                                                   requirements_json JSON NOT NULL,
                                                   deliverables_json JSON NOT NULL,
                                                   deadline DATETIME NULL,
                                                   checklist_json JSON NOT NULL,
                                                   high_score_tips MEDIUMTEXT NOT NULL,

                                                   suggested_structure_json JSON NULL,
                                                   risk_warnings_json JSON NULL,

                                                   created_at DATETIME NOT NULL,

                                                   INDEX idx_assignment_analyses_user_course (user_id, course_id),
    INDEX idx_assignment_analyses_document (document_id),
    INDEX idx_assignment_analyses_created_at (created_at)
    );

CREATE TABLE IF NOT EXISTS rubric_analyses (
                                               id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                               user_id BIGINT NOT NULL,
                                               course_id BIGINT NOT NULL,
                                               document_id BIGINT NOT NULL,

                                               criteria_json JSON NOT NULL,
                                               excellent_band_json JSON NOT NULL,
                                               common_mistakes MEDIUMTEXT NOT NULL,
                                               high_score_strategy MEDIUMTEXT NOT NULL,

                                               created_at DATETIME NOT NULL,

                                               INDEX idx_rubric_analyses_user_course (user_id, course_id),
    INDEX idx_rubric_analyses_document (document_id),
    INDEX idx_rubric_analyses_created_at (created_at)
    );