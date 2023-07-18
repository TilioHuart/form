package fr.openent.formulaire.service.impl;

import fr.openent.form.core.models.ShareMember;
import fr.openent.form.helpers.FutureHelper;
import fr.openent.formulaire.service.FormService;
import fr.wseduc.webutils.DefaultAsyncResult;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.storage.FileInfos;
import org.entcore.common.storage.StorageException;
import org.entcore.common.storage.impl.PostgresqlApplicationStorage;
import org.entcore.common.user.UserInfos;

import java.util.List;
import java.util.stream.Collectors;

import static fr.openent.form.core.constants.Constants.FORMULAIRE;
import static fr.openent.form.core.constants.Fields.*;
import static fr.openent.form.core.constants.Fields.BODY;
import static fr.openent.form.core.constants.Tables.*;

public class FormulaireApplicationStorage extends PostgresqlApplicationStorage {
    private static final Logger log = LoggerFactory.getLogger(FormulaireApplicationStorage.class);
    private final TimelineHelper timelineHelper;
    private final FormService formService;
    private final FileInfos fileInfos;

    public FormulaireApplicationStorage(TimelineHelper timelineHelper, EventBus eb) {
        super(DB_SCHEMA, RESPONSE_FILE_TABLE);
        this.timelineHelper = timelineHelper;
        this.formService = new DefaultFormService();
        this.fileInfos = new FileInfos();
    }

    @Override
    protected void getInfoProcess(final String fileId, final Handler<AsyncResult<FileInfos>> handler) {
        JsonObject promiseInfos = new JsonObject();
        getInfectedFileInfos(fileId)
            .compose(infos -> {
                fileInfos.setApplication(FORMULAIRE);
                fileInfos.setId(fileId);
                fileInfos.setName(infos.getString(FILENAME));
                fileInfos.setOwner(infos.getString(RESPONDER_ID));
                promiseInfos.put(RESPONDER_NAME, infos.getString(RESPONDER_NAME));
                promiseInfos.put(FORM_ID, infos.getLong(FORM_ID));

                return formService.get(infos.getLong(FORM_ID).toString());
            })
            .compose(form -> {
                if (!form.isPresent()) {
                    String errorMessage = " No form found for id " + promiseInfos.getLong(FORM_ID);
                    return Future.failedFuture(errorMessage);
                }
                promiseInfos.put(FORM, form.get().toJson());
                return formService.listContributors(promiseInfos.getLong(FORM_ID).toString());
            })
            .onSuccess(contributors -> {
                List<String> contributorsId = contributors.stream()
                        .map(ShareMember::getId)
                        .collect(Collectors.toList());
                sendNotification(fileInfos.getOwner(), promiseInfos.getString(RESPONDER_NAME), promiseInfos.getJsonObject(FORM), contributorsId);
                handler.handle(new DefaultAsyncResult<>(fileInfos));
            })
            .onFailure(error -> {
                log.error("[Formulaire@FormulaireApplicationStorage::getInfoProcess] " + error.getMessage());
                handler.handle(new DefaultAsyncResult<>(new StorageException(error.getMessage())));
            });
    }

    private Future<JsonObject> getInfectedFileInfos(String fileId) {
        Promise<JsonObject> promise = Promise.promise();

        String query = "SELECT rf.filename, d.responder_id, d.responder_name, d.form_id AS form_id " +
                "FROM " + RESPONSE_FILE_TABLE + " rf " +
                "INNER JOIN " + RESPONSE_TABLE + " r ON r.id = rf.response_id " +
                "INNER JOIN " + DISTRIBUTION_TABLE + " d ON d.id = r.distribution_id " +
                "WHERE rf.id = ?;";
        JsonArray params = new JsonArray().add(fileId);

        String errorMessage = "[Formulaire@FormulaireApplicationStorage::getInfectedFileInfos] Fail to get infos for infected file with id " + fileId + " : ";
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(FutureHelper.handlerEither(promise, errorMessage)));

        return promise.future();
    }


    private void sendNotification(String userId, String userName, JsonObject form, List<String> contributorsId) {
        UserInfos user = new UserInfos();
        user.setUserId(userId);
        user.setUsername(userName);
        String formResultsUri = "/formulaire#/form/" + form.getInteger(ID) + "/results/1";

        JsonObject params = new JsonObject()
                .put(ANONYMOUS, form.getBoolean(ANONYMOUS))
                .put(PARAM_USER_URI, "/userbook/annuaire#" + user.getUserId())
                .put(USERNAME, user.getUsername())
                .put(PARAM_FORM_URI, "/formulaire#/form/" + form.getInteger(ID) + "/edit")
                .put(PARAM_FORM_NAME, form.getString(TITLE))
                .put(PARAM_FORM_RESULTS_URI, formResultsUri)
                .put(FILENAME, fileInfos.getName())
                .put(PARAM_PUSH_NOTIF, new JsonObject().put(TITLE, "push.notif.formulaire.infectedFile").put(BODY, ""))
                .put(PARAM_RESOURCE_URI, formResultsUri);

        timelineHelper.notifyTimeline(null, "formulaire.infected_file_notification", user, contributorsId, params);
    }
}
