package edu.kit.aifb.gwifi.yxu.annotation.weighting.graph;

import java.text.DecimalFormat;

import edu.kit.aifb.gwifi.annotation.detection.TopicReference;

public class TopicReferenceVertex extends Vertex {

	private TopicReference topicReference;
	private double prWeight;

	public TopicReferenceVertex(TopicReference reference) {
		this.topicReference = reference;
		this.prWeight = 0.0;
	}

	public TopicReference getTopicReference() {
		return topicReference;
	}

	public double getPRWeight() {
		return prWeight;
	}

	public void setPRWeight(double prWeight) {
		this.prWeight = prWeight;
		totalWeight = this.prWeight;
	}

	public void updatePRWeight() {
		double tempPRWeight = 0.0;
		double tempWeight = 0.0;
		for (Edge e : outEdges) {
			if (e.getTarget() instanceof TopicVertex) {
				tempWeight = ((TopicVertex) e.getTarget()).getPRWeight();
			}else if (e.getTarget() instanceof TopicReferenceVertex) {
				tempWeight = ((TopicReferenceVertex) e.getTarget()).getPRWeight();
			}else{
				//unexpected
			}
				tempPRWeight +=  e.getRelativeOutWeight()*tempWeight ;
		}
		setPRWeight(tempPRWeight);
	}

	public String toString() {
		return "[Label:" + topicReference.getLabel().getText() + "] : "
				+ new DecimalFormat("#.##").format(totalWeight);
	}

	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (!(obj instanceof TopicReferenceVertex))
			return false;
		return topicReference.equals(((TopicReferenceVertex) obj)
				.getTopicReference());
	}

	public int hashCode() {
		return topicReference.hashCode();
	}
}
