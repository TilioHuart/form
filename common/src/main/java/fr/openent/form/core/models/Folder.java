package fr.openent.form.core.models;

import static fr.openent.form.core.constants.Fields.*;
import io.vertx.core.json.JsonObject;

public class Folder implements Model<Folder> {
    private Number id;
    private Number parentId;
    private String name;
    private String userId;
    private Number nbFolderChildren;
    private Number nbFormChildren;


    // Constructors

    public Folder() {}

    public Folder(JsonObject folder) {
        this.id = folder.getNumber(ID, null);
        this.parentId = folder.getNumber(PARENT_ID, null);
        this.name = folder.getString(NAME, null);
        this.userId = folder.getString(USER_ID, null);
        this.nbFolderChildren = folder.getNumber(NB_FOLDER_CHILDREN, null);
        this.nbFormChildren = folder.getNumber(NB_FORM_CHILDREN, null);
    }


    // Getters

    public Number getId() { return id; }

    public Number getParentId() { return parentId; }

    public String getName() { return name; }

    public String getUserId() { return userId; }

    public Number getNbFolderChildren() { return nbFolderChildren; }

    public Number getNbFormChildren() { return nbFormChildren; }


    // Setters

    public Folder setId(Number id) {
        this.id = id;
        return this;
    }

    public Folder setParentId(Number parentId) {
        this.parentId = parentId;
        return this;
    }

    public Folder setName(String name) {
        this.name = name;
        return this;
    }

    public Folder setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public Folder setNbFolderChildren(Number nbFolderChildren) {
        this.nbFolderChildren = nbFolderChildren;
        return this;
    }

    public Folder setNbFormChildren(Number nbFormChildren) {
        this.nbFormChildren = nbFormChildren;
        return this;
    }


    // Functions

    public JsonObject toJson() {
        return new JsonObject()
                .put(ID, this.id)
                .put(PARENT_ID, this.parentId)
                .put(NAME, this.name)
                .put(USER_ID, this.userId)
                .put(NB_FOLDER_CHILDREN, this.nbFolderChildren)
                .put(NB_FORM_CHILDREN, this.nbFormChildren);
    }

    @Override
    public Folder model(JsonObject folder){
        return new Folder(folder);
    }
}

