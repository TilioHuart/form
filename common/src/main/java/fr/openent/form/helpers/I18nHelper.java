package fr.openent.form.helpers;

import fr.wseduc.webutils.I18n;
import fr.openent.form.core.enums.I18nKeys;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class I18nHelper {
    private static final Logger log = LoggerFactory.getLogger(I18nHelper.class);

    private I18nHelper() {}

    public static String getI18nValue(I18nKeys i18nKey, HttpServerRequest request) {
        return I18n.getInstance().translate(i18nKey.getKey(), I18n.DEFAULT_DOMAIN, I18n.acceptLanguage(request));
    }

    public static String getI18nValue(I18nKeys i18nKey, String locale) {
        return I18n.getInstance().translate(i18nKey.getKey(), I18n.DEFAULT_DOMAIN, locale);
    }
}
