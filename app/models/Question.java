package models;

import java.util.Date;

import play.data.validation.Required;
import siena.DateTime;
import siena.Generator;
import siena.Id;
import siena.Model;

/**
 * Modeled after Ideatorrent way of work and terminology.
 */
public class Question extends Model {

	@Id(Generator.AUTO_INCREMENT)
	public Long id;

	@DateTime
	@Required
	public Date createdAt;

	public String title;

	public String description;

	public String owner;

	public Boolean close;

	/*
	 * Votes (yes/no/don't care)
	 * 
	 * TODO Improve concurrent access (sharding?):
	 * http://code.google.com/intl/en/appengine/articles/sharding_counters.html
	 */
	public Long yes;

	public Long no;

	public Long dontCare;

	public Question() {
		this.yes = 0L;
		this.no = 0L;
		this.dontCare = 0L;
	}

	public void vote(String value) {
		for (;;) {
			if (value.equals("yes")) {
				this.yes = this.yes + 1;
				break;
			}

			if (value.equals("no")) {
				this.no = this.no + 1;
				break;
			}

			if (value.equals("dontCare")) {
				this.dontCare = this.dontCare + 1;
				break;
			}

			break;
		}
	}
}
