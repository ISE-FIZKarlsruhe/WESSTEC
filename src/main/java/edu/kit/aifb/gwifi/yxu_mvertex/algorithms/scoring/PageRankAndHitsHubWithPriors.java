package edu.kit.aifb.gwifi.yxu_mvertex.algorithms.scoring;

import org.apache.commons.collections15.Transformer;

import edu.kit.aifb.gwifi.yxu_mvertex.annotation.weighting.graph.TopicCategoryVertex;
import edu.kit.aifb.gwifi.yxu_mvertex.annotation.weighting.graph.TopicVertex;
import edu.uci.ics.jung.algorithms.scoring.AbstractIterativeScorerWithPriors;
import edu.uci.ics.jung.algorithms.scoring.PageRank;
import edu.uci.ics.jung.algorithms.scoring.util.UniformDegreeWeight;
import edu.uci.ics.jung.graph.Hypergraph;

/**
 * A generalization of PageRank that permits non-uniformly-distributed random
 * jumps. The 'vertex_priors' (that is, prior probabilities for each vertex) may
 * be thought of as the fraction of the total 'potential' that is assigned to
 * that vertex at each step out of the portion that is assigned according to
 * random jumps (this portion is specified by 'alpha').
 * 
 * @see "Algorithms for Estimating Relative Importance in Graphs by Scott White and Padhraic Smyth, 2003"
 * @see PageRank
 */
