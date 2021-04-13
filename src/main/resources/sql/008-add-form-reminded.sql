ALTER TABLE formulaire.form
    ADD COLUMN reminded boolean NOT NULL DEFAULT FALSE;

ALTER TABLE formulaire.distribution
    ADD COLUMN structure varchar;