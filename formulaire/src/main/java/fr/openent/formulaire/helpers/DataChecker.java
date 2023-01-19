package fr.openent.formulaire.helpers;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static fr.openent.form.core.constants.Constants.RGPD_LIFETIME_VALUES;
import static fr.openent.form.core.constants.Fields.*;
import static fr.openent.form.helpers.UtilsHelper.getByProp;

public class DataChecker {
    private static final Logger log = LoggerFactory.getLogger(DataChecker.class);
    private static final SimpleDateFormat formDateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    private DataChecker() {}

    // Check if date_ending is before date_opening or before current time
    public static boolean checkFormDatesValidity(JsonArray forms) {
        boolean areDateValid = true;
        int i = 0;
        while (areDateValid && i < forms.size()) {
            String openingDate = forms.getJsonObject(i).getString(DATE_OPENING, null);
            String endingDate = forms.getJsonObject(i).getString(DATE_ENDING, null);
            if (endingDate != null) {
                try {
                    Date finalOpeningDate = openingDate != null ? formDateFormatter.parse(openingDate) : new Date();
                    areDateValid = formDateFormatter.parse(endingDate).after(finalOpeningDate);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            i++;
        }

        return areDateValid;
    }

    public static boolean checkFormDatesValidity(JsonObject form) {
        return checkFormDatesValidity(new JsonArray().add(form));
    }

    // Check if RGPD lifetime is in [3, 6, 9, 12]
    public static boolean checkRGPDLifetimeValidity(JsonArray forms) {
        boolean areRGPDLifeTimeValid = true;
        int i = 0;
        while (areRGPDLifeTimeValid && i < forms.size()) {
            Integer rgpdLifetime = forms.getJsonObject(i).getInteger(RGPD_LIFETIME);
            areRGPDLifeTimeValid = rgpdLifetime != null && RGPD_LIFETIME_VALUES.contains(rgpdLifetime);
            i++;
        }

        return areRGPDLifeTimeValid;
    }

    // Check if one of the folders is not owned by the connected user
    public static boolean checkFolderIdsValidity(JsonArray folders, String userId) {
        JsonArray userIds = getByProp(folders, USER_ID);
        boolean areUserIdsOk = true;
        int i = 0;
        while (areUserIdsOk && i < userIds.size()) {
            if (!userIds.getValue(i).equals(userId)) {
                areUserIdsOk = false;
            }
            i++;
        }
        return areUserIdsOk;
    }

    // Check if one of the sections has a wrong position value
    public static boolean checkSectionPositionsValidity(JsonArray sections) {
        JsonArray positions = getByProp(sections, POSITION);
        boolean arePositionsOk = true;
        int i = 0;
        while (arePositionsOk && i < positions.size()) {
            Integer position = (Integer) positions.getValue(i);
            if (position == null || position < 1) {
                arePositionsOk = false;
            }
            i++;
        }
        return arePositionsOk;
    }

    // Check if one of the forms is public
    public static boolean hasPublicForm(JsonArray forms) {
        JsonArray publicProps = getByProp(forms, IS_PUBLIC);
        int i = 0;
        while (i < publicProps.size()) {
            boolean publicProp = publicProps.getBoolean(i);
            if (publicProp) {
                return true;
            }
            i++;
        }
        return false;
    }

}
