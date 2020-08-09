/*
 *    DocumentTagger.java
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

package edu.kit.aifb.gwifi.annotation.tagging;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;
import java.util.Vector;

import edu.kit.aifb.gwifi.annotation.detection.Topic;
import edu.kit.aifb.gwifi.annotation.detection.TopicReference;
import edu.kit.aifb.gwifi.annotation.preprocessing.PreprocessedDocument;
import edu.kit.aifb.gwifi.service.Service.RepeatMode;
import edu.kit.aifb.gwifi.util.Position;

/**
 * Tags documents by adding markup to to the topics it mentions.
 * <p>
 * You can use this to tag all topics by giving it the output of the TopicDetector, or just those that should be linked
 * to (from LinkDetector) or those that the document is about (from Indexers). If using the latter two, then you should
 * first modify the list of topics to include only those that are most likely to be links or key topics (i.e those above
 * a certain weight).
 * 
 * @author David Milne
 */
public abstract class DocumentTagger {

	/**
	 * Specifies how terms in the document will be replaced by tags. A tagger for html, for example, might return a link
	 * to the relevant Wikipedia article.
	 * 
	 * @param term
	 *            the text in the original document that will be tagged.
	 * @param topic
	 *            the relevant topic that the term was disambiguated to.
	 * @return the tag that will replace the given term.
	 */
	public abstract String getTag(String term, Topic topic);

	public String tag(PreprocessedDocument doc, Collection<Topic> topics, RepeatMode repeatMode) {
		return tag(doc, topics, repeatMode, null, null);
	}

	/**
	 * Tags the given text with occurrences of the given topics.
	 * 
	 * @param doc
	 *            the document to be tagged
	 * @param detectedTopics
	 *            a set of automatically detected topics, i.e. from TopicDetector or LinkDetector
	 * @param repeatMode
	 *            ALL, FIRST, or FIRST_IN_REGION
	 * @return the tagged text
	 */
	public String tag(PreprocessedDocument doc, Collection<Topic> detectedTopics, RepeatMode repeatMode,
			Collection<Position> overlappedPositions, Collection<Position> filteredPositions) {

		doc.resetRegionTracking();

		HashMap<Integer, List<Topic>> topicsById = new HashMap<Integer, List<Topic>>();
		for (Topic topic : detectedTopics) {
			List<Topic> list = topicsById.get(topic.getId());
			if (list == null) {
				list = new ArrayList<Topic>();
				topicsById.put(topic.getId(), list);
			}
			list.add(topic);
		}

		Vector<TopicReference> references = resolveCollisions(detectedTopics, overlappedPositions);

		String originalText = doc.getOriginalText();
		StringBuffer wikifiedText = new StringBuffer();
		int lastIndex = 0;

		HashSet<Integer> doneIds = new HashSet<Integer>();

		for (TopicReference reference : references) {
			int start = reference.getPosition().getStart();
			int end = reference.getPosition().getEnd();
			int id = reference.getTopicId();

			List<Topic> list = topicsById.get(id);
			// TODO: check if it returns the topic with highest score 
			Topic topic = list.get(0);

			if (repeatMode == RepeatMode.FIRST_IN_REGION)
				doneIds = doc.getDoneIdsInCurrentRegion(start);

			if (topic != null && (repeatMode == RepeatMode.ALL || !doneIds.contains(id))) {
				doneIds.add(id);
				wikifiedText.append(originalText.substring(lastIndex, start));
				wikifiedText.append(getTag(originalText.substring(start, end), topic));

				lastIndex = end;
			} else if (filteredPositions != null) {
				filteredPositions.add(reference.getPosition());
			}
		}

		wikifiedText.append(originalText.substring(lastIndex));
		return wikifiedText.toString();
	}

