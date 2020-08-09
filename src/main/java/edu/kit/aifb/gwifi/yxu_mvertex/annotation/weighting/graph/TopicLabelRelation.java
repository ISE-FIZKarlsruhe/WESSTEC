package edu.kit.aifb.gwifi.yxu_mvertex.annotation.weighting.graph;

public class TopicLabelRelation extends Edge {

	public final static double WEIGHT = 1.0;
	
	public TopicLabelRelation(Vertex label, Vertex topic) {
		super(label, topic, TopicLabelRelation.WEIGHT);
	}

}
