package fr.openent.form.core.models.Sharing.ShareInfo;

import fr.openent.form.core.models.IModel;
import fr.openent.form.helpers.IModelHelper;
import io.vertx.core.json.JsonObject;

import static fr.openent.form.core.constants.Fields.*;

public class ShareInfosUsersVisible implements IModel<ShareInfosUsersVisible> {
    private String firstName;
    private String id;
    private String lastName;
    private String login;
    private String profile;
    private String username;

    // Constructors

    public ShareInfosUsersVisible() {}

    public ShareInfosUsersVisible(JsonObject shareInfosUsersVisible) {
        this.firstName = shareInfosUsersVisible.getString(PARAM_FIRST_NAME, null);
        this.id = shareInfosUsersVisible.getString(ID, null);
        this.lastName = shareInfosUsersVisible.getString(PARAM_LAST_NAME, null);
        this.login = shareInfosUsersVisible.getString(LOGIN, null);
        this.profile = shareInfosUsersVisible.getString(PROFILE, null);
        this.username = shareInfosUsersVisible.getString(USERNAME, null);
    }


    // Getters

    public String getFirstName() { return firstName; }

    public String getId() { return id; }

    public String getLastName() { return lastName; }

    public String getLogin() { return login; }

    public String getProfile() { return profile; }

    public String getUsername() { return username; }


    // Setters

    public ShareInfosUsersVisible setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public ShareInfosUsersVisible setType(String id) {
        this.id = id;
        return this;
    }

    public ShareInfosUsersVisible setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public ShareInfosUsersVisible setLogin(String login) {
        this.login = login;
        return this;
    }

    public ShareInfosUsersVisible setProfile(String profile) {
        this.profile = profile;
        return this;
    }

    public ShareInfosUsersVisible setUsername(String username) {
        this.username = username;
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
    public ShareInfosUsersVisible model(JsonObject shareInfosUsersVisible){
        return IModelHelper.toModel(shareInfosUsersVisible, ShareInfosUsersVisible.class).orElse(null);
    }
}

