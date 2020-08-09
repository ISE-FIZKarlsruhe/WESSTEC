package edu.kit.aifb.gwifi.yxu.annotation.weighting.graph;

import java.util.HashSet;
import java.util.Set;

/**
 * Abstract class of vertex, which defines vertex (V) in a weighted directive
 * graph (G=(V,E)).
 * 
 * @author admin123
 *
 */
public abstract class Vertex {

	protected Set<Edge> inEdges;
	protected Set<Edge> outEdges;
	protected double totalWeight;
	protected double totalInEdgesWeight;
	protected double totalOutEdgesWeight;

	public Vertex() {
		inEdges = new HashSet<Edge>();
		outEdges = new HashSet<Edge>();
		totalWeight = 0.0;
		totalInEdgesWeight = 0.0;
		totalOutEdgesWeight = 0.0;
	}

	public Vertex(double weight) {
		inEdges = new HashSet<Edge>();
		outEdges = new HashSet<Edge>();
		totalWeight = weight;
		totalInEdgesWeight = 0.0;
		totalOutEdgesWeight = 0.0;
	}
	
	public Set<Edge> getInEdges() {
		return inEdges;
	}

	public boolean addInEdge(Edge in) {
		if (!inEdges.contains(in)) {
			inEdges.add(in);
			totalInEdgesWeight += in.getWeight();
			return true;
		}
		return false;
	}

	public Set<Edge> getOutEdges() {
		return outEdges;
	}

	public boolean addOutEdge(Edge out) {
		if (!outEdges.contains(out)) {
			outEdges.add(out);
			totalOutEdgesWeight += out.getWeight();
			return true;
		}
		return false;
	}

	public double getTotalWeight() {
		return totalWeight;
	}
	
	public void setTotalWeight(double totalWeight){
		this.totalWeight = totalWeight;
	}

	public double getTotalInEdgesWeight() {
		return totalInEdgesWeight;
	}

	public void setTotalInEdgesWeight(double inWeight) {
		totalInEdgesWeight = inWeight;
	}

	public double getTotalOutEdgesWeight() {
		return totalOutEdgesWeight;
	}

	public void setTotalOutEdgesWeight(double outWeight) {
		totalOutEdgesWeight = outWeight;
	}

	/**
	 * @param edge
	 * @return if inEdge, 1; if outEdge, -1; if not edge, 0.
	 */
	public int containsEdge(Edge edge) {
		if (inEdges.contains(edge)) {
			return 1;
		} else if (outEdges.contains(edge)) {
			return -1;
		} else {
			return 0;
		}
	}

}
