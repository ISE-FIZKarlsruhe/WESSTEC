package edu.kit.aifb.gwifi.model;

import edu.kit.aifb.gwifi.db.WEnvironment;
import edu.kit.aifb.gwifi.db.struct.DbPage;

/**
 * Represents disambiguation pages in Wikipedia, which lists possible senses for an ambiguous term
 */
public class Disambiguation extends Article {

	
	/**
	 * Initialises a newly created Disambiguation so that it represents the disambiguation page given by <em>id</em>.
	 * 
	 * @param env	an active WikipediaEnvironment
	 * @param id	the unique identifier of the article
	 */
	public Disambiguation(WEnvironment env, int id) {
		super(env, id) ;
	}
	
	protected Disambiguation(WEnvironment env, int id, DbPage pd) {
		super(env, id, pd) ;
	}
	
}
