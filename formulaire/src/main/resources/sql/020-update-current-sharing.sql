DELETE FROM formulaire.form_shares
WHERE action = 'fr-openent-formulaire-controllers-ResponseController|deleteByQuestionAndDistribution';

INSERT INTO formulaire.form_shares (
    SELECT DISTINCT member_id, resource_id, 'fr-openent-formulaire-controllers-ResponseController|deleteByQuestionAndDistribution'
    FROM formulaire.form_shares
    WHERE action = 'fr-openent-formulaire-controllers-FormController|initResponderResourceRight'
);

WITH form_shares_to_update AS (
    SELECT DISTINCT member_id, resource_id
    FROM formulaire.form_shares
    WHERE action IN (
        'fr-openent-formulaire-controllers-FormController|initResponderResourceRight',
        'fr-openent-formulaire-controllers-FormController|initContribResourceRight',
        'fr-openent-formulaire-controllers-FormController|initManagerResourceRight'
    )
),
form_shares_already_updated AS (
    SELECT DISTINCT member_id, resource_id
    FROM formulaire.form_shares
    WHERE action = 'fr-openent-formulaire-controllers-FormController|get'
)
INSERT INTO formulaire.form_shares (
    SELECT DISTINCT member_id, resource_id, 'fr-openent-formulaire-controllers-FormController|get'
    FROM formulaire.form_shares
    WHERE (member_id, resource_id) IN (SELECT * FROM form_shares_to_update)
    AND (member_id, resource_id) NOT IN (SELECT * FROM form_shares_already_updated)
);