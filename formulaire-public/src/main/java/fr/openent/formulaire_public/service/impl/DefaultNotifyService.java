package fr.openent.formulaire_public.service.impl;

import fr.openent.formulaire_public.service.NotifyService;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.notification.TimelineHelper;

import static fr.openent.form.core.constants.Fields.*;

public class DefaultNotifyService implements NotifyService {
    private final TimelineHelper timelineHelper;

    public DefaultNotifyService(TimelineHelper timelineHelper){
        this.timelineHelper = timelineHelper;
    }

    @Override
    public void notifyResponse(HttpServerRequest request, JsonObject form, JsonArray managers) {
        JsonObject params = new JsonObject()
                .put(PARAM_FORM_URI, "/formulaire#/form/" + form.getInteger(ID) + "/edit")
                .put(PARAM_FORM_NAME, form.getString(TITLE))
                .put(PARAM_FORM_RESULTS_URI, "/formulaire#/form/" + form.getInteger(ID) + "/results/1")
                .put(PARAM_PUSH_NOTIF, new JsonObject().put(TITLE, "push.notif.formulaire.public.response").put(BODY, ""));

        timelineHelper.notifyTimeline(request, "formulaire-public.response_notification", null, managers.getList(), params);
    }
}
