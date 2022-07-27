package fr.openent.formulaire_public;

import fr.openent.formulaire_public.controllers.CaptchaController;
import fr.openent.formulaire_public.controllers.FormulairePublicController;
import fr.openent.formulaire_public.controllers.FormController;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.http.BaseServer;
import org.entcore.common.notification.TimelineHelper;

public class FormulairePublic extends BaseServer {
	private static final Logger log = LoggerFactory.getLogger(FormulairePublic.class);

	@Override
	public void start() throws Exception {
		super.start();

		final EventBus eb = getEventBus(vertx);
		final TimelineHelper timelineHelper = new TimelineHelper(vertx, eb, config);
		EventStore eventStore = EventStoreFactory.getFactory().getEventStore(FormulairePublic.class.getSimpleName());

		// Init controllers
		addController(new CaptchaController());
		addController(new FormController(timelineHelper));
		addController(new FormulairePublicController());
	}
}