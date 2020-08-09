package edu.kit.aifb.gwifi.yxu_bk.algorithms.scoring;

import java.util.HashMap;

import org.apache.commons.collections15.Transformer;

import edu.kit.aifb.gwifi.yxu_bk.annotation.weighting.graph.TopicCategoryVertex;
import edu.kit.aifb.gwifi.yxu_bk.annotation.weighting.graph.TopicVertex;
import edu.uci.ics.jung.graph.Hypergraph;

public class PageRankAndHitsHubGeneralWithPriors<V, E> extends
		PageRankAndHitsHubWithPriors<V, E> {
	
	private double MaxCateWeight = 0.0;

	public PageRankAndHitsHubGeneralWithPriors(Hypergraph<V, E> g,
			Transformer<V, Double> vertex_priors, double topicAlpha,
			double cateAlpha, double hitsCoeff) {
		super(g, vertex_priors, topicAlpha, hitsCoeff);
		this.topicAlpha = topicAlpha;
		this.cateAlpha = cateAlpha;
	}

	public PageRankAndHitsHubGeneralWithPriors(Hypergraph<V, E> g,
			Transformer<E, ? extends Number> edge_weights,
			Transformer<V, Double> vertex_priors, double topicAlpha,
			double cateAlpha, double hitsCoeff) {
		super(g, edge_weights, vertex_priors, topicAlpha, hitsCoeff);
		this.topicAlpha = topicAlpha;
		this.cateAlpha = cateAlpha;
	}

	@Override
	public void step() {
		swapOutputForCurrent();
		
		//pagerank hitshub with category
//		for (V v : graph.getVertices()) {
//			preUpdate(v);
//		}
////		updateTree();
//		for (V v : graph.getVertices()) {
//			update(v);
//		}
		
		
		//------------cate-norm----------
		MaxCateWeight = 0.0;
		HashMap<V,Double> topicV2hub = new HashMap<V,Double>();
		for (V v : graph.getVertices()) {
			cateUpdate(v);
		}
		if(MaxCateWeight==0.0)
			MaxCateWeight = 1.0;
		for (V v : graph.getVertices()) {
			mergeInitCateWeight(v);
		}
		for (V v : graph.getVertices()) {
			topicCateUpdate(v, topicV2hub);
		}
		normalizeMapValue(topicV2hub);
		for (V v : graph.getVertices()) {
			topicTopicUpdate(v,topicV2hub);
		}
		
		total_iterations++;
		afterStep();
	}
	//----------------------cate-norm------------------------------------------
	
	protected double cateUpdate(V v) {
		if (v instanceof TopicCategoryVertex) {
			int incident_count = 0;
			double v_auth = 0;
			for (E e : graph.getInEdges(v)) {
				incident_count = getAdjustedIncidentCount(e);
				for (V w : graph.getIncidentVertices(e)) {
					if (w instanceof TopicVertex) {
						if (!w.equals(v) || hyperedges_are_self_loops) {
							v_auth += (getCurrentValue(w)
									* getEdgeWeight(w, e).doubleValue() / incident_count);
						}
					}
				}
			}
			setOutputValue(v, v_auth);
			MaxCateWeight = Math.max(MaxCateWeight, v_auth);
			return Math.abs(getCurrentValue(v) - v_auth);
		} else {
			return 0.0;
		}
	}
	
	private void mergeInitCateWeight(V v){
		if (v instanceof TopicCategoryVertex) {
			//TODO use the initial weight/ the previous weight
			double normalizedCateWeight = getOutputValue(v)/MaxCateWeight;
			double mergedCateWeight = cateAlpha*normalizedCateWeight + (1-cateAlpha)*getVertexPrior(v);
			//double mergedCateWeight = cateAlpha*normalizedCateWeight + (1-cateAlpha)*getCurrentValue(v);
			setOutputValue(v, mergedCateWeight);
		}
	}
	
	protected double topicCateUpdate(V v, HashMap<V,Double> topicV2hub) {
		if (v instanceof TopicVertex) {
			int incident_count = 0;
			double v_hub = 0;
			for (E e : graph.getOutEdges(v)) {
				incident_count = getAdjustedIncidentCount(e);
				for (V w : graph.getIncidentVertices(e)) {
					if (!w.equals(v) || hyperedges_are_self_loops)
						if (w instanceof TopicCategoryVertex)
							v_hub += (getOutputValue(w)
									* getEdgeWeight(v, e).doubleValue() / incident_count);
				}
			}
			topicV2hub.put(v, v_hub);
			return Math.abs(getCurrentValue(v) - v_hub);
		} else {
			return 0.0;
		}
	}

	private void normalizeMapValue(HashMap<V,Double> map){
		double totalWeight = 0.0;
		for(Double val:map.values()){
			totalWeight += val;
		}
		if(totalWeight==0.0){
			totalWeight=1.0;
		}
		for(V v:map.keySet()){
			double relativeWeight = map.get(v)/totalWeight;
			map.put(v, relativeWeight);
		}
	}
	
	protected double topicTopicUpdate(V v, HashMap<V,Double> topicV2hub) {
		int incident_count = 0;
		double v_input = 0;
		if (v instanceof TopicCategoryVertex) {
			return Math.abs(getCurrentValue(v) - getOutputValue(v));
		} else {
			// collectDisappearingPotential(v);
			for (E e : graph.getInEdges(v)) {
				// For hypergraphs, this divides the potential coming from w
				// by the number of vertices in the connecting edge e.
				incident_count = getAdjustedIncidentCount(e);
				for (V w : graph.getIncidentVertices(e)) {
					if (!w.equals(v) || hyperedges_are_self_loops)
						v_input += (getCurrentValue(w)
								* getEdgeWeight(w, e).doubleValue() / incident_count);
				}
			}
			double new_value = v_input * (1 - hitsCoeff) + topicV2hub.get(v) * hitsCoeff;
			if (topicAlpha >= 0) {
				new_value = new_value * topicAlpha + getVertexPrior(v)
						* (1 - topicAlpha);
			}
			setOutputValue(v, new_value);
			return Math.abs(getCurrentValue(v) - new_value);
		}
	}
	//--------------------------------------------------------------------------
	protected double preUpdate(V v) {
		if (v instanceof TopicCategoryVertex) {
			int incident_count = 0;
			double v_auth = 0;
			for (E e : graph.getInEdges(v)) {
				incident_count = getAdjustedIncidentCount(e);
				for (V w : graph.getIncidentVertices(e)) {
					if (w instanceof TopicVertex) {
						if (!w.equals(v) || hyperedges_are_self_loops) {
							v_auth += (getCurrentValue(w)
									* getEdgeWeight(w, e).doubleValue() / incident_count);
						}
					}
				}
			}
			if (cateAlpha >= 0) {
				v_auth = v_auth * cateAlpha + getVertexPrior(v)
						* (1 - cateAlpha);
			}
			setOutputValue(v, v_auth);
			return Math.abs(getCurrentValue(v) - v_auth);
		} else {
			return 0.0;
		}
	}

	protected void updateTree() {
		for (V v : graph.getVertices()) {
			if (v instanceof TopicCategoryVertex) {
				setOutputValue(v, 0.0);
			}
		}
		for (V v : graph.getVertices()) {
			if (v instanceof TopicCategoryVertex) {
				if (graph.getOutEdges(v).size() == 0) {
					if (getOutputValue(v) == 0.0)
						upUpdateTreeEle(v);
				}
			}
		}
		for (V v : graph.getVertices()) {
			if (v instanceof TopicCategoryVertex) {
				if (graph.getOutEdges(v).size() == 0) {
					if (getOutputValue(v) > 0.0)
						downUpdateTreeEle(v);
				}
			}
		}
	}

	private double upUpdateTreeEle(V v) {
		double v_auth = 0.0;
		int incident_count = 0;
		for (E e : graph.getInEdges(v)) {
			incident_count = getAdjustedIncidentCount(e);
			for (V w : graph.getIncidentVertices(e)) {
				if (!w.equals(v) || hyperedges_are_self_loops) {
					if (w instanceof TopicCategoryVertex) {
						if(getOutputValue(w)==0.0)
						upUpdateTreeEle(w);
						v_auth += (getOutputValue(w)
								* getEdgeWeight(w, e).doubleValue() / incident_count);
					} else {
						v_auth += (getCurrentValue(w)
								* getEdgeWeight(w, e).doubleValue() / incident_count);
					}
				}
			}
		}
		v_auth = v_auth * (1 - cateAlpha) + getVertexPrior(v)
				* cateAlpha;
		setOutputValue(v, v_auth);
		return v_auth;
	}

	private double downUpdateTreeEle(V v) {
		double v_hub = 0.0;
		int incident_count = 0;
		for (E e : graph.getInEdges(v)) {
			incident_count = getAdjustedIncidentCount(e);
			for (V w : graph.getIncidentVertices(e)) {
				if (!w.equals(v) || hyperedges_are_self_loops) {
					if (w instanceof TopicCategoryVertex) {
						v_hub = (getOutputValue(v)
								* getEdgeWeight(w, e).doubleValue() / incident_count);
						setOutputValue(w, getOutputValue(w)+v_hub);
						downUpdateTreeEle(w);
					}
				}
			}
		}
		return getOutputValue(v);
	}
	

	/**
	 * Updates the value for this vertex. Called by <code>step()</code>.
	 */
	@Override
	protected double update(V v) {
		collectDisappearingPotential(v);
		int incident_count = 0;
		double v_input = 0;
		double v_hub = 0;
		if (v instanceof TopicCategoryVertex) {
			return Math.abs(getCurrentValue(v) - getOutputValue(v));
		} else {
			for (E e : graph.getInEdges(v)) {
				// For hypergraphs, this divides the potential coming from w
				// by the number of vertices in the connecting edge e.
				incident_count = getAdjustedIncidentCount(e);
				for (V w : graph.getIncidentVertices(e)) {
					if (!w.equals(v) || hyperedges_are_self_loops)
						v_input += (getCurrentValue(w)
								* getEdgeWeight(w, e).doubleValue() / incident_count);
				}
			}
			for (E e : graph.getOutEdges(v)) {
				incident_count = getAdjustedIncidentCount(e);
				for (V w : graph.getIncidentVertices(e)) {
					if (!w.equals(v) || hyperedges_are_self_loops)
						if (w instanceof TopicCategoryVertex)
							v_hub += (getOutputValue(w)
									* getEdgeWeight(w, e).doubleValue() / incident_count);
				}
			}
			double new_value = v_input * (1 - hitsCoeff) + v_hub * hitsCoeff;
			if (topicAlpha >= 0) {
				new_value = new_value * topicAlpha + getVertexPrior(v)
						* (1 - topicAlpha);
			}
			setOutputValue(v, new_value);
			return Math.abs(getCurrentValue(v) - new_value);
		}
	}
	
	@Override
	protected void normalizeOutputValue() {
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
		for (V v : graph.getVertices()) {
			if (v instanceof TopicCategoryVertex) {
				// do nothing
			} else {
				setOutputValue(v, getOutputValue(v) / newSumWeight);
				updateMaxDelta(v,
						Math.abs(getCurrentValue(v) - getOutputValue(v)));
			}
		}
//		System.out.println(total_iterations + ":\t" + newSumWeight + ":\t"
//				+ max_delta);
	}
}
