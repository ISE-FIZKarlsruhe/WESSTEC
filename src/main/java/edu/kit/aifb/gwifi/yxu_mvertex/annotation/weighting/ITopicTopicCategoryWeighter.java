package edu.kit.aifb.gwifi.yxu_mvertex.annotation.weighting;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import edu.kit.aifb.gwifi.annotation.detection.Topic;
import edu.kit.aifb.gwifi.annotation.weighting.ITopicWeighter;
import edu.kit.aifb.gwifi.util.RelatednessCache;
import edu.kit.aifb.gwifi.yxu_mvertex.annotation.detection.TopicCategory;

public interface ITopicTopicCategoryWeighter extends ITopicWeighter {

	// TODO topic category is now generated out of topics inside
	// without using the input parameter
	public void getWeightedTopicTopicCategory(Collection<Topic> allTopics, 
			Map<String, Set<Topic>> label2Topics, Map<Integer, Topic> index2NonLabelledTopic,
			Collection<TopicCategory> topicCategories, RelatednessCache rc);
}
