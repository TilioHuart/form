package fr.openent.formulaire.service;

import fr.openent.form.core.models.Question;
import fr.openent.form.core.models.QuestionSpecificFields;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;

import java.util.List;
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
    Future<List<QuestionSpecificFields>> listByIds(List<Long> questionIds);

    /**
     * Add specific fields to a question
     * @param question QuestionSpecificFields data
     * @param questionId question identifier
     */
    Future<Optional<QuestionSpecificFields>> create(QuestionSpecificFields question, Long questionId);

    /**
     * Update specific fields of questions
     * @param questions JsonArray data
     */
    Future<List<QuestionSpecificFields>> update(List<Question> questions);
}
