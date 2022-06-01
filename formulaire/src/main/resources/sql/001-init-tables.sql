CREATE SCHEMA formulaire;

CREATE TABLE formulaire.scripts (
    filename    VARCHAR NOT NULL PRIMARY KEY,
    passed      timestamp without time zone NOT NULL DEFAULT now()
);

CREATE TABLE formulaire.form (
    id                  bigserial PRIMARY KEY,
    title               VARCHAR NOT NULL,
    description         VARCHAR,
    picture             VARCHAR,
    owner_id            VARCHAR(36) NOT NULL,
    owner_name          VARCHAR NOT NULL,
    date_creation       timestamp with time zone NOT NULL DEFAULT now(),
    date_modification   timestamp with time zone NOT NULL DEFAULT now(),
    sent                boolean NOT NULL DEFAULT FALSE,
    collab              boolean NOT NULL DEFAULT FALSE,
    archived            boolean NOT NULL DEFAULT FALSE,
    date_opening        timestamp with time zone NOT NULL DEFAULT now(),
    date_ending         timestamp with time zone,
    multiple            boolean NOT NULL DEFAULT FALSE,
    anonymous           boolean NOT NULL DEFAULT FALSE,
    reminded            boolean NOT NULL DEFAULT FALSE
);

CREATE TABLE formulaire.question_type (
    id      bigserial PRIMARY KEY,
    code    bigint UNIQUE NOT NULL,
    name    VARCHAR NOT NULL
);

CREATE TABLE formulaire.question (
    id                      bigserial PRIMARY KEY,
    form_id                 bigint NOT NULL,
    title                   VARCHAR NOT NULL,
    position                bigint NOT NULL,
    question_type           bigint NOT NULL,
    statement               VARCHAR,
    mandatory               boolean NOT NULL DEFAULT FALSE,
    original_question_id   bigint,
    CONSTRAINT fk_form_id FOREIGN KEY (form_id) REFERENCES formulaire.form (id) ON UPDATE NO ACTION ON DELETE CASCADE,
    CONSTRAINT fk_question_type FOREIGN KEY (question_type) REFERENCES formulaire.question_type (code) ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE TABLE formulaire.question_choice (
    id              bigserial PRIMARY KEY,
    question_id     bigint NOT NULL,
    value           VARCHAR NOT NULL,
    type            VARCHAR NOT NULL,
    position        bigint NOT NULL DEFAULT 0,
    CONSTRAINT fk_question_id FOREIGN KEY (question_id) REFERENCES formulaire.question (id) ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE TABLE formulaire.distribution (
    id                  bigserial PRIMARY KEY,
    form_id             bigint NOT NULL,
    sender_id           VARCHAR(36) NOT NULL,
    sender_name         VARCHAR NOT NULL,
    responder_id        VARCHAR(36) NOT NULL,
    responder_name      VARCHAR NOT NULL,
    status              VARCHAR NOT NULL,
    date_sending        timestamp with time zone NOT NULL DEFAULT now(),
    date_response       timestamp with time zone,
    active              boolean NOT NULL DEFAULT TRUE,
    structure           varchar,
    CONSTRAINT fk_form_id FOREIGN KEY (form_id) REFERENCES formulaire.form (id) ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE TABLE formulaire.response (
    id                  bigserial PRIMARY KEY,
    question_id         bigint NOT NULL,
    answer              VARCHAR NOT NULL,
    responder_id        VARCHAR(36) NOT NULL,
    choice_id           bigint,
    distribution_id     bigint NOT NULL DEFAULT 0,
    CONSTRAINT fk_question_id FOREIGN KEY (question_id) REFERENCES formulaire.question (id) ON UPDATE NO ACTION ON DELETE CASCADE,
    CONSTRAINT fk_choice_id FOREIGN KEY (choice_id) REFERENCES formulaire.question_choice (id) ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE TABLE formulaire.response_file (
    id              VARCHAR(36) PRIMARY KEY,
    response_id     bigint UNIQUE NOT NULL,
    filename        VARCHAR NOT NULL,
    type            VARCHAR NOT NULL
);

INSERT INTO formulaire.question_type(code, name)
VALUES (1, 'FREETEXT'), (2, 'SHORTANSWER'), (3, 'LONGANSWER'), (4, 'SINGLEANSWER'), (5, 'MULTIPLEANSWER'), (6, 'DATE'), (7, 'TIME'), (8, 'FILE');
