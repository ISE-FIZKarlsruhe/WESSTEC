package edu.kit.aifb.gwifi.annotation.weighting.graph;

import java.util.HashMap;

import edu.kit.aifb.gwifi.annotation.detection.Topic;
import edu.uci.ics.jung.algorithms.scoring.DegreeScorer;
import edu.uci.ics.jung.graph.DirectedSparseGraph;

public class DegreeTopicWeighter extends GraphBasedTopicWeighter {

	// Assigns a score to each vertex equal to its degree.
	public HashMap<Integer, Double> getTopicWeights(DirectedSparseGraph<Vertex, Edge> graph) {

		HashMap<Integer, Double> topicWeights = new HashMap<Integer, Double>();

		DegreeScorer<Vertex> ranker = new DegreeScorer<Vertex>(graph);

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
