ALTER TABLE formulaire.form
    ADD COLUMN multiple boolean NOT NULL DEFAULT FALSE,
    ADD COLUMN anonymous boolean NOT NULL DEFAULT FALSE;