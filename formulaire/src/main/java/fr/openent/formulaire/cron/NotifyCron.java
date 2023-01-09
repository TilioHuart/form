package fr.openent.formulaire.cron;

import fr.openent.form.helpers.UtilsHelper;
import fr.openent.formulaire.service.*;
import fr.openent.formulaire.service.impl.*;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.notification.TimelineHelper;
import static fr.openent.form.core.constants.Fields.*;

public class NotifyCron extends ControllerHelper implements Handler<Long> {
    private static final Logger log = LoggerFactory.getLogger(NotifyCron.class);
    private final FormService formService;
    private final DistributionService distributionService;
    private final NotifyService notifyService;

    public NotifyCron(TimelineHelper timelineHelper) {
        this.formService = new DefaultFormService();
        this.distributionService = new DefaultDistributionService();
        this.notifyService = new DefaultNotifyService(timelineHelper, eb);
    }

    @Override
    public void handle(Long event) {
        log.info("[Formulaire@NotifyCron::handle] Formulaire Notify cron started");
        launchNotifications(notificationsEvt -> {
            if (notificationsEvt.isLeft()) {
                log.error("[Formulaire@NotifyCron::handle] Notify cron failed");
            }
            else {
                log.info("[Formulaire@NotifyCron::handle] Notify cron launch successful");
            }
        });
    }

    public void launchNotifications(Handler<Either<String, JsonObject>> handler) {
        JsonObject composeInfos = new JsonObject();
        formService.listSentFormsOpeningToday()
            .compose(forms -> {
                composeInfos.put(FORMS, forms);
                JsonArray formIds = UtilsHelper.getIds(forms);
                return distributionService.listByForms(formIds);
            })
            .onSuccess(distributions -> {
                JsonArray respondersIds = UtilsHelper.getByProp(distributions, RESPONDER_ID);
                composeInfos.getJsonArray(FORMS).stream().forEach(form -> notifyService.notifyNewFormFromCRON((JsonObject)form, respondersIds));
                handler.handle(new Either.Right<>(new JsonObject()));
            })
            .onFailure(err -> {
                log.error("[Formulaire@NotifyCron::launchNotifications] Failed to send notifications to forms opening today.");
                handler.handle(new Either.Left<>(err.getMessage()));
            });
    }
}
