CREATE TABLE match_request (
    id BIGINT NOT NULL AUTO_INCREMENT,
    campaign_id BIGINT NOT NULL,
    requesting_team_id BIGINT NOT NULL,
    target_team_id BIGINT NOT NULL,
    request_type VARCHAR(20) NOT NULL,
    request_status VARCHAR(20) NOT NULL,
    message VARCHAR(1000) NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_match_request_campaign_id FOREIGN KEY (campaign_id) REFERENCES beta_campaign (id),
    CONSTRAINT fk_match_request_requesting_team_id FOREIGN KEY (requesting_team_id) REFERENCES team (id),
    CONSTRAINT fk_match_request_target_team_id FOREIGN KEY (target_team_id) REFERENCES team (id),
    CONSTRAINT ck_match_request_different_teams CHECK (requesting_team_id <> target_team_id)
);

CREATE INDEX ix_match_request_campaign_requesting_status
    ON match_request (campaign_id, requesting_team_id, request_status);

CREATE INDEX ix_match_request_target_status_created_at
    ON match_request (target_team_id, request_status, created_at);

CREATE TABLE match_request_status_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    request_id BIGINT NOT NULL,
    from_status VARCHAR(20) NULL,
    to_status VARCHAR(20) NOT NULL,
    changed_by_user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_match_request_status_history_request_id FOREIGN KEY (request_id) REFERENCES match_request (id)
);

CREATE INDEX ix_match_request_status_history_request_created_at
    ON match_request_status_history (request_id, created_at);

CREATE TABLE match_assignment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    request_id BIGINT NOT NULL,
    tester_team_id BIGINT NOT NULL,
    target_team_id BIGINT NOT NULL,
    assignment_status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_match_assignment_request_id FOREIGN KEY (request_id) REFERENCES match_request (id),
    CONSTRAINT fk_match_assignment_tester_team_id FOREIGN KEY (tester_team_id) REFERENCES team (id),
    CONSTRAINT fk_match_assignment_target_team_id FOREIGN KEY (target_team_id) REFERENCES team (id),
    CONSTRAINT ux_match_assignment_request_id UNIQUE (request_id),
    CONSTRAINT ck_match_assignment_different_teams CHECK (tester_team_id <> target_team_id)
);

CREATE INDEX ix_assignment_tester_team_status_created_at
    ON match_assignment (tester_team_id, assignment_status, created_at);

CREATE INDEX ix_assignment_target_team_status_created_at
    ON match_assignment (target_team_id, assignment_status, created_at);

CREATE TABLE match_notification (
    id BIGINT NOT NULL AUTO_INCREMENT,
    team_id BIGINT NOT NULL,
    notification_type VARCHAR(40) NOT NULL,
    reference_type VARCHAR(40) NOT NULL,
    reference_id BIGINT NOT NULL,
    message VARCHAR(500) NOT NULL,
    read_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_match_notification_team_id FOREIGN KEY (team_id) REFERENCES team (id)
);

CREATE INDEX ix_match_notification_team_read_created_at
    ON match_notification (team_id, read_at, created_at);
