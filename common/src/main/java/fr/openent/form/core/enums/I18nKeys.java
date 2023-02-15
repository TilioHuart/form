package fr.openent.form.core.enums;

public enum I18nKeys {
    ARCHIVE_ZIP_NAME("formulaire.archive.zip.name"),
    COPY("formulaire.copy"),
    OTHER("formulaire.other");

    private final String key;

    I18nKeys(String key) {
        this.key = key;
    }

    public String getKey() {
        return this.key;
    }
}