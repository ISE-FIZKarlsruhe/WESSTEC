package edu.kit.aifb.gwifi.yxu_mvertex.annotation.weighting.graph;

import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Set;

public class TopicLabelVertex extends Vertex {

	private String label;
	
	public TopicLabelVertex(String label){
		super();
		this.label = label;
	}
	
	public String getLabel(){
		return this.label;
	}
	
	public Set<TopicVertex> getTopicVs(){
		Set<TopicVertex> topicVs = new HashSet<TopicVertex>();
		Vertex topicV;
		for(Edge topicLabelRel: getOutEdges()){
			topicV = topicLabelRel.getTarget();
			if(topicV instanceof TopicVertex)
				topicVs.add((TopicVertex)topicV);
		}
		return topicVs;
	}
	
	public boolean containsTopic(TopicVertex topicV){
		return getTopicVs().contains(topicV);
	}
	
	public long getTopicNum(){
		return Math.round(getTotalOutEdgesWeight());
	}
	
 	public String toString() {
		return "[Label:" + label + "] : "
				+ new DecimalFormat("#.##").format(totalWeight);
	}

	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (!(obj instanceof TopicLabelVertex))
			return false;
		return label.equals(((TopicLabelVertex) obj).getLabel());
	}

	public int hashCode() {
		return label.hashCode();
	}
}
