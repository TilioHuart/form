ALTER TABLE formulaire.question
    ADD CONSTRAINT question_unique_form_position UNIQUE (form_id, position),
    ADD CONSTRAINT question_unique_section_position UNIQUE (section_id, section_position),
    ADD CONSTRAINT question_unique_matrix_position UNIQUE (matrix_id, matrix_position),
    ADD CONSTRAINT question_check_pairing
    CHECK (
        (section_id IS NOT NULL AND section_position IS NOT NULL AND matrix_id IS NULL AND matrix_position IS NULL AND position IS NULL) OR
        (matrix_id IS NOT NULL AND matrix_position IS NOT NULL AND section_id IS NULL AND section_position IS NULL AND position IS NULL) OR
        (position IS NOT NULL AND section_id IS NULL AND section_position IS NULL AND matrix_id IS NULL AND matrix_position IS NULL) OR
        (position IS NULL AND section_id IS NULL AND section_position IS NULL AND matrix_id IS NULL AND matrix_position IS NULL)
    );

ALTER TABLE formulaire.section
    ADD CONSTRAINT section_unique_form_position UNIQUE (form_id, position);