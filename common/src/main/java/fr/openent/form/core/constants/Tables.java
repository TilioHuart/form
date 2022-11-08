package fr.openent.form.core.constants;

public class Tables {
    // For PSQL requests
    public static final String DB_SCHEMA = "formulaire";
    public static final String CAPTCHA_TABLE = DB_SCHEMA + ".captcha";
    public static final String DELEGATE_TABLE = DB_SCHEMA + ".delegate";
    public static final String DISTRIBUTION_TABLE = DB_SCHEMA + ".distribution";
    public static final String FOLDER_TABLE = DB_SCHEMA + ".folder";
    public static final String FORM_TABLE = DB_SCHEMA + ".form";
    public static final String FORM_SHARES_TABLE = DB_SCHEMA + ".form_shares";
    public static final String GROUPS_TABLE = DB_SCHEMA + ".groups";
    public static final String MEMBERS_TABLE = DB_SCHEMA + ".members";
    public static final String QUESTION_CHOICE_TABLE = DB_SCHEMA + ".question_choice";
    public static final String QUESTION_TABLE = DB_SCHEMA + ".question";
    public static final String QUESTION_TYPE_TABLE = DB_SCHEMA + ".question_type";

    public static final String QUESTION_SPECIFIC_FIELDS_TABLE = DB_SCHEMA + ".question_specific_fields";
    public static final String REL_FORM_FOLDER_TABLE = DB_SCHEMA + ".rel_form_folder";
    public static final String RESPONSE_TABLE = DB_SCHEMA + ".response";
    public static final String RESPONSE_FILE_TABLE = DB_SCHEMA + ".response_file";
    public static final String SECTION_TABLE = DB_SCHEMA + ".section";
    public static final String USERS_TABLE = DB_SCHEMA + ".users";

    // Names
    public static final String CAPTCHA = "captcha";
    public static final String DELEGATE = "delegate";
    public static final String DISTRIBUTION = "distribution";
    public static final String FOLDER = "folder";
    public static final String FORM = "form";
    public static final String FORM_SHARES = "form_shares";
    public static final String GROUPS = "groups";
    public static final String MEMBERS = "members";
    public static final String QUESTION_CHOICE = "question_choice";
    public static final String QUESTION = "question";
    public static final String QUESTION_SPECIFIC_FIELDS = "question_specific_fields";
    public static final String QUESTION_TYPE = "question_type";
    public static final String REL_FORM_FOLDER = "rel_form_folder";
    public static final String RESPONSE = "response";
    public static final String RESPONSE_FILE = "response_file";
    public static final String SECTION = "section";
    public static final String USERS = "users";

    private Tables() {
        throw new IllegalStateException("Utility class");
    }
}

