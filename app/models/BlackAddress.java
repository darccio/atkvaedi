package models;

import siena.Generator;
import siena.Id;
import siena.Model;

public class BlackAddress extends Model {

	@Id(Generator.AUTO_INCREMENT)
	public Long id;
	
	public String address;
}
