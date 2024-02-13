package fr.openent.formulaire.service;

import fr.openent.form.core.models.FormShare;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import org.entcore.common.user.UserInfos;

import java.util.List;

public interface FormSharesService {
    void getSharedWithMe(String formId, UserInfos user, Handler<Either<String, JsonArray>> handler);

    /**
     * Delete all sharing methods matching the formId and the rights
     * @param formId        Form identifier
     * @param rightMethods  List of methods to delete form the table
     */
    Future<List<FormShare>> deleteForFormAndRight(Number formId, List<String> rightMethods);
}