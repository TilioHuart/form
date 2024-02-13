package fr.openent.formulaire.controllers;

import fr.openent.form.helpers.RightsHelper;
import fr.openent.form.helpers.UtilsHelper;
import fr.openent.formulaire.service.MonitoringService;
import fr.openent.formulaire.service.impl.DefaultMonitoringService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.SuperAdminFilter;

import static fr.openent.form.core.constants.Fields.*;
import static fr.openent.form.core.constants.ShareRights.*;
import static fr.openent.form.core.constants.Tables.DB_SCHEMA;

public class MonitoringController extends ControllerHelper {
    private static final Logger log = LoggerFactory.getLogger(MonitoringController.class);
    private final MonitoringService monitoringService;


    public MonitoringController() {
        super();
        this.monitoringService = new DefaultMonitoringService();

    }

    @Get("/positions/duplicates")
    @ApiDoc("Get questions or sections with duplicated position")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SuperAdminFilter.class)
    public void getPositionDuplicates(final HttpServerRequest request) {
        monitoringService.getFormIdsWithPositionDuplicates()
            .compose(formIdsInfos -> {
                JsonArray formIds = UtilsHelper.getByProp(formIdsInfos, FORM_ID);
                return monitoringService.getPositionDuplicates(formIds);
            })
            .onSuccess(result -> {
                if (result.isEmpty()) {
                    String message = "No duplicates found.";
                    result.add(new JsonObject().put(MESSAGE, message));
                }
                renderJson(request, result);
            })
            .onFailure(err -> {
                log.error(err.getMessage());
                renderError(request);
            });
    }

    @Get("/positions/duplicates/clean")
    @ApiDoc("Reset duplicated position according to their id")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SuperAdminFilter.class)
    public void cleanPositionDuplicates(final HttpServerRequest request) {
        monitoringService.getFormIdsWithPositionDuplicates()
            .compose(formIdsInfos -> {
                JsonArray formIds = UtilsHelper.getByProp(formIdsInfos, FORM_ID);
                return monitoringService.cleanPositionDuplicates(formIds);
            })
            .onSuccess(result -> {
                if (result.isEmpty()) {
                    String message = "No duplicates found to clean.";
                    renderJson(request,new JsonObject().put(MESSAGE, message));
                    return;
                }
                renderJson(request, result);
            })
            .onFailure(err -> {
                log.error("[Formulaire@MonitoringController::cleanPositionDuplicates] Fail to clean duplicates : " + err.getMessage());
                renderError(request);
            });
    }

    @Get("/scripts")
    @ApiDoc("Get all scripts")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SuperAdminFilter.class)
    public void getAllScripts(HttpServerRequest request) {
        monitoringService.getScripts()
                .onSuccess(result -> {
                    if (result.isEmpty()) {
                        String message = "No script found.";
                        renderJson(request, new JsonObject().put(MESSAGE, message));
                        return;
                    }
                    renderJson(request, result);
                })
                .onFailure(err -> {
                    log.error("[Formulaire@MonitoringController::getAllScripts] Fail to get scripts information : " + err.getMessage());
                    renderError(request);
                });
    }

    @Get("/rights/:right")
    @ApiDoc("Get all methods using giving right")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SuperAdminFilter.class)
    public void getRightMethods(HttpServerRequest request) {
        String right = DB_SCHEMA + "." + request.getParam("right");
        JsonObject finalResult = new JsonObject();

        switch (right) {
            case MANAGER_RESOURCE_RIGHT:
                finalResult.put(MANAGER_RESOURCE_RIGHT, RightsHelper.getRightMethods(MANAGER_RESOURCE_RIGHT, securedActions));
            case CONTRIB_RESOURCE_RIGHT:
                finalResult.put(CONTRIB_RESOURCE_RIGHT, RightsHelper.getRightMethods(CONTRIB_RESOURCE_RIGHT, securedActions));
                finalResult.put(READ_RESOURCE_RIGHT, RightsHelper.getRightMethods(READ_RESOURCE_RIGHT, securedActions));
                render(request, finalResult);
                break;
            case RESPONDER_RESOURCE_RIGHT:
                finalResult.put(RESPONDER_RESOURCE_RIGHT, RightsHelper.getRightMethods(RESPONDER_RESOURCE_RIGHT, securedActions));
            case READ_RESOURCE_RIGHT:
                finalResult.put(READ_RESOURCE_RIGHT, RightsHelper.getRightMethods(READ_RESOURCE_RIGHT, securedActions));
                render(request, finalResult);
                break;
            default:
                finalResult.put(ERROR, "The given right is not known.");
                renderError(request);
                break;
        }
    }


}