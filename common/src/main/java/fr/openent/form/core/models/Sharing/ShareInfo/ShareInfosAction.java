package fr.openent.form.core.models.Sharing.ShareInfo;

import fr.openent.form.core.models.IModel;
import fr.openent.form.helpers.IModelHelper;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.stream.Collectors;

import static fr.openent.form.core.constants.Fields.*;

public class ShareInfosAction implements IModel<ShareInfosAction> {
    private String displayName;
    private List<String> name;
    private String type;

    // Constructors

    public ShareInfosAction() {}

    public ShareInfosAction(JsonObject shareInfosActions) {
        this.displayName = shareInfosActions.getString(PARAM_DISPLAY_NAME, null);
        this.name = shareInfosActions.getJsonArray(NAME, new JsonArray()).stream().map(String.class::cast).collect(Collectors.toList());
        this.type = shareInfosActions.getString(TYPE, null);
    }


    // Getters

    public String getDisplayName() { return displayName; }

    public List<String> getName() { return name; }

    public String getType() { return type; }


    // Setters

    public ShareInfosAction setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public ShareInfosAction setName(List<String> name) {
        this.name = name;
        return this;
    }

    public ShareInfosAction setType(String type) {
        this.type = type;
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
    public ShareInfosAction model(JsonObject shareInfosActions){
        return IModelHelper.toModel(shareInfosActions, ShareInfosAction.class).orElse(null);
    }
}

