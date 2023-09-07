package fr.openent.form.helpers;

import fr.openent.form.core.constants.DateFormats;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.TimeZone;

import static fr.openent.form.core.constants.DateFormats.DD_MM_YYYY;

public class DateHelper {

    public static final String PARIS_TIMEZONE = "Europe/Paris";

    private DateHelper() {
        throw new IllegalStateException("Utility class");
    }

    public static String now(String format, String timezone) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone(timezone));

        return simpleDateFormat.format(new Date());
    }

    public static String convertStringDateToOtherFormat(String stringDate, String actualFormat, String newFormat) {
        Date date;
        try {
            date = new SimpleDateFormat(actualFormat).parse(stringDate);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return new SimpleDateFormat(newFormat).format(date);
    }

    public static String formatDate(String inputDateStr) {
        DateFormats dateFormats = new DateFormats();
        for (String dateFormat : dateFormats.getDateFormats()) {
            try {
                LocalDate date = LocalDate.parse(inputDateStr, DateTimeFormatter.ofPattern(dateFormat));
                return DateTimeFormatter.ofPattern(DD_MM_YYYY).format(date);
            } catch (DateTimeParseException e) {
                // Try next format
            }
        }
        // No format working => is already in DAY Format
        return inputDateStr;
    }


    public static Date formatDateToModel(String inputDateStr, String format) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(format);
        Date result = new Date();
        try {
            result = inputDateStr != null ? dateFormatter.parse(inputDateStr) : null;
        }
        catch (ParseException e) { e.printStackTrace(); }
        return result;
    }
}
