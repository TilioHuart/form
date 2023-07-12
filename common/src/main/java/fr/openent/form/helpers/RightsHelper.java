package fr.openent.form.helpers;

import fr.openent.form.core.constants.ShareRights;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class RightsHelper {
    private static final Logger log = LoggerFactory.getLogger(RightsHelper.class);

    private RightsHelper() {}

    public static boolean hasSharingRight(String right) {
        return right.equals(ShareRights.RESPONDER_RESOURCE_BEHAVIOUR) ||
                right.equals(ShareRights.CONTRIB_RESOURCE_BEHAVIOUR) ||
                right.equals(ShareRights.MANAGER_RESOURCE_BEHAVIOUR);
    }
}
