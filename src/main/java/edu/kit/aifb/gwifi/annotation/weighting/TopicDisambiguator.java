package edu.kit.aifb.gwifi.annotation.weighting;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.kit.aifb.gwifi.annotation.detection.DisambiguationUtil;
import edu.kit.aifb.gwifi.annotation.detection.Topic;
import edu.kit.aifb.gwifi.annotation.weighting.graph.HitsHubNoPriorsTopicWeighter;
import edu.kit.aifb.gwifi.annotation.weighting.graph.HitsHubTopicWeighter;
import edu.kit.aifb.gwifi.annotation.weighting.graph.KStepMarkovTopicWeighter;
import edu.kit.aifb.gwifi.annotation.weighting.graph.MarkovCentralityTopicWeighter;
import edu.kit.aifb.gwifi.annotation.weighting.graph.PageRankNoPriorsTopicWeighter;
import edu.kit.aifb.gwifi.annotation.weighting.graph.PageRankTopicWeighter;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.service.Service.DisambiguationModel;
import edu.kit.aifb.gwifi.util.CategoryAssociation;
import edu.kit.aifb.gwifi.util.RelatednessCache;

public class TopicDisambiguator {

	private Map<DisambiguationModel, ITopicWeighter> model2topicWeighter;

	public TopicDisambiguator(DisambiguationUtil disambiguator) throws IOException {
		this.model2topicWeighter = new HashMap<DisambiguationModel, ITopicWeighter>();
	}

	public List<Topic> getWeightedTopics(Collection<Topic> topics, RelatednessCache rc, DisambiguationModel model) {
		long start = System.currentTimeMillis();

		ITopicWeighter topicWeighter = getTopicWeighter(model);
		List<Topic> weightedTopics = topicWeighter.getWeightedTopics(topics, rc);

		long end = System.currentTimeMillis();
		//System.out.println("Time for topic disambiguation: " + (end - start) + " ms");

		return weightedTopics;
	}

	public List<Topic> getWeightedTopics(Collection<Topic> topics, DisambiguationModel model, RelatednessCache rc,
			Map<String, Double> categories, CategoryAssociation ca, double alpha) {
		long start = System.currentTimeMillis();

		ITopicWeighter topicWeighter = getTopicWeighter(model);
		List<Topic> weightedTopics = topicWeighter.getWeightedTopics(topics, rc, categories, ca, alpha);

		long end = System.currentTimeMillis();
		//System.out.println("Time for topic disambiguation: " + (end - start) + " ms");

		return weightedTopics;
	}

	private ITopicWeighter getTopicWeighter(DisambiguationModel model) {
		ITopicWeighter topicWeighter = model2topicWeighter.get(model);
		if (topicWeighter == null) {
			if (model.equals(DisambiguationModel.PRIOR))
				topicWeighter = new DummyTopicWeighter();
			else if (model.equals(DisambiguationModel.EIGEN_VECTOR_CENTRALITY))
				topicWeighter = new MarkovCentralityTopicWeighter();
			else if (model.equals(DisambiguationModel.PAGERANK))
				topicWeighter = new PageRankTopicWeighter();
			else if (model.equals(DisambiguationModel.HITSHUB))
				topicWeighter = new HitsHubTopicWeighter();
			else if (model.equals(DisambiguationModel.PAGERANK_NP))
				topicWeighter = new PageRankNoPriorsTopicWeighter();
			else if (model.equals(DisambiguationModel.HITSHUB_NP))
				topicWeighter = new HitsHubNoPriorsTopicWeighter();
			else if (model.equals(DisambiguationModel.KSMARKOV))
				topicWeighter = new KStepMarkovTopicWeighter();
			else if (model.equals(DisambiguationModel.MARKOV_CENTRALITY))
				topicWeighter = new MarkovCentralityTopicWeighter();
			else if (model.equals(DisambiguationModel.KSMARKOV_10))
				topicWeighter = new KStepMarkovTopicWeighter(10);
			model2topicWeighter.put(model, topicWeighter);
		}

		return topicWeighter;
	}

}
