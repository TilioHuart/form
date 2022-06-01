package fr.openent.formulaire.controllers;

import fr.openent.formulaire.service.DelegateService;
import fr.openent.formulaire.service.impl.DefaultDelegateService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.controller.ControllerHelper;

import static fr.openent.form.core.constants.ConsoleRights.RGPD_RIGHT;
import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;

public class DelegateController extends ControllerHelper {
    private static final Logger log = LoggerFactory.getLogger(DelegateController.class);
    private final DelegateService delegateService;

    public DelegateController() {
        super();
        this.delegateService = new DefaultDelegateService();
    }

    // Init classic rights

    @SecuredAction(RGPD_RIGHT)
    public void initRGPDRight(final HttpServerRequest request) {
    }

    // API

    @Get("/delegates")
    @ApiDoc("List all delegates of the platform")
    public void list(HttpServerRequest request) {
        delegateService.list(arrayResponseHandler(request));
    }
}