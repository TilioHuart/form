CREATE TABLE formulaire.folder (
    id                  bigserial PRIMARY KEY,
    parent_id           bigint DEFAULT 1,
    name                VARCHAR NOT NULL,
    user_id             VARCHAR(36) NOT NULL,
    nb_folder_children  bigint DEFAULT 0,
    nb_form_children    bigint DEFAULT 0,
    CONSTRAINT fk_parent_id FOREIGN KEY (parent_id) REFERENCES formulaire.folder(id) ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE TABLE formulaire.rel_form_folder (
   user_id      VARCHAR(36) NOT NULL,
   form_id      bigint NOT NULL,
   folder_id    bigint NOT NULL,
   CONSTRAINT fk_form_id FOREIGN KEY (form_id) REFERENCES formulaire.form(id) ON UPDATE NO ACTION ON DELETE CASCADE,
   CONSTRAINT fk_folder_id FOREIGN KEY (folder_id) REFERENCES formulaire.folder(id) ON UPDATE NO ACTION ON DELETE CASCADE,
   CONSTRAINT unique_rel_form_folder UNIQUE (user_id, form_id, folder_id)
);

INSERT INTO formulaire.folder (parent_id, name, user_id, nb_folder_children, nb_form_children) VALUES
(null, 'Mes formulaires', '', null, null),
(null, 'Partagés avec moi', '', null, null),
(null, 'Corbeille', '', null, null);

WITH forms AS (SELECT id, owner_id FROM formulaire.form GROUP BY id, owner_id)
INSERT INTO formulaire.rel_form_folder (user_id, form_id, folder_id) SELECT owner_id, id, 1 FROM forms;