ALTER TABLE formulaire.response_file
    DROP CONSTRAINT response_file_pkey;

ALTER TABLE formulaire.response_file
    ADD CONSTRAINT response_file_pkey UNIQUE (id, response_id);