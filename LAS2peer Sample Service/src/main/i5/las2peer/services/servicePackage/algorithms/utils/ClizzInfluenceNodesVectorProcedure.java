package i5.las2peer.services.servicePackage.algorithms.utils;

import java.util.HashSet;
import java.util.Set;

import org.la4j.vector.functor.VectorProcedure;

public class ClizzInfluenceNodesVectorProcedure implements VectorProcedure {

	private Set<Integer> influencingNodeIndices = new HashSet<Integer>();
	
	/**
	 * Returns for a node the indices of the nodes influencing it. 
	 * @return The indices of the influencing nodes.
	 * @precondition The vector procedure was run on the node's
	 * distance vector (only).
	 */
	public Set<Integer> getInfluencingNodeIndices() {
		return influencingNodeIndices;
	}

	@Override
	public void apply(int i, double value) {
		if(value > 0) {
			influencingNodeIndices.add(i);
		}
	}

}