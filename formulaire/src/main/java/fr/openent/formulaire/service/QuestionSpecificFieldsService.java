package fr.openent.formulaire.service;

import fr.openent.form.core.models.Question;
import fr.openent.form.core.models.QuestionSpecificFields;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Optional;

public interface QuestionSpecificFieldsService {

    /**
     * Sync all the specifics fields of question
     * @param questions JsonArray data
     */
    Future<JsonArray> syncQuestionSpecs(JsonArray questions);

    /**
     * List all the specifics fields of question from a list of ids
     * @param questionIds questions identifiers
     */
    Future<JsonArray> listByIds(JsonArray questionIds);

    /**
     * Add specific fields to a question
     * @param question QuestionSpecificFields data
     * @param questionId question identifier
     */
    Future<Optional<QuestionSpecificFields>> create(QuestionSpecificFields question, Long questionId);

    /**
     * Update specific fields of questions
     * @param questions JsonArray data
     * @param handler function handler returning JsonArray data
     */
    void update(JsonArray questions, Handler<Either<String, JsonArray>> handler);

    /**
     * Update specific fields of questions
     * @param questions JsonArray data
     */
    Future<JsonArray> update(JsonArray questions);
}
