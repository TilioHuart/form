package fr.openent.formulaire_public.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

public interface CaptchaService {
    /**
     * Get captcha by id
     * @param captchaId captcha identifier
     * @param handler function handler returning JsonObject data
     */
    void get(String captchaId, Handler<Either<String, JsonObject>> handler);
}