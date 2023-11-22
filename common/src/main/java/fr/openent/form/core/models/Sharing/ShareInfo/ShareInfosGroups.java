package fr.openent.form.core.models.Sharing.ShareInfo;

import fr.openent.form.core.models.IModel;
import fr.openent.form.helpers.IModelHelper;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static fr.openent.form.core.constants.Fields.*;

public class ShareInfosGroups implements IModel<ShareInfosGroups> {

    private Map<String, List<String>> checked;
    private List<ShareInfosGroupsVisible> visibles;

    // Constructors

    public ShareInfosGroups() {}

    public ShareInfosGroups(JsonObject shareInfosGroups) {
        this.checked = shareInfosGroups.getJsonObject(CHECKED, null).getMap().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> (List<String>)e));
        this.visibles = shareInfosGroups.getJsonArray(VISIBLES, new JsonArray()).stream()
                .map(JsonObject.class::cast)
                .map(ShareInfosGroupsVisible::new)
                .collect(Collectors.toList());
    }


    // Getters

    public Map<String, List<String>> getChecked() { return checked; }

    public List<ShareInfosGroupsVisible> getVisibles() { return visibles; }


    // Setters

    public ShareInfosGroups setChecked(Map<String, List<String>> checked) {
        this.checked = checked;
        return this;
    }

    public ShareInfosGroups setVisibles(List<ShareInfosGroupsVisible> visibles) {
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
    public ShareInfosGroups model(JsonObject shareInfosGroups){
        return IModelHelper.toModel(shareInfosGroups, ShareInfosGroups.class).orElse(null);
    }
}

