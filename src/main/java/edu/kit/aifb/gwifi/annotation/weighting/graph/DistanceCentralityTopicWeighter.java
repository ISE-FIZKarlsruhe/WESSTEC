package edu.kit.aifb.gwifi.annotation.weighting.graph;

import java.util.HashMap;

import org.apache.commons.collections15.Transformer;

import edu.kit.aifb.gwifi.annotation.detection.DisambiguationUtil;
import edu.kit.aifb.gwifi.annotation.detection.Topic;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.uci.ics.jung.algorithms.scoring.DistanceCentralityScorer;
import edu.uci.ics.jung.graph.DirectedSparseGraph;

public class DistanceCentralityTopicWeighter extends GraphBasedTopicWeighter {

	// Assigns scores to vertices based on their distances to each other vertex in the graph. This class optionally
	// normalizes its results based on the value of its 'averaging' constructor parameter. If it is true, then the value
	// returned for vertex v is 1 / (_average_ distance from v to all other vertices); this is sometimes called
	// closeness centrality. If it is false, then the value returned is 1 / (_total_ distance from v to all other
	// vertices); this is sometimes referred to as barycenter centrality. (If the average/total distance is 0, the value
	// returned is Double.POSITIVE_INFINITY.)
	public HashMap<Integer, Double> getTopicWeights(DirectedSparseGraph<Vertex, Edge> graph) {

		HashMap<Integer, Double> topicWeights = new HashMap<Integer, Double>();

		DistanceCentralityScorer<Vertex, Edge> ranker = new DistanceCentralityScorer<Vertex, Edge>(graph,
				new Transformer<Edge, Double>() {
					public Double transform(Edge edge) {
						double weight = edge.getWeight();
						return 1- weight;
					}
				}, false);
		
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
