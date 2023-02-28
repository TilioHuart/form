package fr.openent.form.core.models;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.stream.Collectors;

public interface Model<I extends Model<I>> {
    JsonObject toJson();

    I model(JsonObject model);

    @SuppressWarnings("unchecked")
    default List<I> toList(JsonArray results) {
        if (results == null) return null;
        return ((List<JsonObject>) results.getList()).stream().map(this::model).collect(Collectors.toList());
    }

    default JsonArray toJsonArray(List<I> models) {
        if (models == null) return null;
        return new JsonArray(models.stream().map(Model::toJson).collect(Collectors.toList()));
    }
}
