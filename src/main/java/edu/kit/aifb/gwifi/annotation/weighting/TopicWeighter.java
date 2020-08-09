package edu.kit.aifb.gwifi.annotation.weighting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import edu.kit.aifb.gwifi.annotation.detection.DisambiguationUtil;
import edu.kit.aifb.gwifi.annotation.detection.Topic;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.util.CategoryAssociation;
import edu.kit.aifb.gwifi.util.RelatednessCache;

public abstract class TopicWeighter implements ITopicWeighter {

	/**
	 * Weights and sorts the given topics according to some criteria.
	 * 
	 * @param topics
	 *            the topics to be weighted and sorted.
	 * @param rc
	 *            a cache in which relatedness measures will be saved so they aren't repeatedly calculated. This may be
	 *            null.
	 * @return the weighted topics.
	 * @throws Exception
	 *             depends on the implementing class
	 */
	public abstract ArrayList<Topic> getWeightedTopics(Collection<Topic> topics, RelatednessCache rc);

	public abstract ArrayList<Topic> getWeightedTopics(Collection<Topic> topics, RelatednessCache rc,
			Map<String, Double> categories, CategoryAssociation ca, double alpha);

	/**
	 * A convenience method that weights the given topics using getWeightedTopics(), and discards those below a certian
	 * weight.
	 * 
	 * @param topics
	 *            the topics to be weighted and sorted.
	 * @param minimumWeight
	 *            the weight below which topics are discarded
	 * @return the weighted topics.
	 * @throws Exception
	 *             depends on the implementing class
	 */
	public ArrayList<Topic> getBestTopics(Collection<Topic> topics, RelatednessCache rc, double minimumWeight)
			throws Exception {

		ArrayList<Topic> allTopics = getWeightedTopics(topics, rc);
		ArrayList<Topic> bestTopics = new ArrayList<Topic>();

		for (Topic topic : allTopics) {
			if (topic.getWeight() >= minimumWeight)
				bestTopics.add(topic);
			else
				break;
		}

		return bestTopics;
	}

	public ArrayList<Topic> setTopicWeights(Map<Integer, Double> topicWeights, Collection<Topic> topics) {
		ArrayList<Topic> weightedTopics = new ArrayList<Topic>();

		double totalWeight = 0;
		double maxWeight = 0;

		for (Topic topic : topics) {
			double weight = topicWeights.containsKey(topic.getIndex()) ? topicWeights.get(topic.getIndex()) : 0;
			// set the weight of each topic after performing graph algorithm
			// TODO: to be changed
			// weight *= topic.getCommenness();
			topic.setWeight(weight);
			totalWeight += weight;
			maxWeight = Math.max(maxWeight, weight);
		}

		for (Topic topic : topics) {
			if (topicWeights.containsKey(topic.getIndex())) {
				double weight = topic.getWeight();
				weight /= maxWeight;
				topic.setWeight(weight);

				// if (weight < wikipedia.getConfig().getMinWeight())
				// continue;
			} else {
				topic.setWeight(0.0);
			}

			if (topic.getWeight() != 0)
				weightedTopics.add(topic);
		}

		// Collections.sort(weightedTopics);

		return weightedTopics;
	}

	// public ArrayList<Topic> setTopicWeights(Map<Integer, Double> topicWeights, Collection<Topic> topics) {
	// Map<TopicReference, Double> ref2weight = new HashMap<TopicReference, Double>();
	// ArrayList<Topic> weightedTopics = new ArrayList<Topic>();
	//
	// double totalWeight = 0;
	//
	// for (Topic topic : topics) {
	// double weight = topicWeights.containsKey(topic.getIndex()) ? topicWeights.get(topic.getIndex()) : 0;
	// // set the weight of each topic after performing graph algorithm
	// topic.setWeight(weight);
	// totalWeight += weight;
	// for (TopicReference ref : topic.getReferences()) {
	// // set the total weight of topics corresponding to each
	// // reference
	// Double refWeight = ref2weight.get(ref);
	// // weight *= topic.getCommenness();
	// if (refWeight == null)
	// refWeight = weight;
	// else
	// refWeight += weight;
	// ref2weight.put(ref, refWeight);
	// }
	// }
	//
	// for (Topic topic : topics) {
	// double weight = topic.getWeight();
	//
	// // if (weight * topics.size() / totalWeight < DEFAULT_MIN_WEIGHT)
	// // continue;
	//
	// double maxRefWeight = 0;
	// for (TopicReference ref : topic.getReferences()) {
	// maxRefWeight = Math.max(maxRefWeight, ref2weight.get(ref));
	// }
	// // the final weight of a topic w.r.t. a ref = its weight / the total
	// // weight of all topics corresponding to the ref
	// // TODO: maybe will be changed later
	// topic.setWeight(weight / maxRefWeight);
	// weightedTopics.add(topic);
	// }
	//
	// // for (Topic topic : topics) {
	// // if (topicWeights.containsKey(topic.getIndex())) {
	// // double weight = topicWeights.get(topic.getIndex());
	// // topic.setWeight(weight);
	// // } else {
	// // topic.setWeight(0.0);
	// // }
	// //
	// // weightedTopics.add(topic);
	// // }
	//
	// // Collections.sort(weightedTopics);
	//
	// return weightedTopics;
	// }

}
