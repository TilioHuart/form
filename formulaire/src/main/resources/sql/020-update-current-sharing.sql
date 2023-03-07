INSERT INTO formulaire.form_shares (
    SELECT DISTINCT member_id, resource_id, 'fr-openent-formulaire-controllers-ResponseController|deleteByQuestionAndDistribution'
    FROM formulaire.form_shares
    WHERE action = 'fr-openent-formulaire-controllers-FormController|initResponderResourceRight'
);

INSERT INTO formulaire.form_shares (
    SELECT DISTINCT member_id, resource_id, 'fr-openent-formulaire-controllers-FormController|get'
    FROM formulaire.form_shares
    WHERE action IN ('fr-openent-formulaire-controllers-FormController|initResponderResourceRight',
        'fr-openent-formulaire-controllers-FormController|initContribResourceRight',
        'fr-openent-formulaire-controllers-FormController|initManagerResourceRight')
);