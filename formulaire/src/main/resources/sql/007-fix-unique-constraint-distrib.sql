ALTER TABLE formulaire.distribution
    ADD CONSTRAINT unique_distrib UNIQUE (form_id, responder_id, status, date_sending)