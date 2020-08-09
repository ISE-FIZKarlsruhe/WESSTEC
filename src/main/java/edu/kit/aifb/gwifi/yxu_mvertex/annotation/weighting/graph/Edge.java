package edu.kit.aifb.gwifi.yxu_mvertex.annotation.weighting.graph;

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
			}else if(target instanceof TopicVertex){
				isValidOut = ((TopicVertex) source).addOutPREdges(this);
				isValidIn = ((TopicVertex) target).addInPREdge(this);
			}else{
				//unexpected
			}
		}else if(source instanceof TopicCategoryVertex){
			if(target instanceof TopicCategoryVertex){
				isValidOut = source.addOutEdge(this);
				isValidIn = ((TopicCategoryVertex) target).addInCateEdges(this);
			}else{
				//unexpected
			}
		}else if(source instanceof TopicLabelVertex){
			if(target instanceof TopicVertex){
				isValidOut = source.addOutEdge(this);
				isValidIn = ((TopicVertex) target).addInLabelEdge(this);
			}else{
				//unexpected
			}
		}else{
			//unexpected ref -> topic 
			isValidOut = source.addOutEdge(this);
			isValidIn = target.addInEdge(this);//((TopicVertex) target).addInPREdge(this);//
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
			}else if(target instanceof TopicVertex){
				totalOutEdgesWeight = ((TopicVertex) source).getTotalOutPRWeight();
			}else{
				//unexpected
			}
		}
		if(totalOutEdgesWeight == 0.0){
			return 0.0;
		}
		return weight / totalOutEdgesWeight;
	}
	
	public double getRelativeInWeight() {
		double totalInEdgesWeight = target.getTotalInEdgesWeight();
		if(target instanceof TopicCategoryVertex){
			if(source instanceof TopicCategoryVertex){
				totalInEdgesWeight = ((TopicCategoryVertex) target).getTotalInCateWeight();
			}else if(source instanceof TopicVertex){
				totalInEdgesWeight = ((TopicCategoryVertex) target).getTotalInTopicWeight();
			}else{
				//unexpected
			}
		}else if(target instanceof TopicVertex){
			if(source instanceof TopicVertex){
				totalInEdgesWeight = ((TopicVertex) target).getTotalInPRWeight();
			}else if(source instanceof TopicLabelVertex){
				totalInEdgesWeight = ((TopicVertex) target).getTotalInLabelWeight();
			}else{
				//unexpected
			}
		}else{
			//unexpected
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
			}else if(target instanceof TopicVertex){
				double totalOutWeight = ((TopicVertex) source).getTotalOutPRWeight() - oldWeight + newWeight;
				((TopicVertex) source).setTotalOutPRWeight(totalOutWeight);
			}else{
				//unexpected
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
			}else if(source instanceof TopicVertex){
				double totalInWeight = ((TopicCategoryVertex) target).getTotalInTopicWeight() - oldWeight + newWeight;
				((TopicCategoryVertex) target).setTotalInTopicWeight(totalInWeight);
			}else{
				//unexpected
			}
		}else if(target instanceof TopicVertex){
			if(source instanceof TopicVertex){
				double totalInWeight = ((TopicVertex) target).getTotalInPRWeight() - oldWeight + newWeight;
				((TopicVertex) target).setTotalInPRWeight(totalInWeight);
			}else if(source instanceof TopicLabelVertex){
				double totalInWeight = ((TopicVertex) target).getTotalInLabelWeight() - oldWeight + newWeight;
				((TopicVertex) target).setTotalInLabelWeight(totalInWeight);
			}else{
				//unexpected
			}
		}else{
			//unexpected
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
