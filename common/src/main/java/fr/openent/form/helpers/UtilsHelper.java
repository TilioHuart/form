package fr.openent.form.helpers;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import static fr.openent.form.core.constants.Fields.ID;

public class UtilsHelper {
    private static final Logger log = LoggerFactory.getLogger(UtilsHelper.class);

    private UtilsHelper() {}

    public static JsonArray getIds(JsonArray items, boolean toString) {
        JsonArray ids = new JsonArray();
        for (int i = 0; i < items.size(); i++) {
            Integer id = items.getJsonObject(i).getInteger(ID);
            ids.add(toString && id != null ? id.toString() : id);
        }
        return ids;
    }

    public static JsonArray getIds(JsonArray items) {
        return getIds(items, true);
    }

    public static JsonArray getStringIds(JsonArray users) {
        JsonArray userIds = new JsonArray();
        for (int i = 0; i < users.size(); i++) {
            userIds.add(users.getJsonObject(i).getString(ID));
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
