package edu.kit.aifb.gwifi.annotation.weighting.graph;

import java.util.HashMap;

import org.apache.commons.collections15.Transformer;

import edu.kit.aifb.gwifi.annotation.detection.Topic;
import edu.uci.ics.jung.algorithms.scoring.HITS;
import edu.uci.ics.jung.graph.DirectedSparseGraph;

public class HitsHubNoPriorsTopicWeighter extends GraphBasedTopicWeighter {

	private float ALPHA = 0.15f;

	@Override
	public HashMap<Integer, Double> getTopicWeights(DirectedSparseGraph<Vertex, Edge> graph) {

		HashMap<Integer, Double> topicWeights = new HashMap<Integer, Double>();

		HITS<Vertex, Edge> ranker = new HITS<Vertex, Edge>(graph, new Transformer<Edge, Double>() {
			public Double transform(Edge edge) {
				double weight = edge.getWeight();
				Vertex source = edge.getSource();
				double totalWeight = source.getTotalEdgeWeight();
				return weight / totalWeight;
			}
		}, ALPHA);

		ranker.evaluate();

		for (Vertex vertex : graph.getVertices()) {
			if (vertex instanceof TopicVertex) {
				Topic topic = ((TopicVertex) vertex).getTopic();
				HITS.Scores weight = ranker.getVertexScore(vertex);
//				topicWeights.put(topic.getIndex(), (weight.authority + weight.hub) / 2);
//				topicWeights.put(topic.getIndex(), weight.hub);
				topicWeights.put(topic.getIndex(), weight.authority);
			}

		}

		return topicWeights;
	}
}
