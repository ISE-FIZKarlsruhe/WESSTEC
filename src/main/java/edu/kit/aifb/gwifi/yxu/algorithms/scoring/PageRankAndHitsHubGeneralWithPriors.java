package edu.kit.aifb.gwifi.yxu.algorithms.scoring;

import org.apache.commons.collections15.Transformer;

import edu.kit.aifb.gwifi.yxu.annotation.weighting.graph.TopicCategoryVertex;
import edu.kit.aifb.gwifi.yxu.annotation.weighting.graph.TopicVertex;
import edu.uci.ics.jung.graph.Hypergraph;

public class PageRankAndHitsHubGeneralWithPriors<V, E> extends
		PageRankAndHitsHubWithPriors<V, E> {

	private int topicVNum;
//	private double totalTopicWeight;
	
	public PageRankAndHitsHubGeneralWithPriors(Hypergraph<V, E> g,
			Transformer<V, Double> vertex_priors, double topicAlpha,
			double cateAlpha, double hitsCoeff) {
		super(g, vertex_priors, topicAlpha, hitsCoeff);
		this.topicAlpha = topicAlpha;
		this.cateAlpha = cateAlpha;
		this.topicVNum = 0;
//		this.totalTopicWeight = 0.0;
	}

	public PageRankAndHitsHubGeneralWithPriors(Hypergraph<V, E> g,
			Transformer<E, ? extends Number> edge_weights,
			Transformer<V, Double> vertex_priors, double topicAlpha,
			double cateAlpha, double hitsCoeff) {
		super(g, edge_weights, vertex_priors, topicAlpha, hitsCoeff);
		this.topicAlpha = topicAlpha;
		this.cateAlpha = cateAlpha;
		this.topicVNum = 0;
//		this.totalTopicWeight = 0.0;
	}

	@Override
	public void evaluate() {
		//TODO count the number of topic for teleportation
		//initTopicVNum();
//		normalizeOutputValue(true);
		do {
			max_delta = Double.MIN_VALUE;
			step();
			normalizeOutputValue(false);
		} while (!done());
		outputAllCates();
	}
	
	@Override
	public void step() {
		swapOutputForCurrent();
		collectDisappearingPR();
		
		updateCateTree();
		updateTopicGragh();
		
		total_iterations++;
		afterPRStep();
	}
	
	private void initTopicVNum(){
		for (V v : graph.getVertices()) {
			if (v instanceof TopicVertex) {
				//TODO normalized topic weight in each iteration
				// sum of weight is 1, otherwise the sum needs to be calculated here
				this.topicVNum ++;
			}
		}
	}
	
	protected void updateCateTree() {
		for (V v : graph.getVertices()) {
			if (v instanceof TopicCategoryVertex) {
				setCurrentValue(v, -1.0);
			}
			setOutputValue(v, 0.0);
		}
		//bottom-up updating cate tree in currentValue
		for (V v : graph.getVertices()) {
			if (v instanceof TopicCategoryVertex) {
				if (graph.getOutEdges(v).size() == 0) {
					updateParentCate(v);
				}
			}
		}
		//TODO orient cate weight
		//normalize root cate currentValue
		double maxCateWeight = 0.0;
		double totalCateWeight = 0.0;
		for (V v : graph.getVertices()) {
			if (v instanceof TopicCategoryVertex) {
				if(graph.getOutEdges(v).size() > 0) continue;
				if(getCurrentValue(v)<0.0) continue;
				maxCateWeight = Math.max(maxCateWeight, getCurrentValue(v));
				totalCateWeight += getCurrentValue(v);
			}
		}
		if(maxCateWeight==0.0)
			maxCateWeight = 1.0;
		if(totalCateWeight==0.0)
			totalCateWeight = 1.0;
		for (V v : graph.getVertices()) {
			if (v instanceof TopicCategoryVertex) {
				if(graph.getOutEdges(v).size() > 0) continue;
				mergeInitCateWeight(v, maxCateWeight, totalCateWeight);
			}
		}
		for (V v : graph.getVertices()) {
			if (v instanceof TopicCategoryVertex) {
				//TODO update only the topic with parent cates
//				updateChildTopic(v);
				//TODO top-down updating cate tree through outputValue
				setOutputValue(v, getCurrentValue(v));
				updateChildCates(v);
				setOutputValue(v, 0.0);
			}
		}
		
	}
	
	private double updateParentCate(V v) {
		double v_auth = 0.0;
		setCurrentValue(v, -2.0);
		int incident_count = 0;
		double childCurVal = 0.0;
		for (E e : graph.getInEdges(v)) {
			incident_count = getAdjustedIncidentCount(e);
			for (V w : graph.getIncidentVertices(e)) {
				if (!w.equals(v) || hyperedges_are_self_loops) {
					if (w instanceof TopicCategoryVertex) {
						childCurVal = getCurrentValue(w);
						if(childCurVal==-1.0){//reach child not updated, go deeper
							updateParentCate(w);
						}else if(childCurVal==-2.0){//reach child in updating state, form a loop
							continue;			
						}
					}
					v_auth += (getCurrentValue(w) * getEdgeWeight(w, e).doubleValue() / incident_count);
				}
			}
		}
		setCurrentValue(v, v_auth);
		return v_auth;
	}
	
	private void mergeInitCateWeight(V v, double maxCateWeight, double totalCateWeight){
		if (v instanceof TopicCategoryVertex) {
			double normalizedCateWeight = getCurrentValue(v);
			//TODO use the initial weight/ the previous weight
			double mergedCateWeight = cateAlpha*normalizedCateWeight + (1-cateAlpha)*getVertexPrior(v)*totalCateWeight;
			//double mergedCateWeight = cateAlpha*normalizedCateWeight + (1-cateAlpha)*getCurrentValue(v);
			setCurrentValue(v, mergedCateWeight);
		}
	}

	private double updateChildCates(V v) {
		double v_hub = 0.0;
		int incident_count = 0;
		for (E e : graph.getInEdges(v)) {
			incident_count = getAdjustedIncidentCount(e);
			for (V w : graph.getIncidentVertices(e)) {
				if (!w.equals(v) || hyperedges_are_self_loops) {
					v_hub = (getOutputValue(v) * getEdgeWeight(w, e).doubleValue() / incident_count);
					if (w instanceof TopicCategoryVertex) {
						if(getOutputValue(w) > 0.0) continue;
						setOutputValue(w, v_hub);
						updateChildCates(w);
						setOutputValue(w, 0.0);
					}else{
						setOutputValue(w, getOutputValue(w)+v_hub);
					}
				}
			}
		}
		return getOutputValue(v);
	}
	
	private void updateChildTopic(V v){
		double v_hub = 0.0;
		int incident_count = 0;
		for (E e : graph.getInEdges(v)) {
			incident_count = getAdjustedIncidentCount(e);
			for (V w : graph.getIncidentVertices(e)) {
				if (!w.equals(v) || hyperedges_are_self_loops) {
					if (w instanceof TopicVertex) {
						v_hub = (getCurrentValue(v) * getEdgeWeight(w, e).doubleValue() / incident_count);
						setOutputValue(w, getOutputValue(w)+v_hub);
					}
				}
			}
		}
	}
	
	protected void updateTopicGragh() {
		normalizeCateUpdateTopic();
		for (V v : graph.getVertices()) {
			topicUpdateTopic(v);
		}
	}

	private void normalizeCateUpdateTopic(){
		double totalWeight = 0.0;
		double restWeight = 0.0;
		for (V v : graph.getVertices()) {
			if (v instanceof TopicVertex) {
				if(getOutputValue(v)==0.0){
					//getOutputValue(v)<=0.0
//					restWeight += getCurrentValue(v);
				} else {
					totalWeight += getOutputValue(v);
				}
			}
		}
		if(totalWeight==0.0)
			totalWeight=1.0;
		for (V v : graph.getVertices()) {
			if (v instanceof TopicVertex) {
				//TODO the non-cate topic weighted with current weight
				if(getOutputValue(v)==0.0){
					//getOutputValue(v)<=0.0
//					setOutputValue(v, getCurrentValue(v));
				} else {
					setOutputValue(v, getOutputValue(v) / totalWeight);// * (1-restWeight)
				}
			}
		}
	}
	
	private double topicUpdateTopic(V v) {
		int incident_count = 0;
		double v_input = 0;
		if (v instanceof TopicCategoryVertex) {
			return 0.0;
		} else {
			for (E e : graph.getInEdges(v)) {
				// For hypergraphs, this divides the potential coming from w
				// by the number of vertices in the connecting edge e.
				incident_count = getAdjustedIncidentCount(e);
				for (V w : graph.getIncidentVertices(e)) {
					if (!w.equals(v) || hyperedges_are_self_loops)
						//TODO the transition also depends on the weight of both start and end
						v_input += (getCurrentValue(w) //* Math.min(1.0f, getCurrentValue(w)/getCurrentValue(v))
								* getEdgeWeight(w, e).doubleValue() / incident_count);
				}
			}
			double new_value = v_input * (1 - hitsCoeff) + getOutputValue(v) * hitsCoeff;// * totalTopicWeight;
			if (topicAlpha >= 0) {
				//TODO mode of teleportation in the graph
				new_value = new_value * topicAlpha + getVertexPrior(v)// * totalTopicWeight
						* (1 - topicAlpha);
//				new_value = new_value * topicAlpha + 1/((double)topicVNum)
//						* (1 - topicAlpha);
			}
			setOutputValue(v, new_value);
			return Math.abs(getCurrentValue(v) - new_value);
		}
	}

	private void collectDisappearingPR() {
		if (!isDisconnectedGraphOK()) return;
		pr_disappearing_potential = 0;
		for (V v : graph.getVertices()) {
			if (v instanceof TopicVertex) {
				if(((TopicVertex)v).getOutPREdges().size()==0){
					pr_disappearing_potential += getCurrentValue(v);
				}
			}
		}
//		if(totalTopicWeight>0){
//			pr_disappearing_potential = pr_disappearing_potential/totalTopicWeight;
//		}
	}
	
	private void afterPRStep(){
		if (pr_disappearing_potential > 0) {
			for (V v : graph.getVertices()) {
				if (!(v instanceof TopicCategoryVertex)) {
					setOutputValue(v, getOutputValue(v) + topicAlpha
							* (pr_disappearing_potential * getVertexPrior(v)));
				}
			}
			pr_disappearing_potential = 0;
		}
		super.afterStep();
	}
	
	protected void normalizeOutputValue(boolean isInit) {
		double newSumWeight = 0.0;
		for (V v : graph.getVertices()) {
			if (v instanceof TopicCategoryVertex) {
				// do nothing
			} else {
				newSumWeight += getOutputValue(v) * getOutputValue(v);
			}
		}
		newSumWeight = Math.sqrt(newSumWeight);
		if(newSumWeight==0.0)
			newSumWeight=1.0;
//		totalTopicWeight = 0.0;
		double topicWeight = 0.0;
		for (V v : graph.getVertices()) {
			if (v instanceof TopicCategoryVertex) {
				// do nothing
			} else {
				topicWeight = getOutputValue(v) / newSumWeight;
				setOutputValue(v, topicWeight);
//				totalTopicWeight += topicWeight;
				if(!isInit)
					updateMaxDelta(v, Math.abs(getCurrentValue(v) - getOutputValue(v)));
			}
		}
//		System.out.println(total_iterations + ":\t" + newSumWeight + ":\t"
//				+ max_delta + ":\t" + totalTopicWeight);
	}
	
	protected void outputAllCates(){
		for (V v : graph.getVertices()) {
			if (v instanceof TopicCategoryVertex) {
				setOutputValue(v, getCurrentValue(v));
			}
		}
	}
}
