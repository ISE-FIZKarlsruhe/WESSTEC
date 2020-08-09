package edu.kit.aifb.gwifi.yxu_mvertex.annotation.weighting;

import java.util.Collection;
import java.util.Map;

import edu.kit.aifb.gwifi.annotation.detection.Topic;
import edu.kit.aifb.gwifi.yxu_mvertex.annotation.detection.TopicCategory;

public abstract class TopicTopicCategoryWeighter implements
		ITopicTopicCategoryWeighter {

	public void setTopicTopicCategoryWeights(Map<Integer, Double> topicWeights,
			Map<Integer, Double> topicCategoryWeights,
			Collection<Topic> topics, Collection<TopicCategory> topicCategories) {
		setTopicWeights(topicWeights, topics);
		setTopicCategoryWeights(topicCategoryWeights, topicCategories);
	}

	private void setTopicWeights(Map<Integer, Double> topicWeights,
			Collection<Topic> topics) {
		@SuppressWarnings("unused")
		double totalTopicWeight = 0.0;
		double maxTopicWeight = 0.0;

		for (Topic topic : topics) {
			double weight = topicWeights.containsKey(topic.getIndex()) ? topicWeights
					.get(topic.getIndex()) : 0.0;
			topic.setWeight(weight);
			totalTopicWeight += weight;
			maxTopicWeight = Math.max(maxTopicWeight, weight);
		}

		for (Topic topic : topics) {
			if (topicWeights.containsKey(topic.getIndex())) {
				double weight = (maxTopicWeight == 0.0) ? 0.0 : topic
						.getWeight() / maxTopicWeight;
				// if (weight * topics.size() / totalTopicWeight <
				// DEFAULT_MIN_WEIGHT)
				// continue;
				// if (weight < wikipedia.getConfig().getMinWeight())
				// continue;
				topic.setWeight(weight);
			}
		}
	}

	private void setTopicCategoryWeights(
			Map<Integer, Double> topicCategoryWeights,
			Collection<TopicCategory> topicCategories) {
		@SuppressWarnings("unused")
		double totalCateWeight = 0.0;
		double maxCateWeight = 0.0;

		for (TopicCategory cate : topicCategories) {
			double weight = topicCategoryWeights.containsKey(cate.getIndex()) ? topicCategoryWeights
					.get(cate.getIndex()) : 0.0;
			cate.setWeight(weight);
			totalCateWeight += weight;
			maxCateWeight = Math.max(maxCateWeight, weight);
		}

		for (TopicCategory cate : topicCategories) {
			if (topicCategoryWeights.containsKey(cate.getIndex())) {
				double weight = (maxCateWeight == 0.0) ? 0.0 : cate.getWeight() / maxCateWeight;
				// if (weight * topicCategories.size() / totalCateWeight <
				// DEFAULT_MIN_WEIGHT)
				// continue;
				// if (weight < wikipedia.getConfig().getMinWeight())
				// continue;
				cate.setWeight(weight);
			}
		}
	}
}
