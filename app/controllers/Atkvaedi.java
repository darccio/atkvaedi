package controllers;

import java.util.logging.Logger;

import models.BlackAddress;
import models.Question;
import models.Vote;
import play.Play;
import play.modules.gae.GAE;
import play.mvc.Http.Cookie;
import siena.Model;
import siena.Query;
import utils.VotingException;

/**
 * Main class.
 * 
 * It handles all public operations with questions.
 * 
 * <ul>
 * <li>Listing all open questions</li>
 * <li>Showing specific question (with or without results)</li>
 * <li>Voting a question</li>
 * </ul>
 * 
 * It also does the following requirements:
 * 
 * <ul>
 * <li>Firewalling questions (based on IP and cookie data)</li>
 * <li>Checking and "parsing" valid votes</li>
 * </ul>
 * 
 * This is intended to be hosted on Google App Engine (GAE/J), using <a
 * href="http://www.playframework.org/">Play! Framework</a>.
 */
public class Atkvaedi extends AbstractAtkvaediController {

	private static final String ATKVAEDI_COOKIE = "atkvaedi";

	/**
	 * Main page controller.
	 */
	public static void index() {
		if (GAE.isLoggedIn()) {
			if (GAE.isAdmin()) {
				Admin.dashboard();
			}
		}

		// There is no pagination. It shows the last five questions.
		renderArgs.put("questions",
				Model.all(Question.class).order("-createdAt").fetch(5));

		render();
	}

	/**
	 * Loads and format question to show it.
	 * 
	 * @param id
	 *            Question identifier.
	 */
	public static void question(Long id) {
		Question question = Model.all(Question.class).filter("id", id).get();
		if (question == null) {
			notFound();
		}

		question.description = question.description
				.replaceAll("\n", "<br>")
				.replaceAll(
						"(https?://[-A-Za-z0-9+&@#/%?=~_|!:,.;]*[-A-Za-z0-9+&@#/%=~_|])",
						"<a href=\"$1\">$1</a>");

		String control = play.libs.Codec.UUID();
		boolean voted = false;
		try {
			checkVote(id, control, getSourceAddress());
		} catch (VotingException ve) {
			// If we have this kind of exception, it has been already voted
			voted = true;
		}

		renderArgs.put("voted", voted);
		renderArgs.put("control", control);
		renderArgs.put("question", question);
		render();
	}

	/**
	 * Vote a question.
	 * 
	 * @param id
	 *            Question id.
	 * @param control
	 *            Control UUID used to ensure uniqueness.
	 */
	public static void vote(Long id, String control) {
		Question question = Model.all(Question.class).filter("id", id).get();
		if (question == null) {
			flash.error("No existe esta votación");
		} else {
			try {
				if (validation.hasErrors()) {
					params.flash();
					validation.keep();

					// TODO check if it is really always a captcha error
					throw new VotingException("No has resuelto bien el captcha");
				}

				// Ensuring HTTPS connection for voting
				if (!request.secure && Play.mode.isProd()) {
					throw new VotingException("Voto no válido (Error 0x29A5)");
				}

				String value = parseVote();
				if (value == null) {
					throw new VotingException("Voto no válido (Error 0x29A1)");
				}

				String source = getSourceAddress();
				firewall(source);

				checkVote(id, control, source);

				question.vote(value);
				question.update();

				Vote vote = new Vote();
				vote.question = id;
				vote.source = source;
				vote.session = control;
				vote.insert();

				/*
				 * We update the cookie adding question id in a string like
				 * id.id.id.
				 * 
				 * This is a fucking shame but it worked as KISS. It must be
				 * improved, although it is not stored in plain text in user
				 * cookie.
				 */
				StringBuilder sb = new StringBuilder();
				Cookie cookie = getVotingCookie();
				if (cookie != null) {
					sb.append(cookie.value);
				}

				sb.append(".");
				sb.append(id);
				sb.append(".");
				setVotingCookie(sb);

				flash.success("Gracias por votar");
			} catch (VotingException ve) {
				flash.error(ve.getMessage());
				Logger.getLogger("edm-sp").warning(ve.toString());
			}
		}

		// Redirecting to question homepage
		Atkvaedi.question(id);
	}

	/**
	 * Retrieves user cookie contents (encrypted in AES)
	 * 
	 * @return String containing questions voted by user in format "id.id.id."
	 */
	private static Cookie getVotingCookie() {
		Cookie cookie = request.cookies.get(ATKVAEDI_COOKIE);
		if (cookie != null) {
			cookie.value = play.libs.Crypto.decryptAES(cookie.value,
					getPrivateKey());
		}

		return cookie;
	}

	/**
	 * 
	 * @param sb
	 *            Buffer with voted questions id.
	 */
	private static void setVotingCookie(StringBuilder sb) {
		response.setCookie(ATKVAEDI_COOKIE,
				play.libs.Crypto.encryptAES(sb.toString(), getPrivateKey()),
				"4242d");
	}

	private static String getPrivateKey() {
		return Play.configuration.getProperty("application.secret").substring(
				0, 16);
	}

	private static String getSourceAddress() {
		return request.current().remoteAddress;
	}

	/**
	 * Checks the requesting address in our blacklist.
	 * 
	 * @param source
	 * @throws VotingException
	 */
	private static void firewall(String source) throws VotingException {
		BlackAddress address = Model.all(BlackAddress.class)
				.filter("address", source).get();
		if (address != null) {
			throw new VotingException("Voto no válido (Error 0x29A3)");
		}
	}

	/**
	 * 
	 * @param questionId
	 * @param control
	 *            UUID for uniqueness
	 * @param sourceAddress
	 * 
	 * @throws VotingException
	 */
	private static void checkVote(Long questionId, String control,
			String sourceAddress) throws VotingException {
		Vote vote = null;

		Cookie cookie = getVotingCookie();
		if (cookie != null) {
			if (cookie.value.contains("." + questionId + ".")) {
				throw new VotingException("Ya has votado");
			}
		}

		if (Play.mode.isProd()) {
			vote = getVoteFor(questionId).filter("source", sourceAddress).get();
		}

		if (vote == null) {
			vote = getVoteFor(questionId).filter("session", control).get();
		}

		if (vote != null) {
			vote.source = sourceAddress;
			vote.session = control;

			throw new VotingException("Ya has votado", vote);
		}
	}

	/**
	 * Get query for votes done in a question.
	 * 
	 * @param questionId
	 * @return
	 */
	private static Query<Vote> getVoteFor(Long questionId) {
		return Model.all(Vote.class).filter("question", questionId);
	}

	/**
	 * Checks HTTP POST data to know what kind of vote is this.
	 */
	private static String parseVote() {
		String value = null;
		if (params.allSimple().keySet().contains("yes")) {
			value = "yes";
		} else {
			if (params.allSimple().keySet().contains("no")) {
				value = "no";
			} else {
				if (params.allSimple().keySet().contains("dontCare")) {
					value = "dontCare";
				}
			}
		}

		return value;
	}

	/**
	 * Method to redirect to login. Check Play! framework docs about
	 * controllers.
	 */
	public static void login() {
		GAE.login("Atkvaedi.index");
	}

	/**
	 * Method to redirect to logout. Check Play! framework docs about
	 * controllers.
	 */
	public static void logout() {
		GAE.logout("Atkvaedi.index");
	}
}
