package edu.kit.aifb.gwifi.yxu_mvertex.annotation.weighting.graph;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.kit.aifb.gwifi.annotation.detection.DisambiguationUtil;
import edu.kit.aifb.gwifi.annotation.detection.Topic;
import edu.kit.aifb.gwifi.annotation.detection.TopicReference;
import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.util.RelatednessCache;
import edu.kit.aifb.gwifi.yxu_mvertex.annotation.detection.TopicCategory;
import edu.kit.aifb.gwifi.yxu_mvertex.annotation.weighting.TopicTopicCategoryWeighter;
import edu.uci.ics.jung.graph.DirectedSparseGraph;

public abstract class GraphBasedTopicTopicCategoryWeighter extends
		TopicTopicCategoryWeighter {

	private DisambiguationUtil disambiguator;

	private static Logger logger = Logger
			.getLogger(GraphBasedTopicTopicCategoryWeighter.class);

	/**
	 * @param topicWeights
	 * @param topicCategoryWeights
	 */
	public abstract void getTopicTopicCategoryWeights(
			DirectedSparseGraph<Vertex, Edge> graph,
			HashMap<Integer, Double> topicWeights,
			HashMap<Integer, Double> topicCategoryWeights);

	@Override
	public void getWeightedTopicTopicCategory(Collection<Topic> topics,
			Map<String, Set<Topic>> label2Topics, Map<Integer, Topic> index2NonLabelledTopic,
			Collection<TopicCategory> topicCategories, RelatednessCache rc) {
		if (rc == null)
			rc = new RelatednessCache(disambiguator.getArticleComparer());

		long start = System.currentTimeMillis();
		DirectedSparseGraph<Vertex, Edge> graph = buildGraph(topics, 
				label2Topics, index2NonLabelledTopic, topicCategories, rc);//
		long end = System.currentTimeMillis();
		logger.debug("Time for building disambiguation graph: " + (end - start)
				+ " ms");

		long newStart = System.currentTimeMillis();
		HashMap<Integer, Double> topicWeights = new HashMap<Integer, Double>();
		HashMap<Integer, Double> topicTopicCategoryWeights = new HashMap<Integer, Double>();
		getTopicTopicCategoryWeights(graph, topicWeights, topicTopicCategoryWeights);
		end = System.currentTimeMillis();
		logger.debug("Time for performing graph algorithm: " + (end - newStart) + " ms");
		setTopicTopicCategoryWeights(topicWeights, topicTopicCategoryWeights,
				topics, topicCategories);
		end = System.currentTimeMillis();
		logger.debug("Total time for topic topic-category weighting: "
				+ (end - start) + " ms\n");
	}

	public DirectedSparseGraph<Vertex, Edge> buildGraph(Collection<Topic> topics,
			Map<String, Set<Topic>> label2Topics, Map<Integer, Topic> index2NonLabelledTopic,
			Collection<TopicCategory> topicCategories, RelatednessCache rc) {

		DirectedSparseGraph<Vertex, Edge> graph = new DirectedSparseGraph<Vertex, Edge>();

		long start = System.currentTimeMillis();

		Set<TopicReferenceVertex> refVs = new HashSet<TopicReferenceVertex>();
		Map<String, TopicLabelVertex> title2LabelV = new HashMap<String, TopicLabelVertex>();
		Map<String, Set<TopicVertex>> label2TopicVs = new HashMap<String, Set<TopicVertex>>();
		Map<Integer, TopicVertex> index2topicVs = new HashMap<Integer, TopicVertex>();
		Map<Integer, Set<TopicVertex>> pid2topicVs = new HashMap<Integer, Set<TopicVertex>>();
		Map<Integer, TopicCategoryVertex> index2cateVs = new HashMap<Integer, TopicCategoryVertex>();
		Set<Edge> refTopicEs = new HashSet<Edge>();
		Set<Edge> labelTopicEs = new HashSet<Edge>();
		Set<Edge> topicTopicEs = new HashSet<Edge>();
		Set<Edge> topicCateEs = new HashSet<Edge>();
		Set<Edge> cateCateEs = new HashSet<Edge>();

		Map<TopicReference, Set<Topic>> ref2topics = new HashMap<TopicReference, Set<Topic>>();
		Map<Topic, Set<TopicReference>> topic2refs = new HashMap<Topic, Set<TopicReference>>();

		initAndCreateTopicVAndLabelV(label2Topics, title2LabelV, label2TopicVs, index2topicVs, pid2topicVs, topic2refs, ref2topics);
//		initAndCreateTopicV(topics, index2topicVs, pid2topicVs, topic2refs, ref2topics);
		long end = System.currentTimeMillis();
//		logger.debug("Time for graph preprocessing: " + (end - start) + " ms");
		logger.debug("Time for initializing and creating labelled topic vertex and label vertex: " + (end - start) + " ms");

		// with refV
//		start = System.currentTimeMillis();
//		createRefVAndRefTopicE(ref2topics, refVs, index2topicVs, refTopicEs);
//		end = System.currentTimeMillis();
//		logger.debug("Time for creating reference-topic edges: "
//				+ (end - start) + " ms");

		// without refV
		 start = System.currentTimeMillis();
		 initTopicVWeight(ref2topics, index2topicVs);
		 end = System.currentTimeMillis();
		 logger.debug("Time for initializing weight of topic vertex with refs: "
		 + (end - start) + " ms");
		 
		 start = System.currentTimeMillis();
		 createLabelTopicE(labelTopicEs, title2LabelV, label2TopicVs);
		 end = System.currentTimeMillis();
		 logger.debug("Time for creating label-topic edges: "
		 + (end - start) + " ms");
		 
		 start = System.currentTimeMillis();
		 initAndCreateTopicV(index2NonLabelledTopic.values(), index2topicVs, pid2topicVs, topic2refs, ref2topics);
		 end = System.currentTimeMillis();
		 logger.debug("Time for initializing and creating non-labelled topic vertex : "
		 + (end - start) + " ms");

		start = System.currentTimeMillis();
		createTopicTopicE(index2topicVs, pid2topicVs, rc, topic2refs,
				topicTopicEs);
		end = System.currentTimeMillis();
		logger.debug("Time for creating topic-topic edges: " + (end - start)
				+ " ms");

		// normalizeRefTopicEandTopicTopicE(refTopicEs, topicTopicEs, refVs,
		// index2topicVs);

		start = System.currentTimeMillis();
		createCateVandTopicCateE(index2topicVs, topicCategories, index2cateVs,
				topicCateEs);
		end = System.currentTimeMillis();
		logger.debug("Time for creating category vertex and topic-category edges: " + (end - start)
				+ " ms");
		
		// normalizeTopicCateE(topicCateEs, index2topicVs);
		
		// TODO with category tree
		 start = System.currentTimeMillis();
		 createCateCateE(topicCategories, index2cateVs, cateCateEs);
		 end = System.currentTimeMillis();
		 logger.debug("Time for creating category-category edges: "
		 + (end - start) + " ms");


		start = System.currentTimeMillis();
		addVandEinG(graph, refVs, index2cateVs, index2topicVs, title2LabelV, refTopicEs, labelTopicEs, topicTopicEs, topicCateEs, cateCateEs);
		end = System.currentTimeMillis();
		logger.debug("Time for adding vertices and edges into the graph: "
				+ (end - start) + " ms");

		return graph;
	}
	
	private int initAndCreateTopicVAndLabelV(
			Map<String, Set<Topic>> label2Topics,
			Map<String, TopicLabelVertex> title2LabelV,
			Map<String, Set<TopicVertex>> label2TopicVs,
			Map<Integer, TopicVertex> index2topicVs,
			Map<Integer, Set<TopicVertex>> pid2topicVs,
			Map<Topic, Set<TopicReference>> topic2refs,
			Map<TopicReference, Set<Topic>> ref2topics) {
		for(String label: label2Topics.keySet()){
			TopicLabelVertex topicLabel = new TopicLabelVertex(label);
			Set<Topic> topicsOfLabel = label2Topics.get(label);
			double labelWeight = topicsOfLabel.iterator().next().getOccurances();
			topicLabel.setTotalWeight(labelWeight);
			title2LabelV.put(label, topicLabel);
			Set<TopicVertex> topicVs = initAndCreateTopicV(topicsOfLabel, index2topicVs, pid2topicVs, topic2refs, ref2topics);
			label2TopicVs.put(label, topicVs);
		}
		return 0;
	}

	private Set<TopicVertex> initAndCreateTopicV(Collection<Topic> topics,
			Map<Integer, TopicVertex> index2topicVs,
			Map<Integer, Set<TopicVertex>> pid2topicVs,
			Map<Topic, Set<TopicReference>> topic2refs,
			Map<TopicReference, Set<Topic>> ref2topics) {
		Set<TopicVertex> topicVs = new HashSet<TopicVertex>();
		for (Topic topic : topics) {
			TopicVertex topicVertex = new TopicVertex(topic);
			index2topicVs.put(topic.getIndex(), topicVertex);
			topicVs.add(topicVertex);

			Set<TopicVertex> topicVertices = pid2topicVs.get(topic.getId());
			if (topicVertices == null) {
				topicVertices = new HashSet<TopicVertex>();
				pid2topicVs.put(topic.getId(), topicVertices);
			}
			topicVertices.add(topicVertex);

			Set<TopicReference> topicReferences = topic2refs.get(topic);
			if (topicReferences == null) {
				topicReferences = new HashSet<TopicReference>();
				topic2refs.put(topic, topicReferences);
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
		return topicVs;
	}

	private int createRefVAndRefTopicE(
			Map<TopicReference, Set<Topic>> ref2topics,
			Set<TopicReferenceVertex> refVs,
			Map<Integer, TopicVertex> index2topicVs, Set<Edge> refTopicEs) {
		Set<Topic> referredTopics;
		Vertex tempTopicV;
		for (TopicReference reference : ref2topics.keySet()) {
			TopicReferenceVertex refV = new TopicReferenceVertex(reference);
			refV.setPRWeight(reference.getLabel().getLinkProbability());
			refVs.add(refV);
			referredTopics = ref2topics.get(reference);
			for (Topic topic : referredTopics) {
				tempTopicV = index2topicVs.get(topic.getIndex());
				double linkWeight = topic.getCommenness();
				Link refTopicE = new Link(refV, tempTopicV, linkWeight);
				if (refTopicE.isValid())
					refTopicEs.add(refTopicE);
				// for symm. graph
				Link topicRefE = new Link(tempTopicV, refV, linkWeight);
				if (refTopicE.isValid())
					refTopicEs.add(topicRefE);
			}
		}
		return 0;
	}

	private int initTopicVWeight(Map<TopicReference, Set<Topic>> ref2topics,
			Map<Integer, TopicVertex> index2topicVs) {
		Set<Topic> referredTopics;
		Vertex tempTopicV;
		for (TopicReference reference : ref2topics.keySet()) {
//			double refWeight = reference.getLabel().getLinkProbability();
			referredTopics = ref2topics.get(reference);
			for (Topic topic : referredTopics) {
				tempTopicV = index2topicVs.get(topic.getIndex());
//				double linkWeight = topic.getCommenness();
				//TODO consider only the frequency of label
				double newTopicVWeight = tempTopicV.getTotalWeight() + 1.0;//linkWeight * refWeight;
				tempTopicV.setTotalWeight(newTopicVWeight);
			}
		}
		// normalize initial weight of topic
		double totalWeight = 0.0;
		for(TopicVertex topicV:index2topicVs.values()){
			totalWeight += topicV.getTotalWeight();
		}
		for(TopicVertex topicV:index2topicVs.values()){
			double relativeWeight = topicV.getTotalWeight()/totalWeight;
			topicV.setTotalWeight(relativeWeight);
		}
		return 0;
	}
	
	private int createLabelTopicE(Set<Edge> labelTopicEs,
			Map<String, TopicLabelVertex> title2LabelV, 
			Map<String, Set<TopicVertex>> label2TopicVs){
		TopicLabelVertex labelV;
		for(String label: title2LabelV.keySet()){
			labelV = title2LabelV.get(label);
			for(TopicVertex topicV: label2TopicVs.get(label)){
				TopicLabelRelation labelTopicE = new TopicLabelRelation(labelV, topicV);
				labelTopicEs.add(labelTopicE);
			}
		}
		return 0;
	}

	private int createTopicTopicE(Map<Integer, TopicVertex> index2topicVs,
			Map<Integer, Set<TopicVertex>> pid2topicVs, RelatednessCache rc,
			Map<Topic, Set<TopicReference>> topic2refs, Set<Edge> topicTopicEs) {
		TopicVertex target;
		Article[] linksIn;
		Set<TopicVertex> sources;
		Set<TopicReference> sourceReferenceSet;
		Set<TopicReference> targetReferenceSet;
		Set<TopicReference> intersection;
		for (int index : index2topicVs.keySet()) {
			target = index2topicVs.get(index);
			// considering the pairs of topic vertices that involve the same
			// entity
			for (int index2 : index2topicVs.keySet()) {
				if (index2 != index) {
					TopicVertex source = index2topicVs.get(index2);
					if (source.getTopic().getId() == target.getTopic().getId()) {
						//TODO filter the link in between topics with the same label
						if(checkTopicVsInSameLabel(target, source)) continue;//unexcepted
						sourceReferenceSet = topic2refs.get(target.getTopic());
						targetReferenceSet = topic2refs.get(source.getTopic());
						intersection = new HashSet<TopicReference>(
								sourceReferenceSet);
						intersection.retainAll(targetReferenceSet);
						if (intersection.size() != 0)
							continue;
						Link topicTopicE = new Link(source, target, 1.0);
						if (topicTopicE.isValid())
							topicTopicEs.add(topicTopicE);
						topicTopicE = new Link(target, source, 1.0);
						if (topicTopicE.isValid())
							topicTopicEs.add(topicTopicE);
					}
				}
			}

			// considering the pairs of topic vertices that are directly
			// connected
			// in the data graph using pageLinksIn
			linksIn = target.getTopic().getLinksIn();
			for (Article article : linksIn) {
				sources = pid2topicVs.get(article.getId());
				if (sources == null)
					continue;
				double relatedness = rc.getRelatedness(target.getTopic(),
						article);
				if (relatedness == 0)
					continue;
				for (TopicVertex source : sources) {
					//TODO filter the link in between topics with the same label
					if(checkTopicVsInSameLabel(target, source)) continue;
					sourceReferenceSet = topic2refs.get(target.getTopic());
					targetReferenceSet = topic2refs.get(source.getTopic());
					intersection = new HashSet<TopicReference>(
							sourceReferenceSet);
					intersection.retainAll(targetReferenceSet);
					if (intersection.size() != 0)
						continue;
					Link topicTopicE = new Link(source, target, relatedness);
					if (topicTopicE.isValid())
						topicTopicEs.add(topicTopicE);
					topicTopicE = new Link(target, source, relatedness);
					if (topicTopicE.isValid())
						topicTopicEs.add(topicTopicE);
				}
			}
		}
		// considering the pairs of topic vertices that are directly connected
		// in the data graph using pageLinksOut
		// for (int index : index2topicVertices.keySet()) {
		// TopicVertex source = index2topicVertices.get(index);
		// Article[] linksOut = source.getTopic().getLinksOut();
		// ...
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
		return 0;
	}

	private int normalizeRefTopicEandTopicTopicE(Set<Edge> refTopicEs,
			Set<Edge> topicTopicEs, Set<TopicReferenceVertex> refVs,
			Map<Integer, TopicVertex> index2topicVs) {
		for (Edge rtE : refTopicEs) {
			rtE.setWeight(rtE.getRelativeOutWeight(), false, true);
		}
		for (Edge ttE : topicTopicEs) {
			ttE.setWeight(ttE.getRelativeOutWeight(), false, true);
		}
		for (TopicReferenceVertex refV : refVs) {
			refV.setTotalOutEdgesWeight(1.0);
		}
		for (TopicVertex topicV : index2topicVs.values()) {
			topicV.setTotalOutPRWeight(1.0);
		}
		return 0;
	}

	private int createCateVandTopicCateE(
			Map<Integer, TopicVertex> index2topicVs,
			Collection<TopicCategory> topicCategories,
			Map<Integer, TopicCategoryVertex> index2cateVs,
			Set<Edge> topicCateEs) {
		TopicVertex topicV;
		Double tempWeight = 0.0;
		for (TopicCategory cate : topicCategories) {
//			TODO without cate cate edge
//			if(cate.getTopicNum()==0) continue;
			TopicCategoryVertex cateV = new TopicCategoryVertex(cate);
			cateV.setTotalWeight(cate.getWeight());
			index2cateVs.put(cate.getIndex(), cateV);
			for (Topic topic : cate.getChildTopics()) {
				topicV = index2topicVs.get(topic.getIndex());
				tempWeight = cate.getRelatednessOfTopic(topic);
				CategoryRelation topicCateE = new CategoryRelation(topicV,
						cateV, tempWeight);
				if (topicCateE.isValid())
					topicCateEs.add(topicCateE);
			}
		}
		return 0;
	}

	private int createCateCateE(Collection<TopicCategory> topicCategories,
			Map<Integer, TopicCategoryVertex> index2cateVs, Set<Edge> cateCateEs) {
		TopicCategoryVertex tCateV;
		TopicCategoryVertex sCateV;
		Double tempWeight = 0.0;
		for (TopicCategory tCate : topicCategories) {
			if(tCate.getChildNum()==0) continue;
			tCateV = index2cateVs.get(tCate.getIndex());
//			if(tCateV == null) continue;
			for (TopicCategory sCate : tCate.getChildCates()) {
				sCateV = index2cateVs.get(sCate.getIndex());
//				if(sCateV == null) continue;
				//TODO s -> t : child relatedness
				tempWeight = sCate.getRelatednessOfParent(tCate);
//				tempWeight = tCate.getRelatednessOfChild(sCate);
				CategoryRelation cateCateE = new CategoryRelation(sCateV,
						tCateV, tempWeight);
				if (cateCateE.isValid())
					cateCateEs.add(cateCateE);
			}
		}
		return 0;
	}

	private int normalizeTopicCateE(Set<Edge> topicCateEs,
			Map<Integer, TopicVertex> index2topicVs) {
		for (Edge tcE : topicCateEs) {
			tcE.setWeight(tcE.getRelativeOutWeight(), false, true);
		}
		for (TopicVertex topicV : index2topicVs.values()) {
			topicV.setTotalOutHITSWeight(1.0);
		}
		return 0;
	}

	private boolean addVandEinG(DirectedSparseGraph<Vertex, Edge> graph,
			Set<TopicReferenceVertex> refVs,
			Map<Integer, TopicCategoryVertex> index2cateVs,
			Map<Integer, TopicVertex> index2topicVs, 
			Map<String, TopicLabelVertex> title2LabelV,
			Set<Edge> refTopicEs, Set<Edge> labelTopicEs,
			Set<Edge> topicTopicEs, Set<Edge> topicCateEs, Set<Edge> cateCateEs) {
		boolean added = false;
		for (Vertex vertex : refVs) {
			added = graph.addVertex(vertex);
			if (!added) {
				return false;
			} else {
				// System.out.println(vertex.toString());
			}
		}
		for (Vertex vertex : index2topicVs.values()) {
			added = graph.addVertex(vertex);
			if (!added) {
				return false;
			} else {
//				 System.out.println(vertex.toString());
			}
		}
		for (Vertex vertex : index2cateVs.values()) {
			added = graph.addVertex(vertex);
			if (!added) {
				return false;
			} else {
//				 System.out.println(vertex.toString());
			}
		}
		for (Vertex vertex : title2LabelV.values()) {
			added = graph.addVertex(vertex);
			if (!added) {
				return false;
			} else {
//				 System.out.println(vertex.toString());
			}
		}
		for (Edge edge : refTopicEs) {
			added = graph.addEdge(edge, edge.getSource(), edge.getTarget());
			if (!added) {
				return false;
			} else {
				// System.out.println(edge.toString());
			}
		}
		for (Edge edge : labelTopicEs) {
			added = graph.addEdge(edge, edge.getSource(), edge.getTarget());
			if (!added) {
				return false;
			} else {
				// System.out.println(edge.toString());
			}
		}
		for (Edge edge : topicTopicEs) {
			added = graph.addEdge(edge, edge.getSource(), edge.getTarget());
			if (!added) {
				return false;
			} else {
				// System.out.println(edge.toString());
			}
		}
		for (Edge edge : topicCateEs) {
			added = graph.addEdge(edge, edge.getSource(), edge.getTarget());
			if (!added) {
				return false;
			} else {
				// System.out.println(edge.toString());
			}
		}
		for (Edge edge : cateCateEs) {
			added = graph.addEdge(edge, edge.getSource(), edge.getTarget());
			if (!added) {
				return false;
			} else {
//				System.out.println(edge.toString());
			}
		}
		return true;
	}
	
	private boolean checkTopicVsInSameLabel(TopicVertex topicV1, TopicVertex topicV2){
		if(!topicV1.isLabelled()||!topicV2.isLabelled()) return false;
		String label1 = topicV1.getTopic().getReferences().get(0).getLabel().getText();
		String label2 = topicV2.getTopic().getReferences().get(0).getLabel().getText();
		if(label1.equals(label2)) return true;
		return false;
	}
}
