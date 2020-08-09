package edu.kit.aifb.gwifi.yxu_bk.annotation.weighting.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections15.Transformer;

import edu.kit.aifb.gwifi.annotation.detection.Topic;
import edu.kit.aifb.gwifi.util.CategoryAssociation;
import edu.kit.aifb.gwifi.util.RelatednessCache;
import edu.kit.aifb.gwifi.yxu_bk.algorithms.scoring.PageRankAndHitsHubGeneralWithPriors;
import edu.kit.aifb.gwifi.yxu_bk.annotation.detection.TopicCategory;
//import edu.uci.ics.jung.algorithms.scoring.PageRankWithPriors;
//import edu.uci.ics.jung.algorithms.scoring.HITS;
//import edu.uci.ics.jung.algorithms.scoring.HITSWithPriors;
import edu.uci.ics.jung.graph.DirectedSparseGraph;

public class PageRankAndHitsHubTopicTopicCategoryWeighter extends
		GraphBasedTopicTopicCategoryWeighter {

	private float ALPHA = 0.94f;// TODO 0.96f
	private float CALPHA = 0.02f;// TODO 0.02f
	private float hitsCoeff = 0.08f;// TODO 0.1f

	@Override
	public void getTopicTopicCategoryWeights(
			DirectedSparseGraph<Vertex, Edge> graph,
			HashMap<Integer, Double> topicWeights,
			HashMap<Integer, Double> topicCategoryWeights) {
		
		//pagerank + hitshub
		PageRankAndHitsHubGeneralWithPriors<Vertex, Edge> ranker = new PageRankAndHitsHubGeneralWithPriors<Vertex, Edge>(
				graph, new Transformer<Edge, Double>() {
					public Double transform(Edge edge) {
						return edge.getRelativeOutWeight();
					}
				}, new Transformer<Vertex, Double>() {
					public Double transform(Vertex vertex) {
						return vertex.getTotalWeight();
					}
				}, ALPHA, CALPHA, hitsCoeff);

		//pagerank
//		PageRankWithPriors<Vertex, Edge> ranker = new PageRankWithPriors<Vertex, Edge>(graph,
//				new Transformer<Edge, Double>() {
//					public Double transform(Edge edge) {
//						return edge.getRelativeOutWeight();
//					}
//				}, new Transformer<Vertex, Double>() {
//					public Double transform(Vertex vertex) {
//						return vertex.getTotalWeight();
//					}
//				}, ALPHA);

		// hitshub
//		HITSWithPriors<Vertex, Edge> ranker = new HITSWithPriors<Vertex, Edge>(graph, new Transformer<Edge, Double>() {
//			public Double transform(Edge edge) {
//				return edge.getRelativeOutWeight();
//			}
//		}, new Transformer<Vertex, HITS.Scores>() {
//			public HITS.Scores transform(Vertex vertex) {
//				return new HITS.Scores(vertex.getTotalWeight(), vertex.getTotalWeight());
//			}
//		}, ALPHA);
		
		ranker.evaluate();

		for (Vertex vertex : graph.getVertices()) {
			if (vertex instanceof TopicVertex) {
				Topic topic = ((TopicVertex) vertex).getTopic();
				double weight = ranker.getVertexScore(vertex);
//				double weight = ranker.getVertexScore(vertex).authority;
				topicWeights.put(topic.getIndex(), weight);
			} else if (vertex instanceof TopicReferenceVertex) {
				// TopicReference topicRef = ((TopicReferenceVertex) vertex)
				// .getTopicReference();
				// double weight = ranker.getVertexScore(vertex);
				// not added into the result map
			} else if (vertex instanceof TopicCategoryVertex) {
				TopicCategory cate = ((TopicCategoryVertex) vertex)
						.getTopicCategory();
				double weight = ranker.getVertexScore(vertex);
//				double weight = ranker.getVertexScore(vertex).authority;
				topicCategoryWeights.put(cate.getIndex(), weight);
			}
		}
	}
	
	public void setAlpha(float talpha, float calpha, float beta){
		this.ALPHA = talpha;
		this.CALPHA = calpha;
		this.hitsCoeff = beta;
	}

	@Override
	public ArrayList<Topic> getWeightedTopics(Collection<Topic> topics,
			RelatednessCache rc) {
		// do nothing
		return null;
	}

	@Override
	public ArrayList<Topic> getWeightedTopics(Collection<Topic> topics,
			RelatednessCache rc, Map<String, Double> categories,
			CategoryAssociation ca, double alpha) {
		// do nothing
		return null;
	}
}
