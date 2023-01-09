package fr.openent.formulaire.cron;

import fr.openent.formulaire.controllers.ResponseFileController;
import fr.openent.formulaire.service.DistributionService;
import fr.openent.formulaire.service.ResponseFileService;
import fr.openent.formulaire.service.ResponseService;
import fr.openent.formulaire.service.impl.DefaultDistributionService;
import fr.openent.formulaire.service.impl.DefaultResponseFileService;
import fr.openent.formulaire.service.impl.DefaultResponseService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.storage.Storage;

import static fr.openent.form.core.constants.Fields.FORM_ID;
import static fr.openent.form.core.constants.Fields.RESPONDER_ID;
import static fr.openent.form.helpers.UtilsHelper.getIds;
import static fr.openent.form.helpers.UtilsHelper.mapByProps;

public class RgpdCron extends ControllerHelper implements Handler<Long> {
    private static final Logger log = LoggerFactory.getLogger(RgpdCron.class);
    private final Storage storage;
    private final DistributionService distributionService;
    private final ResponseService responseService;
    private final ResponseFileService responseFileService;

    public RgpdCron(Storage storage) {
        this.storage = storage;
        this.distributionService = new DefaultDistributionService();
        this.responseService = new DefaultResponseService();
        this.responseFileService = new DefaultResponseFileService();
    }

    @Override
    public void handle(Long event) {
        log.info("[Formulaire@RgpdCron::handle] Formulaire RGPD cron started");
        deleteOldDataForRgpd(deleteEvt -> {
            if (deleteEvt.isLeft()) {
                log.error("[Formulaire@RgpdCron::handle] RGPD cron failed");
            }
            else {
                log.info("[Formulaire@RgpdCron::handle] RGPD cron launch successful");
            }
        });
    }

    public void deleteOldDataForRgpd(Handler<Either<String, JsonObject>> handler) {
        distributionService.deleteOldDistributions(deleteDistribsEvt -> {
            if (deleteDistribsEvt.isLeft()) {
                log.error("[Formulaire@RgpdCron::deleteOldDataForRgpd] An error occurred while deleting distributions for old responses");
                handler.handle(new Either.Left<>(deleteDistribsEvt.left().getValue()));
                return;
            }

            JsonArray deletedDistrib = deleteDistribsEvt.right().getValue();
            if (deletedDistrib.isEmpty()) {
                handler.handle(new Either.Right<>(new JsonObject()));
                return;
            }

            JsonArray deletedDistribIds = getIds(deletedDistrib);
            responseService.deleteOldResponse(deletedDistribIds, deleteResponseEvt -> {
                if (deleteResponseEvt.isLeft()) {
                    log.error("[Formulaire@RgpdCron::deleteOldDataForRgpd] Failed to delete old responses for RGPD forms");
                    handler.handle(new Either.Left<>(deleteResponseEvt.left().getValue()));
                    return;
                }

                logDeletedDistribInfos(deletedDistrib);

                if (deleteResponseEvt.right().getValue().isEmpty()) {
                    handler.handle(new Either.Right<>(new JsonObject()));
                    return;
                }

                JsonArray deletedRepIds = getIds(deleteResponseEvt.right().getValue());
                responseFileService.deleteAllByResponse(deletedRepIds, deleteFilesEvt -> {
                    if (deleteFilesEvt.isLeft()) {
                        log.error("[Formulaire@RgpdCron::deleteOldDataForRgpd] An error occurred while deleting files for responses " + deletedRepIds);
                        handler.handle(new Either.Left<>(deleteFilesEvt.left().getValue()));
                        return;
                    }

                    JsonArray deletedFiles = deleteFilesEvt.right().getValue();
                    if (!deletedFiles.isEmpty()) {
                        ResponseFileController.deleteFiles(storage, deletedFiles, handler);
                    }
                });
            });
        });
    }

    private void logDeletedDistribInfos(JsonArray deletedDistribs) {
        JsonArray distribInfos = mapByProps(deletedDistribs, new JsonArray().add(FORM_ID).add(RESPONDER_ID));
        log.info("[Formulaire@RgpdCron::deleteOldDataForRgpd] Distributions and response successfully deleted for : " + distribInfos);
    }
}
