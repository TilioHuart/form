package fr.openent.form.core.models;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

public interface IModel<I extends IModel<I>> {
    JsonObject toJson();

    /**
     * @deprecated Should instead use IModelHelper directly
     */
    @Deprecated
    default I model(JsonObject model) {
        return null;
    }

    /**
     * @deprecated Should instead use IModelHelper directly
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    default List<I> toList(JsonArray results) {
        if (results == null) return null;
        try {
            return ((List<JsonObject>) results.getList()).stream().map(this::model).collect(Collectors.toList());
        }
        catch (Exception e) {
            return ((List<LinkedHashMap>) results.getList()).stream().map(map -> this.model(new JsonObject(map))).collect(Collectors.toList());
        }
    }

    /**
     * @deprecated Should instead use IModelHelper directly
     */
    @Deprecated
    default JsonArray toJsonArray(List<I> models) {
        if (models == null) return null;
        return new JsonArray(models.stream().map(IModel::toJson).collect(Collectors.toList()));
    }
}
