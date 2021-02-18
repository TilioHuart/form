ALTER TABLE formulaire.form
    ADD date_opening    timestamp without time zone NOT NULL DEFAULT now(),
    ADD date_ending     timestamp without time zone NOT NULL;

ALTER TABLE formulaire.question_choice
    ADD position    bigint NOT NULL;