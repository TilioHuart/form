package fr.openent.form.core.models;

import io.vertx.core.json.JsonObject;

import static fr.openent.form.core.constants.Fields.*;

public class ShareUser implements Model<ShareUser> {
    private String id;
    private String username;


    // Constructors

    public ShareUser() {}

    public ShareUser(JsonObject user) {
        this.id = user.getString(ID, null);
        this.username = user.getString(USERNAME, null);
    }


    // Getters

    public String getId() { return id; }

    public String getUsername() { return username; }


    // Setters

    public ShareUser setId(String id) {
        this.id = id;
        return this;
    }

    public ShareUser setUsername(String username) {
        this.username = username;
        return this;
    }


    // Functions

    public JsonObject toJson() {
        return new JsonObject()
                .put(ID, this.id)
                .put(USERNAME, this.username);
    }

    @Override
    public ShareUser model(JsonObject user){
        return new ShareUser(user);
    }
}

