ALTER TABLE formulaire.question_choice
    RENAME COLUMN next_section_id TO next_form_element_id;

ALTER TABLE formulaire.question_choice
    ADD COLUMN next_form_element_type varchar,
    DROP CONSTRAINT fk_next_section_id,
    ADD CONSTRAINT check_next_form_element_logic CHECK (
        (next_form_element_id IS NULL AND next_form_element_type IS NULL) OR
        (next_form_element_id IS NOT NULL AND next_form_element_type IS NOT NULL));

UPDATE formulaire.question_choice SET next_form_element_type = 'SECTION'
WHERE next_form_element_id IS NOT NULL;