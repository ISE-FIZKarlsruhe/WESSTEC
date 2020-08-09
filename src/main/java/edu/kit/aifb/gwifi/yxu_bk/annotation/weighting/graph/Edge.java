package edu.kit.aifb.gwifi.yxu_bk.annotation.weighting.graph;

import java.text.DecimalFormat;

/**
 * Class of edge, which defines edge (E) in a weighted directive graph
 * (G=(V,E)).
 * 
 * @author admin123
 *
 */
public class Edge {

	private Vertex source;
	private Vertex target;
	private double weight;
	private boolean isValid;

	public Edge(Vertex source, Vertex target, double weight) {
		this.source = source;
		this.target = target;
		this.weight = weight;
		boolean isValidIn = false;
		boolean isValidOut = false;
		if(source instanceof TopicVertex){
			if(target instanceof TopicCategoryVertex){
				isValidOut = ((TopicVertex) source).addOutHITSEdges(this);
				isValidIn = ((TopicCategoryVertex) target).addInTopicEdges(this);
			}else{
				isValidOut = ((TopicVertex) source).addOutPREdges(this);
				isValidIn = target.addInEdge(this);
			}
		}else if(source instanceof TopicCategoryVertex){
			if(target instanceof TopicCategoryVertex){
				isValidOut = source.addOutEdge(this);
				isValidIn = ((TopicCategoryVertex) target).addInCateEdges(this);
			}else{
				//unexpected
			}
		}else{
			isValidOut = source.addOutEdge(this);
			isValidIn = target.addInEdge(this);
		}
		this.isValid = isValidIn&&isValidOut;
	}

	public double getWeight() {
		return weight;
	}

	public double getRelativeOutWeight() {
		double totalOutEdgesWeight = source.getTotalOutEdgesWeight();
		if(source instanceof TopicVertex){
			if(target instanceof TopicCategoryVertex){
				//TODO not normalized topic category edge weight
				totalOutEdgesWeight = 1.0;//((TopicVertex) source).getTotalOutHITSWeight();
			}else{
				totalOutEdgesWeight = ((TopicVertex) source).getTotalOutPRWeight();
			}
		}
		if(totalOutEdgesWeight == 0.0){
			totalOutEdgesWeight = 1.0;
		}
		return weight / totalOutEdgesWeight;
	}
	
	public double getRelativeInWeight() {
		double totalInEdgesWeight = target.getTotalInEdgesWeight();
		if(target instanceof TopicCategoryVertex){
			if(source instanceof TopicCategoryVertex){
				totalInEdgesWeight = ((TopicCategoryVertex) target).getTotalInCateWeight()+((TopicCategoryVertex) target).getTotalInTopicWeight();
			}else{
				totalInEdgesWeight = ((TopicCategoryVertex) target).getTotalInCateWeight()+((TopicCategoryVertex) target).getTotalInTopicWeight();
			}
		}
		if(totalInEdgesWeight == 0.0){
			totalInEdgesWeight = 1.0;
		}
		return weight / totalInEdgesWeight;
	}

	public void setWeight(double newWeight, Boolean isSrcUpdated, Boolean isTarUpdated) {
		if(isSrcUpdated)updateToSrc(weight, newWeight);
		if(isTarUpdated)updateToTar(weight, newWeight);
		weight = newWeight;
	}

	private void updateToSrc(double oldWeight, double newWeight) {
		if(source instanceof TopicVertex){
			if(target instanceof TopicCategoryVertex){
				double totalOutWeight = ((TopicVertex) source).getTotalOutHITSWeight() - oldWeight + newWeight;
				((TopicVertex) source).setTotalOutHITSWeight(totalOutWeight);
			}else{
				double totalOutWeight = ((TopicVertex) source).getTotalOutPRWeight() - oldWeight + newWeight;
				((TopicVertex) source).setTotalOutPRWeight(totalOutWeight);
			}
		}else{
			double totalOutWeight = source.getTotalOutEdgesWeight() - oldWeight + newWeight;
			source.setTotalOutEdgesWeight(totalOutWeight);
		}
	}
	
	private void updateToTar(double oldWeight, double newWeight) {
		if(target instanceof TopicCategoryVertex){
			if(source instanceof TopicCategoryVertex){
				double totalInWeight = ((TopicCategoryVertex) target).getTotalInCateWeight() - oldWeight + newWeight;
				((TopicCategoryVertex) target).setTotalInCateWeight(totalInWeight);
			}else{
				double totalInWeight = ((TopicCategoryVertex) target).getTotalInTopicWeight() - oldWeight + newWeight;
				((TopicCategoryVertex) target).setTotalInTopicWeight(totalInWeight);
			}
		}else{
			double totalInWeight = target.getTotalInEdgesWeight() - oldWeight + newWeight;
			target.setTotalInEdgesWeight(totalInWeight);
		}
	}

	public Vertex getSource() {
		return source;
	}

	public Vertex getTarget() {
		return target;
	}
	
	public boolean isValid(){
		return isValid;
	}

	public String toString() {
		return "[" + source + ", " + target + "] : "
				+ new DecimalFormat("#.##").format(weight);
	}

	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (!(obj instanceof Edge))
			return false;
		Edge edge = (Edge) obj;
		if (!source.equals(edge.getSource()))
			return false;
		if (!target.equals(edge.getTarget()))
			return false;
		return true;
	}

	public int hashCode() {
		int hash = 1;
		hash = hash * 17 + source.hashCode();
		hash = hash * 31 + target.hashCode();
		return hash;
	}

}
