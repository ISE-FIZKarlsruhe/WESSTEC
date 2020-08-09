package edu.kit.aifb.gwifi.annotation.weighting.graph;

import java.util.HashMap;

import org.apache.commons.collections15.Transformer;

import edu.kit.aifb.gwifi.annotation.detection.Topic;
import edu.uci.ics.jung.algorithms.scoring.KStepMarkov;
import edu.uci.ics.jung.graph.DirectedSparseGraph;

public class KStepMarkovTopicWeighter extends GraphBasedTopicWeighter {
	
	private int steps = 6;
	
	public KStepMarkovTopicWeighter() {}
	
	public KStepMarkovTopicWeighter(int steps) {
		this.steps = steps;
	}

	@Override
	public HashMap<Integer, Double> getTopicWeights(DirectedSparseGraph<Vertex, Edge> graph) {

		HashMap<Integer, Double> topicWeights = new HashMap<Integer, Double>();

		KStepMarkov<Vertex, Edge> ranker = new KStepMarkov<Vertex, Edge>(graph,
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
				}, steps);
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
