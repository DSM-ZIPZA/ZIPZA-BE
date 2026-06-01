CREATE TABLE recovery_analysis
(
    id                       BINARY(16)       NOT NULL,
    request_id               BINARY(16)       NOT NULL,
    estimated_property_value BIGINT           NOT NULL,
    total_encumbrance        BIGINT           NOT NULL,
    deposit_amount           BIGINT           NOT NULL,
    available_for_tenant     BIGINT           NOT NULL,
    recovery_rate            DOUBLE PRECISION NOT NULL,
    recovery_grade           VARCHAR(10)      NOT NULL,
    risk_score               INT              NOT NULL,
    risk_reason              TEXT             NOT NULL,
    analyzed_at              DATETIME(6)      NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_recovery_analysis_request FOREIGN KEY (request_id) REFERENCES analysis_request (id)
);
