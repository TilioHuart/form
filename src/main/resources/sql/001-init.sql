CREATE SCHEMA formulaire;

CREATE TABLE formulaire.scripts (
    filename    VARCHAR NOT NULL PRIMARY KEY,
    passed      timestamp without time zone NOT NULL DEFAULT now()
);

CREATE TABLE formulaire.form (
    id          bigserial PRIMARY KEY,
    owner_id    VARCHAR NOT NULL,
    owner_name  VARCHAR NOT NULL,
    title       VARCHAR NOT NULL,
    description VARCHAR,
    picture     VARCHAR,
    created     timestamp without time zone NOT NULL DEFAULT now(),
    modified    timestamp without time zone NOT NULL DEFAULT now(),
    sent        boolean NOT NULL DEFAULT FALSE,
    shared      boolean NOT NULL DEFAULT FALSE
);

CREATE TABLE formulaire.question (
    id              bigserial PRIMARY KEY,
    form_id         bigint NOT NULL,
    position        bigint NOT NULL,
    question_type   VARCHAR NOT NULL,
    statement       VARCHAR NOT NULL,
    mandatory       boolean NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_form_id FOREIGN KEY (form_id) REFERENCES formulaire.form (id) ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE TABLE formulaire.response (
    id              bigserial PRIMARY KEY,
    user_id         VARCHAR NOT NULL,
    user_name       VARCHAR NOT NULL,
    question_id     bigint NOT NULL,
    answer          VARCHAR NOT NULL,
    created         timestamp without time zone NOT NULL DEFAULT now(),
    modified        timestamp without time zone NOT NULL DEFAULT now(),
    CONSTRAINT fk_question_id FOREIGN KEY (question_id) REFERENCES formulaire.question (id) ON UPDATE NO ACTION ON DELETE CASCADE
);

