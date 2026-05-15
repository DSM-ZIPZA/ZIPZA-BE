CREATE TABLE fraud_pattern_analysis
(
    id                            BINARY(16)  NOT NULL,
    request_id                    BINARY(16)  NOT NULL,
    has_frequent_mortgage_change  BIT(1)      NOT NULL,
    has_post_ownership_mortgage   BIT(1)      NOT NULL,
    has_over_leveraged            BIT(1)      NOT NULL,
    suspicion_level               VARCHAR(10) NOT NULL,
    risk_score                    INT         NOT NULL,
    risk_reason                   TEXT        NOT NULL,
    analyzed_at                   DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_fraud_pattern_analysis_request FOREIGN KEY (request_id) REFERENCES analysis_request (id)
);
