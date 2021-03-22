ALTER TABLE formulaire.form
    DROP COLUMN date_ending,
    ADD date_ending     timestamp without time zone;

CREATE TABLE formulaire.response_file (
    id              VARCHAR(36) PRIMARY KEY,
    response_id     bigint UNIQUE NOT NULL,
    filename        VARCHAR NOT NULL,
    type            VARCHAR NOT NULL,
    CONSTRAINT fk_response_id FOREIGN KEY (response_id) REFERENCES formulaire.response (id) ON UPDATE NO ACTION ON DELETE CASCADE
);

ALTER TABLE formulaire.question
    ADD COLUMN duplicate_question_id bigint;

ALTER TABLE formulaire.form
    ADD COLUMN multiple boolean NOT NULL DEFAULT FALSE;