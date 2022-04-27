ALTER TABLE formulaire.form
    ADD COLUMN is_public    boolean NOT NULL DEFAULT false,
    ADD COLUMN public_key   VARCHAR(36);

ALTER TABLE formulaire.distribution
    ADD COLUMN public_key   VARCHAR(36);

DELETE FROM formulaire.form_shares WHERE action = 'fr-openent-formulaire-controllers-QuestionChoiceController|createMultiple';
DELETE FROM formulaire.form_shares WHERE action = 'fr-openent-formulaire-controllers-ResponseController|fillResponses';