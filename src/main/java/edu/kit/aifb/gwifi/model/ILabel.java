package edu.kit.aifb.gwifi.model;

public interface ILabel {

	/**
	 * @return the text used to refer to concepts.
	 */
	public String getText();

	/**
	 * @return true if this label has ever been used to refer to an article, otherwise false
	 */
	public boolean exists();

	/**
	 * @return the number of articles that contain links with this label used as an anchor.
	 */
	public long getLinkDocCount();

	/**
	 * @return the number of links that use this label as an anchor.
	 */
	public long getLinkOccCount();

	/**
	 * @return the number of articles that mention this label (either as links or in plain text).
	 */
	public long getDocCount();

	/**
	 * @return the number of times this label is mentioned in articles (either as links or in plain text).
	 */
	public long getOccCount();

	/**
	 * @return the probability that this label is used as a link in Wikipedia ({@link #getLinkDocCount()}/
	 *         {@link #getDocCount()}.
	 */
	public double getLinkProbability();

	/**
	 * @return an array of {@link MongoSense Senses}, sorted by {@link MongoSense#getPriorProbability()}, that this label refers
	 *         to.
	 */
	public ISense[] getSenses();

}
