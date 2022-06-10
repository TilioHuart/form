package fr.openent.formulaire_public.controllers;

import fr.openent.formulaire_public.service.CaptchaService;
import fr.openent.formulaire_public.service.DistributionService;
import fr.openent.formulaire_public.service.impl.DefaultCaptchaService;
import fr.openent.formulaire_public.service.impl.DefaultDistributionService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.controller.ControllerHelper;

import static fr.openent.form.helpers.RenderHelper.renderInternalError;

public class CaptchaController extends ControllerHelper {
    private static final Logger log = LoggerFactory.getLogger(CaptchaController.class);
    private final CaptchaService publicCaptchaService;
    private final DistributionService publicDistributionService;

    public CaptchaController() {
        super();
        this.publicCaptchaService = new DefaultCaptchaService();
        this.publicDistributionService = new DefaultDistributionService();
    }

    @Get("/captcha/:distributionKey")
    @ApiDoc("Get a captcha by id or send a new one")
    public void get(HttpServerRequest request) {
        String distributionKey = request.getParam("distributionKey");
        String captchaId = request.getParam("captcha_id");

        if (distributionKey == null || distributionKey.isEmpty()) {
            String message = "[FormulairePublic@getCaptcha] DistributionKey must be not null.";
            log.error(message);
            badRequest(request, message);
            return;
        }

        if (captchaId != null && !captchaId.isEmpty() && !captchaId.equals("undefined")) {
            publicCaptchaService.get(captchaId, captchaHandler(request, captchaId));
        }
        else {
            publicDistributionService.updateCaptchaDistribution(distributionKey, distributionEvt -> {
                if (distributionEvt.isLeft()) {
                    log.error("[FormulairePublic@getCaptcha] Fail to generate new captcha for distribution with key : " + distributionKey);
                    renderInternalError(request, distributionEvt);
                    return;
                }

                String newCaptchaId = distributionEvt.right().getValue().getInteger("captcha_id").toString();
                publicCaptchaService.get(newCaptchaId, captchaHandler(request, newCaptchaId));
            });
        }
    }

    private Handler<Either<String, JsonObject>> captchaHandler(HttpServerRequest request, String captchaId) {
        return captchaEvt -> {
            if (captchaEvt.isLeft()) {
                log.error("[FormulairePublic@getCaptcha] Fail to get captcha with id : " + captchaId);
                renderInternalError(request, captchaEvt);
                return;
            }

            JsonObject captcha = new JsonObject()
                    .put("captcha_id", captchaEvt.right().getValue().getInteger("id"))
                    .put("title", captchaEvt.right().getValue().getString("question"))
                    .put("question_type", 2);
            renderJson(request, captcha);
        };
    }
}