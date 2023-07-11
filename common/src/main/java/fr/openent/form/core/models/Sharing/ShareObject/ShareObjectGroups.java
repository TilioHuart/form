package fr.openent.form.core.models.Sharing.ShareObject;

import fr.openent.form.core.models.IModel;
import fr.openent.form.helpers.IModelHelper;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ShareObjectGroups implements IModel<ShareObjectGroups> {
    private Map<String, List<String>> groupsRights;

    // Constructors

    public ShareObjectGroups() {}

    public ShareObjectGroups(JsonObject shareObjectGroups) {
        this.groupsRights = shareObjectGroups.getMap().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> (List<String>)e));
    }


    // Getters

    public Map<String, List<String>> getGroupsRights() { return groupsRights; }


    // Setters

    public ShareObjectGroups setGroupsRights(Map<String, List<String>> groupsRights) {
        this.groupsRights = groupsRights;
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
    public ShareObjectGroups model(JsonObject shareObjectGroups){
        return IModelHelper.toModel(shareObjectGroups, ShareObjectGroups.class).orElse(null);
    }
}

