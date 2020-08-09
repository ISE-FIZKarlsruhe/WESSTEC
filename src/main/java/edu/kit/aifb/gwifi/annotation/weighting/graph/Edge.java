package edu.kit.aifb.gwifi.annotation.weighting.graph;

import java.text.DecimalFormat;

public class Edge {
	
	private Vertex source;
	private Vertex target;
	
	private double weight;
	
	public Edge(Vertex source, Vertex target) {
		this.source = source;
		this.target = target;
	}
	
	public void setWeight(double weight) {
		this.weight = weight;
	}
	
	public Vertex getSource() {
		return source;
	}
	
	public Vertex getTarget() {
		return target;
	}

	public double getWeight() {
		return weight; 
	}
	
	public String toString() {
		return "[" + source + ", " + target + "] : " 
				+ new DecimalFormat("#.##").format(weight);
	}
	
	public boolean equals(Object obj) {
		if(obj == this)
			return true;
		if(!(obj instanceof Edge))
			return false;
		Edge edge = (Edge)obj;
		if(!source.equals(edge.getSource()))
			return false;
		if(!target.equals(edge.getTarget()))
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
