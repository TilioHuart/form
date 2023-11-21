package fr.openent.formulaire.controllers;

import fr.openent.form.core.enums.I18nKeys;
import fr.openent.form.core.models.Distribution;
import fr.openent.form.core.models.Response;
import fr.openent.form.core.models.ResponseFile;
import fr.openent.form.helpers.I18nHelper;
import fr.openent.form.helpers.StorageHelper;
import fr.openent.formulaire.helpers.folder_exporter.FolderExporterZip;
import fr.openent.formulaire.security.CustomShareAndOwner;
import fr.openent.formulaire.security.ResponseRight;
import fr.openent.formulaire.service.DistributionService;
import fr.openent.formulaire.service.ResponseFileService;
import fr.openent.formulaire.service.ResponseService;
import fr.openent.formulaire.service.impl.DefaultDistributionService;
import fr.openent.formulaire.service.impl.DefaultResponseFileService;
import fr.openent.formulaire.service.impl.DefaultResponseService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static fr.openent.form.core.constants.Fields.*;
import static fr.openent.form.core.constants.ShareRights.*;
import static fr.openent.form.helpers.RenderHelper.renderInternalError;
import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class ResponseFileController extends ControllerHelper {
    private static final Logger log = LoggerFactory.getLogger(ResponseFileController.class);
    private final ResponseFileService responseFileService;
    private final ResponseService responseService;
    private final DistributionService distributionService;
    private final Storage storage;
    private final SimpleDateFormat dateGetter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("ddMMyyyy_HH'h'mm_");

    public ResponseFileController(Storage storage) {
        super();
        this.responseFileService = new DefaultResponseFileService();
        this.responseService = new DefaultResponseService();
        this.distributionService = new DefaultDistributionService();
        this.storage = storage;
        dateFormatter.setTimeZone(TimeZone.getTimeZone("Europe/Paris")); // TODO to adapt for not France timezone
    }

    @Get("/responses/:responseId/files/all")
    @ApiDoc("List all files of a specific response")
    @ResourceFilter(CustomShareAndOwner.class)
    @SecuredAction(value = READ_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void list(HttpServerRequest request) {
        String responseId = request.getParam(PARAM_RESPONSE_ID);
        responseFileService.list(responseId, arrayResponseHandler(request));
    }

    @Get("/questions/:questionId/files/all")
    @ApiDoc("List all files of all responses to a specific question")
    @ResourceFilter(CustomShareAndOwner.class)
    @SecuredAction(value = READ_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void listByQuestion(HttpServerRequest request) {
        String questionId = request.getParam(PARAM_QUESTION_ID);
        responseFileService.listByQuestion(questionId, arrayResponseHandler(request));
    }

    @Get("/responses/files/:fileId")
    @ApiDoc("Get a specific file by id")
    @ResourceFilter(CustomShareAndOwner.class)
    @SecuredAction(value = CONTRIB_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void get(HttpServerRequest request) {
        String fileId = request.getParam(PARAM_FILE_ID);
        responseFileService.get(fileId, defaultResponseHandler(request));
    }

    @Get("/responses/files/:fileId/download")
    @ApiDoc("Download a specific file")
    @ResourceFilter(CustomShareAndOwner.class)
    @SecuredAction(value = CONTRIB_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void download(HttpServerRequest request) {
        String fileId = request.getParam(PARAM_FILE_ID);
        responseFileService.getFinished(fileId)
            .onSuccess(responseFile -> {
                if (!responseFile.isPresent()) {
                    String errMessage = "[Formulaire@ResponseFileController::download] No response file found for id " + fileId;
                    log.error(errMessage);
                    renderError(request);
                    return;
                }

                storage.sendFile(responseFile.get().getId(), responseFile.get().getFilename(), request, false, new JsonObject());
            })
            .onFailure(err -> {
                String errMessage = "[Formulaire@ResponseFileController::download] Failed to get response file for id " + fileId + " : ";
                log.error(errMessage + err.getMessage());
                renderError(request);
            });
    }

    @Get("/responses/:questionId/files/download/zip")
    @ApiDoc("Download all files of a specific question")
    @ResourceFilter(CustomShareAndOwner.class)
    @SecuredAction(value = CONTRIB_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void zipAndDownload(HttpServerRequest request) {
        String questionId = request.getParam(PARAM_QUESTION_ID);
        responseFileService.listByQuestion(questionId, responseFilesEvt -> {
            if (responseFilesEvt.isLeft()) {
                log.error("[Formulaire@zipAndDownload] Error in getting responseFiles for question with id : " + questionId);
                renderInternalError(request, responseFilesEvt);
                return;
            }
            if (responseFilesEvt.right().getValue().isEmpty()) {
                String message = "[Formulaire@zipAndDownload] No response files found for question with id " + questionId;
                log.error(message);
                notFound(request, message);
                return;
            }

            List<JsonObject> listFiles = responseFilesEvt.right().getValue().getList();
            JsonObject root = new JsonObject()
                    .put(ID, UUID.randomUUID().toString())
                    .put(TYPE, FOLDER)
                    .put(NAME, I18nHelper.getI18nValue(I18nKeys.ARCHIVE_ZIP_NAME, request))
                    .put(FOLDERS, new JsonArray());
            JsonObject groupFiles = new JsonObject();

            // Adapt properties for exportAndSendZip functions
            List<JsonObject> listFilesValid = new ArrayList<>();
            for (JsonObject file : listFiles) {
                if (file.getString(DATE_RESPONSE) != null) {
                    String displayDate = "";
                    try {
                        displayDate = dateFormatter.format(dateGetter.parse(file.getString(DATE_RESPONSE)));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                    file.put(NAME, displayDate + file.getString(FILENAME));
                    file.put(FILE, file.getString(ID));
                    file.put(TYPE, FILE);

                    // Sort files by response_id in the groupFiles array (to add folder parents after)
                    String responseId = file.getInteger(RESPONSE_ID).toString();
                    if (!groupFiles.containsKey(responseId)) {
                        groupFiles.put(responseId, new JsonArray());
                    }
                    groupFiles.getJsonArray(responseId).add(file);
                    listFilesValid.add(file);
                }
            }

            // Folder management
            for (String key : groupFiles.getMap().keySet()) {
                JsonArray groupFile = groupFiles.getJsonArray(key);
                int nbFilesInGroup = groupFile.size();
                if (nbFilesInGroup > 1) {
                    JsonObject folder = new JsonObject()
                            .put(ID, UUID.randomUUID().toString())
                            .put(TYPE, FOLDER)
                            .put(PARENT, root.getString(ID))
                            .put(NAME, getFolderName(groupFile.getJsonObject(0)));
                    root.getJsonArray(FOLDERS).add(folder);
                    listFilesValid.add(folder);

                    for (int i = 0; i < nbFilesInGroup; i++) {
                        groupFile.getJsonObject(i).put(PARENT, folder.getString(ID));
                    }
                }
            }

            // Export all files of the 'listFiles' in a folder defined by the 'root' object
            FolderExporterZip zipBuilder = new FolderExporterZip(storage, vertx.fileSystem(), false);
            zipBuilder.exportAndSendZip(root, listFilesValid, request,false).onComplete(zipEvt -> {
                if (zipEvt.failed()) {
                    log.error("[Formulaire@zipAndDownload] Fail to zip and export files");
                    renderInternalError(request, zipEvt.cause().getMessage());
                    return;
                }

                log.info("Zip folder downloaded !");
                ok(request);
            });
        });
    }

    @Post("/responses/:responseId/files")
    @ApiDoc("Upload files of a specific response")
    @ResourceFilter(CustomShareAndOwner.class)
    @SecuredAction(value = RESPONDER_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void upload(HttpServerRequest request) {
        storage.writeUploadFile(request, entries -> {
            if (!OK.equals(entries.getString(STATUS))) {
                log.error("[Formulaire@uploadFile] Fail to create file in storage");
                renderInternalError(request, entries.getString(MESSAGE));
                return;
            }

            try {
                String responseId = request.getParam(PARAM_RESPONSE_ID);
                String fileId = entries.getString(_ID);
                String name = entries.getJsonObject(METADATA).getString(FILENAME);
                String type = entries.getJsonObject(METADATA).getString(CONTENT_TYPE);

                responseFileService.create(responseId, fileId, name, type, createEvt -> {
                    if (createEvt.isLeft()) {
                        log.error("[Formulaire@uploadFile] Fail to create in database file " + fileId);

                        deleteFiles(storage, new JsonArray().add(fileId), deleteFilesEvt -> {
                            if (deleteFilesEvt.isLeft()) {
                                log.error("[Formulaire@uploadFile] Fail to delete files in storage");
                                renderInternalError(request, deleteFilesEvt);
                                return;
                            }

                            renderInternalError(request, createEvt);
                        });
                    }

                    JsonObject response = new JsonObject().put(ID, fileId).put(FILENAME, name);
                    request.response().setStatusCode(201).putHeader("Content-Type", type).end(response.toString());
                });
            }
            catch (NumberFormatException e) {
                renderInternalError(request, e.getMessage());
            }
        });
    }

    @Delete("/responses/:responseId/files")
    @ApiDoc("Delete all files of a specific response")
    @ResourceFilter(CustomShareAndOwner.class)
    @SecuredAction(value = RESPONDER_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void deleteAll(HttpServerRequest request) {
        String responseId = request.getParam(PARAM_RESPONSE_ID);
        List<String> responseIds = new ArrayList<>();
        responseIds.add(responseId);
        deleteAllByResponse(request, responseIds);
    }

    @Delete("/responses/files")
    @ApiDoc("Delete all files of a specific response")
    @ResourceFilter(ResponseRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void deleteAllMultiple(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            RequestUtils.bodyToJsonArray(request, responseIdsJson -> {
                if (responseIdsJson == null || responseIdsJson.isEmpty()) {
                    log.error("[Formulaire@ResponseFileController::deleteAllMultiple] No responseFile to delete.");
                    noContent(request);
                    return;
                }

                List<String> responseIds = responseIdsJson.stream().map(String.class::cast).collect(Collectors.toList());
                List<Long> distributionIds = new ArrayList<>();

                // Check rights for this responses
                distributionService.listByResponder(user)
                    .compose(distributions -> {
                        distributionIds.addAll(distributions.stream().map(Distribution::getId).collect(Collectors.toList()));
                        return responseService.listByIds(responseIds);
                    })
                    .onSuccess(responses -> {
                        List<Long> responsesDistributionIds = responses.stream().map(Response::getDistributionId).collect(Collectors.toList());
                        responsesDistributionIds.retainAll(distributionIds);

                        if (responsesDistributionIds.size() > 0) {
                            log.error("[Formulaire@ResponseFileController::deleteAllByResponse] " + user.getUsername() + " does not have right for all the responses with id " + responseIds);
                            unauthorized(request);
                            return;
                        }

                        deleteAllByResponse(request, responseIds);
                    })
                    .onFailure(err -> {
                        log.error("[Formulaire@ResponseFileController::deleteAllByResponse] An error occurred while checking " +
                                "rights for user " + user.getUsername() + " : " + err.getMessage());
                        renderError(request);
                    });
            });
        });
    }

    private void deleteAllByResponse(HttpServerRequest request, List<String> responseIds) {
        responseFileService.deleteAllByResponse(responseIds)
            .compose(deletedResponseFiles -> {
                if (!deletedResponseFiles.isEmpty()) {
                    List<String> fileIds = deletedResponseFiles.stream()
                            .map(ResponseFile::getId)
                            .map(String.class::cast)
                            .collect(Collectors.toList());
                    return StorageHelper.removeFiles(storage, fileIds);
                }
                return Future.succeededFuture();
            })
            .onSuccess(result -> ok(request))
            .onFailure(err -> {
                log.error("[Formulaire@ResponseFileController::deleteAllByResponse] An error occurred while deleting " +
                        "response files with ids " + responseIds + " and their associated files : " + err.getMessage());
                renderError(request);
            });
    }

    /**
     * @deprecated Should use StorageHelper.removeFiles(Storage storage, List<String> fileIds) instead
     */
    @Deprecated
    public static void deleteFiles(Storage storage, JsonArray fileIds, Handler<Either<String, JsonObject>> handler) {
        storage.removeFiles(fileIds, deleteFilesEvt -> {
            if (!OK.equals(deleteFilesEvt.getString(STATUS))) {
                log.error("[Formulaire@deleteFiles] An error occurred while removing files " + fileIds);
                handler.handle(new Either.Left<>(deleteFilesEvt.getString(MESSAGE)));
            }
            else {
                handler.handle(new Either.Right<>(new JsonObject()));
            }
        });
    }

    private String getFolderName(JsonObject file) {
        String completeName = file.getString(NAME);
        String filename = file.getString(FILENAME);
        int indexOfUnderscore = filename.contains("_") ? filename.indexOf("_") : 0;

        // Check if responder_name and name get from filename are equals
        char[] first = String.join("", file.getString(RESPONDER_NAME).split(" ")).toCharArray();
        char[] second = filename.substring(0, indexOfUnderscore).toCharArray();
        Arrays.sort(first);
        Arrays.sort(second);
        boolean isAnonymous = !Arrays.equals(first, second);

        return isAnonymous ? completeName.substring(0, 14) : completeName.substring(0, 15) + file.getString(RESPONDER_NAME);
    }
}