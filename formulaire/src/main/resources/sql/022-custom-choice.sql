ALTER TABLE formulaire.question_choice
    ADD COLUMN is_custom    boolean NOT NULL DEFAULT FALSE;

ALTER TABLE formulaire.response
    ADD COLUMN custom_answer VARCHAR;