package fr.openent.formulaire;

import fr.openent.formulaire.controller.FormulaireController;
import org.entcore.common.http.BaseServer;

public class Formulaire extends BaseServer {

	@Override
	public void start() throws Exception {
		super.start();

		addController(new FormulaireController());
	}
}