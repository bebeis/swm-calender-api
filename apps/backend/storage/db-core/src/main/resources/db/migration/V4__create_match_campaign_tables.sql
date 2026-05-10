CREATE TABLE service_profile (
    id BIGINT NOT NULL AUTO_INCREMENT,
    team_id BIGINT NOT NULL,
    profile_version INT NOT NULL,
    is_public BOOLEAN NOT NULL,
    service_name VARCHAR(80) NOT NULL,
    summary VARCHAR(120) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(30) NOT NULL,
    demo_url VARCHAR(2048) NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_service_profile_team_id FOREIGN KEY (team_id) REFERENCES team (id),
    CONSTRAINT ux_service_profile_team_id_profile_version UNIQUE (team_id, profile_version)
);

CREATE INDEX ix_service_profile_public_category
    ON service_profile (is_public, category);

CREATE TABLE service_profile_platform (
    service_profile_id BIGINT NOT NULL,
    platform VARCHAR(30) NOT NULL,
    sort_order INT NOT NULL,
    PRIMARY KEY (service_profile_id, platform),
    CONSTRAINT fk_service_profile_platform_profile_id
        FOREIGN KEY (service_profile_id) REFERENCES service_profile (id)
);

CREATE INDEX ix_service_profile_platform_platform
    ON service_profile_platform (platform);

CREATE TABLE service_profile_screenshot (
    id BIGINT NOT NULL AUTO_INCREMENT,
    service_profile_id BIGINT NOT NULL,
    screenshot_url VARCHAR(2048) NOT NULL,
    sort_order INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_service_profile_screenshot_profile_id
        FOREIGN KEY (service_profile_id) REFERENCES service_profile (id)
);

CREATE TABLE active_service_profile (
    team_id BIGINT NOT NULL,
    service_profile_id BIGINT NOT NULL,
    PRIMARY KEY (team_id),
    CONSTRAINT fk_active_service_profile_team_id FOREIGN KEY (team_id) REFERENCES team (id),
    CONSTRAINT fk_active_service_profile_profile_id FOREIGN KEY (service_profile_id) REFERENCES service_profile (id),
    CONSTRAINT ux_active_service_profile_service_profile_id UNIQUE (service_profile_id)
);

CREATE TABLE beta_campaign (
    id BIGINT NOT NULL AUTO_INCREMENT,
    team_id BIGINT NOT NULL,
    service_profile_id BIGINT NOT NULL,
    campaign_title VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    target_team_count INT NOT NULL,
    deadline TIMESTAMP NOT NULL,
    reciprocal_available BOOLEAN NOT NULL,
    requirements TEXT NULL,
    campaign_status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_beta_campaign_team_id FOREIGN KEY (team_id) REFERENCES team (id),
    CONSTRAINT fk_beta_campaign_service_profile_id FOREIGN KEY (service_profile_id) REFERENCES service_profile (id),
    CONSTRAINT ck_beta_campaign_target_team_count_positive CHECK (target_team_count > 0)
);

CREATE INDEX ix_beta_campaign_status_created_at
    ON beta_campaign (campaign_status, created_at);

CREATE INDEX ix_beta_campaign_status_deadline
    ON beta_campaign (campaign_status, deadline);

CREATE INDEX ix_beta_campaign_reciprocal_status
    ON beta_campaign (reciprocal_available, campaign_status);
