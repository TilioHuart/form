package fr.openent.form.core.models.Sharing.ShareObject;

import fr.openent.form.core.models.IModel;
import fr.openent.form.helpers.IModelHelper;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ShareObjectBookmarks implements IModel<ShareObjectBookmarks> {
    private Map<String, List<String>> bookmarksRights;

    // Constructors

    public ShareObjectBookmarks() {}

    public ShareObjectBookmarks(JsonObject bookmarks) {
        this.bookmarksRights = bookmarks.getMap().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> (List<String>) e.getValue()));
    }


    // Getters

    public Map<String, List<String>> getBookmarksRights() { return bookmarksRights; }


    // Setters

    public ShareObjectBookmarks setBookmarksRights(Map<String, List<String>> bookmarksRights) {
        this.bookmarksRights = bookmarksRights;
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
    public ShareObjectBookmarks model(JsonObject shareObjectBookmarks){
        return IModelHelper.toModel(shareObjectBookmarks, ShareObjectBookmarks.class).orElse(null);
    }
}

