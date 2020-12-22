package fr.openent.formulaire;

import fr.openent.formulaire.controller.FormController;
import fr.openent.formulaire.controller.FormulaireController;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.http.BaseServer;

public class Formulaire extends BaseServer {
	private static final Logger log = LoggerFactory.getLogger(Formulaire.class);

	public static String DB_SCHEMA;
	public static String FORM_TABLE ;
	public static String QUESTION_TABLE;
	public static String RESPONSE_TABLE ;

	public static final String view = "formulaire.view";

	@Override
	public void start() throws Exception {
		super.start();

		DB_SCHEMA = config.getString("db-schema");
		FORM_TABLE = DB_SCHEMA + ".form";
		QUESTION_TABLE = DB_SCHEMA + ".question";
		RESPONSE_TABLE = DB_SCHEMA + ".response";

		addController(new FormulaireController());
		addController(new FormController());
//		addController(new QuestionController());
//		addController(new ResponseController());
	}
}