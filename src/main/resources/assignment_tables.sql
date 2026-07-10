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


CREATE TABLE IF NOT EXISTS study_tasks (
                                           id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                           user_id BIGINT NOT NULL,
                                           course_id BIGINT NOT NULL,
                                           document_id BIGINT NULL,

                                           title VARCHAR(255) NOT NULL,
    description TEXT NULL,
    status VARCHAR(30) NOT NULL,
    due_date DATETIME NULL,
    source_type VARCHAR(50) NOT NULL,

    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,

    INDEX idx_study_tasks_user_course (user_id, course_id),
    INDEX idx_study_tasks_document (document_id),
    INDEX idx_study_tasks_status (status),
    INDEX idx_study_tasks_source_type (source_type),
    INDEX idx_study_tasks_due_date (due_date)
    );

CREATE TABLE IF NOT EXISTS revision_packs (
                                              id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                              user_id BIGINT NOT NULL,
                                              course_id BIGINT NOT NULL,

                                              title VARCHAR(255) NOT NULL,
    summary MEDIUMTEXT NOT NULL,

    weak_topics_json JSON NOT NULL,
    review_order_json JSON NOT NULL,
    recommended_actions_json JSON NOT NULL,
    related_documents_json JSON NOT NULL,
    study_tasks_json JSON NOT NULL,
    suggested_flashcards_json JSON NULL,

    generated_quiz_id BIGINT NULL,
    created_at DATETIME NOT NULL,

    INDEX idx_revision_packs_user_course (user_id, course_id),
    INDEX idx_revision_packs_created_at (created_at)
    );