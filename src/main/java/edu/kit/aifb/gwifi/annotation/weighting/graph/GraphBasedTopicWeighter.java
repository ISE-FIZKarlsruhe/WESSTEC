package edu.kit.aifb.gwifi.annotation.weighting.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.kit.aifb.gwifi.annotation.detection.DisambiguationUtil;
import edu.kit.aifb.gwifi.annotation.detection.Topic;
import edu.kit.aifb.gwifi.annotation.detection.TopicReference;
import edu.kit.aifb.gwifi.annotation.weighting.TopicWeighter;
import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.util.CategoryAssociation;
import edu.kit.aifb.gwifi.util.RelatednessCache;
import edu.uci.ics.jung.graph.DirectedSparseGraph;

public abstract class GraphBasedTopicWeighter extends TopicWeighter {

	private DisambiguationUtil disambiguator;

	private static Logger logger = Logger.getLogger(GraphBasedTopicWeighter.class);

	/**
	 * @return the mappings of the topic index and its weight
	 */
	public abstract HashMap<Integer, Double> getTopicWeights(DirectedSparseGraph<Vertex, Edge> graph);

	/**
	 * Weights and sorts the given topics according to some criteria.
	 * 
	 * @param topics
	 *            the topics to be weighted and sorted.
	 * @param rc
	 *            a cache in which relatedness measures will be saved so they aren't repeatedly calculated. This may be
	 *            null.
	 * @return the weighted topics.
	 * @throws Exception
	 *             depends on the implementing class
	 */
	public ArrayList<Topic> getWeightedTopics(Collection<Topic> topics, RelatednessCache rc) {

		if (rc == null)
			rc = new RelatednessCache(disambiguator.getArticleComparer());

		long start = System.currentTimeMillis();
		DirectedSparseGraph<Vertex, Edge> graph = buildGraph(topics, rc);
		long end = System.currentTimeMillis();
		logger.debug("Time for building disambiguation graph: " + (end - start) + " ms");

		long newStart = System.currentTimeMillis();
		HashMap<Integer, Double> topicWeights = getTopicWeights(graph);
		end = System.currentTimeMillis();
		logger.debug("Time for performing graph algorithm: " + (end - newStart) + " ms");

		ArrayList<Topic> weightedTopics = setTopicWeights(topicWeights, topics);

		end = System.currentTimeMillis();
		logger.debug("Total time for topic weighting: " + (end - start) + " ms\n");

		return weightedTopics;
	}

	public ArrayList<Topic> getWeightedTopics(Collection<Topic> topics, RelatednessCache rc,
			Map<String, Double> categories, CategoryAssociation ca, double alpha) {

		if (rc == null)
			rc = new RelatednessCache(disambiguator.getArticleComparer());

		long start = System.currentTimeMillis();
		DirectedSparseGraph<Vertex, Edge> graph = buildGraph(topics, rc, categories, ca, alpha);
		long end = System.currentTimeMillis();
		logger.debug("Time for building disambiguation graph: " + (end - start) + " ms");

		long newStart = System.currentTimeMillis();
		HashMap<Integer, Double> topicWeights = getTopicWeights(graph);
		end = System.currentTimeMillis();
		logger.debug("Time for performing graph algorithm: " + (end - newStart) + " ms");

		ArrayList<Topic> weightedTopics = setTopicWeights(topicWeights, topics);

		end = System.currentTimeMillis();
		logger.debug("Total time for topic weighting: " + (end - start) + " ms\n");

		return weightedTopics;
	}

