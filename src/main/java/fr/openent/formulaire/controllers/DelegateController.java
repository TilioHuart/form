package fr.openent.formulaire.controllers;

import fr.openent.formulaire.Formulaire;
import fr.openent.formulaire.security.AccessRight;
import fr.openent.formulaire.security.RgpdRight;
import fr.openent.formulaire.service.DelegateService;
import fr.openent.formulaire.service.impl.DefaultDelegateService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;

public class DelegateController extends ControllerHelper {
    private static final Logger log = LoggerFactory.getLogger(DelegateController.class);
    private final DelegateService delegateService;

    public DelegateController() {
        super();
        this.delegateService = new DefaultDelegateService();
    }

    // Init classic rights

    @SecuredAction(Formulaire.RGPD_RIGHT)
    public void initRGPDRight(final HttpServerRequest request) {
    }

    @Get("/delegates")
    @ApiDoc("List all delegates of the platform")
    @ResourceFilter(RgpdRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void list(HttpServerRequest request) {
        delegateService.list(arrayResponseHandler(request));
    }
}