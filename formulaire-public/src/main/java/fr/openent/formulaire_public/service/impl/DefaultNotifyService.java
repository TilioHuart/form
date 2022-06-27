package fr.openent.formulaire_public.service.impl;

import fr.openent.formulaire_public.service.NotifyService;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.notification.TimelineHelper;

public class DefaultNotifyService implements NotifyService {
    private final TimelineHelper timelineHelper;

    public DefaultNotifyService(TimelineHelper timelineHelper){
        this.timelineHelper = timelineHelper;
    }

    @Override
    public void notifyResponse(HttpServerRequest request, JsonObject form, JsonArray managers) {
        JsonObject params = new JsonObject()
                .put("formUri", "/formulaire#/form/" + form.getInteger("id") + "/edit")
                .put("formName", form.getString("title"))
                .put("formResultsUri", "/formulaire#/form/" + form.getInteger("id") + "/results/1")
                .put("pushNotif", new JsonObject().put("title", "push.notif.formulaire.public.response").put("body", ""));

        timelineHelper.notifyTimeline(request, "formulaire_public.response_notification", null, managers.getList(), params);
    }
}
