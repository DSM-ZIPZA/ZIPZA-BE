CREATE TABLE manual_check_item
(
    id               BINARY(16)   NOT NULL,
    request_id       BINARY(16)   NOT NULL,
    check_type       VARCHAR(40)  NOT NULL,
    title            VARCHAR(200) NOT NULL,
    badge_text       VARCHAR(50)  NULL,
    severity         VARCHAR(20)  NOT NULL,
    guide_text       TEXT         NOT NULL,
    procedure_steps  TEXT         NOT NULL,
    official_url     VARCHAR(500) NULL,
    expert_consult   BOOLEAN      NOT NULL DEFAULT FALSE,
    is_completed     BOOLEAN      NOT NULL DEFAULT FALSE,
    checked_at       DATETIME(6)  NULL,
    created_at       DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_manual_check_item_request FOREIGN KEY (request_id) REFERENCES analysis_request (id)
);

CREATE INDEX idx_manual_check_item_request ON manual_check_item (request_id);
