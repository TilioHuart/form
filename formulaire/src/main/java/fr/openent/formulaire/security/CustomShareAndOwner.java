package fr.openent.formulaire.security;

import fr.wseduc.webutils.http.Binding;
import fr.wseduc.webutils.http.HttpMethod;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlConf;
import org.entcore.common.sql.SqlConfs;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;
import java.util.ArrayList;
import java.util.List;

import static fr.openent.form.core.constants.Fields.*;
import static fr.openent.form.core.constants.Tables.*;


public class CustomShareAndOwner implements ResourcesProvider {
    public void authorize(final HttpServerRequest request, Binding binding, UserInfos user, final Handler<Boolean> handler) {
        SqlConf conf = SqlConfs.getConf(binding.getServiceMethod().substring(0, binding.getServiceMethod().indexOf(124)));
        String key = getKeyByBinding(binding);
        String id = request.params().get(key);

        if (id != null && !id.trim().isEmpty()) {
            request.pause();
            String sharedMethod = binding.getServiceMethod().replaceAll("\\.", "-");
            List<String> gu = new ArrayList();
            gu.add(user.getUserId());
            if (user.getGroupsIds() != null) {
                gu.addAll(user.getGroupsIds());
            }

            Object[] groupsAndUserIds = gu.toArray();
            String query = "SELECT count(*) FROM " + conf.getSchema() + conf.getTable() +
                    " LEFT JOIN " + conf.getSchema() + conf.getShareTable() +
                    " ON id = resource_id WHERE ((member_id IN " + Sql.listPrepared(groupsAndUserIds) + " AND action = ?) OR owner_id = ?) AND id = ";
            JsonArray values = new JsonArray(gu).add(sharedMethod).add(user.getUserId());

            switch (key) {
                case ID:
                case PARAM_FORM_ID:
                    query += "?";
                    break;
                case PARAM_DISTRIBUTION_ID:
                    query += "(SELECT form_id FROM " + DISTRIBUTION_TABLE + " WHERE id = ?)";
                    break;
                case PARAM_SECTION_ID:
                    query += "(SELECT form_id FROM " + SECTION_TABLE + " WHERE id = ?)";
                    break;
                case PARAM_QUESTION_ID:
                    query += "(SELECT form_id FROM " + QUESTION_TABLE + " WHERE id = ?)";
                    break;
                case PARAM_CHOICE_ID:
                    query += "(SELECT form_id FROM " + QUESTION_TABLE + " WHERE id =" +
                            "(SELECT question_id FROM " + QUESTION_CHOICE_TABLE + " WHERE id = ?))";
                    break;
                case PARAM_RESPONSE_ID:
                    query += "(SELECT form_id FROM " + QUESTION_TABLE + " WHERE id =" +
                            "(SELECT question_id FROM " + RESPONSE_TABLE + " WHERE id = ?))";
                    break;
                case PARAM_FILE_ID:
                    query += "(SELECT form_id FROM " + QUESTION_TABLE + " WHERE id =" +
                            "(SELECT question_id FROM " + RESPONSE_TABLE + " WHERE id = " +
                            "(SELECT response_id FROM " + RESPONSE_FILE_TABLE + " WHERE id = ?)))";
                    break;
                default: break;
            }
            values.add(Sql.parseId(id));


            Sql.getInstance().prepared(query, values, message -> {
                request.resume();
                Long count = SqlResult.countResult(message);
                handler.handle(count != null && count > 0L);
            });
        }
        else {
            handler.handle(false);
        }
    }


    private String getKeyByBinding(Binding binding) {
        if (isGetForm(binding) || isCountDistribution(binding) || isGetByFormResponderAndStatusDistribution(binding) ||
                isUpdateForm(binding) || isDeleteForm(binding) || isSendReminderForm(binding) || isUpdateFormElement(binding) ||
                isCreateQuestion(binding) || isListByFormResponse(binding) || isDeleteResponse(binding) ||
                isExportResponse(binding) || isCreateSection(binding) || isUpdateSection(binding) || isUpdateQuestion(binding)) {
            return PARAM_FORM_ID;
        }
        else if (isGetDistribution(binding) || isAddDistribution(binding) || isUpdateDistribution(binding) ||
                isDuplicateWithResponsesDistribution(binding) || isReplaceDistribution(binding) ||
                isDeleteDistribution(binding) || isListByDistributionResponse(binding) || isDeleteByQuestionResponse(binding)) {
            return PARAM_DISTRIBUTION_ID;
        }
        else if (isGetSection(binding) || isDeleteSection(binding)) {
            return PARAM_SECTION_ID;
        }
        else if (isListMineByDistributionResponse(binding) || isCreateQuestionChoice(binding) || isGetQuestion(binding) ||
                isDeleteQuestion(binding) || isListResponse(binding) || isCreateResponse(binding) ||
                isZipAndDownloadResponseFile(binding)) {
            return PARAM_QUESTION_ID;
        }
        else if (isUpdateQuestionChoice(binding) || isDeleteQuestionChoice(binding)) {
            return PARAM_CHOICE_ID;
        }
        else if (isUpdateResponse(binding) || isUploadResponseFile(binding) || isDeleteAllResponseFile(binding)) {
            return PARAM_RESPONSE_ID;
        }
        else if (isGetResponseFile(binding) || isDownloadResponseFile(binding)) {
            return PARAM_FILE_ID;
        }
        else {
            return ID;
        }
    }

