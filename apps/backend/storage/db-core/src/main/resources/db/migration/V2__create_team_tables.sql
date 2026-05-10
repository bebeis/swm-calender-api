CREATE TABLE team (
    id BIGINT NOT NULL AUTO_INCREMENT,
    team_name VARCHAR(50) NOT NULL,
    description VARCHAR(500) NULL,
    invite_code VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT ux_team_invite_code UNIQUE (invite_code)
);

CREATE TABLE team_member (
    id BIGINT NOT NULL AUTO_INCREMENT,
    team_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    member_name VARCHAR(100) NOT NULL,
    member_email VARCHAR(255) NOT NULL,
    member_role VARCHAR(20) NOT NULL,
    joined_at TIMESTAMP NOT NULL,
    removed_at TIMESTAMP NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_team_member_team_id FOREIGN KEY (team_id) REFERENCES team (id),
    CONSTRAINT ux_team_member_team_id_user_id UNIQUE (team_id, user_id)
);

CREATE INDEX ix_team_member_team_id_removed_at ON team_member (team_id, removed_at);
CREATE INDEX ix_team_member_user_id_removed_at ON team_member (user_id, removed_at);

CREATE TABLE sub_service_activation (
    team_id BIGINT NOT NULL,
    calendar_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    match_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    calendar_enabled_at TIMESTAMP NULL,
    match_enabled_at TIMESTAMP NULL,
    calendar_disabled_at TIMESTAMP NULL,
    match_disabled_at TIMESTAMP NULL,
    PRIMARY KEY (team_id),
    CONSTRAINT fk_sub_service_activation_team_id FOREIGN KEY (team_id) REFERENCES team (id)
);
