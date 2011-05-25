package models;

import siena.Generator;
import siena.Id;
import siena.Model;

/**
 * Vote. Used for avoiding double voting based in IP and cookie.
 * 
 * It doesn't stores what kind of vote was issued, so it keeps anonymity of the
 * vote because it is not possible to know what was voted.
 * 
 * You can only know that somebody with an IP at an unknown time voted in a
 * question.
 */
public class Vote extends Model {

	@Id(Generator.AUTO_INCREMENT)
	public Long id;

	public Long question;

	public String source;

	public String session;

	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("Id: ");
		sb.append(id);
		sb.append(", Question: ");
		sb.append(question);
		sb.append(", Source: ");
		sb.append(source);
		sb.append(", Session: ");
		sb.append(session);

		return sb.toString();
	}
}
