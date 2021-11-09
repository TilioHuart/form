ALTER TABLE formulaire.form
    ADD COLUMN response_notified boolean NOT NULL DEFAULT FALSE,
    ADD COLUMN editable boolean NOT NULL DEFAULT FALSE;

ALTER TABLE formulaire.distribution
    ADD COLUMN original_id bigint;

ALTER TABLE formulaire.response
    ADD COLUMN original_id bigint;