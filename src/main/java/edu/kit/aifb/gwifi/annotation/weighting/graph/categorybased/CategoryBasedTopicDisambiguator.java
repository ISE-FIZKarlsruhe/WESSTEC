package edu.kit.aifb.gwifi.annotation.weighting.graph.categorybased;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.kit.aifb.gwifi.annotation.detection.DisambiguationUtil;
import edu.kit.aifb.gwifi.annotation.detection.Topic;
import edu.kit.aifb.gwifi.annotation.weighting.DummyTopicWeighter;
import edu.kit.aifb.gwifi.annotation.weighting.ITopicWeighter;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.service.Service.DisambiguationModel;
import edu.kit.aifb.gwifi.util.RelatednessCache;

public class CategoryBasedTopicDisambiguator {
	
	private Wikipedia wikipedia;
	private DisambiguationUtil disambiguator;

	private Map<DisambiguationModel, ITopicWeighter> model2topicWeighter;
	
	public CategoryBasedTopicDisambiguator(Wikipedia wikipedia, DisambiguationUtil disambiguator) throws IOException {
		this.wikipedia = wikipedia;
		this.disambiguator = disambiguator;

		this.model2topicWeighter = new HashMap<DisambiguationModel, ITopicWeighter>();
	}

	public List<Topic> getWeightedTopics(Collection<Topic> topics, RelatednessCache rc, DisambiguationModel model) {
		long start = System.currentTimeMillis();
		
		ITopicWeighter topicWeighter = getTopicWeighter(model);
		List<Topic> weightedTopics = topicWeighter.getWeightedTopics(topics, rc);
		
		long end = System.currentTimeMillis();
		System.out.println("Time for topic disambiguation: " + (end - start) + " ms");
		
		return weightedTopics;
	}
	
	private ITopicWeighter getTopicWeighter(DisambiguationModel model) {
		ITopicWeighter topicWeighter = model2topicWeighter.get(model);
		if (topicWeighter == null) {
			if(model.equals(DisambiguationModel.PRIOR)) 
				topicWeighter = new DummyTopicWeighter();
			else if (model.equals(DisambiguationModel.PAGERANK))
				topicWeighter = new PageRankTopicWeighter();
			else if (model.equals(DisambiguationModel.HITSHUB))
				topicWeighter = new HitsHubTopicWeighter();
			model2topicWeighter.put(model, topicWeighter);
		}

		return topicWeighter;
	}
	
}
