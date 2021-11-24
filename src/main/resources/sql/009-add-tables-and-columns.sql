ALTER TABLE formulaire.form
    ADD COLUMN response_notified boolean NOT NULL DEFAULT FALSE,
    ADD COLUMN editable boolean NOT NULL DEFAULT FALSE,
    ADD COLUMN rgpd boolean NOT NULL DEFAULT FALSE,
    ADD COLUMN rgpd_goal VARCHAR,
    ADD COLUMN rgpd_lifetime bigint NOT NULL DEFAULT 12;

ALTER TABLE formulaire.distribution
    ADD COLUMN original_id bigint;

ALTER TABLE formulaire.response
    ADD COLUMN original_id bigint;

CREATE TABLE formulaire.delegate (
    id      bigserial PRIMARY KEY,
    entity  VARCHAR NOT NULL,
    mail    VARCHAR NOT NULL,
    address VARCHAR NOT NULL,
    zipcode bigint NOT NULL,
    city    VARCHAR NOT NULL
)