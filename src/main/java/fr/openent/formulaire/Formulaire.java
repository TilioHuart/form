package fr.openent.formulaire;

import fr.openent.formulaire.controllers.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.http.BaseServer;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.share.impl.SqlShareService;
import org.entcore.common.sql.SqlConf;
import org.entcore.common.sql.SqlConfs;
import org.entcore.common.storage.Storage;
import org.entcore.common.storage.StorageFactory;

public class Formulaire extends BaseServer {
	private static final Logger log = LoggerFactory.getLogger(Formulaire.class);

	public static String DB_SCHEMA;
	public static String FORM_TABLE;
	public static String QUESTION_TABLE;
	public static String QUESTION_TYPE_TABLE;
	public static String QUESTION_CHOICE_TABLE;
	public static String RESPONSE_TABLE;
	public static String DISTRIBUTION_TABLE;

	public static final String ACCESS_RIGHT = "formulaire.access";
	public static final String CREATION_RIGHT = "formulaire.creation";
	public static final String SHARING_RIGHT = "formulaire.sharing";
	public static final String SENDING_RIGHT = "formulaire.sending";
	public static final String RESPONSE_RIGHT = "formulaire.response";

	public static final String READ_RESOURCE_RIGHT = "formulaire.read";
	public static final String RESPONDER_RESOURCE_RIGHT = "formulaire.responder";
	public static final String CONTRIB_RESOURCE_RIGHT = "formulaire.contrib";
	public static final String MANAGER_RESOURCE_RIGHT = "formulaire.manager";

	public static final String TO_DO = "TO_DO";
	public static final String IN_PROGRESS = "IN_PROGRESS";
	public static final String FINISHED = "FINISHED";

	@Override
	public void start() throws Exception {
		super.start();

		final EventBus eb = getEventBus(vertx);

		DB_SCHEMA = config.getString("db-schema");
		FORM_TABLE = DB_SCHEMA + ".form";
		QUESTION_TABLE = DB_SCHEMA + ".question";
		QUESTION_TYPE_TABLE = DB_SCHEMA + ".question_type";
		QUESTION_CHOICE_TABLE = DB_SCHEMA + ".question_choice";
		RESPONSE_TABLE = DB_SCHEMA + ".response";
		DISTRIBUTION_TABLE = DB_SCHEMA + ".distribution";

		final Storage storage = new StorageFactory(vertx, config, null).getStorage();

		SqlConf formConf = SqlConfs.createConf(FormController.class.getName());
		formConf.setSchema("formulaire");
		formConf.setTable("form");
		formConf.setShareTable("form_shares");

		FormController formController = new FormController(storage);
		formController.setShareService(new SqlShareService("formulaire", "form_shares", eb, securedActions, null));
		formController.setCrudService(new SqlCrudService("formulaire", "form"));

		addController(new FormulaireController());
		addController(formController);
		addController(new QuestionController());
		addController(new QuestionTypeController());
		addController(new QuestionChoiceController());
		addController(new ResponseController());
		addController(new DistributionController());
	}
}