	// public String tag(PreprocessedDocument doc, Collection<Topic> detectedTopics, RepeatMode repeatMode,
	// Collection<Topic> nonOverlappedTopics) {
	//
	// doc.resetRegionTracking();
	//
	// HashMap<Integer, List<Topic>> topicsById = new HashMap<Integer, List<Topic>>();
	// for (Topic topic : detectedTopics) {
	// List<Topic> list = topicsById.get(topic.getId());
	// if (list == null) {
	// list = new ArrayList<Topic>();
	// topicsById.put(topic.getId(), list);
	// }
	// list.add(topic);
	// }
	//
	// Vector<TopicReference> references = resolveCollisions(detectedTopics);
	//
	// String originalText = doc.getOriginalText();
	// StringBuffer wikifiedText = new StringBuffer();
	// int lastIndex = 0;
	//
	// HashSet<Integer> doneIds = new HashSet<Integer>();
	// HashSet<Topic> nonoverlapped = new HashSet<Topic>();
	//
	// for (TopicReference reference : references) {
	// int start = reference.getPosition().getStart();
	// int end = reference.getPosition().getEnd();
	// int id = reference.getTopicId();
	//
	// List<Topic> list = topicsById.get(id);
	// Topic topic = list.get(0);
	// for (Topic t : list) {
	// if (t.getReferences().contains(reference))
	// nonoverlapped.add(t);
	// }
	//
	// // System.out.println("considering tagging " + topic + " at " +
	// // reference.getPosition()) ;
	//
	// if (repeatMode == RepeatMode.FIRST_IN_REGION)
	// doneIds = doc.getDoneIdsInCurrentRegion(start);
	//
	// if (topic != null && (repeatMode == RepeatMode.ALL || !doneIds.contains(id))) {
	//
	// doneIds.add(id);
	// wikifiedText.append(originalText.substring(lastIndex, start));
	// wikifiedText.append(getTag(originalText.substring(start, end), topic));
	//
	// lastIndex = end;
	//
	// // System.out.println(" - tagged") ;
	// }
	// }
	//
	// wikifiedText.append(originalText.substring(lastIndex));
	// return wikifiedText.toString();
	// }

	// public String tag(PreprocessedDocument doc, Collection<Topic> topics,
	// RepeatMode repeatMode) {
	//
	// doc.resetRegionTracking() ;
	//
	// HashMap<Integer,Topic> topicsById = new HashMap<Integer, Topic>() ;
	// for (Topic topic: topics)
	// topicsById.put(topic.getId(), topic) ;
	//
	// Vector<TopicReference> references = resolveCollisions(topics) ;
	//
	// String originalText = doc.getOriginalText() ;
	// StringBuffer wikifiedText = new StringBuffer() ;
	// int lastIndex = 0 ;
	//
	// HashSet<Integer> doneIds = new HashSet<Integer>() ;
	//
	// for (TopicReference reference:references) {
	// int start = reference.getPosition().getStart() ;
	// int end = reference.getPosition().getEnd() ;
	// int id = reference.getTopicId() ;
	//
	// Topic topic = topicsById.get(id) ;
	//
	// //System.out.println("considering tagging " + topic + " at " +
	// reference.getPosition()) ;
	//
	// if (repeatMode == RepeatMode.FIRST_IN_REGION)
	// doneIds = doc.getDoneIdsInCurrentRegion(start) ;
	//
	// if (topic != null && (repeatMode == RepeatMode.ALL ||
	// !doneIds.contains(id))) {
	//
	// doneIds.add(id) ;
	// wikifiedText.append(originalText.substring(lastIndex, start)) ;
	// wikifiedText.append(getTag(originalText.substring(start, end), topic)) ;
	//
	// lastIndex = end ;
	//
	// //System.out.println(" - tagged") ;
	// }
	// }
	//
	// wikifiedText.append(originalText.substring(lastIndex)) ;
	// return wikifiedText.toString() ;
	// }

