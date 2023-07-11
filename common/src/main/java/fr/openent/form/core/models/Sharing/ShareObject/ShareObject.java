package fr.openent.form.core.models.Sharing.ShareObject;

import fr.openent.form.core.models.IModel;
import fr.openent.form.helpers.IModelHelper;
import fr.openent.form.helpers.RightsHelper;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.List;
import java.util.Map;

import static fr.openent.form.core.constants.Fields.*;

public class ShareObject implements IModel<ShareObject> {
    private static final Logger log = LoggerFactory.getLogger(ShareObject.class);
    private ShareObjectUsers users;
    private ShareObjectGroups groups;
    private ShareObjectBookmarks bookmarks;
    private String role;


    // Constructors

    public ShareObject() { }

    public ShareObject(JsonObject shareObject) {
        this.users = new ShareObjectUsers(shareObject.getJsonObject(USERS, null));
        this.groups = new ShareObjectGroups(shareObject.getJsonObject(GROUPS, null));
        this.bookmarks = new ShareObjectBookmarks(shareObject.getJsonObject(BOOKMARKS, null));
        this.role = shareObject.getString(ROLE, null);
    }


    // Getters

    public ShareObjectUsers getUsers() { return users; }

    public ShareObjectGroups getGroups() { return groups; }

    public ShareObjectBookmarks getBookmarks() { return bookmarks; }

    public String getRole() { return role; }


    // Setters

    public ShareObject setUsers(ShareObjectUsers users) {
        this.users = users;
        return this;
    }

    public ShareObject setGroups(ShareObjectGroups groups) {
        this.groups = groups;
        return this;
    }

    public ShareObject setBookmarks(ShareObjectBookmarks bookmarks) {
        this.bookmarks = bookmarks;
        return this;
    }

    public ShareObject setRole(String role) {
        this.role = role;
        return this;
    }


    // Functions

    public JsonObject toJson() {
        return IModelHelper.toJson(this, true, true);
    }

    public JsonObject toJson(boolean snakeCase) {
        return IModelHelper.toJson(this, true, snakeCase);
    }

    @Deprecated
    @Override
    public ShareObject model(JsonObject shareObject){
        return IModelHelper.toModel(shareObject, ShareObject.class).orElse(null);
    }

    public void addCommonRights(List<String> commonRights) {
        addCommonRightByField(this.users.getUsersRights(), commonRights);
        addCommonRightByField(this.groups.getGroupsRights(), commonRights);
        addCommonRightByField(this.bookmarks.getBookmarksRights(), commonRights);
    }

    private void addCommonRightByField(Map<String, List<String>> rightsById, List<String> commonRights) {
        if (rightsById != null && !rightsById.isEmpty()) {
            rightsById.values().stream()
                .filter(rights -> rights.stream().anyMatch(RightsHelper::hasSharingRight))
                .forEach(rights -> rights.addAll(commonRights));
        }
    }
}

