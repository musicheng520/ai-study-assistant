CREATE TABLE IF NOT EXISTS ai_workflow_runs (
                                                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                                user_id BIGINT NOT NULL,
                                                course_id BIGINT NULL,
                                                workflow_type VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    input_json JSON NULL,
    output_json JSON NULL,
    error_message TEXT NULL,
    started_at DATETIME NOT NULL,
    completed_at DATETIME NULL,

    INDEX idx_ai_workflow_runs_user_course (user_id, course_id),
    INDEX idx_ai_workflow_runs_status (status),
    INDEX idx_ai_workflow_runs_type (workflow_type),
    INDEX idx_ai_workflow_runs_started_at (started_at)
    );

CREATE TABLE IF NOT EXISTS ai_workflow_steps (
                                                 id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                                 workflow_run_id BIGINT NOT NULL,
                                                 step_name VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL,
    started_at DATETIME NOT NULL,
    completed_at DATETIME NULL,
    error_message TEXT NULL,

    INDEX idx_ai_workflow_steps_run_id (workflow_run_id),
    INDEX idx_ai_workflow_steps_status (status),

    CONSTRAINT fk_ai_workflow_steps_run
    FOREIGN KEY (workflow_run_id)
    REFERENCES ai_workflow_runs(id)
    ON DELETE CASCADE
    );

CREATE TABLE IF NOT EXISTS notes (
                                     id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                     user_id BIGINT NOT NULL,
                                     course_id BIGINT NOT NULL,
                                     document_id BIGINT NULL,
                                     title VARCHAR(255) NOT NULL,
    content MEDIUMTEXT NOT NULL,
    topic VARCHAR(100) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,

    INDEX idx_notes_user_course (user_id, course_id),
    INDEX idx_notes_document (document_id),
    INDEX idx_notes_topic (topic)
    );