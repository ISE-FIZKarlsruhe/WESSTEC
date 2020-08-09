package edu.kit.aifb.gwifi.yxu_bk.annotation.weighting.graph;

import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Set;

import edu.kit.aifb.gwifi.annotation.detection.Topic;

public class TopicVertex extends Vertex {

	private Topic topic;
	private double hubWeight;
	private double prWeight;
	private Set<Edge> outPREdges;
	private Set<Edge> outHITSEdges;
	private double totalOutPRWeight;
	private double totalOutHITSWeight;

	public TopicVertex(Topic topic) {
		super();
		this.topic = topic;
		outPREdges = new HashSet<Edge>();
		outHITSEdges = new HashSet<Edge>();
		hubWeight = 0.0;
		prWeight = 0.0;
		totalOutPRWeight = 0.0;
		totalOutHITSWeight = 0.0;
	}

	public Topic getTopic() {
		return topic;
	}

	public double getHubWeight() {
		return hubWeight;
	}

	public void setHubWeight(double hubWeight) {
		this.hubWeight = hubWeight;
	}

	public double getPRWeight() {
		return prWeight;
	}

	public void setPRWeight(double prWeight) {
		this.prWeight = prWeight;
	}

	public void updateHubWeight() {
		double tempHubWeight = 0.0;
		double tempAuthWeight = 0.0;
		for (Edge e : outHITSEdges) {
			if (e.getTarget() instanceof TopicCategoryVertex) {
				tempAuthWeight = ((TopicCategoryVertex) e.getTarget()).getAuthWeight();
			}else{
				//unexpected
			}
			tempHubWeight += e.getRelativeOutWeight()*tempAuthWeight;
		}
		hubWeight = tempHubWeight;
	}

	public void updatePRWeight() {
		double tempPRWeight = 0.0;
		double tempWeight = 0.0;
		for (Edge e : inEdges) {
			if (e.getSource() instanceof TopicVertex) {
				tempWeight = ((TopicVertex) e.getTarget()).getPRWeight();
			} else if (e.getSource() instanceof TopicReferenceVertex) {
				tempWeight = ((TopicReferenceVertex) e.getSource()).getPRWeight();
			}else{
				//unexpected
			}
			tempPRWeight +=  e.getRelativeOutWeight()* tempWeight;
		}
		prWeight = tempPRWeight;
	}

	public Set<Edge> getOutPREdges() {
		return outPREdges;
	}

	public boolean addOutPREdges(Edge outPREdge) {
		if (!outPREdges.contains(outPREdge)) {
			outPREdges.add(outPREdge);
			totalOutPRWeight += outPREdge.getWeight();
			return true;
		}
		return false;
	}

	public Set<Edge> getOutHITSEdges() {
		return outHITSEdges;
	}

	public boolean addOutHITSEdges(Edge outHITSEdge) {
		if (!outHITSEdges.contains(outHITSEdge)) {
			outHITSEdges.add(outHITSEdge);
			totalOutHITSWeight += outHITSEdge.getWeight();
			return true;
		}
		return false;
	}

	public double getTotalOutPRWeight() {
		return totalOutPRWeight;
	}

	public void setTotalOutPRWeight(double totalOutPRWeight) {
		this.totalOutPRWeight = totalOutPRWeight;
	}

	public double getTotalOutHITSWeight() {
		return totalOutHITSWeight;
	}

	public void setTotalOutHITSWeight(double totalOutHITSWeight) {
		this.totalOutHITSWeight = totalOutHITSWeight;
	}

	public String toString() {
		return "[Topic:" + topic.getTitle() + "] : "
				+ new DecimalFormat("#.##").format(totalWeight);
	}

	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (!(obj instanceof TopicVertex))
			return false;
		return topic.equals(((TopicVertex) obj).getTopic());
	}

	public int hashCode() {
		return topic.hashCode();
	}
}
