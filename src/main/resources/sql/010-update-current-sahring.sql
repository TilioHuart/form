INSERT INTO formulaire.form_shares
(
    SELECT member_id, resource_id, 'fr-openent-formulaire-controllers-ResponseController|listByDistribution'
    FROM formulaire.form_shares
    WHERE action = 'fr-openent-formulaire-controllers-FormController|initResponderResourceRight'
),
(
    SELECT member_id, resource_id, 'fr-openent-formulaire-controllers-DistributionController|getByFormResponderAndStatus'
    FROM formulaire.form_shares
    WHERE action = 'fr-openent-formulaire-controllers-FormController|initResponderResourceRight'
)