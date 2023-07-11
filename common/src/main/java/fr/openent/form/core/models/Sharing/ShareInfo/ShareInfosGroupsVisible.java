package fr.openent.form.core.models.Sharing.ShareInfo;

import fr.openent.form.core.models.IModel;
import fr.openent.form.helpers.IModelHelper;
import io.vertx.core.json.JsonObject;

import static fr.openent.form.core.constants.Fields.*;

public class ShareInfosGroupsVisible implements IModel<ShareInfosGroupsVisible> {
    private String groupDisplayName;
    private String id;
    private String name;
    private String structureName;

    // Constructors

    public ShareInfosGroupsVisible() {}

    public ShareInfosGroupsVisible(JsonObject shareInfosGroupsVisible) {
        this.groupDisplayName = shareInfosGroupsVisible.getString(PARAM_DISPLAY_NAME, null);
        this.id = shareInfosGroupsVisible.getString(TYPE, null);
        this.name = shareInfosGroupsVisible.getString(NAME, null);
        this.structureName = shareInfosGroupsVisible.getString(PARAM_STRUCTURE_NAME, null);
    }


    // Getters

    public String getDisplayName() { return groupDisplayName; }

    public String getType() { return id; }

    public String getName() { return name; }

    public String getStructureName() { return structureName; }


    // Setters

    public ShareInfosGroupsVisible setDisplayName(String groupDisplayName) {
        this.groupDisplayName = groupDisplayName;
        return this;
    }

    public ShareInfosGroupsVisible setType(String id) {
        this.id = id;
        return this;
    }

    public ShareInfosGroupsVisible setName(String name) {
        this.name = name;
        return this;
    }

    public ShareInfosGroupsVisible setStructureName(String structureName) {
        this.structureName = structureName;
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
    public ShareInfosGroupsVisible model(JsonObject shareInfosGroupsVisible){
        return IModelHelper.toModel(shareInfosGroupsVisible, ShareInfosGroupsVisible.class).orElse(null);
    }
}

