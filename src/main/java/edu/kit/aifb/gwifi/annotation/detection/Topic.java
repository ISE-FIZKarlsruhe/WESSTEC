/*
 *    Topic.java
 *    Copyright (C) 2007 David Milne, d.n.milne@gmail.com
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package edu.kit.aifb.gwifi.annotation.detection;

import java.util.Vector;

import edu.kit.aifb.gwifi.model.*;
import edu.kit.aifb.gwifi.util.*;

/**
 * This class represents a topic that was automatically detected and disambiguated in a document.
 * 
 * @author David Milne
 */
public class Topic extends Article {

	// since the same label text may occur more than once, but will always be
	// disambiguated the same way, one topic will be generated for each label text
	// and identified by the index
	private int index;

	// the references with the same label referring to this sense
	private Vector<TopicReference> references;
	private Vector<Position> positions;

	private double relatednessToContext;
	private double relatednessToAllTopics;

	// the probability that this label is used as a link in Wikipedia
	private double totalLinkProbability;
	private double maxLinkProbability;

	// the word-based context similarity between the reference and the sense
	private double totalSimilarity;
	private double maxSimilarity;

	private double totalDisambigConfidence;
	private double maxDisambigConfidence;

	// the probability that the surrounding label goes to this destination
	private double commonness;

	private double docLength;

	private String displayName;
	private String URI;

	/**
	 * Initializes a new topic
	 * 
	 * @param wikipedia
	 *            an active instance of Wikipedia
	 * @param id
	 *            the id of the article that this topic represents
	 * @param relatednessToContext
	 *            the extent to which this topic relates to the surrounding unambiguous context
	 * @param docLength
	 *            the length of the document, in characters
	 */
	public Topic(Wikipedia wikipedia, int id, double relatednessToContext, double commonness, double docLength) {
		super(wikipedia.getEnvironment(), id);

		this.relatednessToContext = relatednessToContext;
		this.relatednessToAllTopics = -1;
		this.docLength = docLength;
		this.commonness = commonness;

		references = new Vector<TopicReference>();
		positions = new Vector<Position>();
		totalLinkProbability = 0;
		maxLinkProbability = 0;
		totalSimilarity = 0;
		maxSimilarity = 0;
		totalDisambigConfidence = 0;
		maxDisambigConfidence = 0;
	}

	public void setTotalSimilarity(double totalSimilarity) {
		this.totalSimilarity = totalSimilarity;
	}

	public void setMaxSimilarity(double maxSimilarity) {
		this.maxSimilarity = maxSimilarity;
	}

	public void setTotalDisambigConfidence(double totalDisambigConfidence) {
		this.totalDisambigConfidence = totalDisambigConfidence;
	}

