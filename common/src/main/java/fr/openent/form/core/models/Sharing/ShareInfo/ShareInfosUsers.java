package fr.openent.form.core.models.Sharing.ShareInfo;

import fr.openent.form.core.models.IModel;
import fr.openent.form.helpers.IModelHelper;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static fr.openent.form.core.constants.Fields.CHECKED;
import static fr.openent.form.core.constants.Fields.VISIBLES;

public class ShareInfosUsers implements IModel<ShareInfosUsers> {

    private Map<String, List<String>> checked;
    private List<ShareInfosUsersVisible> visibles;

    // Constructors

    public ShareInfosUsers() {}

    public ShareInfosUsers(JsonObject shareInfosUsers) {
        this.checked = shareInfosUsers.getJsonObject(CHECKED, null).getMap().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> (List<String>)e));
        this.visibles = shareInfosUsers.getJsonArray(VISIBLES, new JsonArray()).stream().map(ShareInfosUsersVisible.class::cast).collect(Collectors.toList());
    }


    // Getters

    public Map<String, List<String>> getChecked() { return checked; }

    public List<ShareInfosUsersVisible> getVisibles() { return visibles; }


    // Setters

    public ShareInfosUsers setChecked(Map<String, List<String>> checked) {
        this.checked = checked;
        return this;
    }

    public ShareInfosUsers setVisibles(List<ShareInfosUsersVisible> visibles) {
        this.visibles = visibles;
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
    public ShareInfosUsers model(JsonObject shareInfosUsers){
        return IModelHelper.toModel(shareInfosUsers, ShareInfosUsers.class).orElse(null);
    }
}

