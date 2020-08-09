package edu.kit.aifb.gwifi.yxu_mvertex.annotation.weighting.graph;

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
	private Set<Edge> inPREdges;
	private Set<Edge> inLabelEdges;
	private double totalInPREdgesWeight;
	private double totalInLabelEdgesWeight;

	public TopicVertex(Topic topic) {
		super();
		this.topic = topic;
		outPREdges = new HashSet<Edge>();
		outHITSEdges = new HashSet<Edge>();
		inPREdges = new HashSet<Edge>();
		inLabelEdges = new HashSet<Edge>();
		hubWeight = 0.0;
		prWeight = 0.0;
		totalOutPRWeight = 0.0;
		totalOutHITSWeight = 0.0;
		totalInPREdgesWeight = 0.0;
		totalInLabelEdgesWeight = 0.0;
	}

	public Topic getTopic() {
		return topic;
	}
	
	public boolean isLabelled(){
		return (topic.getIndex()>=0)&&(topic.getReferences().size()>0) ;
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
			outEdges.add(outPREdge);
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
			outEdges.add(outHITSEdge);
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
	
	public Set<Edge> getInPREdges() {
		return inPREdges;
	}

	public boolean addInPREdge(Edge inPREdge) {
		if (!inPREdges.contains(inPREdge)) {
			inPREdges.add(inPREdge);
			inEdges.add(inPREdge);
			totalInPREdgesWeight += inPREdge.getWeight();
			return true;
		}
		return false;
	}

	public double getTotalInPRWeight() {
		return totalInPREdgesWeight;
	}

	public void setTotalInPRWeight(double totalInPREdgesWeight) {
		this.totalInPREdgesWeight = totalInPREdgesWeight;
	}
	
	public Set<Edge> getInLabelEdges(){
		return inLabelEdges;
	}
	
	public boolean addInLabelEdge(Edge inLabelEdge){
		if (!inLabelEdges.contains(inLabelEdge)) {
			inLabelEdges.add(inLabelEdge);
			inEdges.add(inLabelEdge);
			totalInLabelEdgesWeight += inLabelEdge.getWeight();
			return true;
		}
		return false;
	}
	
	public double getTotalInLabelWeight(){
		return totalInLabelEdgesWeight;
	}
	
	public void setTotalInLabelWeight(double totalInLabelEdgesWeight){
		this.totalInLabelEdgesWeight = totalInLabelEdgesWeight;
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
