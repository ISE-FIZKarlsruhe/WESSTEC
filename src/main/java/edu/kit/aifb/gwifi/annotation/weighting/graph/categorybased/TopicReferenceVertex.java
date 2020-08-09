package edu.kit.aifb.gwifi.annotation.weighting.graph.categorybased;

import java.text.DecimalFormat;

import edu.kit.aifb.gwifi.annotation.detection.TopicReference;

public class TopicReferenceVertex extends Vertex {

	private TopicReference topicReference;

	public TopicReferenceVertex(TopicReference reference) {
		this.topicReference = reference;
	}

	public TopicReference getTopicReference() {
		return topicReference;
	}

	public String toString() {
		return "[Label:" + topicReference.getLabel().getText() + "] : " 
				+ new DecimalFormat("#.##").format(weight);
	}

	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (!(obj instanceof TopicReferenceVertex))
			return false;
		TopicReferenceVertex trv = (TopicReferenceVertex) obj;
		return topicReference.equals(trv.getTopicReference());
	}

	public int hashCode() {
		return topicReference.hashCode();
	}

}
