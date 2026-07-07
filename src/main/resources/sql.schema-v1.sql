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

CREATE TABLE IF NOT EXISTS document_chunks (
                                               id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                               user_id BIGINT NOT NULL,
                                               course_id BIGINT NOT NULL,
                                               document_id BIGINT NOT NULL,
                                               chunk_index INT NOT NULL,
                                               content MEDIUMTEXT NOT NULL,
                                               content_hash VARCHAR(64) NOT NULL,
                                               page_number INT,
                                               section_title VARCHAR(255),
                                               token_count INT,
                                               vector_key VARCHAR(255),
                                               vector_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
                                               embedding_model VARCHAR(100),
                                               embedding_dimension INT,
                                               created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                               CONSTRAINT fk_chunks_user
                                                   FOREIGN KEY (user_id) REFERENCES users(id)
                                                       ON DELETE CASCADE,

                                               CONSTRAINT fk_chunks_course
                                                   FOREIGN KEY (course_id) REFERENCES courses(id)
                                                       ON DELETE CASCADE,

                                               CONSTRAINT fk_chunks_document
                                                   FOREIGN KEY (document_id) REFERENCES documents(id)
                                                       ON DELETE CASCADE
);

CREATE INDEX idx_chunks_document
    ON document_chunks(document_id);

CREATE INDEX idx_chunks_course_user
    ON document_chunks(user_id, course_id);

CREATE INDEX idx_chunks_vector_status
    ON document_chunks(vector_status);

CREATE TABLE IF NOT EXISTS chat_sessions (
                                             id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                             user_id BIGINT NOT NULL,
                                             course_id BIGINT NOT NULL,
                                             title VARCHAR(255),
                                             scope_type VARCHAR(30) NOT NULL,
                                             document_id BIGINT NULL,
                                             created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                             updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                                             CONSTRAINT fk_chat_sessions_user
                                                 FOREIGN KEY (user_id) REFERENCES users(id)
                                                     ON DELETE CASCADE,

                                             CONSTRAINT fk_chat_sessions_course
                                                 FOREIGN KEY (course_id) REFERENCES courses(id)
                                                     ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS chat_messages (
                                             id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                             session_id BIGINT NOT NULL,
                                             user_id BIGINT NOT NULL,
                                             course_id BIGINT NOT NULL,
                                             role VARCHAR(30) NOT NULL,
                                             content MEDIUMTEXT NOT NULL,
                                             workflow_type VARCHAR(50),
                                             no_answer BOOLEAN NOT NULL DEFAULT FALSE,
                                             model_name VARCHAR(100),
                                             created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                             CONSTRAINT fk_chat_messages_session
                                                 FOREIGN KEY (session_id) REFERENCES chat_sessions(id)
                                                     ON DELETE CASCADE,

                                             CONSTRAINT fk_chat_messages_user
                                                 FOREIGN KEY (user_id) REFERENCES users(id)
                                                     ON DELETE CASCADE,

                                             CONSTRAINT fk_chat_messages_course
                                                 FOREIGN KEY (course_id) REFERENCES courses(id)
                                                     ON DELETE CASCADE
);
CREATE TABLE IF NOT EXISTS chat_message_citations (
                                                      id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                                      message_id BIGINT NOT NULL,
                                                      document_id BIGINT NOT NULL,
                                                      chunk_id BIGINT NOT NULL,
                                                      file_name VARCHAR(255),
                                                      page_number INT,
                                                      section_title VARCHAR(255),
                                                      snippet TEXT,
                                                      created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                                      CONSTRAINT fk_chat_citations_message
                                                          FOREIGN KEY (message_id) REFERENCES chat_messages(id)
                                                              ON DELETE CASCADE,

                                                      CONSTRAINT fk_chat_citations_document
                                                          FOREIGN KEY (document_id) REFERENCES documents(id)
                                                              ON DELETE CASCADE,

                                                      CONSTRAINT fk_chat_citations_chunk
                                                          FOREIGN KEY (chunk_id) REFERENCES document_chunks(id)
                                                              ON DELETE CASCADE
);
CREATE INDEX idx_chat_sessions_user_course
    ON chat_sessions(user_id, course_id);

CREATE INDEX idx_chat_messages_session
    ON chat_messages(session_id);

CREATE INDEX idx_chat_citations_message
    ON chat_message_citations(message_id);

CREATE INDEX idx_chat_citations_document
    ON chat_message_citations(document_id);

CREATE INDEX idx_chat_citations_chunk
    ON chat_message_citations(chunk_id);