package fr.openent.form.core.models.Sharing.ShareObject;

import fr.openent.form.core.models.IModel;
import fr.openent.form.helpers.IModelHelper;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ShareObjectUsers implements IModel<ShareObjectUsers> {
    private Map<String, List<String>> usersRights;

    // Constructors

    public ShareObjectUsers() {}

    public ShareObjectUsers(JsonObject users) {
        this.usersRights = users.getMap().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> (List<String>)e));
    }


    // Getters

    public Map<String, List<String>> getUsersRights() { return usersRights; }


    // Setters

    public ShareObjectUsers setUsersRights(Map<String, List<String>> usersRights) {
        this.usersRights = usersRights;
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
    public ShareObjectUsers model(JsonObject shareObjectUsers){
        return IModelHelper.toModel(shareObjectUsers, ShareObjectUsers.class).orElse(null);
    }
}

