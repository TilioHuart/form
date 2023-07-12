INSERT INTO formulaire.form_shares (
    SELECT member_id, resource_id, 'fr-openent-formulaire-controllers-FormController|initReadResourceRight'
    FROM formulaire.form_shares
    WHERE action = 'fr-openent-formulaire-controllers-FormController|initContribResourceRight'
       OR action = 'fr-openent-formulaire-controllers-FormController|initResponderResourceRight'
) ON CONFLICT DO NOTHING;

INSERT INTO formulaire.form_shares (
    SELECT member_id, resource_id, 'fr-openent-formulaire-controllers-DistributionController|listByFormAndResponder'
    FROM formulaire.form_shares
    WHERE action = 'fr-openent-formulaire-controllers-FormController|initReadResourceRight'
) ON CONFLICT DO NOTHING;

INSERT INTO formulaire.form_shares (
    SELECT member_id, resource_id, 'fr-openent-formulaire-controllers-FormController|get'
    FROM formulaire.form_shares
    WHERE action = 'fr-openent-formulaire-controllers-FormController|initReadResourceRight'
) ON CONFLICT DO NOTHING;

INSERT INTO formulaire.form_shares (
    SELECT member_id, resource_id, 'fr-openent-formulaire-controllers-FormController|getMyFormRights'
    FROM formulaire.form_shares
    WHERE action = 'fr-openent-formulaire-controllers-FormController|initReadResourceRight'
) ON CONFLICT DO NOTHING;

INSERT INTO formulaire.form_shares (
    SELECT member_id, resource_id, 'fr-openent-formulaire-controllers-FormElementController|countFormElements'
    FROM formulaire.form_shares
    WHERE action = 'fr-openent-formulaire-controllers-FormController|initReadResourceRight'
) ON CONFLICT DO NOTHING;

INSERT INTO formulaire.form_shares (
    SELECT member_id, resource_id, 'fr-openent-formulaire-controllers-FormElementController|getByPosition'
    FROM formulaire.form_shares
    WHERE action = 'fr-openent-formulaire-controllers-FormController|initReadResourceRight'
) ON CONFLICT DO NOTHING;

INSERT INTO formulaire.form_shares (
    SELECT member_id, resource_id, 'fr-openent-formulaire-controllers-SectionController|list'
    FROM formulaire.form_shares
    WHERE action = 'fr-openent-formulaire-controllers-FormController|initReadResourceRight'
) ON CONFLICT DO NOTHING;

INSERT INTO formulaire.form_shares (
    SELECT member_id, resource_id, 'fr-openent-formulaire-controllers-QuestionController|listForForm'
    FROM formulaire.form_shares
    WHERE action = 'fr-openent-formulaire-controllers-FormController|initReadResourceRight'
) ON CONFLICT DO NOTHING;

INSERT INTO formulaire.form_shares (
    SELECT member_id, resource_id, 'fr-openent-formulaire-controllers-QuestionController|listForSection'
    FROM formulaire.form_shares
    WHERE action = 'fr-openent-formulaire-controllers-FormController|initReadResourceRight'
) ON CONFLICT DO NOTHING;

INSERT INTO formulaire.form_shares (
    SELECT member_id, resource_id, 'fr-openent-formulaire-controllers-QuestionController|listForFormAndSection'
    FROM formulaire.form_shares
    WHERE action = 'fr-openent-formulaire-controllers-FormController|initReadResourceRight'
) ON CONFLICT DO NOTHING;

INSERT INTO formulaire.form_shares (
    SELECT member_id, resource_id, 'fr-openent-formulaire-controllers-QuestionChoiceController|list'
    FROM formulaire.form_shares
    WHERE action = 'fr-openent-formulaire-controllers-FormController|initReadResourceRight'
) ON CONFLICT DO NOTHING;

INSERT INTO formulaire.form_shares (
    SELECT member_id, resource_id, 'fr-openent-formulaire-controllers-ResponseFileController|list'
    FROM formulaire.form_shares
    WHERE action = 'fr-openent-formulaire-controllers-FormController|initReadResourceRight'
) ON CONFLICT DO NOTHING;

