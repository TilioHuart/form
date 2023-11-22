package fr.openent.form.core.models.Sharing;

import fr.openent.form.core.models.IModel;
import io.vertx.core.json.JsonObject;

import static fr.openent.form.core.constants.Fields.ID;
import static fr.openent.form.core.constants.Fields.NAME;

public class ShareBookmark implements IModel<ShareBookmark> {
    private String id;
    private String name;


    // Constructors

    public ShareBookmark() {}

    public ShareBookmark(JsonObject group) {
        this.id = group.getString(ID, null);
        this.name = group.getString(NAME, null);
    }


    // Getters

    public String getId() { return id; }

    public String getName() { return name; }


    // Setters

    public ShareBookmark setId(String id) {
        this.id = id;
        return this;
    }

    public ShareBookmark setName(String name) {
        this.name = name;
        return this;
    }


    // Functions

    public JsonObject toJson() {
        return new JsonObject()
                .put(ID, this.id)
                .put(NAME, this.name);
    }

    @Override
    public ShareBookmark model(JsonObject group){
        return new ShareBookmark(group);
    }
}

