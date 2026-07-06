CREATE TABLE IF NOT EXISTS users (
                                     id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                     email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100),
    role VARCHAR(30) NOT NULL DEFAULT 'USER',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS courses (
                                       id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                       user_id BIGINT NOT NULL,
                                       name VARCHAR(150) NOT NULL,
    code VARCHAR(50),
    description TEXT,
    color VARCHAR(30),
    progress_score DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_courses_user
    FOREIGN KEY (user_id) REFERENCES users(id)
                                                           ON DELETE CASCADE
    );

CREATE TABLE IF NOT EXISTS documents (
                                         id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                         user_id BIGINT NOT NULL,
                                         course_id BIGINT NOT NULL,
                                         original_file_name VARCHAR(255) NOT NULL,
                                         stored_file_path VARCHAR(500) NOT NULL,
                                         file_type VARCHAR(20) NOT NULL,
                                         document_type VARCHAR(50) NOT NULL DEFAULT 'OTHER',
                                         file_size BIGINT NOT NULL,
                                         status VARCHAR(30) NOT NULL DEFAULT 'PROCESSING',
                                         error_message TEXT,
                                         version INT NOT NULL DEFAULT 1,
                                         total_pages INT,
                                         chunk_count INT NOT NULL DEFAULT 0,
                                         processed_at DATETIME NULL,
                                         created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                         updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                                         CONSTRAINT fk_documents_user
                                             FOREIGN KEY (user_id) REFERENCES users(id)
                                                 ON DELETE CASCADE,

                                         CONSTRAINT fk_documents_course
                                             FOREIGN KEY (course_id) REFERENCES courses(id)
                                                 ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS document_processing_jobs (
                                                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                                        user_id BIGINT NOT NULL,
                                                        course_id BIGINT NOT NULL,
                                                        document_id BIGINT NOT NULL,
                                                        status VARCHAR(30) NOT NULL DEFAULT 'QUEUED',
                                                        step VARCHAR(50) NOT NULL DEFAULT 'UPLOAD',
                                                        error_message TEXT,
                                                        retry_count INT NOT NULL DEFAULT 0,
                                                        started_at DATETIME NULL,
                                                        completed_at DATETIME NULL,
                                                        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                                        CONSTRAINT fk_doc_jobs_user
                                                            FOREIGN KEY (user_id) REFERENCES users(id)
                                                                ON DELETE CASCADE,

                                                        CONSTRAINT fk_doc_jobs_course
                                                            FOREIGN KEY (course_id) REFERENCES courses(id)
                                                                ON DELETE CASCADE,

                                                        CONSTRAINT fk_doc_jobs_document
                                                            FOREIGN KEY (document_id) REFERENCES documents(id)
                                                                ON DELETE CASCADE
);