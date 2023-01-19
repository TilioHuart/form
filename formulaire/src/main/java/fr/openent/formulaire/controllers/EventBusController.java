package fr.openent.formulaire.controllers;

import fr.openent.form.helpers.BusResultHelper;
import fr.openent.formulaire.service.QuestionChoiceService;
import fr.openent.formulaire.service.QuestionService;
import fr.openent.formulaire.service.QuestionSpecificFieldService;
import fr.openent.formulaire.service.SectionService;
import fr.openent.formulaire.service.impl.DefaultQuestionChoiceService;
import fr.openent.formulaire.service.impl.DefaultQuestionService;
import fr.openent.formulaire.service.impl.DefaultQuestionSpecificFieldService;
import fr.openent.formulaire.service.impl.DefaultSectionService;
import fr.wseduc.bus.BusAddress;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;

import static fr.openent.form.core.constants.EbFields.*;
import static fr.openent.form.core.constants.EbFields.ACTION;
import static fr.openent.form.core.constants.Fields.*;

public class EventBusController extends ControllerHelper {
    private final SectionService sectionService = new DefaultSectionService();
    private final QuestionService questionService = new DefaultQuestionService();
    private final QuestionChoiceService questionChoiceService = new DefaultQuestionChoiceService();
    private final QuestionSpecificFieldService questionSpecificFieldService = new DefaultQuestionSpecificFieldService();

    @BusAddress(FORMULAIRE_ADDRESS)
    public void bus(final Message<JsonObject> message) {
        JsonObject body = message.body();
        String action = body.getString(ACTION);
        switch (action) {
            case LIST_SECTIONS:
                String formId = body.getString(PARAM_FORM_ID);
                sectionService.list(formId, BusResultHelper.busResponseHandlerEitherArray(message));
                break;
            case LIST_QUESTION_FOR_FORM_AND_SECTION:
                formId = body.getString(PARAM_FORM_ID);
                questionService.listForFormAndSection(formId)
                        .onSuccess(listQuestionsEvt -> {
                            BusResultHelper.busArrayHandler(questionSpecificFieldService.syncQuestionSpecs(listQuestionsEvt), message);
                        })
                        .onFailure(error -> {
                            String errMessage = String.format("[Formulaire@%s::bus]:  " +
                                            "an error has occurred while getting list question event: %s",
                                    this.getClass().getSimpleName(), error.getMessage());
                            log.error(errMessage);
                            message.reply((new JsonObject()).put(STATUS, ERROR).put(MESSAGE, error.getMessage()));
                        });
                break;
            case LIST_QUESTION_CHILDREN:
                JsonArray questionIds = body.getJsonArray(PARAM_QUESTION_IDS);
                questionService.listChildren(questionIds, BusResultHelper.busResponseHandlerEitherArray(message));
                break;
            case LIST_QUESTION_CHOICES:
                questionIds = body.getJsonArray(PARAM_QUESTION_IDS);
                questionChoiceService.listChoices(questionIds, BusResultHelper.busResponseHandlerEitherArray(message));
                break;
            default:
                message.reply(new JsonObject()
                        .put(STATUS, ERROR)
                        .put(MESSAGE, "Invalid action."));
        }
    }
}
