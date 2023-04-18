package fr.openent.formulaire.controllers;

import fr.openent.form.core.models.Section;
import fr.openent.form.helpers.FutureHelper;
import fr.openent.form.helpers.UtilsHelper;
import fr.openent.formulaire.helpers.DataChecker;
import fr.openent.formulaire.security.AccessRight;
import fr.openent.formulaire.security.CustomShareAndOwner;
import fr.openent.formulaire.service.DistributionService;
import fr.openent.formulaire.service.FormElementService;
import fr.openent.formulaire.service.QuestionService;
import fr.openent.formulaire.service.SectionService;
import fr.openent.formulaire.service.impl.DefaultDistributionService;
import fr.openent.formulaire.service.impl.DefaultFormElementService;
import fr.openent.formulaire.service.impl.DefaultQuestionService;
import fr.openent.formulaire.service.impl.DefaultSectionService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static fr.openent.form.core.constants.Fields.*;
import static fr.openent.form.core.constants.ShareRights.CONTRIB_RESOURCE_RIGHT;
import static fr.openent.form.helpers.RenderHelper.renderInternalError;
import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class SectionController extends ControllerHelper {
    private static final Logger log = LoggerFactory.getLogger(SectionController.class);
    private final SectionService sectionService;
    private final QuestionService questionService;
    private final FormElementService formElementService;
    private final DistributionService distributionService;

    public SectionController() {
        super();
        this.sectionService = new DefaultSectionService();
        this.questionService = new DefaultQuestionService();
        this.formElementService = new DefaultFormElementService();
        this.distributionService = new DefaultDistributionService();
    }

    @Get("/forms/:formId/sections")
    @ApiDoc("List all the sections of a specific form")
    @ResourceFilter(AccessRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void list(HttpServerRequest request) {
        String formId = request.getParam(PARAM_FORM_ID);
        sectionService.list(formId, arrayResponseHandler(request));
    }

    @Get("/sections/:sectionId")
    @ApiDoc("Get a specific section by id")
    @ResourceFilter(CustomShareAndOwner.class)
    @SecuredAction(value = CONTRIB_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void get(HttpServerRequest request) {
        String sectionId = request.getParam(PARAM_SECTION_ID);
        sectionService.get(sectionId, defaultResponseHandler(request));
    }

    @Post("/forms/:formId/sections")
    @ApiDoc("Create a section in a specific form")
    @ResourceFilter(CustomShareAndOwner.class)
    @SecuredAction(value = CONTRIB_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void create(HttpServerRequest request) {
        String formId = request.getParam(PARAM_FORM_ID);
        RequestUtils.bodyToJson(request, sectionJson -> {
            if (sectionJson == null || sectionJson.isEmpty()) {
                log.error("[Formulaire@SectionController::create] No section to create.");
                noContent(request);
                return;
            }

            Section section = new Section(sectionJson);
            // Check if form is not already responded
            distributionService.countFinished(formId, countRepEvt -> {
                if (countRepEvt.isLeft()) {
                    log.error("[Formulaire@SectionController::create] Failed to count finished distributions form form with id : " + formId);
                    renderInternalError(request, countRepEvt);
                    return;
                }

                int nbResponseTot = countRepEvt.right().getValue().getInteger(COUNT, 0);
                if (nbResponseTot > 0) {
                    String message = "[Formulaire@SectionController::create] You cannot create a question for a form already responded";
                    log.error(message);
                    badRequest(request, message);
                    return;
                }

                // Check position value validity
                Long position = section.getPosition();
                if (position < 1) {
                    String message = "[Formulaire@SectionController::create] You cannot create a section with a position " +
                            "null or under 1 : " + position;
                    log.error(message);
                    badRequest(request, message);
                    return;
                }

                // Check if position is not already used
                formElementService.getTypeAndIdByPosition(formId, position.toString(), formElementEvt -> {
                    if (formElementEvt.isLeft()) {
                        log.error("[Formulaire@SectionController::create] Error in getting form element id of position " + position + " for form " + formId);
                        renderInternalError(request, formElementEvt);
                        return;
                    }

                    if (!formElementEvt.right().getValue().isEmpty()) {
                        String message = "[Formulaire@SectionController::create] You cannot create a section with a " +
                                "position already occupied : " +formElementEvt.right().getValue();
                        log.error(message);
                        badRequest(request, message);
                        return;
                    }

                    sectionService.isTargetValid(section)
                        .compose(sectionValidity -> {
                            if (!sectionValidity) {
                                String errorMessage = "[Formulaire@SectionController::create] Invalid section.";
                                return Future.failedFuture(errorMessage);
                            }
                            return sectionService.create(section, formId);
                        })
                        .onSuccess(result -> renderJson(request, result))
                        .onFailure(err -> {
                            log.error(err.getMessage());
                            renderError(request);
                        });
                });
            });
        });
    }

    @Put("/forms/:formId/sections")
    @ApiDoc("Update a specific section")
    @ResourceFilter(CustomShareAndOwner.class)
    @SecuredAction(value = CONTRIB_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void update(HttpServerRequest request) {
        String formId = request.getParam(PARAM_FORM_ID);

        RequestUtils.bodyToJsonArray(request, sectionsJson -> {
            if (sectionsJson == null || sectionsJson.isEmpty()) {
                log.error("[Formulaire@SectionController::update] No section to update.");
                noContent(request);
                return;
            }

            // Check position values validity
            boolean arePositionsOk = DataChecker.checkSectionPositionsValidity(sectionsJson);
            if (!arePositionsOk) {
                String message = "[Formulaire@SectionController::update] You cannot create a section with a position null or under 1 : " + sectionsJson;
                log.error(message);
                badRequest(request, message);
                return;
            }

            List<Section> sections = new Section().toList(sectionsJson);
            questionService.listForForm(formId)
                .compose(questions -> {
                    JsonArray questionPositions = UtilsHelper.getByProp(questions, POSITION);
                    JsonArray sectionPositions = UtilsHelper.getByProp(sectionsJson, POSITION);

                    List<Object> doubles = questionPositions.stream().filter(sectionPositions::contains).collect(Collectors.toList());
                    if (doubles.size() > 0) {
                        String message = "[Formulaire@SectionController::update] Position(s) " + doubles + " are/is already used by some question(s).";
                        return Future.failedFuture(message);
                    }

                    List<Future<Boolean>> futures = new ArrayList<>();
                    for (Section section : sections) {
                        futures.add(sectionService.isTargetValid(section));
                    }

                    Promise<List<Boolean>> promise = Promise.promise();
                    FutureHelper.all(futures)
                        .onSuccess(result -> promise.complete(result.list()))
                        .onFailure(promise::fail);
                    return promise.future();
                })
                .compose(sectionsValidity -> {
                    if (sectionsValidity.stream().anyMatch(sv -> !sv)) {
                        String errorMessage = "[Formulaire@SectionController::create] At least one section is invalid.";
                        return Future.failedFuture(errorMessage);
                    }
                    return sectionService.update(formId, sectionsJson);
                })
                .onSuccess(updatedSectionsInfos -> {
                    JsonArray updatedSections = new JsonArray();
                    for (int i = 0; i < updatedSectionsInfos.size(); i++) {
                        updatedSections.addAll(updatedSectionsInfos.getJsonArray(i));
                    }
                    renderJson(request, updatedSections);
                })
                .onFailure(err -> {
                    log.error("[Formulaire@SectionController::update] Failed to update sections " + sectionsJson + " : " + err.getMessage());
                    renderError(request);
                });
        });
    }

    @Delete("/sections/:sectionId")
    @ApiDoc("Delete a specific section")
    @ResourceFilter(CustomShareAndOwner.class)
    @SecuredAction(value = CONTRIB_RESOURCE_RIGHT, type = ActionType.RESOURCE)
    public void delete(HttpServerRequest request) {
        String sectionId = request.getParam(PARAM_SECTION_ID);
        sectionService.get(sectionId, getEvt -> {
            if (getEvt.isLeft() || getEvt.right().getValue().isEmpty()) {
                log.error("[Formulaire@deleteSection] Failed to get section with id : " + sectionId);
                renderInternalError(request, getEvt);
                return;
            }
            sectionService.delete(getEvt.right().getValue(), defaultResponseHandler(request));
        });
    }
}