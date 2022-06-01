package fr.openent.formulaire.controllers;

import fr.openent.form.helpers.BusResultHelper;
import fr.openent.formulaire.service.QuestionChoiceService;
import fr.openent.formulaire.service.QuestionService;
import fr.openent.formulaire.service.SectionService;
import fr.openent.formulaire.service.impl.DefaultQuestionChoiceService;
import fr.openent.formulaire.service.impl.DefaultQuestionService;
import fr.openent.formulaire.service.impl.DefaultSectionService;
import fr.wseduc.bus.BusAddress;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;

public class EventBusController extends ControllerHelper {
    private SectionService sectionService = new DefaultSectionService();
    private QuestionService questionService = new DefaultQuestionService();
    private QuestionChoiceService questionChoiceService = new DefaultQuestionChoiceService();

    @BusAddress("fr.openent.formulaire")
    public void bus(final Message<JsonObject> message) {
        JsonObject body = message.body();
        String action = body.getString("action");
        switch (action) {
            case "list-sections":
                String formId = body.getString("formId");
                sectionService.list(formId, BusResultHelper.busResponseHandlerEitherArray(message));
                break;
            case "list-question-for-form-and-section":
                formId = body.getString("formId");
                questionService.listForFormAndSection(formId, BusResultHelper.busResponseHandlerEitherArray(message));
                break;
            case "list-question-choices":
                JsonArray questionIds = body.getJsonArray("questionIds");
                questionChoiceService.listChoices(questionIds, BusResultHelper.busResponseHandlerEitherArray(message));
                break;
            default:
                message.reply(new JsonObject()
                        .put("status", "error")
                        .put("message", "Invalid action."));
        }
    }
}
