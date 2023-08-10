package fr.openent.form.core.enums;

public enum I18nKeys {
    ARCHIVE_ZIP_NAME("formulaire.archive.zip.name"),
    COPY("formulaire.copy"),
    END_FORM("formulaire.access.end"),
    ERROR_QUESTION_DUPLICATE("formulaire.error.question.duplicate"),
    EXPORT_PDF_QUESTIONS_TITLE("formulaire.export.pdf.questions.title"),
    MAX_USERS_SHARING_ERROR("formulaire.share.error.max.users"),
    OTHER("formulaire.other");

    private final String key;

    I18nKeys(String key) {
        this.key = key;
    }

    public String getKey() {
        return this.key;
    }
}