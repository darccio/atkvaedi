package controllers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Date;

import models.BlackAddress;
import models.Question;
import play.modules.gae.GAE;
import play.mvc.Before;
import siena.Model;

import com.google.appengine.api.users.User;

/**
 * Backend controller.
 */
public class Admin extends AbstractAtkvaediController {

	@Before
	static void checkAuthentification() {
		if (!GAE.isLoggedIn()) {
			Atkvaedi.login();
		}

		if (!GAE.isAdmin()) {
			flash.error("Acceso no autorizado.");
			GAE.logout("Atkvaedi.index");
		}
	}

	/**
	 * Backend main. Used for list all the questions.
	 */
	public static void dashboard() {
		renderArgs.put(
				"questions",
				Model.all(Question.class).filter("owner", getUser())
						.order("-createdAt").fetch());
		render();
	}

	/**
	 * Load "create question" form.
	 */
	public static void create() {
		renderArgs.put("submitText", "Crear");
		render("Admin/edit.html");
	}

	/**
	 * Load edition form.
	 * 
	 * @param id
	 */
	public static void edit(Long id) {
		Question question = getQuestion(id);
		if (question == null) {
			flash.error("No encontrado o no tienes permisos.");
			flash.keep();

			// Redirection
			Admin.dashboard();
		}

		renderArgs.put("question", question);
		renderArgs.put("submitText", "Editar");
		render("Admin/edit.html");
	}

	/**
	 * Common method to get question by id (if it belongs to the logged user).
	 * 
	 * @param id
	 *            Question id.
	 * @return
	 */
	private static Question getQuestion(Long id) {
		Question question = Model.all(Question.class).filter("id", id)
				.filter("owner", getUser()).get();

		return question;
	}

	/**
	 * Delete a question.
	 * 
	 * @param id
	 *            Question id.
	 */
	public static void delete(Long id) {
		Question question = getQuestion(id);
		if (question != null) {
			question.delete();
		} else {
			flash.error("No encontrado o no tienes permisos.");
			flash.keep();
		}

		Admin.dashboard();
	}

	/**
	 * Close voting on a question.
	 * 
	 * @param id
	 */
	public static void close(Long id) {
		Question question = getQuestion(id);
		if (question != null) {
			question.close = true;
			question.update();
		} else {
			flash.error("No encontrado.");
			flash.keep();
		}

		Admin.dashboard();
	}

	/**
	 * Open a question for voting.
	 * 
	 * @param id
	 */
	public static void open(Long id) {
		Question question = getQuestion(id);
		if (question != null) {
			question.close = false;
			question.update();
		} else {
			flash.error("No encontrado o no tienes permisos.");
			flash.keep();
			Admin.dashboard();
		}

		Admin.dashboard();
	}

	/**
	 * Update or create a question.
	 * 
	 * @param question
	 */
	public static void update(Question question) {
		// TODO share magic number between backend and frontend (edition
		// question form).
		if (question.title.length() > 140) {
			flash.error("Título demasiado largo");
			edit(question.id);
		}

		// TODO share magic number between backend and frontend (edition
		// question form).
		if (question.description.length() > 1000) {
			flash.error("Descripción demasiado larga");
			edit(question.id);
		}

		question.description = play.utils.HTML.htmlEscape(question.description);
		if (question.id == null) {
			question.createdAt = new Date();
			question.owner = getUser();
			question.close = false;
			question.insert();
		} else {
			Question base = getQuestion(question.id);
			if (base == null) {
				flash.error("No encontrado o no tienes permisos.");
				flash.keep();
			} else {
				question.owner = base.owner;
				question.createdAt = base.createdAt;
				question.update();
			}
		}

		Admin.dashboard();
	}

	/**
	 * Get current user. Based on GAE login system, so the system is ready for
	 * handle different accounts.
	 * 
	 * @return
	 */
	private static String getUser() {
		User user = GAE.getUser();
		String email = null;

		if (user != null) {
			email = user.getEmail();
		}

		return play.libs.Codec.hexMD5(email);
	}

	/**
	 * Load upload blacklist form.
	 */
	public static void blacklist() {
		renderArgs.put("submitText", "Cargar");
		render();
	}

	/**
	 * Updates blacklist from POST'd data (newline separated list).
	 * 
	 * @param list
	 */
	public static void blacklisted(String list) {
		BufferedReader in = new BufferedReader(new StringReader(list));

		String line = null;
		try {
			while ((line = in.readLine()) != null) {
				BlackAddress blackAddress = Model.all(BlackAddress.class)
						.filter("address", line).get();

				if (blackAddress == null) {
					blackAddress = new BlackAddress();
					blackAddress.address = line;
					blackAddress.insert();
				}
			}

			flash.success("Blacklist cargada");
		} catch (IOException e) {
			flash.error(e.getMessage());
			e.printStackTrace();
		}

		Admin.dashboard();
	}
}
