CREATE TABLE match_feedback (
    id BIGINT NOT NULL AUTO_INCREMENT,
    assignment_id BIGINT NOT NULL,
    submitted_by_team_id BIGINT NOT NULL,
    submitted_by_user_id BIGINT NOT NULL,
    usability_score INT NOT NULL,
    value_score INT NOT NULL,
    reliability_score INT NOT NULL,
    recommendation_score INT NOT NULL,
    summary VARCHAR(1000) NOT NULL,
    improvement_suggestion VARCHAR(1000) NULL,
    submitted_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_match_feedback_assignment_id FOREIGN KEY (assignment_id) REFERENCES match_assignment (id),
    CONSTRAINT fk_match_feedback_submitted_by_team_id FOREIGN KEY (submitted_by_team_id) REFERENCES team (id),
    CONSTRAINT ux_match_feedback_assignment_id UNIQUE (assignment_id),
    CONSTRAINT ck_match_feedback_usability_score CHECK (usability_score BETWEEN 1 AND 5),
    CONSTRAINT ck_match_feedback_value_score CHECK (value_score BETWEEN 1 AND 5),
    CONSTRAINT ck_match_feedback_reliability_score CHECK (reliability_score BETWEEN 1 AND 5),
    CONSTRAINT ck_match_feedback_recommendation_score CHECK (recommendation_score BETWEEN 1 AND 5),
    CONSTRAINT ck_match_feedback_summary_length CHECK (CHAR_LENGTH(summary) BETWEEN 10 AND 1000),
    CONSTRAINT ck_match_feedback_improvement_suggestion_length
        CHECK (improvement_suggestion IS NULL OR CHAR_LENGTH(improvement_suggestion) <= 1000)
);

CREATE INDEX ix_match_feedback_submitted_by_team_submitted_at
    ON match_feedback (submitted_by_team_id, submitted_at);
