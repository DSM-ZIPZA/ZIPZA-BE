CREATE TABLE guarantee_analysis
(
    id                       BINARY(16)  NOT NULL,
    request_id               BINARY(16)  NOT NULL,
    deposit_amount           BIGINT      NOT NULL,
    estimated_property_value BIGINT      NOT NULL,
    jeonse_rate              DOUBLE PRECISION NOT NULL,
    region_max_deposit       BIGINT      NOT NULL,
    guarantee_result         VARCHAR(30) NOT NULL,
    risk_score               INT         NOT NULL,
    risk_reason              TEXT        NOT NULL,
    analyzed_at              DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_guarantee_analysis_request FOREIGN KEY (request_id) REFERENCES analysis_request (id)
);
