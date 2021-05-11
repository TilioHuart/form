package fr.openent.formulaire.export;

import fr.openent.formulaire.service.QuestionService;
import fr.openent.formulaire.service.ResponseService;
import fr.openent.formulaire.service.impl.DefaultQuestionService;
import fr.openent.formulaire.service.impl.DefaultResponseService;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.user.UserUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class FormResponsesExport {
  private static final Logger log = LoggerFactory.getLogger(FormResponsesExport.class);

  private String UTF8_BOM = "\uFEFF";
  private String EOL = "\n";
  private String SEPARATOR = ";";
  private ResponseService responseService = new DefaultResponseService();
  private QuestionService questionService = new DefaultQuestionService();
  private EventBus eb;
  private HttpServerRequest request;
  private boolean anonymous;
  private String formName;
  // Creates  new String builder with UTF-8 BOM. Used to open on excel
  private StringBuilder content = new StringBuilder(UTF8_BOM);
  private SimpleDateFormat dateGetter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
  private SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");

  public FormResponsesExport(EventBus eb, HttpServerRequest request, JsonObject form) {
    this.eb = eb;
    this.request = request;
    this.anonymous = form.getBoolean("anonymous");
    this.formName = form.getString("title");
    dateFormatter.setTimeZone(TimeZone.getTimeZone("Europe/Paris")); // TODO to adapt for not France timezone
  }

  public void launch() {
    String formId = request.getParam("formId");
    questionService.export(formId, getQuestionsEvt -> {
      if (getQuestionsEvt.isLeft()) {
        log.error("[Formulaire@FormExport] Failed to retrieve all questions of the form", getQuestionsEvt.left().getValue());
        Renders.renderError(request);
      }

      JsonArray questions = getQuestionsEvt.right().getValue();
      int nbQuestions = questions.size();
      content.append(header(questions));

      responseService.exportResponses(formId, getResponsesEvt -> {
        if (getResponsesEvt.isLeft()) {
          log.error("[Formulaire@FormExport] Failed to retrieve all responses of the form", getResponsesEvt.left().getValue());
          Renders.renderError(request);
        }

        List<JsonObject> allResponses = getResponsesEvt.right().getValue().getList();
        List<String> responders = new ArrayList<>();
        List<String> response_dates = new ArrayList<>();
        List<String> structures = new ArrayList<>();

        for (JsonObject response : allResponses) {
          if (!response_dates.contains(response.getString("date_response"))) {
            responders.add(response.getString("responder_id"));
            response_dates.add(response.getString("date_response"));
            structures.add(response.getString("structure"));
          }
        }

        // Create futures for getting users infos
        List<Future> usersInfos = new ArrayList<>();
        for (int i = 0; i < responders.size(); i++) {
          usersInfos.add(Future.future());
        }

        // Proceed once we've got all the users' infos in our usersInfos list
        CompositeFuture.all(usersInfos).setHandler(evt -> {
          if (evt.failed()) {
            log.error("[Formulaire@ExportCompositeFuture] Failed to retrieve results", evt.cause());
            Future.failedFuture(evt.cause());
          }

          for (int i = 0; i < responders.size(); i++) {
            String responderId = responders.get(i);
            String responseDate = response_dates.get(i);

            // Add responder infos
            content.append(usersInfos.get(i).result());

            // Get responses of this responder
            ArrayList<JsonObject> responses = new ArrayList<>();
            for (JsonObject response : allResponses) {
              if (response.getString("responder_id").equals(responderId) &&
                      response.getString("date_response").equals(responseDate)) {
                responses.add(response);
              }
            }

            // Sort responses by question's position
            Collections.sort(responses, (jsonObjectA, jsonObjectB) -> {
              int compare = 0;
              try {
                int keyA = jsonObjectA.getInteger("position");
                int keyB = jsonObjectB.getInteger("position");
                compare = Integer.compare(keyA, keyB);
              }
              catch(Exception e) {
                e.printStackTrace();
              }
              return compare;
            });

            // Add responses of this responder
            List<JsonObject> allQuestions = questions.getList();
            for (JsonObject question : allQuestions) {
              if (responses.size() > 0) {
                JsonObject response = responses.get(0);
                if (response.getInteger("position") == question.getInteger("position")) {
                  String answer = "";
                  boolean choice = true;
                  int question_id = response.getInteger("question_id");
                  while (choice && responses.size() > 0) {
                    if (response.getInteger("question_id") == question_id) {
                      answer += response.getString("answer") + ";";
                      question_id = response.getInteger("question_id");
                      responses.remove(0);
                      if (responses.size() > 0) {
                        response = responses.get(0);
                      }
                      else {
                        answer = answer.substring(0, answer.length() - 1);
                      }
                    } else {
                      answer = answer.substring(0, answer.length() - 1);
                      choice = false;
                    }
                  }
                  content.append(addResponse(answer, allQuestions.lastIndexOf(question) == nbQuestions - 1));
                }
                else {
                  content.append(addResponse("", allQuestions.lastIndexOf(question) == nbQuestions - 1));
                }
              }
              else {
                content.append(addResponse("", allQuestions.lastIndexOf(question) == nbQuestions - 1));
              }
            }
          }

          send();
        });

        // Get basic infos for all the responders (trigger CompositeFuture above)
        for (int i = 0; i < responders.size(); i++) {
          String responderId = responders.get(i);
          String date_response = response_dates.get(i);
          String structure = structures.get(i);
          getUserInfos(responderId, structure, date_response, usersInfos.get(i));
        }

      });
    });
  }

  private String addResponse(String answer, Boolean endLine) {
    String value = "\"" + answer.replace("\"", "\"\"") + "\"";
    value += endLine ? EOL : SEPARATOR;
    return value;
  }

  private void getUserInfos(String userId, String structure, String sqlDate, Handler<AsyncResult<String>> handler) {
    UserUtils.getUserInfos(eb, userId, user -> {
      if (user != null) {
        StringBuilder builder = new StringBuilder();
        Date date = null;
        try { date = dateGetter.parse(sqlDate); } catch (ParseException e) { e.printStackTrace(); }

        if (!anonymous) {
          builder.append(user.getUserId()).append(SEPARATOR);
          builder.append("\"" + user.getLastName() + "\"").append(SEPARATOR);
          builder.append("\"" + user.getFirstName() + "\"").append(SEPARATOR);
          builder.append("\"" + (structure != null ? structure : user.getStructureNames().get(0)) + "\"").append(SEPARATOR);
        }
        builder.append(dateFormatter.format(date)).append(SEPARATOR);

        handler.handle(Future.succeededFuture(builder.toString()));
      } else {
        log.error("User not found in session.");
        Renders.unauthorized(request);
      }
    });

  }

  private String header(JsonArray questions) {
    ArrayList<String> headers = new ArrayList<>();
    if (!anonymous) {
      headers.add("ID");
      headers.add("Nom");
      headers.add("Prénom");
      headers.add("Établissement");
    }
    headers.add("Date de réponse");

    for (Object o : questions) {
      if (o instanceof JsonObject) {
        JsonObject question = (JsonObject)o;
        headers.add(question.getString("title"));
      }
    }

    StringBuilder builder = new StringBuilder();
    for (String column : headers) {
      builder.append(column + SEPARATOR);
    }

    builder.append(EOL);
    return builder.toString();
  }

  private void send() {
    request.response()
      .putHeader("Content-Type", "text/csv; charset=utf-8")
      .putHeader("Content-Disposition", "attachment; filename=Réponses_" + formName + ".csv")
      .end(content.toString());
  }
}