INSERT INTO formulaire.form_shares (
    SELECT member_id, resource_id, 'fr-openent-formulaire-controllers-ResponseFileController|listByQuestion'
    FROM formulaire.form_shares
    WHERE action = 'fr-openent-formulaire-controllers-FormController|initReadResourceRight'
) ON CONFLICT DO NOTHING;

UPDATE formulaire.form_shares SET action = 'fr-openent-formulaire-controllers-FormController|initManagerResourceRight' WHERE action = 'fr.openent.formulaire.controllers.FormController|initManagerResourceRight';
UPDATE formulaire.form_shares SET action = 'fr-openent-formulaire-controllers-FormController|delete' WHERE action = 'fr.openent.formulaire.controllers.FormController|delete';
UPDATE formulaire.form_shares SET action = 'fr-openent-formulaire-controllers-FormController|initResponderResourceRight' WHERE action = 'fr.openent.formulaire.controllers.FormController|initResponderResourceRight';
UPDATE formulaire.form_shares SET action = 'fr-openent-formulaire-controllers-FormController|update' WHERE action = 'fr.openent.formulaire.controllers.FormController|update';
UPDATE formulaire.form_shares SET action = 'fr-openent-formulaire-controllers-FormController|sendReminder' WHERE action = 'fr.openent.formulaire.controllers.FormController|sendReminder';
UPDATE formulaire.form_shares SET action = 'fr-openent-formulaire-controllers-FormController|initContribResourceRight' WHERE action = 'fr.openent.formulaire.controllers.FormController|initContribResourceRight';
UPDATE formulaire.form_shares SET action = 'fr-openent-formulaire-controllers-SharingController|shareSubmit' WHERE action = 'fr.openent.formulaire.controllers.SharingController|shareSubmit';
UPDATE formulaire.form_shares SET action = 'fr-openent-formulaire-controllers-SharingController|shareResource' WHERE action = 'fr.openent.formulaire.controllers.SharingController|shareResource';
UPDATE formulaire.form_shares SET action = 'fr-openent-formulaire-controllers-SharingController|shareJson' WHERE action = 'fr.openent.formulaire.controllers.SharingController|shareJson';
UPDATE formulaire.form_shares SET action = 'fr-openent-formulaire-controllers-FormElementController|update' WHERE action = 'fr.openent.formulaire.controllers.FormElementController|update';
UPDATE formulaire.form_shares SET action = 'fr-openent-formulaire-controllers-SectionController|update' WHERE action = 'fr.openent.formulaire.controllers.SectionController|update';
UPDATE formulaire.form_shares SET action = 'fr-openent-formulaire-controllers-SectionController|get' WHERE action = 'fr.openent.formulaire.controllers.SectionController|get';
UPDATE formulaire.form_shares SET action = 'fr-openent-formulaire-controllers-SectionController|delete' WHERE action = 'fr.openent.formulaire.controllers.SectionController|delete';
UPDATE formulaire.form_shares SET action = 'fr-openent-formulaire-controllers-SectionController|create' WHERE action = 'fr.openent.formulaire.controllers.SectionController|create';
UPDATE formulaire.form_shares SET action = 'fr-openent-formulaire-controllers-QuestionController|update' WHERE action = 'fr.openent.formulaire.controllers.QuestionController|update';
UPDATE formulaire.form_shares SET action = 'fr-openent-formulaire-controllers-QuestionController|get' WHERE action = 'fr.openent.formulaire.controllers.QuestionController|get';
UPDATE formulaire.form_shares SET action = 'fr-openent-formulaire-controllers-QuestionController|delete' WHERE action = 'fr.openent.formulaire.controllers.QuestionController|delete';
UPDATE formulaire.form_shares SET action = 'fr-openent-formulaire-controllers-QuestionController|create' WHERE action = 'fr.openent.formulaire.controllers.QuestionController|create';
UPDATE formulaire.form_shares SET action = 'fr-openent-formulaire-controllers-QuestionChoiceController|update' WHERE action = 'fr.openent.formulaire.controllers.QuestionChoiceController|update';
UPDATE formulaire.form_shares SET action = 'fr-openent-formulaire-controllers-QuestionChoiceController|delete' WHERE action = 'fr.openent.formulaire.controllers.QuestionChoiceController|delete';
UPDATE formulaire.form_shares SET action = 'fr-openent-formulaire-controllers-QuestionChoiceController|create' WHERE action = 'fr.openent.formulaire.controllers.QuestionChoiceController|create';
UPDATE formulaire.form_shares SET action = 'fr-openent-formulaire-controllers-DistributionController|delete' WHERE action = 'fr.openent.formulaire.controllers.DistributionController|delete';
UPDATE formulaire.form_shares SET action = 'fr-openent-formulaire-controllers-DistributionController|update' WHERE action = 'fr.openent.formulaire.controllers.DistributionController|update';
UPDATE formulaire.form_shares SET action = 'fr-openent-formulaire-controllers-DistributionController|replace' WHERE action = 'fr.openent.formulaire.controllers.DistributionController|replace';
UPDATE formulaire.form_shares SET action = 'fr-openent-formulaire-controllers-DistributionController|get' WHERE action = 'fr.openent.formulaire.controllers.DistributionController|get';
UPDATE formulaire.form_shares SET action = 'fr-openent-formulaire-controllers-DistributionController|duplicateWithResponses' WHERE action = 'fr.openent.formulaire.controllers.DistributionController|duplicateWithResponses';
UPDATE formulaire.form_shares SET action = 'fr-openent-formulaire-controllers-DistributionController|add' WHERE action = 'fr.openent.formulaire.controllers.DistributionController|add';
UPDATE formulaire.form_shares SET action = 'fr-openent-formulaire-controllers-DistributionController|count' WHERE action = 'fr.openent.formulaire.controllers.DistributionController|count';
UPDATE formulaire.form_shares SET action = 'fr-openent-formulaire-controllers-DistributionController|count' WHERE action = 'fr.openent.formulaire.controllers.DistributionController|count';
UPDATE formulaire.form_shares SET action = 'fr-openent-formulaire-controllers-ResponseController|list' WHERE action = 'fr.openent.formulaire.controllers.ResponseController|list';
UPDATE formulaire.form_shares SET action = 'fr-openent-formulaire-controllers-ResponseController|update' WHERE action = 'fr.openent.formulaire.controllers.ResponseController|update';
UPDATE formulaire.form_shares SET action = 'fr-openent-formulaire-controllers-ResponseController|listMineByDistribution' WHERE action = 'fr.openent.formulaire.controllers.ResponseController|listMineByDistribution';
UPDATE formulaire.form_shares SET action = 'fr-openent-formulaire-controllers-ResponseController|listByDistribution' WHERE action = 'fr.openent.formulaire.controllers.ResponseController|listByDistribution';
UPDATE formulaire.form_shares SET action = 'fr-openent-formulaire-controllers-ResponseController|deleteByQuestionAndDistribution' WHERE action = 'fr.openent.formulaire.controllers.ResponseController|deleteByQuestionAndDistribution';
UPDATE formulaire.form_shares SET action = 'fr-openent-formulaire-controllers-ResponseController|delete' WHERE action = 'fr.openent.formulaire.controllers.ResponseController|delete';
UPDATE formulaire.form_shares SET action = 'fr-openent-formulaire-controllers-ResponseController|create' WHERE action = 'fr.openent.formulaire.controllers.ResponseController|create';
UPDATE formulaire.form_shares SET action = 'fr-openent-formulaire-controllers-ResponseController|export' WHERE action = 'fr.openent.formulaire.controllers.ResponseController|export';
UPDATE formulaire.form_shares SET action = 'fr-openent-formulaire-controllers-ResponseFileController|zipAndDownload' WHERE action = 'fr.openent.formulaire.controllers.ResponseFileController|zipAndDownload';
UPDATE formulaire.form_shares SET action = 'fr-openent-formulaire-controllers-ResponseFileController|get' WHERE action = 'fr.openent.formulaire.controllers.ResponseFileController|get';
UPDATE formulaire.form_shares SET action = 'fr-openent-formulaire-controllers-ResponseFileController|download' WHERE action = 'fr.openent.formulaire.controllers.ResponseFileController|download';
UPDATE formulaire.form_shares SET action = 'fr-openent-formulaire-controllers-ResponseFileController|upload' WHERE action = 'fr.openent.formulaire.controllers.ResponseFileController|upload';
UPDATE formulaire.form_shares SET action = 'fr-openent-formulaire-controllers-ResponseFileController|deleteAll' WHERE action = 'fr.openent.formulaire.controllers.ResponseFileController|deleteAll';