	public DirectedSparseGraph<Vertex, Edge> buildGraph(Collection<Topic> topics, RelatednessCache rc) {

		long start = System.currentTimeMillis();

		DirectedSparseGraph<Vertex, Edge> graph = new DirectedSparseGraph<Vertex, Edge>();

		Set<TopicReferenceVertex> refVertices = new HashSet<TopicReferenceVertex>();
		Map<Integer, TopicVertex> index2topicVertices = new HashMap<Integer, TopicVertex>();
		Map<Integer, Set<TopicVertex>> pageId2topicVertices = new HashMap<Integer, Set<TopicVertex>>();
		Set<Edge> refTopicEdges = new HashSet<Edge>();
		Set<Edge> topicTopicEdges = new HashSet<Edge>();

		Map<TopicReference, Set<Topic>> ref2topics = new HashMap<TopicReference, Set<Topic>>();
		Map<Topic, Set<TopicReference>> topic2references = new HashMap<Topic, Set<TopicReference>>();

		// initialize index2topicVertices, pageId2topicVertices, ref2topics, topic2references 
		for (Topic topic : topics) {
			TopicVertex topicVertex = new TopicVertex(topic);
			topicVertex.setWeight(0.0);
			index2topicVertices.put(topic.getIndex(), topicVertex);

			Set<TopicVertex> topicVertices = pageId2topicVertices.get(topic.getId());
			if (topicVertices == null) {
				topicVertices = new HashSet<TopicVertex>();
				pageId2topicVertices.put(topic.getId(), topicVertices);
			}
			topicVertices.add(topicVertex);

			Set<TopicReference> topicReferences = topic2references.get(topic);
			if (topicReferences == null) {
				topicReferences = new HashSet<TopicReference>();
				topic2references.put(topic, topicReferences);
			}

			for (TopicReference ref : topic.getReferences()) {
				topicReferences.add(ref);

				Set<Topic> referredTopics = ref2topics.get(ref);
				if (referredTopics == null) {
					referredTopics = new HashSet<Topic>();
					ref2topics.put(ref, referredTopics);
				}
				referredTopics.add(topic);
			}
		}

		long end = System.currentTimeMillis();
		logger.debug("Time for graph preprocessing: " + (end - start) + " ms");

		start = System.currentTimeMillis();

		for (TopicReference reference : ref2topics.keySet()) {
			TopicReferenceVertex refVertex = new TopicReferenceVertex(reference);
			double vertexWeight = reference.getLabel().getLinkProbability();
			refVertex.setWeight(vertexWeight);
			refVertices.add(refVertex);
			Set<Topic> referredTopics = ref2topics.get(reference);
			for (Topic topic : referredTopics) {
				Vertex topicVertex = index2topicVertices.get(topic.getIndex());
				Edge edge = new Edge(refVertex, topicVertex);
				// TODO: use the context relatedness between mentions and entities
				double edgeWeight = topic.getCommenness();
//				edge.setWeight(0.1);
				edge.setWeight(edgeWeight);
				refVertex.addOutEdge(edge);
				refTopicEdges.add(edge);
			}
		}

		end = System.currentTimeMillis();
		logger.debug("Time for creating reference-topic edges: " + (end - start) + " ms");

		start = System.currentTimeMillis();

		for (int index : index2topicVertices.keySet()) {
			TopicVertex target = index2topicVertices.get(index);
			
			// considering the pairs of topic vertices that involve the same entity
			for (int index2 : index2topicVertices.keySet()) {
				if(index2 != index) {
					TopicVertex source = index2topicVertices.get(index2);
					if(source.getTopic().getId() == target.getTopic().getId()) {
						Set<TopicReference> sourceReferenceSet = topic2references.get(target.getTopic());
						Set<TopicReference> targetReferenceSet = topic2references.get(source.getTopic());
						Set<TopicReference> intersection = new HashSet<TopicReference>(sourceReferenceSet);
						intersection.retainAll(targetReferenceSet);
						if (intersection.size() != 0)
							continue;
						Edge edge = new Edge(target, source);
						edge.setWeight(1.0);
						target.addOutEdge(edge);
						topicTopicEdges.add(edge);

						edge = new Edge(source, target);
						edge.setWeight(1.0);
						source.addOutEdge(edge);
						topicTopicEdges.add(edge);
					}
				}
			}
			
			// considering the pairs of topic vertices that are directly connected
			// in the data graph using pageLinksIn
			Article[] linksIn = target.getTopic().getLinksIn();
			for (Article article : linksIn) {
				int pageId = article.getId();
				Set<TopicVertex> sources = pageId2topicVertices.get(pageId);
				if (sources == null)
					continue;
				double relatedness = rc.getRelatedness(target.getTopic(), article);
				if (relatedness == 0)
					continue;
				for (TopicVertex source : sources) {
					Set<TopicReference> sourceReferenceSet = topic2references.get(target.getTopic());
					Set<TopicReference> targetReferenceSet = topic2references.get(source.getTopic());
					Set<TopicReference> intersection = new HashSet<TopicReference>(sourceReferenceSet);
					intersection.retainAll(targetReferenceSet);
					if (intersection.size() != 0)
						continue;
					Edge edge = new Edge(target, source);
					edge.setWeight(relatedness);
					target.addOutEdge(edge);
					topicTopicEdges.add(edge);

					edge = new Edge(source, target);
					edge.setWeight(relatedness);
					source.addOutEdge(edge);
					topicTopicEdges.add(edge);
				}
			}
		}

		// considering the pairs of topic vertices that are directly connected
		// in the data graph using pageLinksOut
		// for (int index : index2topicVertices.keySet()) {
		// TopicVertex source = index2topicVertices.get(index);
		// Article[] linksOut = source.getTopic().getLinksOut();
		// for (Article article : linksOut) {
		// int pageId = article.getId();
		// Set<TopicVertex> targets = pageId2topicVertices.get(pageId);
		// if (targets == null)
		// continue;
		// double relatedness = rc.getRelatedness(source.getTopic(), article);
		// if (relatedness == 0)
		// continue;
		// for (TopicVertex target : targets) {
		// Set<TopicReference> sourceReferenceSet =
		// topic2references.get(source.getTopic());
		// Set<TopicReference> targetReferenceSet =
		// topic2references.get(target.getTopic());
		// Set<TopicReference> intersection = new
		// HashSet<TopicReference>(sourceReferenceSet);
		// intersection.retainAll(targetReferenceSet);
		// if (intersection.size() != 0)
		// continue;
		// Edge edge = new Edge(source, target);
		// edge.setWeight(relatedness);
		// source.addEdge(edge);
		// topicTopicEdges.add(edge);
		//
		// edge = new Edge(target, source);
		// edge.setWeight(relatedness);
		// target.addEdge(edge);
		// topicTopicEdges.add(edge);
		// }
		// }
		// }

		// considering all pairs of topic vertices in the disambiguation graph
		// for (int sourceIndex : index2topicVertices.keySet()) {
		// TopicVertex source = index2topicVertices.get(sourceIndex);
		// for (int targetIndex : index2topicVertices.keySet()) {
		// if (targetIndex == sourceIndex)
		// continue;
		// TopicVertex target = index2topicVertices.get(targetIndex);
		// double relatedness = rc.getRelatedness(source.getTopic(),
		// target.getTopic());
		// if (relatedness == 0)
		// continue;
		// Set<TopicReference> sourceReferenceSet =
		// topic2references.get(source.getTopic());
		// Set<TopicReference> targetReferenceSet =
		// topic2references.get(target.getTopic());
		// Set<TopicReference> intersection = new
		// HashSet<TopicReference>(sourceReferenceSet);
		// intersection.retainAll(targetReferenceSet);
		// if (intersection.size() != 0)
		// continue;
		// Edge edge = new Edge(source, target);
		// edge.setWeight(relatedness);
		// source.addEdge(edge);
		// topicTopicEdges.add(edge);
		// }
		// }

		end = System.currentTimeMillis();
		logger.debug("Time for creating topic-topic edges: " + (end - start) + " ms");

		start = System.currentTimeMillis();

		for (Vertex vertex : refVertices) {
			boolean added = graph.addVertex(vertex);
			// if (added)
			// Logger.getLogger(GraphBasedTopicWeighter.class).info("Vertex " +
			// vertex + " is added!");
		}

		for (Vertex vertex : index2topicVertices.values()) {
			boolean added = graph.addVertex(vertex);
			// if (added)
			// Logger.getLogger(GraphBasedTopicWeighter.class).info("Vertex " +
			// vertex + " is added!");
		}

		for (Edge edge : refTopicEdges) {
			boolean added = graph.addEdge(edge, edge.getSource(), edge.getTarget());
			// if (added)
			// Logger.getLogger(GraphBasedTopicWeighter.class).info("Edge " +
			// edge + " is added!");
		}

		for (Edge edge : topicTopicEdges) {
			boolean added = graph.addEdge(edge, edge.getSource(), edge.getTarget());
			// if (added)
			// Logger.getLogger(GraphBasedTopicWeighter.class).info("Edge " +
			// edge + " is added!");
		}

		end = System.currentTimeMillis();
		logger.debug("Time for adding vertices and edges into the graph: " + (end - start) + " ms");

		return graph;
	}

