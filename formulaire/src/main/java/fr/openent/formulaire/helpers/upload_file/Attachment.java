package fr.openent.formulaire.helpers.upload_file;

import io.vertx.core.json.JsonObject;

import static fr.openent.form.core.constants.Fields.ID;
import static fr.openent.form.core.constants.Fields.METADATA;

public class Attachment {
    private final String id;
    private final Metadata metadata;

    public Attachment(String id, Metadata metadata) {
        this.id = id;
        this.metadata = metadata;
    }

    public String id() {
        return id;
    }

    public Metadata metadata() {
        return metadata;
    }

    public JsonObject toJson() {
        return new JsonObject().put(ID, id).put(METADATA ,metadata.toJSON());
    }
}
