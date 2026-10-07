CREATE TABLE PERSISTENT_TOKENS
(
    ID        BIGINT      NOT NULL AUTO_INCREMENT,
    DEVICE_ID VARCHAR(70) NOT NULL,
    TOKEN_ID  VARCHAR(32) NOT NULL,
    PRIMARY KEY (ID),

    CONSTRAINT uq_persistent_tokens_deviceid
        UNIQUE (DEVICE_ID),

    CONSTRAINT uq_persistent_tokens_deviceid_tokenid
        UNIQUE (DEVICE_ID, TOKEN_ID),

    CONSTRAINT fk_persistent_tokens_tokenid
        FOREIGN KEY (TOKEN_ID) REFERENCES TOKENS (TOKEN_ID)
);