	public void setMaxDisambigConfidence(double maxDisambigConfidence) {
		this.maxDisambigConfidence = maxDisambigConfidence;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public int getIndex() {
		return index;
	}

	/**
	 * Adds an ngram occurance in the document that refers to this topic
	 * 
	 * @param reference
	 *            the refering ngram (and it's location)
	 * @param disambigConfidence
	 *            the confidence with which the disambiguator chose this topic as the correct sense for the ngram
	 */
	// public void addReference(TopicReference reference, double
	// disambigConfidence){
	// references.add(reference);
	// positions.add(reference.getPosition()) ;
	//
	// double prob = reference.getLabel().getLinkProbability() ;
	//
	// totalLinkProbability = totalLinkProbability + prob ;
	// if (prob > maxLinkProbability)
	// maxLinkProbability = prob ;
	//
	//
	// totalDisambigConfidence = totalDisambigConfidence + disambigConfidence ;
	// if (disambigConfidence > maxDisambigConfidence)
	// maxDisambigConfidence = disambigConfidence ;
	// }
	public void addReference(TopicReference reference) {
		references.add(reference);
		positions.add(reference.getPosition());

		double prob = reference.getLabel().getLinkProbability();

		totalLinkProbability = totalLinkProbability + prob;
		if (prob > maxLinkProbability)
			maxLinkProbability = prob;
	}

	/**
	 * @return the locations in this document that refer to this topic
	 */
	public Vector<Position> getPositions() {
		return positions;
	}

	/**
	 * @return the references in this document that refer to this topic
	 */
	public Vector<TopicReference> getReferences() {
		return references;
	}

	/**
	 * @return the number of times this topic is refered to.
	 */
	public int getOccurances() {
		return positions.size();
	}

	public double getNormalizedOccurances() {
		return Math.log(positions.size() + 1);
	}

	/**
	 * @return the extent to which this topic relates to surrounding unambiguous context.
	 */
	public double getRelatednessToContext() {
		return relatednessToContext;
	}

	/**
	 * @return the probability that the surrounding label goes to this topic.
	 */
	public double getCommenness() {
		return commonness;
	}

	/**
	 * @return the extent to which this topic relates to all other topics detected in the document.
	 * @throws Exception
	 *             if this has not been calculated yet (this is the last step performed by the topic detector).
	 */
	public double getRelatednessToOtherTopics() throws Exception {

		if (relatednessToAllTopics < 0) {
			throw new Exception("Relatedness to context not calcuated yet!");
		}

		return relatednessToAllTopics;
	}

	/**
	 * Sets the relatedness of this topic to all other topics detected in the document.
	 * 
	 * @param r
	 *            the extent to which this topic relates to all other topics detected in the document.
	 */
	public void setRelatednessToOtherTopics(float r) {
		this.relatednessToAllTopics = r;
	}

	/**
	 * @return the maximum probability that the ngrams which refer to this topic would be links (rather than plain text)
	 *         if found in a random wikipedia article.
	 */
	public double getMaxLinkProbability() {
		return maxLinkProbability;
	}

	public double getNormalizedMaxLinkProbability() {
		double mlp = getMaxLinkProbability();

		mlp = Math.log((mlp * 1000) + 1);
		mlp = mlp / 4;

		return mlp;
	}

	/**
	 * @return the average probability that the ngrams which refer to this topic would be links (rather than plain text)
	 *         if found in a random wikipedia article.
	 */
	public double getAverageLinkProbability() {
		return totalLinkProbability / positions.size();
	}

	public double getNormalizedAverageLinkProbability() {
		double alp = getAverageLinkProbability();

		alp = Math.log((alp * 1000) + 1);
		alp = alp / 4;

		return alp;
	}

	/**
	 * @return the maximum similarity between the contexts of the ngrams and this topic.
	 */
	public double getMaxSimilarity() {
		return maxSimilarity;
	}

	/**
	 * @return the total similarity between the contexts of the ngrams and this topic.
	 */
	public double getTotalSimilarity() {
		return totalSimilarity;
	}

	/**
	 * @return the average similarity between the contexts of the ngrams and this topic.
	 */
	public double getAverageSimilarity() {
		return totalSimilarity / positions.size();
	}

	/**
	 * @return the maximum confidence with which the disambiguator chose this topic as the correct sense for the ngrams
	 *         from which it was mined.
	 */
	public double getMaxDisambigConfidence() {
		return maxDisambigConfidence;
	}

	/**
	 * @return the total confidence with which the disambiguator chose this topic as the correct sense for the ngrams
	 *         from which it was mined.
	 */
	public double getTotalDisambigConfidence() {
		return totalDisambigConfidence;
	}

	/**
	 * @return the average confidence with which the disambiguator chose this topic as the correct sense for the ngrams
	 *         from which it was mined.
	 */
	public double getAverageDisambigConfidence() {
		return totalDisambigConfidence / positions.size();
	}

	/**
	 * @return the distance between the start of the document and the first occurance of this topic, normalized by
	 *         document length
	 */
	public double getFirstOccurance() {
		Position start = positions.firstElement();
		return ((double) start.getStart()) / docLength;
	}

	/**
	 * @return the distance between the end of the document and the last occurance of this topic, normalized by document
	 *         length
	 */
	public double getLastOccurance() {
		Position end = positions.lastElement();
		return ((double) end.getStart()) / docLength;
	}

	/**
	 * @return the distance between the first and last occurances of this topic, normalized by document length
	 */
	public double getSpread() {
		return getLastOccurance() - getFirstOccurance();
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		if (displayName == null)
			return title;
		else
			return displayName;
	}

	public void setURI(String URI) {
		this.URI = URI;
	}

	public String getURI() {
		return URI;
	}
	
	public String toString() {
		return references.get(0).toString() + ":[" + getTitle() + ":" + commonness + ":" + weight + "]";
	}

	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (!(obj instanceof Topic))
			return false;
		Topic tr = (Topic) obj;
		if (!(id == tr.id))
			return false;
		else
			return positions.equals(tr.positions);
	}

	public int hashCode() {
		int hash = 1;
		hash = hash * 17 + positions.hashCode();
		hash = hash * 13 + id;
		return hash;
	}

}
