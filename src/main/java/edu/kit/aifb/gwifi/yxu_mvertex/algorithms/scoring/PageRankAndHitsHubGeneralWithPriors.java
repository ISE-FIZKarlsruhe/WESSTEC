package edu.kit.aifb.gwifi.yxu_mvertex.algorithms.scoring;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections15.Transformer;

import edu.kit.aifb.gwifi.yxu_mvertex.annotation.weighting.graph.TopicCategoryVertex;
import edu.kit.aifb.gwifi.yxu_mvertex.annotation.weighting.graph.TopicVertex;
import edu.kit.aifb.gwifi.yxu_mvertex.annotation.weighting.graph.TopicLabelVertex;
import edu.uci.ics.jung.graph.Hypergraph;

public class PageRankAndHitsHubGeneralWithPriors<V, E> extends
		PageRankAndHitsHubWithPriors<V, E> {

	private int topicVNum;
	private final static double EXP = 1.0;
	
 	public PageRankAndHitsHubGeneralWithPriors(Hypergraph<V, E> g,
			Transformer<V, Double> vertex_priors, double topicAlpha,
			double cateAlpha, double hitsCoeff) {
		super(g, vertex_priors, topicAlpha, hitsCoeff);
		this.topicAlpha = topicAlpha;
		this.cateAlpha = cateAlpha;
		this.topicVNum = 0;
	}

	public PageRankAndHitsHubGeneralWithPriors(Hypergraph<V, E> g,
			Transformer<E, ? extends Number> edge_weights,
			Transformer<V, Double> vertex_priors, double topicAlpha,
			double cateAlpha, double hitsCoeff) {
		super(g, edge_weights, vertex_priors, topicAlpha, hitsCoeff);
		this.topicAlpha = topicAlpha;
		this.cateAlpha = cateAlpha;
		this.topicVNum = 0;
	}

	@Override
	public void evaluate() {
		//TODO count the number of topic for teleportation
//		initTopicVNum();
		initLabel();
		//TODO filter topics with pagerank
//		double backupHitsCoeff = hitsCoeff;
//		hitsCoeff = 0.0;
//		do{
//			max_delta = Double.MIN_VALUE;
//			prStep();
//			normalizeOutputValue();
//		} while(!done());
//		hitsCoeff = backupHitsCoeff;
//		pr_disappearing_potential=0.0;
		do {
			max_delta = Double.MIN_VALUE;
			step();
			normalizeOutputValue();
		} while (!done());
		System.out.println("iterations: "+total_iterations);
		outputAllCates();
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
	
	private void initLabel(){
		for (V v : graph.getVertices()) {
			if (v instanceof TopicLabelVertex) {
				double labelWeight = getOutputValue(v);
				labelWeight = Math.pow(labelWeight, 0.5);
				setCurrentValue(v, labelWeight);
			}
		}
	}
	
//	private void prStep(){
//		swapOutputForCurrent();
//		collectDisappearingPR();
//		updateTopicGragh();
//		total_iterations++;
//		pr_disappearing_potential=0.0;
//	}
	
	@Override
	public void step() {
		//in output
		swapOutputForCurrent();
		collectDisappearingPR();
		//in current
		updateLabel();
		//label in l-current, total topic^2 of label in l-output, topic^2 in t-output
		updateDistTopicWeight();
		//topic->cate in t-output, topic->topic in t-current
		updateCateTree();
		//cate->topic in t-output
		updateTopicGragh();
		//topic in t-output
		total_iterations++;
		pr_disappearing_potential=0.0;
	}
	
	protected void updateLabel(){
		clearTopicOutput();
		for (V v : graph.getVertices()) {
			if (v instanceof TopicLabelVertex) {
				double totalExpTopicWeight = 0.0;
//				double maxTopicWeight = 0.0;
				double curTopicWeight = 0.0;
				double expTopicWeight = 0.0;
				int topicNum = graph.getOutEdges(v).size();
				double exp = Math.pow(topicNum, 0.5);
				for (E e : graph.getOutEdges(v)) {
					for (V w : graph.getIncidentVertices(e)) {
						if(!(w instanceof TopicVertex)) continue;
						if (!w.equals(v) || hyperedges_are_self_loops){
							curTopicWeight = getCurrentValue(w);
							expTopicWeight = Math.pow(curTopicWeight, exp);
							setOutputValue(w, expTopicWeight);
							totalExpTopicWeight += expTopicWeight;
//							if(maxTopicWeight<curTopicWeight)
//								maxTopicWeight = curTopicWeight;
						}
					}
				}
//				setCurrentValue(v, maxTopicWeight);
				setOutputValue(v, totalExpTopicWeight);
			}
		}
	}
	
	protected void updateDistTopicWeight(){
		for (V v : graph.getVertices()) {
			double topic2CateWeight = 0.0;
//			double topic2TopicWeight = 0.0;
			if (v instanceof TopicLabelVertex) {
				double totalExpTopicWeight = getOutputValue(v);
				double labelWeight = getCurrentValue(v);
				for (E e : graph.getOutEdges(v)) {
					for (V w : graph.getIncidentVertices(e)) {
						if(!(w instanceof TopicVertex)) continue;
						if (!w.equals(v) || hyperedges_are_self_loops){
							topic2CateWeight = getTopic2CateWeight(totalExpTopicWeight, getOutputValue(w), labelWeight);
//							topic2TopicWeight = getCurrentValue(w) - topic2CateWeight;
							setOutputValue(w, topic2CateWeight);
//							setCurrentValue(w, topic2TopicWeight);
						}
					}
				}
				setOutputValue(v, labelWeight);
			}else if(v instanceof TopicVertex){
				if(getOutputValue(v)==0.0 && !((TopicVertex)v).isLabelled()){
//					topic2CateWeight = getCurrentValue(v);
//					topic2TopicWeight = getCurrentValue(v) - topic2CateWeight;
//					setOutputValue(v, topic2CateWeight);
//					setCurrentValue(v, topic2TopicWeight);
				}
			}else{
				//do nothing
			}
		}
	}
	
	private void clearTopicOutput(){
		for (V v : graph.getVertices()) {
			if (v instanceof TopicVertex) {
				setOutputValue(v, 0.0);
			}
		}
	}

	private double getTopic2CateWeight(double totalExpTopicWeight,
			double curExpTopicWeight, double labelWeight){
		double topic2CateWeight = curExpTopicWeight/totalExpTopicWeight;
		topic2CateWeight = labelWeight*Math.pow(topic2CateWeight, EXP);
		return topic2CateWeight;
	}
	
	protected void updateCateTree() {
		for (V v : graph.getVertices()) {
			if (v instanceof TopicCategoryVertex) {
				setCurrentValue(v, -1.0);
				setOutputValue(v, 0.0);
			}
		}
		double maxCateWeight = 0.0;
		Map<V,Double> rootCate2Weight = new HashMap<V,Double>();
		for (V v : graph.getVertices()) {
			if (v instanceof TopicCategoryVertex) {
				if (graph.getOutEdges(v).size() == 0) {
					updateParentCate(v);
					rootCate2Weight.put(v, getCurrentValue(v));
					maxCateWeight = Math.max(maxCateWeight, getCurrentValue(v));
				}
			}
		}
		//TODO orient the root cates in the output
//		orientRootCate(rootCate2Weight);
//		double entropy = entropy(rootCate2Weight.values());
//		double cateFactor = 1 - entropy;
//		cateFactor = cateFactor>0.1? Math.pow(cateFactor, 0.5) : 0.0;
//		cateFactor = cateFactor>0.1? cateFactor : 0.0;
//		cateFactor = cateFactor>0.1? 1.0 : Math.pow(cateFactor, 2);
//		cateFactor = cateFactor>0.1? 1.0 : cateFactor;
//		cateFactor = cateFactor>0.1? Math.pow(cateFactor, 0.5) : Math.pow(cateFactor, 2);
//		cateFactor = cateFactor>0.1? 1.0 : 0.0;
		
//		if(maxCateWeight==0.0)
//			maxCateWeight = 1.0;
//		for (V v : graph.getVertices()) {
//			if (v instanceof TopicCategoryVertex) {
//				if (graph.getOutEdges(v).size() == 0) {
//					mergeInitCateWeight(v, maxCateWeight);
//				}
//			}
//		}
		clearTopicOutput();
		
		//TODO update the topic and all cates with parent cates
//		for (V v : graph.getVertices()) {
//			if (v instanceof TopicCategoryVertex) {
//				if (graph.getOutEdges(v).size() == 0) {
//					if (getOutputValue(v) > 0.0)
//						updateChildCates(v);
//				}
//			}
//		}
		//TODO update the topic with parent cates only under the chosen rootcate
//		double chosenCateWeight = maxCateWeight*entropy;
//		for (V v : graph.getVertices()) {
//			if (v instanceof TopicCategoryVertex) {
//				if (graph.getOutEdges(v).size() == 0) {
//					if (getOutputValue(v) > chosenCateWeight)
//						updateChildTopicUnderRootCate(v);
//				}
//			}
//		}
		//TODO update only the topic with parent cates
		for (V v : graph.getVertices()) {
			if (v instanceof TopicCategoryVertex) {
				updateChildTopic(v);
			}
		}
		normalizeTopicOutput(1.0);//cateFactor
	}
	
	private void orientRootCate(Map<V,Double> rootCate2Weight){
		double rootCateNum = rootCate2Weight.size();
		double weightExp = Math.pow(rootCateNum, 0.5);
		double totalRootCateWeight = 0.0;
		double curRootCateWeight = 0.0;
		for(Entry<V,Double> rootC2W: rootCate2Weight.entrySet()){
			curRootCateWeight = rootC2W.getValue();
			curRootCateWeight = Math.pow(curRootCateWeight, weightExp);
			rootC2W.setValue(curRootCateWeight);
			totalRootCateWeight+=curRootCateWeight;
		}
		if(totalRootCateWeight<=0.0)
			totalRootCateWeight = 1.0;
		for(Entry<V,Double> rootC2W: rootCate2Weight.entrySet()){
			curRootCateWeight = rootC2W.getValue();
			curRootCateWeight = curRootCateWeight/totalRootCateWeight;
			rootC2W.setValue(curRootCateWeight);
			setOutputValue(rootC2W.getKey(), curRootCateWeight);
		}
	}
	
	private double entropy(Collection<Double> eles) {
		double totalEle = 0.0;
		int eleNum = 0;
		for(Double ele:eles){
			if(ele<=0.0) continue;
			eleNum ++;
			totalEle += ele;
		}
		if(totalEle == 0.0) return 1.0;
		if(eleNum == 1.0) return 0.0;
		double entropy = 0.0;
		double relEle;
		for(Double ele:eles){
			if(ele<=0.0) continue;
			relEle = ele/totalEle;
			entropy -= relEle * Math.log(relEle)/Math.log(eleNum);
		}
		return entropy;
	}
	
	private void mergeInitCateWeight(V v, double maxCateWeight){
		if (v instanceof TopicCategoryVertex) {
			double normalizedCateWeight = getCurrentValue(v)/maxCateWeight;
			double mergedCateWeight = cateAlpha*normalizedCateWeight + (1-cateAlpha)*getVertexPrior(v);
			setOutputValue(v, mergedCateWeight);
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
						if(childCurVal==-1.0){
							updateParentCate(w);//reach child not updated, go deeper
						}else if(childCurVal==-2.0){
							continue;			//reach child in updating state, form a loop
						}
						v_auth += (getCurrentValue(w) * getEdgeWeight(w, e).doubleValue() / incident_count);
					}else if(w instanceof TopicVertex) {
						v_auth += (getOutputValue(w) * getEdgeWeight(w, e).doubleValue() / incident_count);
					}else{
						//unexcepted
					}
				}
			}
		}
		setCurrentValue(v, v_auth);
		return v_auth;
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
						setOutputValue(w, v_hub);
						updateChildCates(w);
