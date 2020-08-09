package edu.kit.aifb.gwifi.annotation.weighting.graph.categorybased;

import java.text.DecimalFormat;

import edu.kit.aifb.gwifi.annotation.detection.Topic;

public class TopicVertex extends Vertex {

	private Topic topic;

	public TopicVertex(Topic topic) {
		this.topic = topic;
	}

	public Topic getTopic() {
		return topic;
	}

	public String toString() {
		return "[Topic:" + topic.getTitle() + "] : "
				+ new DecimalFormat("#.##").format(weight);
	}

	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (!(obj instanceof TopicVertex))
			return false;
		TopicVertex tv = (TopicVertex) obj;
		return topic.equals(tv.getTopic());
	}

	public int hashCode() {
		return topic.hashCode();
	}

}
