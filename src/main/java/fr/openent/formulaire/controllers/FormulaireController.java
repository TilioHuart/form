package fr.openent.formulaire.controllers;

import fr.openent.formulaire.Formulaire;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import org.vertx.java.core.http.RouteMatcher;

import java.util.Map;

public class FormulaireController extends ControllerHelper {
    private static final Logger log = LoggerFactory.getLogger(FormulaireController.class);
    private EventStore eventStore;

    public FormulaireController(EventStore eventStore) {
        super();
        this.eventStore = eventStore;
    }

    @Override
    public void init(Vertx vertx, JsonObject config, RouteMatcher rm, Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
        super.init(vertx, config, rm, securedActions);
    }

    @Get("")
    @ApiDoc("Render view")
    @SecuredAction(Formulaire.ACCESS_RIGHT)
    public void render(HttpServerRequest request) {
        renderView(request);
        eventStore.createAndStoreEvent(Formulaire.FormulaireEvent.ACCESS.name(), request);
    }
}