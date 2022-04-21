package fr.openent.formulaire.helpers;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class UtilsHelper {
    private static final Logger log = LoggerFactory.getLogger(UtilsHelper.class);

    private UtilsHelper() {}

    public static JsonArray getIds(JsonArray items, boolean toString) {
        JsonArray ids = new JsonArray();
        for (int i = 0; i < items.size(); i++) {
            Integer item = items.getJsonObject(i).getInteger("id");
            ids.add(toString ? item.toString() : item);
        }
        return ids;
    }

    public static JsonArray getIds(JsonArray items) {
        return getIds(items, true);
    }

    public static JsonArray getStringIds(JsonArray users) {
        JsonArray userIds = new JsonArray();
        for (int i = 0; i < users.size(); i++) {
            userIds.add(users.getJsonObject(i).getString("id"));
        }
        return userIds;
    }

    public static JsonArray getByProp(JsonArray items, String prop) {
        JsonArray values = new JsonArray();
        for (int i = 0; i < items.size(); i++) {
            values.add(items.getJsonObject(i).getValue(prop));
        }
        return values;
    }

    public static JsonArray mapByProps(JsonArray items, JsonArray props) {
        JsonArray values = items.copy();
        for (int i = 0; i < items.size(); i++) {
            JsonObject item = values.getJsonObject(i);
            item.getMap().keySet().removeIf(k -> !props.contains(k));
        }
        return values;
    }
}
