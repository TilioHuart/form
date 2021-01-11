CREATE SCHEMA formulaire;

CREATE TABLE formulaire.scripts (
    filename    VARCHAR NOT NULL PRIMARY KEY,
    passed      timestamp without time zone NOT NULL DEFAULT now()
);

CREATE TABLE formulaire.form (
    id          bigserial PRIMARY KEY,
    title       VARCHAR NOT NULL,
    description VARCHAR,
    picture     VARCHAR,
    owner_id    VARCHAR NOT NULL,
    owner_name  VARCHAR NOT NULL,
    created     timestamp without time zone NOT NULL DEFAULT now(),
    modified    timestamp without time zone NOT NULL DEFAULT now(),
    sent        boolean NOT NULL DEFAULT FALSE,
    shared      boolean NOT NULL DEFAULT FALSE,
    archived    boolean NOT NULL DEFAULT FALSE
);

CREATE TABLE formulaire.question_type (
    id      bigserial PRIMARY KEY,
    code    bigint UNIQUE NOT NULL,
    name    VARCHAR NOT NULL
);

CREATE TABLE formulaire.question (
    id              bigserial PRIMARY KEY,
    form_id         bigint NOT NULL,
    position        bigint NOT NULL,
    question_type   bigint NOT NULL,
    statement       VARCHAR NOT NULL,
    mandatory       boolean NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_form_id FOREIGN KEY (form_id) REFERENCES formulaire.form (id) ON UPDATE NO ACTION ON DELETE CASCADE,
    CONSTRAINT fk_question_type FOREIGN KEY (question_type) REFERENCES formulaire.question_type (code) ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE TABLE formulaire.question_choice (
    id              bigserial PRIMARY KEY,
    question_id     bigint NOT NULL,
    value           VARCHAR NOT NULL,
    type            VARCHAR NOT NULL,
    CONSTRAINT fk_question_id FOREIGN KEY (question_id) REFERENCES formulaire.question (id) ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE TABLE formulaire.response (
    id              bigserial PRIMARY KEY,
    question_id     bigint NOT NULL,
    answer          VARCHAR NOT NULL,
    user_id         VARCHAR NOT NULL,
    created         timestamp without time zone NOT NULL DEFAULT now(),
    CONSTRAINT fk_question_id FOREIGN KEY (question_id) REFERENCES formulaire.question (id) ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE TABLE formulaire.distribution (
    id          bigserial PRIMARY KEY,
    form_id     bigint NOT NULL,
    user_id     VARCHAR NOT NULL,
    user_name   VARCHAR NOT NULL,
    status      VARCHAR NOT NULL,
    created     timestamp without time zone NOT NULL DEFAULT now(),
    CONSTRAINT fk_form_id FOREIGN KEY (form_id) REFERENCES formulaire.form (id) ON UPDATE NO ACTION ON DELETE CASCADE
);