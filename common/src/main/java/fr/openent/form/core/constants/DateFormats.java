package fr.openent.form.core.constants;

public class DateFormats {
    public static final String YYYY_MM_DD_T_HH_MM_SS_SSS = "yyyy-MM-dd'T'HH:mm:ss.SSS";
    public static final String DD_MM_YYYY_HH_MM = "dd/MM/yyyy HH:mm";
    public static final String DD_MM_YYYY = "dd/MM/yyyy";
    public static final String HH_MM = "HH:mm";

    private DateFormats() {
        throw new IllegalStateException("Utility class");
    }
}

