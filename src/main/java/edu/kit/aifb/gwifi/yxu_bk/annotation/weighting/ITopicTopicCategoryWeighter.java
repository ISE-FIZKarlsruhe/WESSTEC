package edu.kit.aifb.gwifi.yxu_bk.annotation.weighting;

import java.util.Collection;

import edu.kit.aifb.gwifi.annotation.detection.Topic;
import edu.kit.aifb.gwifi.annotation.weighting.ITopicWeighter;
import edu.kit.aifb.gwifi.util.RelatednessCache;
import edu.kit.aifb.gwifi.yxu_bk.annotation.detection.TopicCategory;

public interface ITopicTopicCategoryWeighter extends ITopicWeighter {

	// TODO topic category is now generated out of topics inside
	// without using the input parameter
	public void getWeightedTopicTopicCategory(Collection<Topic> topics,
			Collection<TopicCategory> topicCategories, RelatednessCache rc);
}
