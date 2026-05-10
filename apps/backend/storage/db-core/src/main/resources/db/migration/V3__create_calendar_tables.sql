CREATE TABLE team_calendar (
    id BIGINT NOT NULL AUTO_INCREMENT,
    team_id BIGINT NOT NULL,
    google_calendar_id VARCHAR(255) NULL,
    activated_by_user_id BIGINT NOT NULL,
    calendar_status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_team_calendar_team_id FOREIGN KEY (team_id) REFERENCES team (id),
    CONSTRAINT ux_team_calendar_team_id UNIQUE (team_id)
);

CREATE TABLE mentoring_schedule (
    id BIGINT NOT NULL AUTO_INCREMENT,
    team_id BIGINT NOT NULL,
    external_source_id VARCHAR(255) NOT NULL,
    schedule_title VARCHAR(200) NOT NULL,
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP NOT NULL,
    location VARCHAR(255) NULL,
    description TEXT NULL,
    google_event_id VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_mentoring_schedule_team_id FOREIGN KEY (team_id) REFERENCES team (id),
    CONSTRAINT ux_mentoring_schedule_team_id_external_source_id UNIQUE (team_id, external_source_id)
);

CREATE INDEX ix_mentoring_schedule_team_id_starts_at_ends_at
    ON mentoring_schedule (team_id, starts_at, ends_at);

CREATE TABLE when2meet_link (
    id BIGINT NOT NULL AUTO_INCREMENT,
    team_id BIGINT NOT NULL,
    url VARCHAR(2048) NOT NULL,
    link_status VARCHAR(20) NOT NULL,
    failure_reason VARCHAR(500) NULL,
    last_parsed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_when2meet_link_team_id FOREIGN KEY (team_id) REFERENCES team (id),
    CONSTRAINT ux_when2meet_link_team_id UNIQUE (team_id)
);

CREATE TABLE availability_slot (
    id BIGINT NOT NULL AUTO_INCREMENT,
    team_id BIGINT NOT NULL,
    team_calendar_id BIGINT NULL,
    when2meet_link_id BIGINT NULL,
    availability_source VARCHAR(30) NOT NULL,
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP NOT NULL,
    available_member_count INT NOT NULL,
    busy_member_count INT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_availability_slot_team_id FOREIGN KEY (team_id) REFERENCES team (id),
    CONSTRAINT fk_availability_slot_team_calendar_id FOREIGN KEY (team_calendar_id) REFERENCES team_calendar (id),
    CONSTRAINT fk_availability_slot_when2meet_link_id FOREIGN KEY (when2meet_link_id) REFERENCES when2meet_link (id),
    CONSTRAINT ck_availability_slot_source_reference CHECK (
        (availability_source = 'GOOGLE_CALENDAR' AND team_calendar_id IS NOT NULL AND when2meet_link_id IS NULL) OR
        (availability_source = 'WHEN2MEET' AND when2meet_link_id IS NOT NULL AND team_calendar_id IS NULL)
    ),
    CONSTRAINT ck_availability_slot_non_negative_counts CHECK (
        available_member_count >= 0 AND busy_member_count >= 0
    ),
    CONSTRAINT ck_availability_slot_valid_range CHECK (ends_at > starts_at)
);

CREATE INDEX ix_availability_slot_team_id_starts_at_ends_at
    ON availability_slot (team_id, starts_at, ends_at);
