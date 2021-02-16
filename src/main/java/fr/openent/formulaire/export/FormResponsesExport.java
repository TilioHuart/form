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
  // Creates  new String builder with UTF-8 BOM. Used to open on excel
  private StringBuilder content = new StringBuilder(UTF8_BOM);
  private SimpleDateFormat dateGetter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
  private SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");

  public FormResponsesExport(EventBus eb, HttpServerRequest request) {
    this.eb = eb;
    this.request = request;
  }

  public void launch() {
    String formId = request.getParam("formId");
    questionService.list(formId, getQuestionsEvt -> {
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

        List<JsonObject> allResponses = getResponsesEvt.right().getValue().<List<JsonObject>>getList();
        List<String> responders = new ArrayList<>();
        List<String> response_dates = new ArrayList<>();

        for (JsonObject response : allResponses) {
          if (!responders.contains(response.getString("responder_id"))) {
            responders.add(response.getString("responder_id"));
            response_dates.add(response.getString("date_response"));
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

            // Add responder infos
            content.append(usersInfos.get(i).result());

            // Get responses of this responder
            ArrayList<JsonObject> responses = new ArrayList<>();
            for (JsonObject response : allResponses) {
              if (response.getString("responder_id").equals(responderId)) {
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
            for (int j = 1; j <= nbQuestions; j++) {
              JsonObject response = responses.get(0);
              if (response.getInteger("position") == j) {
                content.append(addResponse(response, j == nbQuestions));
                responses.remove(0);
              }
              else {
                response.put("answer", "");
                content.append(addResponse(response, j == nbQuestions));
              }
            }
          }

          send();
        });

        // Get basic infos for all the responders (trigger CompositeFuture above)
        for (int i = 0; i < responders.size(); i++) {
          String responderId = responders.get(i);
          String date_response = response_dates.get(i);
          getUserInfos(responderId, date_response, usersInfos.get(i));
        }

      });
    });
  }

  private String addResponse(JsonObject response, Boolean endLine) {

    String value = "\"" + response.getString("answer").replace("\"", "\"\"") + "\"";
    value += endLine ? EOL : SEPARATOR;
    return value;
  }

  private void getUserInfos(String userId, String sqlDate, Handler<AsyncResult<String>> handler) {
    UserUtils.getUserInfos(eb, userId, user -> {
      if (user != null) {
        StringBuilder builder = new StringBuilder();
        Date date = null;
        try { date = dateGetter.parse(sqlDate); } catch (ParseException e) { e.printStackTrace(); }

        builder.append(user.getUserId()).append(SEPARATOR);
        builder.append("\"" + user.getLastName() + "\"").append(SEPARATOR);
        builder.append("\"" + user.getFirstName() + "\"").append(SEPARATOR);
        builder.append(dateFormatter.format(date)).append(SEPARATOR);
        builder.append("\"" + user.getStructureNames().get(0) + "\"").append(SEPARATOR); // TODO et si on a plusieurs etab on affiche lequel ?

        handler.handle(Future.succeededFuture(builder.toString()));
      } else {
        log.error("User not found in session.");
        Renders.unauthorized(request);
      }
    });

  }

  private String header(JsonArray questions) {
    ArrayList<String> headers = new ArrayList<>();
    headers.add("ID");
    headers.add("Nom");
    headers.add("Prénom");
    headers.add("Date de réponse");
    headers.add("Établissement");

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
      .putHeader("Content-Disposition", "attachment; filename=responsesExport.csv") // TODO later bring here name of the form
      .end(content.toString());
  }
}
