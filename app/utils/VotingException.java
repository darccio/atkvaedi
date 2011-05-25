package utils;

/**
 * Used to handle exceptions related with checking a vote.
 */
public class VotingException extends Exception {

	private Object[] objects;

	public VotingException(String message, Object... objects) {
		super(message);

		this.objects = objects;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder(getMessage());
		sb.append("[");

		int i = 0;
		for (Object object : objects) {
			sb.append(object.toString());

			if (i < objects.length - 1) {
				sb.append(";");
			}
		}
		sb.append("]");

		return sb.toString();
	}
}
