package edu.kit.aifb.gwifi.annotation.weighting.graph;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import edu.kit.aifb.gwifi.annotation.detection.Topic;
import edu.uci.ics.jung.algorithms.importance.MarkovCentrality;
import edu.uci.ics.jung.graph.DirectedSparseGraph;

public class MarkovCentralityTopicWeighter extends GraphBasedTopicWeighter {

	// "Algorithms for Estimating Relative Importance in Graphs by Scott White and Padhraic Smyth, 2003"
	public HashMap<Integer, Double> getTopicWeights(DirectedSparseGraph<Vertex, Edge> graph) {

		Set<Vertex> rootNodes = new HashSet<Vertex>();
		Collection<Vertex> vertices = graph.getVertices();
		for(Vertex vertex : vertices) {
			if(vertex instanceof TopicReferenceVertex) {
				rootNodes.add(vertex);
			}
		}
		
		HashMap<Integer, Double> topicWeights = new HashMap<Integer, Double>();

		MarkovCentrality<Vertex, Edge> ranker = new MarkovCentrality<Vertex, Edge>(graph, rootNodes);
		ranker.evaluate();
		
		for (Vertex vertex : graph.getVertices()) {
			if (vertex instanceof TopicVertex) {
				Topic topic = ((TopicVertex) vertex).getTopic();
				double weight = ranker.getVertexRankScore(vertex);
				topicWeights.put(topic.getIndex(), weight);
			}

		}

		return topicWeights;
	}
}
