ALTER TABLE formulaire.form
    ADD date_opening    timestamp without time zone NOT NULL DEFAULT now(),
    ADD date_ending     timestamp without time zone NOT NULL DEFAULT '9999-12-31 23:59:59';

ALTER TABLE formulaire.question_choice
    ADD position    bigint NOT NULL DEFAULT 0;