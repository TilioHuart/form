package fr.openent.form.core.models;

import static fr.openent.form.core.constants.Fields.*;
import io.vertx.core.json.JsonObject;

public class ShareGroup implements IModel<ShareGroup> {
    private Number id;
    private String name;


    // Constructors

    public ShareGroup() {}

    public ShareGroup(JsonObject group) {
        this.id = group.getNumber(ID, null);
        this.name = group.getString(NAME, null);
    }


    // Getters

    public Number getId() { return id; }

    public String getName() { return name; }


    // Setters

    public ShareGroup setId(Number id) {
        this.id = id;
        return this;
    }

    public ShareGroup setName(String name) {
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
    public ShareGroup model(JsonObject group){
        return new ShareGroup(group);
    }
}

