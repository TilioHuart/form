UPDATE formulaire.form_shares SET action = 'fr-openent-formulaire-controllers-ResponseController|listMineByDistribution'
WHERE action = 'fr-openent-formulaire-controllers-ResponseController|listMine';

UPDATE formulaire.distribution SET date_sending = date_sending + interval '2 hour';
UPDATE formulaire.distribution SET date_response = date_response + interval '2 hour';
UPDATE formulaire.form SET date_creation = date_creation + interval '2 hour';
UPDATE formulaire.form SET date_modification = date_modification + interval '2 hour';
UPDATE formulaire.form SET date_opening = date_opening + interval '2 hour';
UPDATE formulaire.form SET date_ending = date_ending + interval '2 hour';

ALTER TABLE formulaire.distribution ALTER COLUMN date_sending TYPE timestamp with time zone;
ALTER TABLE formulaire.distribution ALTER COLUMN date_response TYPE timestamp with time zone;
ALTER TABLE formulaire.form ALTER COLUMN date_creation TYPE timestamp with time zone;
ALTER TABLE formulaire.form ALTER COLUMN date_modification TYPE timestamp with time zone;
ALTER TABLE formulaire.form ALTER COLUMN date_opening TYPE timestamp with time zone;
ALTER TABLE formulaire.form ALTER COLUMN date_ending TYPE timestamp with time zone;