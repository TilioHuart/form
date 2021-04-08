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
                case "questionId":
                    query += "(SELECT form_id FROM " + Formulaire.QUESTION_TABLE + " WHERE id = ?)";
                    break;
                case "responseId":
                    query += "(SELECT form_id FROM " + Formulaire.QUESTION_TABLE + " WHERE id = (SELECT question_id FROM " + Formulaire.RESPONSE_TABLE + " WHERE id = ?))";
                    break;
                default: break;
            }
            values.add(Sql.parseId(id));


            Sql.getInstance().prepared(query, values, message -> {
                request.resume();
                Long count = SqlResult.countResult(message);
                handler.handle(count != null && count > 0L);
            });
        } else {
            handler.handle(false);
        }
    }


    private String getKeyByBinding(Binding binding) {
        if (isUpdateForm(binding) || isDeleteForm(binding) || isExportForm(binding) || isCountDistribution(binding) ||
                isCreateDistribution(binding) || isSendReminderForm(binding)) {
            return "formId";
        }
        else if (isDeleteDistribution(binding)) {
            return "distributionId";
        }
        else if (isListMineResponse(binding) || isCreateResponse(binding) || isListResponseFile(binding) ||
                isZipAndDownloadResponseFile(binding)) {
            return "questionId";
        }
        else if (isUpdateResponse(binding) || isDeleteResponse(binding) || isDownloadResponseFile(binding)) {
            return "responseId";
        }
        else {
            return "id";
        }
    }

    private boolean bindingIsThatMethod(final Binding binding, final HttpMethod thatHttpMethod, final String thatServiceMethod) {
        return (thatHttpMethod.equals(binding.getMethod()) && thatServiceMethod.equals(binding.getServiceMethod()));
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

    private boolean isExportForm(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.GET, "fr.openent.formulaire.controllers.FormController|export");
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

    private boolean isCountDistribution(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.GET, "fr.openent.formulaire.controllers.DistributionController|count");
    }

    private boolean isCreateDistribution(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.POST, "fr.openent.formulaire.controllers.DistributionController|create");
    }

    private boolean isDeleteDistribution(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.DELETE, "fr.openent.formulaire.controllers.DistributionController|delete");
    }

    private boolean isListMineResponse(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.GET, "fr.openent.formulaire.controllers.ResponseController|listMine");
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

    private boolean isListResponseFile(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.GET, "fr.openent.formulaire.controllers.ResponseFileController|list");
    }

    private boolean isDownloadResponseFile(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.GET, "fr.openent.formulaire.controllers.ResponseFileController|download");
    }

    private boolean isZipAndDownloadResponseFile(final Binding binding) {
        return bindingIsThatMethod(binding, HttpMethod.GET, "fr.openent.formulaire.controllers.ResponseFileController|zipAndDownload");
    }
}