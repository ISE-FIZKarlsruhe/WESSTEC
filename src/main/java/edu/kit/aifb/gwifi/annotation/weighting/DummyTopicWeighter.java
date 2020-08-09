package edu.kit.aifb.gwifi.annotation.weighting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import edu.kit.aifb.gwifi.annotation.detection.Topic;
import edu.kit.aifb.gwifi.util.CategoryAssociation;
import edu.kit.aifb.gwifi.util.RelatednessCache;

public class DummyTopicWeighter extends TopicWeighter {

	public ArrayList<Topic> getWeightedTopics(Collection<Topic> topics, RelatednessCache rc) {

		ArrayList<Topic> weightedTopics = new ArrayList<Topic>();

		for (Topic topic : topics) {
			topic.setWeight(topic.getCommenness());
			weightedTopics.add(topic);
		}

		return weightedTopics;
	}

	public ArrayList<Topic> getWeightedTopics(Collection<Topic> topics, RelatednessCache rc,
			Map<String, Double> categories, CategoryAssociation ca, double alpha) {
		return getWeightedTopics(topics, rc);
	}

}
