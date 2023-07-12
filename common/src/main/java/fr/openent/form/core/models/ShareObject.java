package fr.openent.form.core.models;

import fr.openent.form.helpers.RightsHelper;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static fr.openent.form.core.constants.Fields.*;

public class ShareObject implements IModel<ShareObject> {
    private static final Logger log = LoggerFactory.getLogger(ShareObject.class);
    private JsonObject users;
    private JsonObject groups;
    private JsonObject bookmarks;
    private String role;


    // Constructors

    public ShareObject() { }

    public ShareObject(JsonObject shareObject) {
        this.users = shareObject.getJsonObject(USERS, null);
        this.groups = shareObject.getJsonObject(GROUPS, null);
        this.bookmarks = shareObject.getJsonObject(BOOKMARKS, null);
        this.role = shareObject.getString(ROLE, null);
    }


    // Getters

    public JsonObject getUsers() { return users; }

    public JsonObject getGroups() { return groups; }

    public JsonObject getBookmarks() { return bookmarks; }

    public String getRole() { return role; }


    // Setters

    public ShareObject setUsers(JsonObject users) {
        this.users = users;
        return this;
    }

    public ShareObject setGroups(JsonObject groups) {
        this.groups = groups;
        return this;
    }

    public ShareObject setBookmarks(JsonObject bookmarks) {
        this.bookmarks = bookmarks;
        return this;
    }

    public ShareObject setRole(String role) {
        this.role = role;
        return this;
    }


    // Functions

    public JsonObject toJson() {
        return new JsonObject()
                .put(USERS, this.users)
                .put(GROUPS, this.groups)
                .put(BOOKMARKS, this.bookmarks)
                .put(ROLE, this.role);
    }

    @Override
    public ShareObject model(JsonObject shareObject){
        return new ShareObject(shareObject);
    }

    public ShareObject addCommonRights() {
        JsonArray commonRights = RightsHelper.getCommonRights();

        addCommonRightByField(USERS, commonRights);
        addCommonRightByField(GROUPS, commonRights);
        addCommonRightByField(BOOKMARKS, commonRights);

        return this;
    }

    private void addCommonRightByField(String field, JsonArray commonRights) {
        JsonObject rightsById = getJsonObjectFieldInfos(field);

        if (!rightsById.isEmpty()) {
            List<String> ids = new ArrayList<>(rightsById.fieldNames());

            for (String id : ids) {
                JsonArray rights = rightsById.getJsonArray(id);
                boolean shouldAddCommonRight = rights.stream().anyMatch(right -> RightsHelper.hasSharingRight((String) right));
                if (shouldAddCommonRight) rights.addAll(commonRights);
            }
        }
    }

    private JsonObject getJsonObjectFieldInfos(String field) {
        switch (field) {
            case USERS:
                return getUsers();
            case GROUPS:
                return getGroups();
            case BOOKMARKS:
                return getBookmarks();
            default:
                log.error("[Formulaire@ShareObject::getJsonObjectField] Field " + field + " doesn't exists or is not a JsonObject type.");
                return null;
        }
    }
}

