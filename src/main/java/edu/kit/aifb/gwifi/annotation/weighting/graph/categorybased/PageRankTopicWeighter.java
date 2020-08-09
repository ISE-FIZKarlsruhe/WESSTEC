package edu.kit.aifb.gwifi.annotation.weighting.graph.categorybased;

import java.util.Collection;
import java.util.HashMap;

import org.apache.commons.collections15.Transformer;

import edu.kit.aifb.gwifi.annotation.detection.DisambiguationUtil;
import edu.kit.aifb.gwifi.annotation.detection.Topic;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.uci.ics.jung.algorithms.scoring.PageRankWithPriors;
import edu.uci.ics.jung.graph.DirectedSparseGraph;

public class PageRankTopicWeighter extends GraphBasedTopicWeighter {

	private float ALPHA = 0.15f;

	@Override
	public HashMap<Integer, Double> getTopicWeights(DirectedSparseGraph<Vertex, Edge> graph) {

		HashMap<Integer, Double> topicWeights = new HashMap<Integer, Double>();

		PageRankWithPriors<Vertex, Edge> ranker = new PageRankWithPriors<Vertex, Edge>(graph,
				new Transformer<Edge, Double>() {
					public Double transform(Edge edge) {
						double weight = edge.getWeight();
						Vertex source = edge.getSource();
						double totalWeight = source.getTotalEdgeWeight();
						return weight / totalWeight;
					}
				}, new Transformer<Vertex, Double>() {
					public Double transform(Vertex vertex) {
						return vertex.getWeight();
					}
				}, ALPHA);

		ranker.evaluate();

		for (Vertex vertex : graph.getVertices()) {
			if (vertex instanceof TopicVertex) {
				Topic topic = ((TopicVertex) vertex).getTopic();
				double weight = ranker.getVertexScore(vertex);
				topicWeights.put(topic.getIndex(), weight);
			}

		}

		return topicWeights;
	}
}
