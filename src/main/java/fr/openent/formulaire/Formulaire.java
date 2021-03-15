package fr.openent.formulaire;

import fr.openent.formulaire.controllers.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.http.BaseServer;
import org.entcore.common.http.filter.sql.ShareAndOwner;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.share.impl.SqlShareService;
import org.entcore.common.sql.SqlConf;
import org.entcore.common.sql.SqlConfs;
import org.entcore.common.storage.Storage;
import org.entcore.common.storage.StorageFactory;

import java.util.ArrayList;
import java.util.List;

public class Formulaire extends BaseServer {
	private static final Logger log = LoggerFactory.getLogger(Formulaire.class);

	public static String DB_SCHEMA;
	public static String DISTRIBUTION_TABLE;
	public static String FORM_TABLE;
	public static String FORM_SHARES_TABLE;
	public static String GROUPS_TABLE;
	public static String MEMBERS_TABLE;
	public static String QUESTION_CHOICE_TABLE;
	public static String QUESTION_TABLE;
	public static String QUESTION_TYPE_TABLE;
	public static String RESPONSE_TABLE;
	public static String RESPONSE_FILE_TABLE;
	public static String USERS_TABLE;

	public static final String ACCESS_RIGHT = "formulaire.access";
	public static final String CREATION_RIGHT = "formulaire.creation";
	public static final String RESPONSE_RIGHT = "formulaire.response";

	public static final String CONTRIB_RESOURCE_RIGHT = "formulaire.contrib";
	public static final String MANAGER_RESOURCE_RIGHT = "formulaire.manager";
	public static final String RESPONDER_RESOURCE_RIGHT = "formulaire.comment";

	public static final String CONTRIB_RESOURCE_BEHAVIOUR = "fr-openent-formulaire-controllers-FormController|initContribResourceRight";
	public static final String MANAGER_RESOURCE_BEHAVIOUR = "fr-openent-formulaire-controllers-FormController|initManagerResourceRight";
	public static final String RESPONDER_RESOURCE_BEHAVIOUR = "fr-openent-formulaire-controllers-FormController|initResponderResourceRight";

	public static final String TO_DO = "TO_DO";
	public static final String IN_PROGRESS = "IN_PROGRESS";
	public static final String FINISHED = "FINISHED";

	@Override
	public void start() throws Exception {
		super.start();

		final EventBus eb = getEventBus(vertx);

		DB_SCHEMA = config.getString("db-schema");
		DISTRIBUTION_TABLE = DB_SCHEMA + ".distribution";
		FORM_TABLE = DB_SCHEMA + ".form";
		FORM_SHARES_TABLE = DB_SCHEMA + ".form_shares";
		GROUPS_TABLE = DB_SCHEMA + ".groups";
		MEMBERS_TABLE = DB_SCHEMA + ".members";
		QUESTION_CHOICE_TABLE = DB_SCHEMA + ".question_choice";
		QUESTION_TABLE = DB_SCHEMA + ".question";
		QUESTION_TYPE_TABLE = DB_SCHEMA + ".question_type";
		RESPONSE_TABLE = DB_SCHEMA + ".response";
		RESPONSE_FILE_TABLE = DB_SCHEMA + ".response_file";
		USERS_TABLE = DB_SCHEMA + ".users";

		final Storage storage = new StorageFactory(vertx, config).getStorage();

		SqlConf formConf = SqlConfs.createConf(FormController.class.getName());
		SqlConf distribConf = SqlConfs.createConf(DistributionController.class.getName());
		SqlConf responseConf = SqlConfs.createConf(ResponseController.class.getName());
		SqlConf responseFileConf = SqlConfs.createConf(ResponseFileController.class.getName());
		List<SqlConf> confs = new ArrayList<>();
		confs.add(formConf); confs.add(distribConf); confs.add(responseConf); confs.add(responseFileConf);

		for (SqlConf conf : confs) {
			conf.setSchema("formulaire");
			conf.setTable("form");
			conf.setShareTable("form_shares");
		}

		FormController formController = new FormController(storage);
		formController.setShareService(new SqlShareService(DB_SCHEMA, "form_shares", eb, securedActions, null));
		formController.setCrudService(new SqlCrudService(DB_SCHEMA, "form", "form_shares"));




		addController(new DistributionController());
		addController(formController);
		addController(new FormulaireController());
		addController(new QuestionChoiceController());
		addController(new QuestionController());
		addController(new QuestionTypeController());
		addController(new ResponseController());
		addController(new ResponseFileController(storage));
	}
}