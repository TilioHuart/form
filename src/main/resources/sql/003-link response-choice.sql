ALTER TABLE formulaire.response
    ADD choice_id    bigint,
    ADD CONSTRAINT fk_choice_id FOREIGN KEY (choice_id) REFERENCES formulaire.question_choice (id) ON UPDATE NO ACTION ON DELETE CASCADE;