	public DirectedSparseGraph<Vertex, Edge> buildGraph(Collection<Topic> topics, RelatednessCache rc,
			Map<String, Double> categories, CategoryAssociation ca, double alpha) {

		long start = System.currentTimeMillis();

		DirectedSparseGraph<Vertex, Edge> graph = new DirectedSparseGraph<Vertex, Edge>();

		Set<TopicReferenceVertex> refVertices = new HashSet<TopicReferenceVertex>();
		Set<CategoryVertex> cateVertices = new HashSet<CategoryVertex>();
		Map<String, CategoryVertex> cate2cateVertices = new HashMap<String, CategoryVertex>();
		Map<Integer, TopicVertex> index2topicVertices = new HashMap<Integer, TopicVertex>();
		Map<Integer, Set<TopicVertex>> pageId2topicVertices = new HashMap<Integer, Set<TopicVertex>>();
		Set<Edge> refTopicEdges = new HashSet<Edge>();
		Set<Edge> topicTopicEdges = new HashSet<Edge>();
		Set<Edge> cateTopicEdges = new HashSet<Edge>();

		Map<TopicReference, Set<Topic>> ref2topics = new HashMap<TopicReference, Set<Topic>>();
		Map<Topic, Set<TopicReference>> topic2references = new HashMap<Topic, Set<TopicReference>>();

		for (Topic topic : topics) {
			TopicVertex topicVertex = new TopicVertex(topic);
			topicVertex.setWeight(0.0);
			index2topicVertices.put(topic.getIndex(), topicVertex);

			Set<TopicVertex> topicVertices = pageId2topicVertices.get(topic.getId());
			if (topicVertices == null) {
				topicVertices = new HashSet<TopicVertex>();
				pageId2topicVertices.put(topic.getId(), topicVertices);
			}
			topicVertices.add(topicVertex);

			Set<TopicReference> topicReferences = topic2references.get(topic);
			if (topicReferences == null) {
				topicReferences = new HashSet<TopicReference>();
				topic2references.put(topic, topicReferences);
			}

			for (TopicReference ref : topic.getReferences()) {
				topicReferences.add(ref);

				Set<Topic> referredTopics = ref2topics.get(ref);
				if (referredTopics == null) {
					referredTopics = new HashSet<Topic>();
					ref2topics.put(ref, referredTopics);
				}
				referredTopics.add(topic);
			}
		}

		long end = System.currentTimeMillis();
		logger.debug("Time for graph preprocessing: " + (end - start) + " ms");

		start = System.currentTimeMillis();

		for (TopicReference reference : ref2topics.keySet()) {
			TopicReferenceVertex refVertex = new TopicReferenceVertex(reference);
			double vertexWeight = reference.getLabel().getLinkProbability();
			refVertex.setWeight(alpha*vertexWeight);
//			refVertex.setWeight(0);
			refVertices.add(refVertex);
			Set<Topic> referredTopics = ref2topics.get(reference);
			for (Topic topic : referredTopics) {
				Vertex topicVertex = index2topicVertices.get(topic.getIndex());
				Edge edge = new Edge(refVertex, topicVertex);
				double edgeWeight = topic.getCommenness();
				edge.setWeight(edgeWeight);
				refVertex.addOutEdge(edge);
				refTopicEdges.add(edge);
			}
		}

		end = System.currentTimeMillis();
		logger.debug("Time for creating reference-topic edges: " + (end - start) + " ms");

		start = System.currentTimeMillis();

		for (String category : categories.keySet()) {
			CategoryVertex cateVertex = new CategoryVertex(category);
			cateVertex.setWeight((1-alpha)*categories.get(category));
			cate2cateVertices.put(category, cateVertex);
		}

		for (int index : index2topicVertices.keySet()) {
			TopicVertex topicVertex = index2topicVertices.get(index);
			String topicTitle = topicVertex.getTopic().getTitle();
			Map<String, Double> cateWithWeights = ca.getCategoriesWithWeights(topicTitle);
			for (String category : cateWithWeights.keySet()) {
				if (cate2cateVertices.keySet().contains(category)) {
					CategoryVertex cateVertex = cate2cateVertices.get(category);
					Edge edge = new Edge(cateVertex, topicVertex);
					double edgeWeight = cateWithWeights.get(category);
					edge.setWeight(edgeWeight);
					cateVertex.addOutEdge(edge);
					cateTopicEdges.add(edge);
				}
			}
		}

		end = System.currentTimeMillis();
		logger.debug("Time for creating category-topic edges: " + (end - start) + " ms");

		start = System.currentTimeMillis();

		// considering the pairs of topic vertices that are directly connected
		// in the data graph using pageLinksIn
		for (int index : index2topicVertices.keySet()) {
			TopicVertex target = index2topicVertices.get(index);
			Article[] linksIn = target.getTopic().getLinksIn();
			for (Article article : linksIn) {
				int pageId = article.getId();
				Set<TopicVertex> sources = pageId2topicVertices.get(pageId);
				if (sources == null)
					continue;
				double relatedness = rc.getRelatedness(target.getTopic(), article);
				if (relatedness == 0)
					continue;
				for (TopicVertex source : sources) {
					Set<TopicReference> sourceReferenceSet = topic2references.get(target.getTopic());
					Set<TopicReference> targetReferenceSet = topic2references.get(source.getTopic());
					Set<TopicReference> intersection = new HashSet<TopicReference>(sourceReferenceSet);
					intersection.retainAll(targetReferenceSet);
					if (intersection.size() != 0)
						continue;

					Edge edge = new Edge(source, target);
					edge.setWeight(relatedness);
					source.addOutEdge(edge);
					topicTopicEdges.add(edge);

					edge = new Edge(target, source);
					edge.setWeight(relatedness);
					target.addOutEdge(edge);
					topicTopicEdges.add(edge);
				}
			}
		}

		end = System.currentTimeMillis();
		logger.debug("Time for creating topic-topic edges: " + (end - start) + " ms");

		start = System.currentTimeMillis();

		for (Vertex vertex : refVertices) {
			boolean added = graph.addVertex(vertex);
			// if (added)
			// Logger.getLogger(GraphBasedTopicWeighter.class).info("Vertex " +
			// vertex + " is added!");
		}

		for (Vertex vertex : cateVertices) {
			boolean added = graph.addVertex(vertex);
			// if (added)
			// Logger.getLogger(GraphBasedTopicWeighter.class).info("Vertex " +
			// vertex + " is added!");
		}

		for (Vertex vertex : index2topicVertices.values()) {
			boolean added = graph.addVertex(vertex);
			// if (added)
			// Logger.getLogger(GraphBasedTopicWeighter.class).info("Vertex " +
			// vertex + " is added!");
		}

		for (Edge edge : refTopicEdges) {
			boolean added = graph.addEdge(edge, edge.getSource(), edge.getTarget());
			// if (added)
			// Logger.getLogger(GraphBasedTopicWeighter.class).info("Edge " +
			// edge + " is added!");
		}

		for (Edge edge : cateTopicEdges) {
			boolean added = graph.addEdge(edge, edge.getSource(), edge.getTarget());
			// if (added)
			// Logger.getLogger(GraphBasedTopicWeighter.class).info("Edge " +
			// edge + " is added!");
		}

		for (Edge edge : topicTopicEdges) {
			boolean added = graph.addEdge(edge, edge.getSource(), edge.getTarget());
			// if (added)
			// Logger.getLogger(GraphBasedTopicWeighter.class).info("Edge " +
			// edge + " is added!");
		}

		end = System.currentTimeMillis();
		logger.debug("Time for adding vertices and edges into the graph: " + (end - start) + " ms");

		return graph;
	}

}
