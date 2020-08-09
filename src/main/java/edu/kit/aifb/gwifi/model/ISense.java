package edu.kit.aifb.gwifi.model;

import edu.kit.aifb.gwifi.model.Page.PageType;

public interface ISense {

	/**
	 * @return the unique identifier
	 */
	public int getId();

	/**
	 * @return the title
	 */
	public String getTitle();
	
	/**
	 * @return	the type of the page
	 */
	public PageType getType();
	
	/**
	 * Returns the number of documents that contain links that use the surrounding label as anchor text, and point to this sense as the destination.
	 * 
	 * @return the number of documents that contain links that use the surrounding label as anchor text, and point to this sense as the destination.  
	 */
	public long getLinkDocCount();
	
	/**
	 * Returns the number of links that use the surrounding label as anchor text, and point to this sense as the destination.
	 * 
	 * @return the number of links that use the surrounding label as anchor text, and point to this sense as the destination.
	 */
	public long getLinkOccCount();
	
//	/**
//	 * Returns true if the surrounding label is used as a title for this sense article, otherwise false
//	 * 
//	 * @return true if the surrounding label is used as a title for this sense article, otherwise false
//	 */
//	public boolean isFromTitle();
//
//	/**
//	 * Returns true if the surrounding label is used as a redirect for this sense article, otherwise false
//	 * 
//	 * @return true if the surrounding label is used as a redirect for this sense article, otherwise false
//	 */
//	public boolean isFromRedirect(); 
	
	/**
	 * Returns the probability that the surrounding label goes to this destination 
	 * 
	 * @return the probability that the surrounding label goes to this destination 
	 */
	public double getPriorProbability();
	
	public double getMatchSimilarity();
	
//	/**
//	 * Returns true if this is the most likely sense for the surrounding label, otherwise false
//	 * 
//	 * @return true if this is the most likely sense for the surrounding label, otherwise false
//	 */
//	public boolean isPrimary();
	
}
