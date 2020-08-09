package edu.kit.aifb.gwifi.yxu_bk.annotation.weighting.graph;

import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Set;

import edu.kit.aifb.gwifi.yxu_bk.annotation.detection.TopicCategory;

public class TopicCategoryVertex extends Vertex {
	private TopicCategory topicCategory;
	protected double authWeight;
	protected double hubWeight;
	private Set<Edge> inTopicEdges;
	private Set<Edge> inCateEdges;
	private double totalInTopicWeight;
	private double totalInCateWeight;

	public TopicCategoryVertex(TopicCategory topicCategory) {
		super();
		this.topicCategory = topicCategory;
		inTopicEdges = new HashSet<Edge>();
		inCateEdges = new HashSet<Edge>();
		authWeight = 0.0;
		hubWeight = 0.0;
		totalInTopicWeight = 0.0;
		totalInCateWeight = 0.0;
	}
	
	public TopicCategoryVertex(TopicCategory topicCategory, double totalWeight) {
		super(totalWeight);
		this.topicCategory = topicCategory;
		inTopicEdges = new HashSet<Edge>();
		inCateEdges = new HashSet<Edge>();
		authWeight = 0.0;
		hubWeight = 0.0;
		totalInTopicWeight = 0.0;
		totalInCateWeight = 0.0;
	}

	public TopicCategory getTopicCategory() {
		return topicCategory;
	}

	public double getAuthWeight() {
		return authWeight;
	}

	public void setAuthWeight(double authWeight) {
		this.authWeight = authWeight;
	}

	public double getHubWeight() {
		return hubWeight;
	}

	public void setHubWeight(double hubWeight) {
		this.hubWeight = hubWeight;
	}

	public void updateAuthWeight() {
		double tempAuthWeight = 0.0;
		double tempHubWeight = 0.0;
		for (Edge e : inTopicEdges) {
			if (e.getSource() instanceof TopicVertex) {
				tempHubWeight = ((TopicVertex) e.getSource()).getHubWeight();
			}else{
				//unexpected
			}
			tempAuthWeight +=e.getRelativeOutWeight()* tempHubWeight;
		}
		for (Edge e : inCateEdges) {
			if (e.getSource() instanceof TopicCategoryVertex){
				tempHubWeight = ((TopicCategoryVertex) e.getSource()).getHubWeight();
			}else{
				//unexpected
			}
			tempAuthWeight +=e.getRelativeOutWeight()* tempHubWeight;
		}
		authWeight = tempAuthWeight;
	}

	public void updateHubWeight() {
		double tempHubWeight = 0.0;
		double tempAuthWeight = 0.0;
		for (Edge e : outEdges) {
			if (e.getTarget() instanceof TopicCategoryVertex) {
				tempAuthWeight = ((TopicCategoryVertex) e.getTarget()).getAuthWeight();
			}else{
				//unexpected
			}
			tempHubWeight +=e.getRelativeOutWeight()* tempAuthWeight;
		}
		hubWeight = tempHubWeight;
	}
	
	public Set<Edge> getInTopicEdges() {
		return this.inTopicEdges;
	}

	public boolean addInTopicEdges(Edge inTopicEdge) {
		if (!inTopicEdges.contains(inTopicEdge)) {
			inTopicEdges.add(inTopicEdge);
			totalInTopicWeight += inTopicEdge.getWeight();
			return true;
		}
		return false;
	}
	
	public Set<Edge> getInCateEdges() {
		return this.inCateEdges;
	}

	public boolean addInCateEdges(Edge inCateEdge) {
		if (!inCateEdges.contains(inCateEdge)) {
			inCateEdges.add(inCateEdge);
			totalInCateWeight += inCateEdge.getWeight();
			return true;
		}
		return false;
	}

	public double getTotalInTopicWeight() {
		return totalInTopicWeight;
	}

	public void setTotalInTopicWeight(double totalInTopicWeight) {
		this.totalInTopicWeight = totalInTopicWeight;
	}

	public double getTotalInCateWeight() {
		return totalInCateWeight;
	}

	public void setTotalInCateWeight(double totalInCateWeight) {
		this.totalInCateWeight = totalInCateWeight;
	}

	public String toString() {
		return "[TopicCategory " + topicCategory.getIndex()+": "+ topicCategory.getTitle() + "] : "
				+ new DecimalFormat("#.##").format(totalWeight);
	}

	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (!(obj instanceof TopicCategoryVertex))
			return false;
		return topicCategory.equals(((TopicCategoryVertex) obj)
				.getTopicCategory());
	}

	public int hashCode() {
		return topicCategory.hashCode();
	}
}
