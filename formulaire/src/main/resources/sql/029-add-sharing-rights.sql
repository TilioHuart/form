INSERT INTO formulaire.form_shares (
    SELECT member_id, resource_id, 'fr-openent-formulaire-controllers-QuestionChoiceController|createMultiple'
    FROM formulaire.form_shares
    WHERE action = 'fr-openent-formulaire-controllers-FormController|initContribResourceRight'
) ON CONFLICT DO NOTHING;