//						setOutputValue(w, 0.0);
					}else if (w instanceof TopicVertex) {
						setOutputValue(w, getOutputValue(w)+v_hub);
					}
				}
			}
		}
		return getOutputValue(v);
	}
	
	private double updateChildTopicUnderRootCate(V v) {
		double v_hub = 0.0;
		int incident_count = 0;
		for (E e : graph.getInEdges(v)) {
			incident_count = getAdjustedIncidentCount(e);
			for (V w : graph.getIncidentVertices(e)) {
				if (!w.equals(v) || hyperedges_are_self_loops) {
					if (w instanceof TopicCategoryVertex) {
						updateChildTopicUnderRootCate(w);
					}else if (w instanceof TopicVertex) {
						v_hub = (getCurrentValue(v) * getEdgeWeight(w, e).doubleValue() / incident_count);
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
		for (V v : graph.getVertices()) {
			topicUpdateTopic(v);
		}
		normalizeTopicOutput(1.0);
		for (V v : graph.getVertices()) {
			if (v instanceof TopicVertex) {
				double new_value = getOutputValue(v);
				if (topicAlpha >= 0) {
					//TODO mode of teleportation in the graph
					new_value = new_value * topicAlpha + getVertexPrior(v) * (1 - topicAlpha);
//					new_value = new_value * topicAlpha + 1/((double)topicVNum) * (1 - topicAlpha);
				}
				setOutputValue(v, new_value);
			}
		}
	}
	
	private double topicUpdateTopic(V v) {
		int incident_count = 0;
		double v_input = 0;
		if (v instanceof TopicVertex) {
			for (E e : graph.getInEdges(v)) {
				// For hypergraphs, this divides the potential coming from w
				// by the number of vertices in the connecting edge e.
				incident_count = getAdjustedIncidentCount(e);
				for (V w : graph.getIncidentVertices(e)) {
					if(!(w instanceof TopicVertex)) continue;
					if (!w.equals(v) || hyperedges_are_self_loops)
						//TODO the transition also depends on the weight of both start and end
						v_input += (getCurrentValue(w) //* Math.min(1.0f, getCurrentValue(w)/getCurrentValue(v))
								* getEdgeWeight(w, e).doubleValue() / incident_count);
				}
			}
			if(pr_disappearing_potential>0.0){
				v_input += pr_disappearing_potential*getCurrentValue(v);
			}
			//TODO the non-cate topic weighted with current weight
			double new_value = v_input * (1 - hitsCoeff) + getOutputValue(v) * hitsCoeff;
			setOutputValue(v, new_value);
			return Math.abs(getCurrentValue(v) - new_value);
		} else {
			return 0.0;
		}
	}
	
	private void collectDisappearingPR() {
		if (!isDisconnectedGraphOK()) return;
		for (V v : graph.getVertices()) {
			if (v instanceof TopicVertex) {
				if(((TopicVertex)v).getOutPREdges().size()==0){
					pr_disappearing_potential += getCurrentValue(v);
				}
			}
		}
	}

 	private void normalizeTopicOutput(double cateFactor){
		double totalOutputWeight = 0.0;
		double totalCurWeight = 0.0;
		for (V v : graph.getVertices()) {
			if (v instanceof TopicVertex) {
				totalCurWeight += getCurrentValue(v);
				if(getOutputValue(v)==0.0){
				} else {
					totalOutputWeight += getOutputValue(v);
				}
			}
		}
		if(totalOutputWeight==0.0)
			totalOutputWeight=1.0;
		for (V v : graph.getVertices()) {
			if (v instanceof TopicVertex) {
				if(getOutputValue(v)==0.0){
				} else {
					setOutputValue(v, cateFactor * getOutputValue(v) * totalCurWeight / totalOutputWeight);
				}
			}
		}
	}
	
	@Override
	protected void normalizeOutputValue() {
		double newSumWeight = 0.0;
		for (V v : graph.getVertices()) {
			if (v instanceof TopicVertex) {
				newSumWeight += getOutputValue(v);// * getOutputValue(v);
			} else {
				// do nothing
			}
		}
//		newSumWeight = Math.sqrt(newSumWeight);
		if(newSumWeight==0.0)
			newSumWeight=1.0;
		for (V v : graph.getVertices()) {
			if (v instanceof TopicVertex) {
				setOutputValue(v, getOutputValue(v) / newSumWeight);
				updateMaxDelta(v, Math.abs(getCurrentValue(v) - getOutputValue(v)));
			} else {
				// do nothing
			}
		}
//		System.out.println(total_iterations + ":\t" + newSumWeight + ":\t"
//				+ max_delta);
	}
	
	protected void outputAllCates(){
		for (V v : graph.getVertices()) {
			if (v instanceof TopicLabelVertex) {
				double maxTopicWeight = 0.0;
				double curTopicWeight = 0.0;
				int maxTopicNum = 0;
				for (E e : graph.getOutEdges(v)) {
					for (V w : graph.getIncidentVertices(e)) {
						if(!(w instanceof TopicVertex)) continue;
						if (!w.equals(v) || hyperedges_are_self_loops){
							curTopicWeight = getOutputValue(w);
							if(curTopicWeight>maxTopicWeight){
								maxTopicWeight = curTopicWeight;
								maxTopicNum = 1;
							}else if(curTopicWeight == maxTopicWeight){
								maxTopicNum++;
							}
							setCurrentValue(w, curTopicWeight);
						}
					}
				}
				double labelWeight = getCurrentValue(v);
				double topic2CateWeight = 0.0;
				for (E e : graph.getOutEdges(v)) {
					for (V w : graph.getIncidentVertices(e)) {
						if(!(w instanceof TopicVertex)) continue;
						if (!w.equals(v) || hyperedges_are_self_loops){
							if(getOutputValue(w)<maxTopicWeight){
								setOutputValue(w, 0.0);
							}else{
								topic2CateWeight = getTopic2CateWeight((double)maxTopicNum, 1.0, labelWeight);
								setOutputValue(w, topic2CateWeight);
							}
						}
					}
				}
			}
		}
		for (V v : graph.getVertices()) {
			if (v instanceof TopicCategoryVertex) {
				setCurrentValue(v, -1.0);
				setOutputValue(v, 0.0);
			}
		}
		for (V v : graph.getVertices()) {
			if (v instanceof TopicCategoryVertex) {
				if (graph.getOutEdges(v).size() == 0) {
					updateParentCate(v);
				}
			}
		}
		for (V v : graph.getVertices()) {
			if ((v instanceof TopicCategoryVertex)||(v instanceof TopicVertex)) {
				setOutputValue(v, getCurrentValue(v));
			}
		}
	}
}
