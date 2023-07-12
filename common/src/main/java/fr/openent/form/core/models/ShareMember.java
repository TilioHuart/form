package fr.openent.form.core.models;

import static fr.openent.form.core.constants.Fields.*;
import io.vertx.core.json.JsonObject;

public class ShareMember implements IModel<ShareMember> {
    private String id;
    private String userId;
    private String groupId;


    // Constructors

    public ShareMember() {}

    public ShareMember(JsonObject member) {
        this.id = member.getString(ID, null);
        this.userId = member.getString(USER_ID, null);
        this.groupId = member.getString(GROUP_ID, null);
    }


    // Getters

    public String getId() { return id; }

    public String getUserId() { return userId; }

    public String getGroupId() { return groupId; }


    // Setters

    public ShareMember setId(String id) {
        this.id = id;
        return this;
    }

    public ShareMember setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public ShareMember setGroupId(String groupId) {
        this.groupId = groupId;
        return this;
    }


    // Functions

    public JsonObject toJson() {
        return new JsonObject()
                .put(ID, this.id)
                .put(USER_ID, this.userId)
                .put(GROUP_ID, this.groupId);
    }

    @Override
    public ShareMember model(JsonObject member){
        return new ShareMember(member);
    }
}

