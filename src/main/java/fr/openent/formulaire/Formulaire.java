package fr.openent.formulaire;

import fr.openent.formulaire.controllers.*;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.http.BaseServer;

public class Formulaire extends BaseServer {
	private static final Logger log = LoggerFactory.getLogger(Formulaire.class);

	public static String DB_SCHEMA;
	public static String FORM_TABLE;
	public static String QUESTION_TABLE;
	public static String QUESTION_TYPE_TABLE;
	public static String RESPONSE_TABLE;
	public static String DISTRIBUTION_TABLE;

	public static final String ACCESS_RIGHT = "formulaire.access";
	public static final String CREATION_RIGHT = "formulaire.creation";
	public static final String SHARING_RIGHT = "formulaire.sharing";
	public static final String SENDING_RIGHT = "formulaire.sending";
	public static final String RESPONSE_RIGHT = "formulaire.response";

	public static final String TO_DO = "TO_DO";
	public static final String IN_PROGRESS = "IN_PROGRESS";
	public static final String FINISHED = "FINISHED";

	@Override
	public void start() throws Exception {
		super.start();

		DB_SCHEMA = config.getString("db-schema");
		FORM_TABLE = DB_SCHEMA + ".form";
		QUESTION_TABLE = DB_SCHEMA + ".question";
		QUESTION_TYPE_TABLE = DB_SCHEMA + ".question_type";
		RESPONSE_TABLE = DB_SCHEMA + ".response";
		DISTRIBUTION_TABLE = DB_SCHEMA + ".distribution";

		addController(new FormulaireController());
		addController(new FormController());
		addController(new QuestionController());
		addController(new QuestionTypeController());
		addController(new ResponseController());
		addController(new DistributionController());
	}
}