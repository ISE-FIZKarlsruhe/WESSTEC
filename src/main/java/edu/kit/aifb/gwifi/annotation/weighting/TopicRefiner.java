package edu.kit.aifb.gwifi.annotation.weighting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.kit.aifb.gwifi.annotation.detection.Topic;
import edu.kit.aifb.gwifi.annotation.detection.TopicReference;
import edu.kit.aifb.gwifi.util.Position;
import edu.kit.aifb.gwifi.util.RelatednessCache;
import gnu.trove.iterator.TDoubleIterator;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.set.hash.TDoubleHashSet;

public class TopicRefiner {
	
	public List<Topic> getRefinedTopics(List<Topic> topics, RelatednessCache rc) {
		// 1. sort detectedTopics
		// 2. iterate sorted detected topics
		//  2.1. store a topic as + and its reference positions, if one of its reference positions are not stored
		//  2.2. store a topic as - if all of its reference positions are stored.
		// ...
		
		long start = System.currentTimeMillis();
		
		List<Topic> tempTopics = new ArrayList<Topic>(topics);
		Set<Position> tempPositions = new HashSet<Position>();
		List<Topic> relTopics = new ArrayList<Topic>();
		List<Topic> nonRelTopics = new ArrayList<Topic>();
		
		// sorting topics to ensure that a topic with larger weight will be considered first
		Collections.sort(tempTopics);
		for (Topic topic : tempTopics) {
			boolean isRelTopic = false;
			for (TopicReference tr : topic.getReferences()) {
				Position position = tr.getPosition();
				if(! tempPositions.contains(position)) {
					relTopics.add(topic);
					isRelTopic = true;
				} 
				tempPositions.add(position);
			}
			if(isRelTopic == false) {
				nonRelTopics.add(topic);
			}
		}
		
		for (Topic topic : topics) {
			TIntDoubleHashMap relTopicIndex2score = new TIntDoubleHashMap();
			TIntDoubleHashMap nonRelTopicIndex2score = new TIntDoubleHashMap();
			TDoubleHashSet thresholds = new TDoubleHashSet();
			
			for(Topic relTopic : relTopics) {
//				if(topic.equals(relTopic))
//					continue;
				double relatedness = rc.getRelatedness(topic, relTopic);
				relTopicIndex2score.put(relTopic.getIndex(), relatedness);
				thresholds.add(relatedness);
			}
			for(Topic nonRelTopic : nonRelTopics) {
//				if(topic.equals(nonRelTopic))
//					continue;
				double relatedness = rc.getRelatedness(topic, nonRelTopic);
				nonRelTopicIndex2score.put(nonRelTopic.getIndex(), relatedness);
				thresholds.add(relatedness);
			}

			double informationGain = 0; 
			TDoubleIterator thresholdIt = thresholds.iterator();
			while (thresholdIt.hasNext()) {
				double threshold = thresholdIt.next();

				int sPlusRel = 0;
				int sPlusNonrel = 0;
				int sMinusRel = 0;
				int sMinusNonrel = 0;
				
				for (Topic relTopic : relTopics) {
					if (relTopicIndex2score.get(relTopic.getIndex()) > threshold) {
						sPlusRel++;
					} else {
						sMinusRel++;
					}
				}
				
				for (Topic nonRelTopic : nonRelTopics) {
					if (nonRelTopicIndex2score.get(nonRelTopic.getIndex()) > threshold) {
						sPlusNonrel++;
					} else {
						sMinusNonrel++;
					}
				}
				
				double sumPlus = sPlusRel + sPlusNonrel;
				double sumMinus = sMinusRel + sMinusNonrel;

				double ig = 1;
				if (sumPlus > 0) {
					ig -= entropy(sPlusRel, sPlusNonrel) * sumPlus / (sumPlus + sumMinus);
				}
				if (sumMinus > 0) {
					ig -= entropy(sMinusRel, sMinusNonrel) * sumMinus / (sumPlus + sumMinus);
				}

				informationGain = Math.max(informationGain, ig);
			}
			
			topic.setWeight(informationGain);
		}
		
		long end = System.currentTimeMillis();
		System.out.println("Time for topic refinement: " + (end - start) + " ms");
		
		return topics;
	}
	
	private static double entropy(int countClassA, int countClassB) {
		double pA = (double) countClassA / (countClassA + countClassB);
		double pB = (double) countClassB / (countClassA + countClassB);

		double e = 0;
		if (pA > 0) {
			e -= pA * Math.log(pA);
		}
		if (pB > 0) {
			e -= pB * Math.log(pB);
		}
		return e;
	}
}
