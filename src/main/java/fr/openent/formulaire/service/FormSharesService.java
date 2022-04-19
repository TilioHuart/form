package fr.openent.formulaire.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import org.entcore.common.user.UserInfos;

public interface FormSharesService {
    void getSharedWithMe(String formId, UserInfos user, Handler<Either<String, JsonArray>> handler);
}