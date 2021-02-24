ALTER TABLE formulaire.form
    ADD date_opening    timestamp without time zone NOT NULL DEFAULT now(),
    ADD date_ending     timestamp without time zone NOT NULL DEFAULT now() + interval '1 year';

ALTER TABLE formulaire.question_choice
    ADD position    bigint NOT NULL DEFAULT 0;