package fr.openent.form.core.models;

import fr.openent.form.core.enums.ApiVersions;
import fr.openent.form.helpers.IModelHelper;
import io.vertx.core.json.JsonObject;

public class ApiVersion implements IModel<ApiVersion> {
    private String version;


    // Constructors

    public ApiVersion() {}

    public ApiVersion(String version) {
        this.version = version;
    }


    // Getters

    public String getVersion() { return version; }


    // Setters

    public ApiVersion setVersion(String version) {
        this.version = version;
        return this;
    }


    // Functions

    public JsonObject toJson() {
        return IModelHelper.toJson(this, true, true);
    }

    public boolean isBefore(ApiVersions apiVersion) {
        return this.version == null || this.version.isEmpty() || this.version.equals("null") || this.version.compareTo(apiVersion.getValue()) < 0;
    }
}

