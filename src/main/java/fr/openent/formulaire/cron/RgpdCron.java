package fr.openent.formulaire.cron;

import fr.openent.formulaire.controllers.ResponseFileController;
import fr.openent.formulaire.service.ResponseFileService;
import fr.openent.formulaire.service.ResponseService;
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
    private final ResponseService responseService;
    private final ResponseFileService responseFileService;

    public RgpdCron(Storage storage) {
        this.storage = storage;
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
        responseService.deleteOldResponse(deleteResponseEvent -> {
            if (deleteResponseEvent.isLeft()) {
                log.error("[Formulaire@deleteOldDataForRgpd] Failed to delete old responses for RGPD forms");
                return;
            }

            JsonArray deletedRepIds = deleteResponseEvent.right().getValue();

            if (deletedRepIds.size() <= 0) {
                handler.handle(new Either.Right<>(new JsonObject()));
                return;
            }

            responseFileService.deleteAllByResponse(deletedRepIds, deleteFilesEvent -> {
                if (deleteFilesEvent.isLeft()) {
                    log.error("[Formulaire@deleteOldDataForRgpd] An error occurred while deleting files for responses " + deletedRepIds);
                }

                JsonArray deletedFiles = deleteFilesEvent.right().getValue();
                if (!deletedFiles.isEmpty()) {
                    ResponseFileController.deleteFiles(storage, deletedFiles, handler);
                }
            });
        });
    }
}
