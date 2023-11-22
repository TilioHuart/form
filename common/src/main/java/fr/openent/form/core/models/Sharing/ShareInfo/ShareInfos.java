package fr.openent.form.core.models.Sharing.ShareInfo;

import fr.openent.form.core.models.IModel;
import fr.openent.form.helpers.IModelHelper;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

import static fr.openent.form.core.constants.Fields.*;

public class ShareInfos implements IModel<ShareInfos> {
    private static final Logger log = LoggerFactory.getLogger(ShareInfos.class);
    private List<ShareInfosAction> actions;
    private ShareInfosGroups groups;
    private ShareInfosUsers users;


    // Constructors

    public ShareInfos() { }

    public ShareInfos(JsonObject shareInfos) {
        this.actions = shareInfos.getJsonArray(ACTIONS, new JsonArray()).stream()
                .map(JsonObject.class::cast)
                .map(ShareInfosAction::new)
                .collect(Collectors.toList());
        this.groups = new ShareInfosGroups(shareInfos.getJsonObject(GROUPS, null));
        this.users = new ShareInfosUsers(shareInfos.getJsonObject(USERS, null));
    }


    // Getters

    public List<ShareInfosAction> getActions() { return actions; }

    public ShareInfosGroups getGroups() { return groups; }

    public ShareInfosUsers getUsers() { return users; }


    // Setters

    public ShareInfos setActions(List<ShareInfosAction> actions) {
        this.actions = actions;
        return this;
    }

    public ShareInfos setGroups(ShareInfosGroups groups) {
        this.groups = groups;
        return this;
    }

    public ShareInfos setUsers(ShareInfosUsers users) {
        this.users = users;
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
    public ShareInfos model(JsonObject shareInfos){
        return IModelHelper.toModel(shareInfos, ShareInfos.class).orElse(null);
    }
}

