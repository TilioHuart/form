package fr.openent.formulaire.service.impl;

import fr.openent.formulaire.service.NotifyService;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import org.entcore.common.notification.TimelineHelper;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.user.UserUtils;

public class DefaultNotifyService implements NotifyService {
    private final static Logger log = LoggerFactory.getLogger(DefaultNotifyService.class);

    private final TimelineHelper timelineHelper;
    private final EventBus eb;


    public DefaultNotifyService(TimelineHelper timelineHelper, EventBus eb){
        this.timelineHelper = timelineHelper;
        this.eb = eb;
    }

    @Override
    public void notifyNewForm(HttpServerRequest request, JsonObject form, JsonArray responders) {
        UserUtils.getUserInfos(eb, request, user -> {
            if (user == null) {
                log.error("User not found in session.");
                Renders.unauthorized(request);
                return;
            }

            JsonObject params = new JsonObject()
                    .put("userUri", "/userbook/annuaire#" + user.getUserId())
                    .put("username", user.getUsername())
                    .put("formUri", "/formulaire#/form/" + form.getInteger("id") + "/new")
                    .put("formName", form.getString("title"))
                    .put("pushNotif", new JsonObject().put("title", "push.notif.formulaire.newForm").put("body", ""));

            timelineHelper.notifyTimeline(request, "formulaire.new_form_notification", user, responders.getList(), params);
        });
    }

    @Override
    public void notifyResponse(HttpServerRequest request, JsonObject form, JsonArray managers) {
        UserUtils.getUserInfos(eb, request, user -> {
            if (user == null) {
                log.error("User not found in session.");
                Renders.unauthorized(request);
                return;
            }

            JsonObject params = new JsonObject()
                    .put("anonymous", form.getBoolean("anonymous"))
                    .put("userUri", "/userbook/annuaire#" + user.getUserId())
                    .put("username", user.getUsername())
                    .put("formUri", "/formulaire#/form/" + form.getInteger("id") + "/edit")
                    .put("formName", form.getString("title"))
                    .put("formResultsUri", "/formulaire#/form/" + form.getInteger("id") + "/results/1")
                    .put("pushNotif", new JsonObject().put("title", "push.notif.formulaire.response").put("body", ""));

            timelineHelper.notifyTimeline(request, "formulaire.response_notification", user, managers.getList(), params);
        });
    }
}