    private boolean bindingIsThatMethod(final Binding binding, final HttpMethod thatHttpMethod, final String thatServiceMethod) {
        return (thatHttpMethod.equals(binding.getMethod()) && thatServiceMethod.equals(binding.getServiceMethod()));
    }

    // Distribution
    private boolean isCountDistribution(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.GET, "fr.openent.formulaire.controllers.DistributionController|count");
    }

    private boolean isGetDistribution(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.GET, "fr.openent.formulaire.controllers.DistributionController|get");
    }

    private boolean isGetByFormResponderAndStatusDistribution(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.GET, "fr.openent.formulaire.controllers.DistributionController|getByFormResponderAndStatus");
    }

    private boolean isAddDistribution(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.POST, "fr.openent.formulaire.controllers.DistributionController|add");
    }

    private boolean isDuplicateWithResponsesDistribution(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.POST, "fr.openent.formulaire.controllers.DistributionController|duplicateWithResponses");
    }

    private boolean isUpdateDistribution(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.PUT, "fr.openent.formulaire.controllers.DistributionController|update");
    }

    private boolean isReplaceDistribution(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.DELETE, "fr.openent.formulaire.controllers.DistributionController|replace");
    }

    private boolean isDeleteDistribution(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.DELETE, "fr.openent.formulaire.controllers.DistributionController|delete");
    }

    // Form
    private boolean isGetForm(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.GET, "fr.openent.formulaire.controllers.FormController|get");
    }

    private boolean isUpdateForm(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.PUT, "fr.openent.formulaire.controllers.FormController|update");
    }

    private boolean isDeleteForm(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.DELETE, "fr.openent.formulaire.controllers.FormController|delete");
    }

    private boolean isSendReminderForm(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.POST, "fr.openent.formulaire.controllers.FormController|sendReminder");
    }

    private boolean isShareJson(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.GET, "fr.openent.formulaire.controllers.FormController|shareJson");
    }

    private boolean isShareSubmit(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.PUT, "fr.openent.formulaire.controllers.FormController|shareSubmit");
    }

    private boolean isShareResource(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.PUT, "fr.openent.formulaire.controllers.FormController|shareResource");
    }

    // FormElement
    private boolean isUpdateFormElement(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.PUT, "fr.openent.formulaire.controllers.FormElementController|update");
    }

    // QuestionChoice
    private boolean isCreateQuestionChoice(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.POST, "fr.openent.formulaire.controllers.QuestionChoiceController|create");
    }

    private boolean isUpdateQuestionChoice(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.PUT, "fr.openent.formulaire.controllers.QuestionChoiceController|update");
    }

    private boolean isDeleteQuestionChoice(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.DELETE, "fr.openent.formulaire.controllers.QuestionChoiceController|delete");
    }

    // Question
    private boolean isGetQuestion(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.GET, "fr.openent.formulaire.controllers.QuestionController|get");
    }

    private boolean isCreateQuestion(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.POST, "fr.openent.formulaire.controllers.QuestionController|create");
    }

    private boolean isUpdateQuestion(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.PUT, "fr.openent.formulaire.controllers.QuestionController|update");
    }

    private boolean isDeleteQuestion(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.DELETE, "fr.openent.formulaire.controllers.QuestionController|delete");
    }

    // Response
    private boolean isListResponse(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.GET, "fr.openent.formulaire.controllers.ResponseController|list");
    }

    private boolean isListMineByDistributionResponse(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.GET, "fr.openent.formulaire.controllers.ResponseController|listMineByDistribution");
    }

    private boolean isListByDistributionResponse(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.GET, "fr.openent.formulaire.controllers.ResponseController|listByDistribution");
    }

    private boolean isListByFormResponse(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.GET, "fr.openent.formulaire.controllers.ResponseController|listByForm");
    }

    private boolean isCreateResponse(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.POST, "fr.openent.formulaire.controllers.ResponseController|create");
    }

    private boolean isUpdateResponse(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.PUT, "fr.openent.formulaire.controllers.ResponseController|update");
    }

    private boolean isDeleteResponse(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.DELETE, "fr.openent.formulaire.controllers.ResponseController|delete");
    }

    private boolean isDeleteByQuestionResponse(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.DELETE, "fr.openent.formulaire.controllers.ResponseController|deleteByQuestionAndDistribution");
    }

    private boolean isExportResponse(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.POST, "fr.openent.formulaire.controllers.ResponseController|export");
    }

    // ResponseFile
    private boolean isGetResponseFile(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.GET, "fr.openent.formulaire.controllers.ResponseFileController|get");
    }

    private boolean isDownloadResponseFile(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.GET, "fr.openent.formulaire.controllers.ResponseFileController|download");
    }

    private boolean isZipAndDownloadResponseFile(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.GET, "fr.openent.formulaire.controllers.ResponseFileController|zipAndDownload");
    }

    private boolean isUploadResponseFile(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.POST, "fr.openent.formulaire.controllers.ResponseFileController|upload");
    }

    private boolean isDeleteAllResponseFile(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.DELETE, "fr.openent.formulaire.controllers.ResponseFileController|deleteAll");
    }

    // Section
    private boolean isGetSection(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.GET, "fr.openent.formulaire.controllers.SectionController|get");
    }

    private boolean isCreateSection(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.POST, "fr.openent.formulaire.controllers.SectionController|create");
    }

    private boolean isUpdateSection(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.PUT, "fr.openent.formulaire.controllers.SectionController|update");
    }

    private boolean isDeleteSection(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.DELETE, "fr.openent.formulaire.controllers.SectionController|delete");
    }

}