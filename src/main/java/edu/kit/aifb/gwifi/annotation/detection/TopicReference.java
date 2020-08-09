/*
 *    TopicReference.java
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


import edu.kit.aifb.gwifi.model.*;
import edu.kit.aifb.gwifi.util.*;
import edu.kit.aifb.gwifi.util.nlp.Language;

/**
 * A term or phrase that is either unambiguous or has been disambiguated so that
 * it refers to a particular wikipedia topic.
 * 
 * @author David Milne
 */
public class TopicReference implements Comparable<TopicReference> {

	private ILabel label;
	private Position position;
	private String context;
	private Language language;
	private String source;
	
	// the best candidate topic of this reference
	private int topicId;

	// private double disambigConfidence ;

	/**
	 * Initializes a disambiguated topic reference.
	 * 
	 * @param label
	 *            the label from which the reference was mined
	 * @param topicId
	 *            the id of the topic it was disambiguated to
	 * @param position
	 *            the location (start and end character indices) from which this
	 *            reference was mined
	 */
	public TopicReference(ILabel label, int topicId, Position position) {
		this.label = label;
		this.topicId = topicId;
		this.position = position;
		// this.disambigConfidence = disambigConfidence ;
	}
	
	//added by yunpeng for TM
	public TopicReference(ILabel label, int topicId, Position position, String source, Language language) {
		this(label,topicId,position);
		this.source=source;
		this.language=language;
		
		// this.disambigConfidence = disambigConfidence ;
	}
	/**
	 * Initializes a topic reference that may or may not be ambiguous
	 * 
	 * @param label
	 *            the label from which the reference was mined
	 * @param position
	 *            the location (start and end character indices) from which this
	 *            reference was mined
	 * @throws SQLException
	 *             if there is a problem with the Wikipedia database that the
	 *             label was obtained from
	 */
	public TopicReference(ILabel label, Position position) {
		this.label = label;
		this.position = position;

		ISense[] senses = label.getSenses();

		if (senses.length == 1) {
			topicId = senses[0].getId();
			// disambigConfidence = 1 ;
		} else {
			topicId = 0;
			// disambigConfidence = 0 ;
		}
	}

	public void setContext(String context) {
		this.context = context;
	}
	
	public void setTopicId(int id) {
		this.topicId = id;
	}

	/**
	 * @return true if the reference has been not been disambiguated yet,
	 *         otherwise false.
	 */
	public boolean isAmbiguous() {
		return topicId == 0;
	}

	/**
	 * @param tr
	 *            the topic reference to check for overlap
	 * @return true if this overlaps the given reference, otherwise false.
	 */
	public boolean overlaps(TopicReference tr) {
		return position.overlaps(tr.getPosition());
	}

	/**
	 * @return the label that reference was mined from
	 */
	public ILabel getLabel() {
		return label;
	}

	/**
	 * @return the id that this reference has been disambiguated to, or 0 if it
	 *         hasnt been disambiguated yet.
	 */
	public Integer getTopicId() {
		return topicId;
	}

	/**
	 * @return the position (start and end character locations) in the document
	 *         where this reference was found.
	 */
	public Position getPosition() {
		return position;
	}

	public String getContext() {
		
		return context;
	}
	
	public Language getLanguage() {
		return language;
	}

	public void setLanguage(Language language) {
		this.language = language;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	// public double getDisambigConfidence() {
	// return disambigConfidence ;
	// }
	
	public String toString() {
		return "[Label:" + label.getText() + "; " + position + "]";
	}

	public boolean equals(Object obj) {
		if(obj == this)
			return true;
		if(!(obj instanceof TopicReference))
			return false;
		TopicReference tr = (TopicReference)obj;
		if(compareTo(tr) == 0)
			return true;
		else 
			return false;
	}
	
	public int hashCode() {
		int hash = 1;
		hash = hash * 17 + position.getStart();
		hash = hash * 31 + position.getEnd();
		hash = hash * 13 + topicId;
		return hash;
	}
	
	public int compareTo(TopicReference tr) {

		if (position != null) {
			// starts first, then goes first
			int c = new Integer(position.getStart()).compareTo(tr.getPosition().getStart());
			if (c != 0)
				return c;

			// starts at same time, so longest one goes first
			c = new Integer(tr.getPosition().getEnd()).compareTo(position.getEnd());
			if (c != 0)
				return c;
		}

		return new Integer(topicId).compareTo(new Integer(tr.getTopicId()));
	}
}
