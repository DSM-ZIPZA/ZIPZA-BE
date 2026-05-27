CREATE TABLE reminder
(
    id            BINARY(16)   NOT NULL,
    request_id    BINARY(16)   NOT NULL,
    user_id       BINARY(16)   NOT NULL,
    reminder_type VARCHAR(20)  NOT NULL,
    remind_date   DATE         NOT NULL,
    channel       VARCHAR(10)  NOT NULL,
    is_sent       TINYINT(1)   NOT NULL DEFAULT 0,
    sent_at       DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_reminder_request FOREIGN KEY (request_id) REFERENCES analysis_request (id),
    CONSTRAINT fk_reminder_user    FOREIGN KEY (user_id)    REFERENCES user (id)
);
