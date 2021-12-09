package fr.openent.formulaire.cron;

import fr.openent.formulaire.controllers.ResponseFileController;
import fr.openent.formulaire.helpers.UtilsHelper;
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
        log.info("[Formulaire@RgpdCron] Formulaire RGPD cron started");
        deleteOldDataForRgpd(deleteEvent -> {
            if (deleteEvent.isLeft()) {
                log.info("[Formulaire@RgpdCron] RGPD cron failed");
            }
            else {
                log.info("[Formulaire@RgpdCron] RGPD cron launch successful");
            }
        });
    }

    public void deleteOldDataForRgpd(Handler<Either<String, JsonObject>> handler) {
        distributionService.deleteOldDistributions(deleteDistribsEvent -> {
            if (deleteDistribsEvent.isLeft()) {
                log.error("[Formulaire@deleteOldDataForRgpd] An error occurred while deleting distributions for old responses");
                handler.handle(new Either.Left<>(deleteDistribsEvent.left().getValue()));
                return;
            }

            JsonArray deletedDistrib = deleteDistribsEvent.right().getValue();
            if (deletedDistrib.isEmpty()) {
                handler.handle(new Either.Right<>(new JsonObject()));
                return;
            }

            JsonArray deletedDistribIds = UtilsHelper.getIds(deletedDistrib);
            responseService.deleteOldResponse(deletedDistribIds, deleteResponseEvent -> {
                if (deleteResponseEvent.isLeft()) {
                    log.error("[Formulaire@deleteOldDataForRgpd] Failed to delete old responses for RGPD forms");
                    handler.handle(new Either.Left<>(deleteResponseEvent.left().getValue()));
                    return;
                }

                logDeletedDistribInfos(deletedDistrib);

                if (deleteResponseEvent.right().getValue().isEmpty()) {
                    handler.handle(new Either.Right<>(new JsonObject()));
                    return;
                }

                JsonArray deletedRepIds = UtilsHelper.getIds(deleteResponseEvent.right().getValue());
                responseFileService.deleteAllByResponse(deletedRepIds, deleteFilesEvent -> {
                    if (deleteFilesEvent.isLeft()) {
                        log.error("[Formulaire@deleteOldDataForRgpd] An error occurred while deleting files for responses " + deletedRepIds);
                        handler.handle(new Either.Left<>(deleteFilesEvent.left().getValue()));
                        return;
                    }

                    JsonArray deletedFiles = deleteFilesEvent.right().getValue();
                    if (!deletedFiles.isEmpty()) {
                        ResponseFileController.deleteFiles(storage, deletedFiles, handler);
                    }
                });
            });
        });
    }

    private void logDeletedDistribInfos(JsonArray deletedDistribs) {
        JsonArray distribInfos = UtilsHelper.mapByProps(deletedDistribs, new JsonArray().add("form_id").add("responder_id"));
        log.info("[Formulaire@deleteOldDataForRgpd] Distributions and response successfully deleted for : " + distribInfos);
    }
}
