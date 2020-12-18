CREATE TABLE formulaire.users (
	id          VARCHAR(36) NOT NULL PRIMARY KEY,
	username    VARCHAR
);

CREATE TABLE formulaire.groups (
	id      VARCHAR(36) NOT NULL PRIMARY KEY,
	name    VARCHAR
);

CREATE TABLE formulaire.members (
	id          VARCHAR(36) NOT NULL PRIMARY KEY,
	user_id     VARCHAR(36),
	group_id    VARCHAR(36),
	CONSTRAINT user_fk FOREIGN KEY(user_id) REFERENCES formulaire.users(id) ON UPDATE CASCADE ON DELETE CASCADE,
	CONSTRAINT group_fk FOREIGN KEY(group_id) REFERENCES formulaire.groups(id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE formulaire.form_shares (
	member_id       VARCHAR(36) NOT NULL,
	resource_id     bigint NOT NULL,
	action          VARCHAR NOT NULL,
	CONSTRAINT resource_share PRIMARY KEY (member_id, resource_id, action),
	CONSTRAINT form_shares_member_fk FOREIGN KEY(member_id) REFERENCES formulaire.members(id) ON UPDATE CASCADE ON DELETE CASCADE
);


CREATE OR REPLACE FUNCTION formulaire.merge_users(key VARCHAR, data VARCHAR) RETURNS VOID AS $$
    BEGIN
        LOOP
            UPDATE formulaire.users SET username = data WHERE id = key;
            IF found THEN
                RETURN;
            END IF;
            BEGIN
                INSERT INTO formulaire.users(id,username) VALUES (key, data);
                RETURN;
            EXCEPTION WHEN unique_violation THEN
            END;
        END LOOP;
    END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION formulaire.insert_users_members() RETURNS TRIGGER AS $$
    BEGIN
		IF (TG_OP = 'INSERT') THEN
            INSERT INTO formulaire.members(id, user_id) VALUES (NEW.id, NEW.id);
            RETURN NEW;
        END IF;
        RETURN NULL;
    END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION formulaire.insert_groups_members() RETURNS TRIGGER AS $$
    BEGIN
		IF (TG_OP = 'INSERT') THEN
            INSERT INTO formulaire.members(id, group_id) VALUES (NEW.id, NEW.id);
            RETURN NEW;
        END IF;
        RETURN NULL;
    END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER users_trigger
AFTER INSERT ON formulaire.users
    FOR EACH ROW EXECUTE PROCEDURE formulaire.insert_users_members();

CREATE TRIGGER groups_trigger
AFTER INSERT ON formulaire.groups
    FOR EACH ROW EXECUTE PROCEDURE formulaire.insert_groups_members();

CREATE TYPE formulaire.share_tuple as (member_id VARCHAR(36), action VARCHAR);
