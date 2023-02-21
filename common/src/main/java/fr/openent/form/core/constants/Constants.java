package fr.openent.form.core.constants;

import fr.openent.form.core.enums.QuestionTypes;
import java.util.List;

public class Constants {
    public static int MAX_RESPONSES_EXPORT_PDF;
    public static int MAX_USERS_SHARING;
    public static final int NB_NEW_LINES = 10;
    public static final String DELETED_USER = "Utilisateur supprimé";
    public static final String DELETED_USER_FILE = "utilisateurSupprimé_Fichier";
    public static final String TRANSACTION_BEGIN_QUERY = "BEGIN;";
    public static final String TRANSACTION_COMMIT_QUERY = "COMMIT;";
    public static final List<Integer> GRAPH_QUESTIONS = QuestionTypes.getGraphQuestions();
    public static final List<Integer> CONDITIONAL_QUESTIONS = QuestionTypes.getConditionalQuestions();
    public static final List<Integer> CHOICES_TYPE_QUESTIONS = QuestionTypes.getChoicesTypeQuestions();
    public static final List<Integer> QUESTIONS_WITHOUT_RESPONSES = QuestionTypes.getQuestionsWithoutResponses();
    public static final List<Integer> FORBIDDEN_QUESTIONS = QuestionTypes.getForbiddenQuestions();
    public static final List<Integer> MATRIX_CHILD_QUESTIONS = QuestionTypes.getMatrixChildQuestions();
    public static final List<Integer> QUESTIONS_WITH_SPECIFICS = QuestionTypes.getQuestionsWithSpecificsFields();

    private Constants() {
        throw new IllegalStateException("Utility class");
    }
}