	// retrieve the non-overlapped topic references
	private Vector<TopicReference> resolveCollisions(Collection<Topic> topics,
			Collection<Position> overlappedPositions) {

		TreeSet<TopicReference> tempReferences = new TreeSet<TopicReference>();
		TreeSet<Position> tempPositions = new TreeSet<Position>();
		List<Topic> tempTopics = new ArrayList<Topic>(topics);

		// sorting topics to ensure that a topic with larger weight will be considered first such that the best
		// candidate topic will be added into the corresponding reference
		Collections.sort(tempTopics);;
		for (Topic topic : tempTopics) {
			for (TopicReference tr : topic.getReferences()) {
				if (tempPositions.contains(tr.getPosition()))
					continue;
				tr.setTopicId(topic.getId());
				tempReferences.add(tr);
				tempPositions.add(tr.getPosition());
			}
		}

		Vector<TopicReference> references = new Vector<TopicReference>();
		references.addAll(tempReferences);

		// removed all overlapped references covered by others
		for (int i = 0; i < references.size(); i++) {
			TopicReference reference = references.elementAt(i);

			Vector<TopicReference> overlappedReferences = new Vector<TopicReference>();

			for (int j = i + 1; j < references.size(); j++) {
				TopicReference reference2 = references.elementAt(j);

				if (reference.overlaps(reference2)) {
					overlappedReferences.add(reference2);
					if (overlappedPositions != null) {
						overlappedPositions.add(reference2.getPosition());
					}
				}
			}

			for (int j = 0; j < overlappedReferences.size(); j++) {
				references.removeElementAt(i + 1);
			}
		}

		return references;
	}

	// private Vector<TopicReference> resolveCollisions(Collection<Topic>
	// topics) {
	//
	// TIntDoubleHashMap topicWeights = new TIntDoubleHashMap() ;
	//
	//
	// TreeSet<TopicReference> temp = new TreeSet<TopicReference>() ;
	//
	// for(Topic topic: topics) {
	// for (Position pos: topic.getPositions()) {
	// topicWeights.put(topic.getId(), topic.getWeight()) ;
	//
	// TopicReference tr = new TopicReference(null, topic.getId(), pos) ;
	// temp.add(tr) ;
	// }
	// }
	//
	// Vector<TopicReference> references = new Vector<TopicReference>() ;
	// references.addAll(temp) ;
	//
	// for (int i=0 ; i<references.size(); i++) {
	// TopicReference reference = references.elementAt(i) ;
	//
	// Vector<TopicReference> overlappedTopics = new Vector<TopicReference>() ;
	//
	// for (int j=i+1 ; j<references.size(); j++){
	// TopicReference reference2 = references.elementAt(j) ;
	//
	// if (reference.overlaps(reference2))
	// overlappedTopics.add(reference2) ;
	// }
	//
	// for (int j=0 ; j<overlappedTopics.size() ; j++) {
	// references.removeElementAt(i+1) ;
	// }
	//
	// //TODO: why is all of this blanked out??
	//
	// //double refWeight = 0 ;
	// //Integer refId = reference.getTopicId() ;
	//
	// //if (topicWeights.containsKey(refId))
	// // refWeight = topicWeights.get(refId) ;
	// /*
	// double overlapWeight = 0 ;
	//
	// for (int j=i+1 ; j<references.size(); j++){
	// TopicReference reference2 = references.elementAt(j) ;
	//
	// if (reference.overlaps(reference2)) {
	// //System.out.println("--" + getNGram(words, c.getStartIndex(),
	// c.getEndIndex()) + " overlaps " + getNGram(words, c1.getStartIndex(),
	// c1.getEndIndex()));
	// overlappingTopics.add(reference2) ;
	//
	// double ref2Weight = 0 ;
	// Integer ref2Id = reference2.getTopicId() ;
	// if (topicWeights.containsKey(ref2Id))
	// ref2Weight = topicWeights.get(ref2Id) ;
	//
	// overlapWeight = overlapWeight + ref2Weight ;
	// } else {
	// break ;
	// }
	// }
	//
	// if (overlappingTopics.size() > 0)
	// overlapWeight = overlapWeight / overlappingTopics.size() ;
	//
	// //if (overlapWeight > refWeight) {
	// // want to keep the overlapped items
	// // references.removeElementAt(i) ;
	// // i = i-1 ;
	// //} else {
	// //want to keep the overlapping item
	// for (int j=0 ; j<overlappingTopics.size() ; j++) {
	// references.removeElementAt(i+1) ;
	// }
	// //}
	// *
	// */
	// }
	// return references ;
	// }
}
