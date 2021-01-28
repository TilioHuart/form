package fr.openent.formulaire.export;

import fr.openent.formulaire.service.QuestionService;
import fr.openent.formulaire.service.ResponseService;
import fr.openent.formulaire.service.impl.DefaultQuestionService;
import fr.openent.formulaire.service.impl.DefaultResponseService;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class FormResponsesExport {
  private static final Logger log = LoggerFactory.getLogger(FormResponsesExport.class);

  private String UTF8_BOM = "\uFEFF";
  private String EOL = "\n";
  private String SEPARATOR = ";"; // TODO Change for a less used separator
  private ResponseService responseService = new DefaultResponseService();
  private QuestionService questionService = new DefaultQuestionService();
  private HttpServerRequest request;
  // Creates  new String builder with UTF-8 BOM. Used to open on excel
  private StringBuilder content = new StringBuilder(UTF8_BOM);

  public FormResponsesExport(HttpServerRequest request) {
    this.request = request;
  }

  public void launch() {
    String formId = request.getParam("formId");
    questionService.list(formId, getQuestionsEvt -> { // TODO later enlever zone texte (no reponse du coup)
      if (getQuestionsEvt.isLeft()) {
        log.error("[Formulaire@FormExport] Failed to retrieve all questions of the form", getQuestionsEvt.left().getValue());
        return;
      }

      JsonArray questions = getQuestionsEvt.right().getValue();
      Integer nbQuestions = questions.size();

      responseService.exportResponses(formId, getResponsesEvt -> {
        if (getResponsesEvt.isLeft()) {
          log.error("[Formulaire@FormExport] Failed to retrieve all responses of the form", getResponsesEvt.left().getValue());
          return;
        }

        List<JsonObject> responses = getResponsesEvt.right().getValue().<List<JsonObject>>getList();
        content.append(header(questions));

        Integer questionCount = 1;
        for (JsonObject response : responses) {
          content.append(generateLine(response, questionCount%nbQuestions == 0));
          questionCount++;
        }

        send();
      });
    });
  }

  private String generateLine(JsonObject response, Boolean endLine) {
    String value = response.getString("answer");
    value += endLine ? EOL : SEPARATOR;

    return value;
  }

  private String header(JsonArray questions) {
    ArrayList<String> headers = new ArrayList<>();
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
