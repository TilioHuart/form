CREATE TABLE formulaire.captcha (
    id          bigserial PRIMARY KEY,
    question    VARCHAR NOT NULL,
    answer      VARCHAR NOT NULL
);

ALTER TABLE formulaire.form
    ADD COLUMN is_public    boolean NOT NULL DEFAULT false,
    ADD COLUMN public_key   VARCHAR(36);

ALTER TABLE formulaire.distribution
    ADD COLUMN public_key   VARCHAR(36),
    ADD COLUMN captcha_id   bigint,
    ADD CONSTRAINT fk_captcha_id FOREIGN KEY (captcha_id) REFERENCES formulaire.captcha(id) ON UPDATE NO ACTION ON DELETE NO ACTION;

DELETE FROM formulaire.form_shares WHERE action = 'fr-openent-formulaire-controllers-QuestionChoiceController|createMultiple';
DELETE FROM formulaire.form_shares WHERE action = 'fr-openent-formulaire-controllers-ResponseController|fillResponses';

INSERT INTO formulaire.captcha(question, answer) VALUES
('Combien font un plus quinze ?', 'seize'),
('Combien font deux plus un ?', 'trois'),
('Combien font trois plus trois ?', 'six'),
('Combien font quatre plus seize ?', 'vingt'),
('Combien font cinq plus six ?', 'onze'),
('Combien font six plus sept ?', 'treize'),
('Combien font sept plus deux ?', 'neuf'),
('Combien font huit plus zéro ?', 'huit'),
('Combien font neuf plus quatre ?', 'treize'),
('Combien font dix plus un ?', 'onze'),
('Combien font onze plus un ?', 'douze'),
('Combien font douze plus deux ?', 'quatorze'),
('Combien font treize plus trois ?', 'seize'),
('Combien font quatorze plus six ?', 'vingt'),
('Combien font quinze plus un ?', 'seize'),
('Combien font quinze moins un ?', 'quatorze'),
('Combien font deux moins un ?', 'un'),
('Combien font trois moins trois ?', 'zero'),
('Combien font seize moins quatre ?', 'douze'),
('Combien font six moins cinq ?', 'un'),
('Combien font sept moins cinq ?', 'deux'),
('Combien font douze moins sept ?', 'cinq'),
('Combien font huit moins zéro ?', 'huit'),
('Combien font quatre moins un ?', 'trois'),
('Combien font dix moins neuf ?', 'un'),
('Combien font onze moins deux ?', 'neuf'),
('Combien font douze moins deux ?', 'dix'),
('Combien font treize moins huit ?', 'cinq'),
('Combien font quatorze moins cinq ?', 'neuf'),
('Combien font quinze moins deux ?', 'treize');