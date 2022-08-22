package fr.openent.form.core.constants;

public class EbFields {

    // Addresses
    public static final String CONVERSATION_ADDRESS = "org.entcore.conversation";
    public static final String FORMULAIRE_ADDRESS = "fr.openent.formulaire";
    public static final String WORKSPACE_ADDRESS = "org.entcore.workspace";


    // Actions
    public static final String LIST_SECTIONS = "list-sections";
    public static final String LIST_QUESTION_FOR_FORM_AND_SECTION = "list-question-for-form-and-section";
    public static final String LIST_QUESTION_CHOICES = "list-question-choices";


    private EbFields() {
        throw new IllegalStateException("Utility class");
    }
}
