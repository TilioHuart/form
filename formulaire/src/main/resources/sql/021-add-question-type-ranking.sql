INSERT INTO formulaire.question_type(code, name)
VALUES (12, 'RANKING');

ALTER TABLE formulaire.response
    ADD COLUMN choice_position bigint;