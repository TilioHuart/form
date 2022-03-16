package fr.openent.formulaire.security;

import fr.openent.formulaire.Formulaire;
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


public class ShareAndOwner implements ResourcesProvider {
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
            JsonArray values = (new fr.wseduc.webutils.collections.JsonArray(gu)).add(sharedMethod).add(user.getUserId());

            switch (key) {
                case "id":
                case "formId":
                    query += "?";
                    break;
                case "distributionId":
                    query += "(SELECT form_id FROM " + Formulaire.DISTRIBUTION_TABLE + " WHERE id = ?)";
                    break;
                case "sectionId":
                    query += "(SELECT form_id FROM " + Formulaire.SECTION_TABLE + " WHERE id = ?)";
                    break;
                case "questionId":
                    query += "(SELECT form_id FROM " + Formulaire.QUESTION_TABLE + " WHERE id = ?)";
                    break;
                case "choiceId":
                    query += "(SELECT form_id FROM " + Formulaire.QUESTION_TABLE + " WHERE id =" +
                            "(SELECT question_id FROM " + Formulaire.QUESTION_CHOICE_TABLE + " WHERE id = ?))";
                    break;
                case "responseId":
                    query += "(SELECT form_id FROM " + Formulaire.QUESTION_TABLE + " WHERE id =" +
                            "(SELECT question_id FROM " + Formulaire.RESPONSE_TABLE + " WHERE id = ?))";
                    break;
                case "fileId":
                    query += "(SELECT form_id FROM " + Formulaire.QUESTION_TABLE + " WHERE id =" +
                            "(SELECT question_id FROM " + Formulaire.RESPONSE_TABLE + " WHERE id = " +
                            "(SELECT response_id FROM " + Formulaire.RESPONSE_FILE_TABLE + " WHERE id = ?)))";
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
        if (isCountDistribution(binding) || isGetByFormResponderAndStatusDistribution(binding) || isAddDistribution(binding) ||
                isUpdateForm(binding) || isDeleteForm(binding) || isExportForm(binding) || isSendReminderForm(binding) ||
                isCreateQuestion(binding) || isFillResponsesResponse(binding) || isDeleteResponse(binding) || isCreateSection(binding)) {
            return "formId";
        }
        else if (isGetDistribution(binding) || isUpdateDistribution(binding) || isDuplicateWithResponsesDistribution(binding) ||
                isReplaceDistribution(binding) || isDeleteDistribution(binding) || isListByDistributionResponse(binding)) {
            return "distributionId";
        }
        else if (isGetSection(binding) || isUpdateSection(binding) || isDeleteSection(binding)) {
            return "sectionId";
        }
        else if (isListMineByDistributionResponse(binding) || isCreateQuestionChoice(binding) ||
                isCreateMultipleQuestionChoice(binding) || isGetQuestion(binding) || isUpdateQuestion(binding) ||
                isDeleteQuestion(binding) || isListResponse(binding) || isCreateResponse(binding) ||
                isZipAndDownloadResponseFile(binding)) {
            return "questionId";
        }
        else if (isUpdateQuestionChoice(binding) || isDeleteQuestionChoice(binding)) {
            return "choiceId";
        }
        else if (isUpdateResponse(binding) || isUploadResponseFile(binding) || isDeleteAllResponseFile(binding)) {
            return "responseId";
        }
        else if (isGetResponseFile(binding) || isDownloadResponseFile(binding)) {
            return "fileId";
        }
        else {
            return "id";
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
    private boolean isUpdateForm(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.PUT, "fr.openent.formulaire.controllers.FormController|update");
    }

    private boolean isDeleteForm(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.DELETE, "fr.openent.formulaire.controllers.FormController|delete");
    }

    private boolean isSendReminderForm(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.POST, "fr.openent.formulaire.controllers.FormController|sendReminder");
    }

    private boolean isExportForm(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.POST, "fr.openent.formulaire.controllers.FormController|export");
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

    // QuestionChoice
    private boolean isCreateQuestionChoice(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.POST, "fr.openent.formulaire.controllers.QuestionChoiceController|create");
    }

    private boolean isCreateMultipleQuestionChoice(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.POST, "fr.openent.formulaire.controllers.QuestionChoiceController|createMultiple");
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

    private boolean isCreateResponse(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.POST, "fr.openent.formulaire.controllers.ResponseController|create");
    }

    private boolean isFillResponsesResponse(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.POST, "fr.openent.formulaire.controllers.ResponseController|fillResponses");
    }

    private boolean isUpdateResponse(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.PUT, "fr.openent.formulaire.controllers.ResponseController|update");
    }

    private boolean isDeleteResponse(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.DELETE, "fr.openent.formulaire.controllers.ResponseController|delete");
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