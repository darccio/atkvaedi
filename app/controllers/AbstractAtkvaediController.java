package controllers;

import play.modules.gae.GAE;
import play.mvc.Before;
import play.mvc.Controller;

/**
 * Main class for Atkvaedi controllers to add some general information.
 */
public abstract class AbstractAtkvaediController extends Controller {

	@Before
	protected static void common() {
		renderArgs.put("name", "Atkvæði - Sistema de Participación");
		renderArgs.put("authenticityToken", session.getAuthenticityToken());

		if (GAE.isLoggedIn()) {
			renderArgs.put("isAdmin", GAE.isAdmin());
		}
	}
}
