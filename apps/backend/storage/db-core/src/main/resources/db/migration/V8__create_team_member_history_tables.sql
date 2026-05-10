CREATE TABLE team_member_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    team_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    actor_user_id BIGINT NOT NULL,
    history_action VARCHAR(30) NOT NULL,
    previous_role VARCHAR(20) NOT NULL,
    changed_role VARCHAR(20) NULL,
    occurred_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_team_member_history_team_id FOREIGN KEY (team_id) REFERENCES team (id),
    CONSTRAINT fk_team_member_history_member_id FOREIGN KEY (member_id) REFERENCES team_member (id),
    CONSTRAINT ck_team_member_history_role_changed
        CHECK (
            (history_action = 'ROLE_CHANGED' AND changed_role IS NOT NULL)
            OR (history_action = 'MEMBER_REMOVED' AND changed_role IS NULL)
        )
);

CREATE INDEX ix_team_member_history_team_id_occurred_at
    ON team_member_history (team_id, occurred_at);

CREATE INDEX ix_team_member_history_member_id_occurred_at
    ON team_member_history (member_id, occurred_at);
