CREATE TABLE candidate_idea (
    id BIGINT NOT NULL AUTO_INCREMENT,
    team_id BIGINT NOT NULL,
    idea_title VARCHAR(100) NOT NULL,
    summary VARCHAR(300) NOT NULL,
    problem TEXT NOT NULL,
    target_users TEXT NOT NULL,
    solution TEXT NOT NULL,
    category VARCHAR(30) NOT NULL,
    visibility VARCHAR(20) NOT NULL,
    created_by_user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_candidate_idea_team_id FOREIGN KEY (team_id) REFERENCES team (id)
);

CREATE INDEX ix_candidate_idea_team_id_created_at
    ON candidate_idea (team_id, created_at, id);

CREATE TABLE candidate_idea_platform (
    candidate_idea_id BIGINT NOT NULL,
    platform VARCHAR(30) NOT NULL,
    sort_order INT NOT NULL,
    PRIMARY KEY (candidate_idea_id, platform),
    CONSTRAINT fk_candidate_idea_platform_idea_id
        FOREIGN KEY (candidate_idea_id) REFERENCES candidate_idea (id)
);

CREATE TABLE duplicate_analysis (
    id BIGINT NOT NULL AUTO_INCREMENT,
    candidate_idea_id BIGINT NOT NULL,
    requested_by_team_id BIGINT NOT NULL,
    requested_by_user_id BIGINT NOT NULL,
    analysis_status VARCHAR(20) NOT NULL,
    scanned_released_service_count INT NOT NULL,
    scanned_candidate_idea_count INT NOT NULL,
    failure_reason VARCHAR(500) NULL,
    generated_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_duplicate_analysis_candidate_idea_id FOREIGN KEY (candidate_idea_id) REFERENCES candidate_idea (id),
    CONSTRAINT fk_duplicate_analysis_requested_by_team_id FOREIGN KEY (requested_by_team_id) REFERENCES team (id),
    CONSTRAINT ck_duplicate_analysis_non_negative_counts CHECK (
        scanned_released_service_count >= 0 AND scanned_candidate_idea_count >= 0
    )
);

CREATE TABLE duplicate_analysis_match (
    id BIGINT NOT NULL AUTO_INCREMENT,
    analysis_id BIGINT NOT NULL,
    match_order INT NOT NULL,
    source_type VARCHAR(40) NOT NULL,
    source_id BIGINT NULL,
    source_team_id BIGINT NULL,
    source_title VARCHAR(200) NULL,
    source_disclosure VARCHAR(20) NOT NULL,
    similarity_level VARCHAR(20) NOT NULL,
    overlap_summary VARCHAR(1000) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_duplicate_analysis_match_analysis_id FOREIGN KEY (analysis_id) REFERENCES duplicate_analysis (id),
    CONSTRAINT ux_duplicate_analysis_match_analysis_id_match_order UNIQUE (analysis_id, match_order),
    CONSTRAINT ck_duplicate_analysis_match_redacted_source CHECK (
        source_disclosure <> 'REDACTED' OR
            (source_id IS NULL AND source_team_id IS NULL AND source_title IS NULL)
    )
);

CREATE TABLE duplicate_analysis_match_dimension (
    match_id BIGINT NOT NULL,
    dimension VARCHAR(30) NOT NULL,
    sort_order INT NOT NULL,
    PRIMARY KEY (match_id, dimension),
    CONSTRAINT fk_duplicate_analysis_match_dimension_match_id
        FOREIGN KEY (match_id) REFERENCES duplicate_analysis_match (id)
);