public class PageRankAndHitsHubWithPriors<V, E> extends
		AbstractIterativeScorerWithPriors<V, E, Double> {

	protected double hitsCoeff;

	protected double topicAlpha;

	protected double cateAlpha;

	/**
	 * Maintains the amount of potential associated with vertices with no
	 * out-edges.
	 */
	protected double pr_disappearing_potential;

	/**
	 * The sum of the potential, at each step, associated with vertices with no
	 * outedges (authority) or no inedges (hub).
	 */
	// not necessary for hits-hub
	// protected HITS.Scores disappearing_potential;

	/**
	 * Creates an instance with the specified graph, vertex priors, and 'random
	 * jump' probability (alpha). The outgoing edge weights for each vertex will
	 * be equal and sum to 1.
	 * 
	 * @param graph
	 *            the input graph
	 * @param vertex_priors
	 *            the prior probabilities for each vertex
	 * @param alpha
	 *            the probability of executing a 'random jump' at each step
	 */
	public PageRankAndHitsHubWithPriors(Hypergraph<V, E> g,
			Transformer<V, Double> vertex_priors, double alpha, double hitsCoeff) {
		super(g, vertex_priors, alpha);
		this.hitsCoeff = hitsCoeff;
		this.topicAlpha = 0.0;
		this.cateAlpha = 0.0;
		this.edge_weights = new UniformDegreeWeight<V, E>(graph);
		pr_disappearing_potential = 0.0;
		// not necessary for hits-hub
		// disappearing_potential = new HITS.Scores(0, 0);
	}

	/**
	 * Creates an instance with the specified graph, edge weights, vertex
	 * priors, and 'random jump' probability (alpha).
	 * 
	 * @param graph
	 *            the input graph
	 * @param edge_weights
	 *            the edge weights, denoting transition probabilities from
	 *            source to destination
	 * @param vertex_priors
	 *            the prior probabilities for each vertex
	 * @param alpha
	 *            the probability of executing a 'random jump' at each step
	 */
	public PageRankAndHitsHubWithPriors(Hypergraph<V, E> g,
			Transformer<E, ? extends Number> edge_weights,
			Transformer<V, Double> vertex_priors, double alpha, double hitsCoeff) {
		super(g, edge_weights, vertex_priors, alpha);
		this.hitsCoeff = hitsCoeff;
		this.topicAlpha = 0.0;
		this.cateAlpha = 0.0;
		pr_disappearing_potential = 0.0;
		// not necessary for hits-hub
		// disappearing_potential = new HITS.Scores(0, 0);
	}

	@Override
	public void evaluate() {
		do {
			max_delta = Double.MIN_VALUE;
			step();
			normalizeOutputValue();
		} while (!done());
	}

	protected void normalizeOutputValue() {
		double newSumWeight = 0.0;
		for (V v : graph.getVertices()) {
			newSumWeight += getOutputValue(v) * getOutputValue(v);
		}
		newSumWeight = Math.sqrt(newSumWeight);
		for (V v : graph.getVertices()) {
			setOutputValue(v, getOutputValue(v) / newSumWeight);
			updateMaxDelta(v, Math.abs(getCurrentValue(v) - getOutputValue(v)));
		}
		System.out.println(total_iterations + ":\t" + newSumWeight + ":\t"
				+ max_delta);
	}

	@Override
	public void step() {
		swapOutputForCurrent();
		for (V v : graph.getVertices()) {
			update(v);
		}
		total_iterations++;
		afterStep();
	}

	/**
	 * Updates the value for this vertex. Called by <code>step()</code>.
	 */
	@Override
	protected double update(V v) {
		collectDisappearingPotential(v);

		int incident_count = 0;
		double v_input = 0;
		double v_auth = 0;
		double v_hub = 0;
		if (v instanceof TopicCategoryVertex) {
			for (E e : graph.getInEdges(v)) {
				incident_count = getAdjustedIncidentCount(e);
				for (V w : graph.getIncidentVertices(e)) {
					if (!w.equals(v) || hyperedges_are_self_loops)
						v_auth += (getCurrentValue(w)
								* getEdgeWeight(w, e).doubleValue() / incident_count);
				}
			}
			if (alpha > 0) {
				v_auth = hitsCoeff * v_auth * (1 - alpha) + getVertexPrior(v)
						* alpha;
			}
			setOutputValue(v, v_auth);
			return Math.abs(getCurrentValue(v) - v_auth);
		} else {
			for (E e : graph.getInEdges(v)) {
				// For hypergraphs, this divides the potential coming from w
				// by the number of vertices in the connecting edge e.
				incident_count = getAdjustedIncidentCount(e);
				for (V w : graph.getIncidentVertices(e)) {
					if (!w.equals(v) || hyperedges_are_self_loops)
						v_input += (getCurrentValue(w)
								* getEdgeWeight(w, e).doubleValue() / incident_count);
				}
			}
			for (E e : graph.getOutEdges(v)) {
				incident_count = getAdjustedIncidentCount(e);
				for (V w : graph.getIncidentVertices(e)) {
					if (!w.equals(v) || hyperedges_are_self_loops)
						if (w instanceof TopicCategoryVertex)
							v_hub += (getCurrentValue(w)
									* getEdgeWeight(w, e).doubleValue() / incident_count);
				}
			}
			double new_value = hitsCoeff * v_hub +  (1 - hitsCoeff) * v_input;
			if (alpha > 0) {
				new_value = new_value * alpha + getVertexPrior(v) * (1 - alpha);
			}
			setOutputValue(v, new_value);
			return Math.abs(getCurrentValue(v) - new_value);
		}
	}

	/**
	 * Cleans up after each step. In this case that involves allocating the
	 * disappearing potential (thus maintaining normalization of the scores)
	 * according to the vertex probability priors, and then calling
	 * <code>super.afterStep</code>.
	 */
	@Override
	protected void afterStep() {
		if (pr_disappearing_potential > 0) {
			for (V v : graph.getVertices()) {
				if (v instanceof TopicVertex) {
					setOutputValue(v, getOutputValue(v) + (1 - alpha)
							* (pr_disappearing_potential * getVertexPrior(v)));
				}
			}
			pr_disappearing_potential = 0;
		}
		// not necessary for hits-hub
		// if (disappearing_potential.hub > 0) {
		// for (V v : graph.getVertices()) {
		// if (v instanceof TopicVertex) {
		// double new_hub = getOutputValue(v)
		// + (1 - alpha)
		// * (hitsCoeff * disappearing_potential.hub * getVertexPrior(v));
		// setOutputValue(v, new_hub);
		// } else if (v instanceof TopicCategoryVertex) {
		// // auth and hub of topic category
		// }
		// }
		// disappearing_potential.hub = 0;
		// }
		// if (disappearing_potential.authority > 0) {
		// for (V v : graph.getVertices()) {
		// if (v instanceof TopicCategoryVertex) {
		// double new_auth = getOutputValue(v)
		// + (1 - alpha)
		// * (hitsCoeff * disappearing_potential.authority * getVertexPrior(v));
		// setOutputValue(v, new_auth);
		// }
		// }
		// disappearing_potential.authority = 0;
		// }

		super.afterStep();
	}

	@Override
	public boolean done() {
		// TODO
		return total_iterations >= 200 || max_delta < 0.001;
	}

	/**
	 * Collects the "disappearing potential" associated with vertices that have
	 * no outgoing edges. Vertices that have no outgoing edges do not directly
	 * contribute to the scores of other vertices. These values are collected at
	 * each step and then distributed across all vertices as a part of the
	 * normalization process.
	 */
	@Override
	protected void collectDisappearingPotential(V v) {
		if (graph.outDegree(v) == 0) {
			if (isDisconnectedGraphOK()) {
				if (v instanceof TopicVertex) {
					pr_disappearing_potential += getCurrentValue(v);
				} else {
					// not necessary for hits-hub
					// disappearing_potential.hub += getCurrentValue(v);
				}
			} else {
				throw new IllegalArgumentException("Outdegree of " + v
						+ " must be > 0");
			}
		}
		// not necessary for hits-hub
		// if (graph.inDegree(v) == 0) {
		// if (isDisconnectedGraphOK()) {
		// if (!(v instanceof TopicReferenceVertex)) {
		// disappearing_potential.authority += getCurrentValue(v);
		// }
		// } else {
		// throw new IllegalArgumentException("Indegree of " + v
		// + " must be > 0");
		// }
		// }
	}

}
