package fr.openent.form.core.constants;

public class Tables {
    public static final String DB_SCHEMA = "formulaire";
    public static final String CAPTCHA = DB_SCHEMA + ".captcha";
    public static final String DELEGATE = DB_SCHEMA + ".delegate";
    public static final String DISTRIBUTION = DB_SCHEMA + ".distribution";
    public static final String FOLDER = DB_SCHEMA + ".folder";
    public static final String FORM = DB_SCHEMA + ".form";
    public static final String FORM_SHARES = DB_SCHEMA + ".form_shares";
    public static final String GROUPS = DB_SCHEMA + ".groups";
    public static final String MEMBERS = DB_SCHEMA + ".members";
    public static final String QUESTION_CHOICE = DB_SCHEMA + ".question_choice";
    public static final String QUESTION = DB_SCHEMA + ".question";
    public static final String QUESTION_TYPE = DB_SCHEMA + ".question_type";
    public static final String REL_FORM_FOLDER = DB_SCHEMA + ".rel_form_folder";
    public static final String RESPONSE = DB_SCHEMA + ".response";
    public static final String RESPONSE_FILE = DB_SCHEMA + ".response_file";
    public static final String SECTION = DB_SCHEMA + ".section";
    public static final String USERS = DB_SCHEMA + ".users";

    private Tables() {
        throw new IllegalStateException("Utility class");
    }
}

