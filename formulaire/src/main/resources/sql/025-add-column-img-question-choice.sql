ALTER TABLE formulaire.question_choice
    ADD COLUMN IF NOT EXISTS image VARCHAR;

ALTER TABLE formulaire.response
    ADD COLUMN IF NOT EXISTS image VARCHAR;