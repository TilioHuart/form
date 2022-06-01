package fr.openent.formulaire;

import fr.openent.formulaire.controllers.*;
import fr.openent.formulaire.cron.RgpdCron;
import fr.openent.formulaire.service.impl.FormulaireRepositoryEvents;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.http.BaseServer;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.share.impl.SqlShareService;
import org.entcore.common.sql.SqlConf;
import org.entcore.common.sql.SqlConfs;
import org.entcore.common.storage.Storage;
import org.entcore.common.storage.StorageFactory;

import java.util.ArrayList;
import java.util.List;
import fr.wseduc.cron.CronTrigger;

import static fr.openent.form.core.constants.Tables.DB_SCHEMA;

public class Formulaire extends BaseServer {
	private static final Logger log = LoggerFactory.getLogger(Formulaire.class);

	@Override
	public void start() throws Exception {
		super.start();

		final EventBus eb = getEventBus(vertx);
		final TimelineHelper timelineHelper = new TimelineHelper(vertx, eb, config);
		EventStore eventStore = EventStoreFactory.getFactory().getEventStore(Formulaire.class.getSimpleName());

		// Set RepositoryEvents implementation used to process events published for transition
		setRepositoryEvents(new FormulaireRepositoryEvents());

		final Storage storage = new StorageFactory(vertx, config).getStorage();


		// Create and parameter confs for all controllers using sharing system
		SqlConf distribConf = SqlConfs.createConf(DistributionController.class.getName());
		SqlConf formConf = SqlConfs.createConf(FormController.class.getName());
		SqlConf questionChoiceConf = SqlConfs.createConf(QuestionChoiceController.class.getName());
		SqlConf questionConf = SqlConfs.createConf(QuestionController.class.getName());
		SqlConf responseConf = SqlConfs.createConf(ResponseController.class.getName());
		SqlConf responseFileConf = SqlConfs.createConf(ResponseFileController.class.getName());
		SqlConf sectionConf = SqlConfs.createConf(SectionController.class.getName());

		List<SqlConf> confs = new ArrayList<>();
		confs.add(distribConf);
		confs.add(formConf);
		confs.add(questionChoiceConf);
		confs.add(questionConf);
		confs.add(responseConf);
		confs.add(responseFileConf);
		confs.add(sectionConf);

		for (SqlConf conf : confs) {
			conf.setSchema("formulaire");
			conf.setTable("form");
			conf.setShareTable("form_shares");
		}

		// Set sharing services to formController
		FormController formController = new FormController(eventStore, storage, timelineHelper);
		formController.setShareService(new SqlShareService(DB_SCHEMA, "form_shares", eb, securedActions, null));
		formController.setCrudService(new SqlCrudService(DB_SCHEMA, "form", "form_shares"));


		// Init controllers
		addController(new DelegateController());
		addController(new DistributionController(timelineHelper));
		addController(new EventBusController());
		addController(new FolderController());
		addController(formController);
		addController(new FormElementController());
		addController(new FormulaireController(eventStore));
		addController(new QuestionChoiceController());
		addController(new QuestionController());
		addController(new QuestionTypeController());
		addController(new ResponseController());
		addController(new ResponseFileController(storage));
		addController(new SectionController());
		addController(new UtilsController(storage));

		// CRON
		RgpdCron rgpdCron = new RgpdCron(storage);
		new CronTrigger(vertx, config.getString("rgpd-cron", "0 0 0 */1 * ? *")).schedule(rgpdCron);
	}
}