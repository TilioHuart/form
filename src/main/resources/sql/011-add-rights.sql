INSERT INTO formulaire.form_shares (member_id, resource_id, action)
SELECT member_id, resource_id, 'fr-openent-formulaire-controllers-QuestionChoiceController|create'
FROM formulaire.form_shares
WHERE action = 'fr-openent-formulaire-controllers-FormController|initContribResourceRight';
INSERT INTO formulaire.form_shares (member_id, resource_id, action)
SELECT member_id, resource_id, 'fr-openent-formulaire-controllers-QuestionChoiceController|createMultiple'
FROM formulaire.form_shares
WHERE action = 'fr-openent-formulaire-controllers-FormController|initContribResourceRight';
INSERT INTO formulaire.form_shares (member_id, resource_id, action)
SELECT member_id, resource_id, 'fr-openent-formulaire-controllers-QuestionChoiceController|update'
FROM formulaire.form_shares
WHERE action = 'fr-openent-formulaire-controllers-FormController|initContribResourceRight';
INSERT INTO formulaire.form_shares (member_id, resource_id, action)
SELECT member_id, resource_id, 'fr-openent-formulaire-controllers-QuestionChoiceController|delete'
FROM formulaire.form_shares
WHERE action = 'fr-openent-formulaire-controllers-FormController|initContribResourceRight';
INSERT INTO formulaire.form_shares (member_id, resource_id, action)
SELECT member_id, resource_id, 'fr-openent-formulaire-controllers-QuestionController|get'
FROM formulaire.form_shares
WHERE action = 'fr-openent-formulaire-controllers-FormController|initContribResourceRight';
INSERT INTO formulaire.form_shares (member_id, resource_id, action)
SELECT member_id, resource_id, 'fr-openent-formulaire-controllers-QuestionController|create'
FROM formulaire.form_shares
WHERE action = 'fr-openent-formulaire-controllers-FormController|initContribResourceRight';
INSERT INTO formulaire.form_shares (member_id, resource_id, action)
SELECT member_id, resource_id, 'fr-openent-formulaire-controllers-QuestionController|update'
FROM formulaire.form_shares
WHERE action = 'fr-openent-formulaire-controllers-FormController|initContribResourceRight';
INSERT INTO formulaire.form_shares (member_id, resource_id, action)
SELECT member_id, resource_id, 'fr-openent-formulaire-controllers-QuestionController|delete'
FROM formulaire.form_shares
WHERE action = 'fr-openent-formulaire-controllers-FormController|initContribResourceRight';
INSERT INTO formulaire.form_shares (member_id, resource_id, action)
SELECT member_id, resource_id, 'fr-openent-formulaire-controllers-ResponseController|list'
FROM formulaire.form_shares
WHERE action = 'fr-openent-formulaire-controllers-FormController|initContribResourceRight';
INSERT INTO formulaire.form_shares (member_id, resource_id, action)
SELECT member_id, resource_id, 'fr-openent-formulaire-controllers-ResponseFileController|get'
FROM formulaire.form_shares
WHERE action = 'fr-openent-formulaire-controllers-FormController|initContribResourceRight';

INSERT INTO formulaire.form_shares (member_id, resource_id, action)
SELECT member_id, resource_id, 'fr-openent-formulaire-controllers-DistributionController|get'
FROM formulaire.form_shares
WHERE action = 'fr-openent-formulaire-controllers-FormController|initResponderResourceRight';
INSERT INTO formulaire.form_shares (member_id, resource_id, action)
SELECT member_id, resource_id, 'fr-openent-formulaire-controllers-DistributionController|add'
FROM formulaire.form_shares
WHERE action = 'fr-openent-formulaire-controllers-FormController|initResponderResourceRight';
INSERT INTO formulaire.form_shares (member_id, resource_id, action)
SELECT member_id, resource_id, 'fr-openent-formulaire-controllers-DistributionController|update'
FROM formulaire.form_shares
WHERE action = 'fr-openent-formulaire-controllers-FormController|initResponderResourceRight';