CREATE OR REPLACE FUNCTION add_distrib(f_id bigint, s_id VARCHAR(36), s_name VARCHAR, r_id VARCHAR(36), r_name VARCHAR) RETURNS boolean AS
$$
DECLARE
    not_finished_distrib_count bigint;
BEGIN
    SELECT COUNT(*) FROM formulaire.distribution WHERE form_id = f_id AND responder_id = r_id AND (status = 'TO_DO' OR status = 'IN_PROGRESS') INTO not_finished_distrib_count;

    IF not_finished_distrib_count <= 0 THEN
        INSERT INTO formulaire.distribution (form_id, sender_id, sender_name, responder_id, responder_name, status, date_sending, active)
        VALUES (f_id, s_id, s_name, r_id, r_name, 'TO_DO', NOW(), true);
        RETURN true;
    END IF;

    RETURN false;
END;
$$ LANGUAGE plpgsql;