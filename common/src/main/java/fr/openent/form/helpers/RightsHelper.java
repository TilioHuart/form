package fr.openent.form.helpers;

import fr.openent.form.core.constants.ShareRights;
import fr.wseduc.webutils.security.SecuredAction;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RightsHelper {
    private static final Logger log = LoggerFactory.getLogger(RightsHelper.class);

    private RightsHelper() {}

    public static boolean hasSharingRight(String right) {
        return right.equals(ShareRights.RESPONDER_RESOURCE_BEHAVIOUR) ||
                right.equals(ShareRights.CONTRIB_RESOURCE_BEHAVIOUR) ||
                right.equals(ShareRights.MANAGER_RESOURCE_BEHAVIOUR);
    }

    public static List<String> getRightMethods(String right, Map<String, SecuredAction> securedActions) {
        return securedActions.values().stream()
                .filter(action -> right.equals(action.getDisplayName()))
                .map(action -> action.getName().replaceAll("\\.", "-"))
                .collect(Collectors.toList());
    }
}
