INSERT INTO formulaire.question_type(code, name)
VALUES (11, 'CURSOR');

CREATE TABLE formulaire.question_specific_fields (
       id                       bigserial PRIMARY KEY,
       question_id              bigint NOT NULL,
       cursor_min_val           bigint NOT NULL,
       cursor_max_val           bigint NOT NULL,
       cursor_step              bigint NOT NULL,
       cursor_min_label         VARCHAR,
       cursor_max_label         VARCHAR,
       CONSTRAINT fk_question_id FOREIGN KEY (question_id) REFERENCES formulaire.question (id) ON UPDATE NO ACTION ON DELETE CASCADE
);