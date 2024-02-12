INSERT INTO formulaire.form_shares (
    SELECT member_id, resource_id, 'fr-openent-formulaire-controllers-ResponseController|listMineByDistributionAndQuestions'
    FROM formulaire.form_shares
    WHERE action = 'fr-openent-formulaire-controllers-FormController|initResponderResourceRight'
) ON CONFLICT DO NOTHING;

INSERT INTO formulaire.form_shares (
    SELECT member_id, resource_id, 'fr-openent-formulaire-controllers-ResponseController|deleteMultipleByQuestionAndDistribution'
    FROM formulaire.form_shares
    WHERE action = 'fr-openent-formulaire-controllers-FormController|initResponderResourceRight'
) ON CONFLICT DO NOTHING;