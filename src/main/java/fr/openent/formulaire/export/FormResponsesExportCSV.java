package fr.openent.formulaire.export;

import fr.openent.formulaire.service.QuestionService;
import fr.openent.formulaire.service.ResponseService;
import fr.openent.formulaire.service.impl.DefaultQuestionService;
import fr.openent.formulaire.service.impl.DefaultResponseService;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class FormResponsesExportCSV {
  private static final Logger log = LoggerFactory.getLogger(FormResponsesExportCSV.class);

  private final String UTF8_BOM = "\uFEFF";
  private final String EOL = "\n";
  private final String SEPARATOR = ";";
  private final ResponseService responseService = new DefaultResponseService();
  private final QuestionService questionService = new DefaultQuestionService();
  private final HttpServerRequest request;
  private final boolean anonymous;
  private final String formName;
  // Creates  new String builder with UTF-8 BOM. Used to open on excel
  private final StringBuilder content = new StringBuilder(UTF8_BOM);
  private final SimpleDateFormat dateGetter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
  private final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");

  public FormResponsesExportCSV(HttpServerRequest request, JsonObject form) {
    this.request = request;
    this.anonymous = form.getBoolean("anonymous");
    this.formName = form.getString("title");
    dateFormatter.setTimeZone(TimeZone.getTimeZone("Europe/Paris")); // TODO to adapt for not France timezone
  }

  public void launch() {
    String formId = request.getParam("formId");
    questionService.export(formId, false, getQuestionsEvt -> {
      if (getQuestionsEvt.isLeft()) {
        String message = "[Formulaire@FormExportCSV] Failed to retrieve all questions of the form " + formId;
        log.error(message + " : " + getQuestionsEvt.left().getValue());
        Renders.renderError(request);
        return;
      }

      JsonArray questions = getQuestionsEvt.right().getValue();
      int nbQuestions = questions.size();
      content.append(header(questions));

      responseService.getExportCSVResponders(formId, getRespondersEvt -> {
        if (getRespondersEvt.isLeft()) {
          String message = "[Formulaire@FormExportCSV] Failed to retrieve responders infos of the form " + formId;
          log.error(message + " : " + getRespondersEvt.left().getValue());
          Renders.renderError(request);
          return;
        }

        JsonArray responders = getRespondersEvt.right().getValue();

        responseService.exportCSVResponses(formId, getResponsesEvt -> {
          if (getResponsesEvt.isLeft()) {
            String message = "[Formulaire@FormExportCSV] Failed to retrieve all responses of the form " + formId;
            log.error(message + " : " + getResponsesEvt.left().getValue());
            Renders.renderError(request);
            return;
          }

          List<JsonObject> allResponses = getResponsesEvt.right().getValue().getList();
          if (allResponses.isEmpty()) {
            log.info("[Formulaire@FormExportCSV] No responses found for the form : " + formId);
            Renders.notFound(request);
            return;
          }

          for (int r = 0; r < responders.size(); r++) {
            // Add responder infos
            content.append(getUserInfos(responders.getJsonObject(r)));

            // Get responses of this responder
            ArrayList<JsonObject> responses = new ArrayList<>();
            boolean allFound = false;
            int previousDistribId = allResponses.get(0).getInteger("id");
            int firstIndexToDelete = -1;
            int lastIndexToDelete = -1;
            int i = 0;

            while (!allFound && i < allResponses.size()) {
              JsonObject response = allResponses.get(i);
              if (!responses.isEmpty() && !response.getInteger("id").equals(previousDistribId)) {
                allFound = true;
                lastIndexToDelete = i;
              }
              else if (response.getInteger("id").equals(previousDistribId)) {
                if (responses.isEmpty()) { firstIndexToDelete = i; }
                responses.add(response);
              }
              previousDistribId = response.getInteger("id");
              i++;
            }

            // Remove found responses to simplify next responders' loops
            if (firstIndexToDelete >= 0 && lastIndexToDelete >= 0) {
              allResponses.subList(firstIndexToDelete, lastIndexToDelete).clear();
            }

            // Map responses
            HashMap<Integer, JsonArray> mapResponses = new HashMap<>();
            for (Object rep : responses) {
              JsonObject response = (JsonObject)rep;
              int questionId = response.getInteger("question_id");
              if (mapResponses.get(questionId) == null) {
                mapResponses.put(questionId, new JsonArray());
              }
              mapResponses.get(questionId).add(response);
            }

            // Add responses of this responder
            List<JsonObject> allQuestions = questions.getList();
            for (JsonObject question : allQuestions) {
              JsonArray questionResponses = mapResponses.get(question.getInteger("id"));
              if (questionResponses != null && questionResponses.size() > 0) {
                String answer = "";
                for (int rep = 0; rep < questionResponses.size(); rep++) {
                  JsonObject response = questionResponses.getJsonObject(rep);
                  answer += response.getString("answer") + (rep < questionResponses.size() - 1 ? ";" : "");
                }
                content.append(addResponse(answer, allQuestions.lastIndexOf(question) == nbQuestions - 1));
              }
              else {
                content.append(addResponse("", allQuestions.lastIndexOf(question) == nbQuestions - 1));
              }
            }
          }

          send();
        });
      });
    });
  }

  private String header(JsonArray questions) {
    ArrayList<String> headers = new ArrayList<>();
    if (!anonymous) {
      headers.add("ID");
      headers.add("Utilisateur");
      headers.add("Établissement");
    }
    headers.add("Date de réponse");

    for (int i = 0; i < questions.size(); i++) {
      JsonObject question = questions.getJsonObject(i);
      Integer element_position = question.getInteger("element_position");
      Integer section_position = question.getInteger("section_position");
      String displayedPosition = element_position + "." + (section_position != null ? section_position + "." : "");
      headers.add(displayedPosition + questions.getJsonObject(i).getString("title"));
    }

    StringBuilder builder = new StringBuilder();
    for (String column : headers) {
      builder.append(column + SEPARATOR);
    }

    builder.append(EOL);
    return builder.toString();
  }

  private String getUserInfos(JsonObject responder) {
    String userId = responder.getString("responder_id");
    String displayName = responder.getString("responder_name");
    String sqlDate = responder.getString("date_response");
    String structure = responder.getString("structure");

    StringBuilder builder = new StringBuilder();
    Date date = null;
    try { date = dateGetter.parse(sqlDate); } catch (ParseException e) { e.printStackTrace(); }

    if (!anonymous) {
      builder.append(userId).append(SEPARATOR);
      builder.append("\"" + displayName + "\"").append(SEPARATOR);
      builder.append("\"" + (structure != null ? structure : "-") + "\"").append(SEPARATOR);
    }
    builder.append(dateFormatter.format(date)).append(SEPARATOR);

    return builder.toString();
  }

  private String addResponse(String answer, Boolean endLine) {
    String cleanAnswer = answer.replace("\"", "\"\"").replace("&nbsp;", "").replaceAll("<[^>]*>", "");
    String value = "\"" + cleanAnswer + "\"";
    value += endLine ? EOL : SEPARATOR;
    return value;
  }

  private void send() {
    request.response()
      .putHeader("Content-Type", "text/csv; charset=utf-8")
      .putHeader("Content-Disposition", "attachment; filename=Réponses_" + formName + ".csv")
      .end(content.toString());
  }
}
