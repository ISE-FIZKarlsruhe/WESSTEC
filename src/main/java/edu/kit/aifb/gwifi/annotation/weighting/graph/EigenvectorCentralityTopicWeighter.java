package edu.kit.aifb.gwifi.annotation.weighting.graph;

import java.util.HashMap;

import org.apache.commons.collections15.Transformer;

import edu.kit.aifb.gwifi.annotation.detection.Topic;
import edu.uci.ics.jung.algorithms.scoring.EigenvectorCentrality;
import edu.uci.ics.jung.graph.DirectedSparseGraph;

public class EigenvectorCentralityTopicWeighter extends GraphBasedTopicWeighter {

	// Calculates eigenvector centrality for each vertex in the graph. The 'eigenvector centrality' for a vertex is
	// defined as the fraction of time that a random walk(er) will spend at that vertex over an infinite time horizon.
	// Assumes that the graph is strongly connected.
	public HashMap<Integer, Double> getTopicWeights(DirectedSparseGraph<Vertex, Edge> graph) {

		HashMap<Integer, Double> topicWeights = new HashMap<Integer, Double>();

		EigenvectorCentrality<Vertex, Edge> ranker = new EigenvectorCentrality<Vertex, Edge>(graph,
				new Transformer<Edge, Double>() {
					public Double transform(Edge edge) {
						double weight = edge.getWeight();
						Vertex source = edge.getSource();
						double totalWeight = source.getTotalEdgeWeight();
						return weight / totalWeight;
					}
				});
		ranker.acceptDisconnectedGraph(true);
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
