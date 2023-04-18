ALTER TABLE formulaire.question_choice
    RENAME COLUMN next_section_id TO next_form_element_id;

ALTER TABLE formulaire.question_choice
    ADD COLUMN next_form_element_type varchar,
    DROP CONSTRAINT fk_next_section_id;

UPDATE formulaire.question_choice SET next_form_element_type = 'SECTION'
WHERE next_form_element_id IS NOT NULL;

ALTER TABLE formulaire.question_choice
    ADD CONSTRAINT check_next_form_element_logic CHECK (
        (next_form_element_id IS NULL AND next_form_element_type IS NULL) OR
        (next_form_element_id IS NOT NULL AND next_form_element_type IS NOT NULL));

ALTER TABLE formulaire.section
    ADD COLUMN next_form_element_id     bigint,
    ADD COLUMN next_form_element_type   varchar,
    ADD CONSTRAINT check_next_form_element_logic CHECK ((next_form_element_id IS NULL AND next_form_element_type IS NULL) OR (next_form_element_id IS NOT NULL AND next_form_element_type IS NOT NULL));

WITH form_elements_infos AS (
    SELECT id, form_id, position, 'QUESTION' AS type FROM formulaire.question
    UNION
    SELECT id, form_id, position, 'SECTION' AS type FROM formulaire.section
)
UPDATE formulaire.section s
SET next_form_element_id = fei.id, next_form_element_type = fei.type
FROM form_elements_infos fei
WHERE fei.form_id = s.form_id AND fei.position = (s.position + 1);