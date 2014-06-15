package i5.las2peer.services.servicePackage.algorithms;

import i5.las2peer.services.servicePackage.graph.Cover;
import i5.las2peer.services.servicePackage.graph.CustomGraph;
import i5.las2peer.services.servicePackage.graph.GraphType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.la4j.matrix.Matrix;
import org.la4j.matrix.dense.Basic2DMatrix;
import org.la4j.matrix.sparse.CCSMatrix;
import org.la4j.vector.Vector;
import org.la4j.vector.dense.BasicVector;

import y.base.Edge;
import y.base.EdgeCursor;
import y.base.Node;
import y.base.NodeCursor;

/**
 * Implements the Random Walk Label Propagation Algorithm
 */
public class RandomWalkLabelPropagationAlgorithm implements
		AbstractOverlappingCommunityDetectionAlgorithm {

	private static final HashSet<GraphType> compatibilities = new HashSet<GraphType>();
	static {
		compatibilities.add(GraphType.WEIGHTED);
		compatibilities.add(GraphType.DIRECTED);
	}
	
	// TODO delete iteration bound
	private int RANDOM_WALK_ITERATION_BOUND = 100000;
	private double profitabilityDelta;
	
	/**
	 * Creates an instance of the algorithm
	 * @param profitabilityDelta The profitability step size for the label propagation phase.
	 */
	protected RandomWalkLabelPropagationAlgorithm(double profitabilityDelta) {
		this.profitabilityDelta = profitabilityDelta;
	}
	
	@Override
	public Set<GraphType> getCompatibleGraphTypes() {
		return compatibilities;
	}

	@Override
	public Cover detectOverlappingCommunities(CustomGraph graph) {
		List<Node> leaders = randomWalkPhase(graph);
		///////////////////////////////////////TEST
		// TODO
		System.out.println("Leaders:");
		System.out.println(leaders);
		/////////////////
		return labelPropagationPhase(graph, leaders);
	}

	/*
	 *  Executes the random walk phase of the algorithm and returns global leaders.
	 */
	protected List<Node> randomWalkPhase(CustomGraph graph) {
		// TODO delete console outputs
		System.out.println("Random Walk Phase began");
		Matrix disassortativityMatrix = getTransposedDisassortativityMatrix(graph);
		System.out.println("Transition Matrix Calculated.");
		System.out.println(disassortativityMatrix);
		Vector disassortativityVector = executeRandomWalk(disassortativityMatrix);
		System.out.println("Random Walk Completed. Vector:");
		System.out.println(disassortativityVector);
		Vector leadershipVector = getLeadershipValues(graph, disassortativityVector);
		System.out.println("Leadership Values Calculated. Vector:");
		System.out.println(leadershipVector);
		Map<Node, Double> followerMap = getFollowerDegrees(graph, leadershipVector);
		System.out.println("Followers Calculated");
		return getGlobalLeaders(followerMap);
	}
	
	/*
	 *  Returns the transposed normalized disassortativity matrix for the random walk phase.
	 */
	protected Matrix getTransposedDisassortativityMatrix(CustomGraph graph) {
		/*
		 * Calculates transposed disassortativity matrix in a special sparse matrix format.
		 */
		Matrix disassortativities = new CCSMatrix(graph.nodeCount(), graph.nodeCount());
		EdgeCursor edges = graph.edges();
		double[] columnSums = new double[graph.nodeCount()];
		while(edges.ok()) {
			Edge edge = edges.edge();
			double disassortativity = Math.abs(graph.getWeightedInDegree(edge.target())
					- graph.getWeightedInDegree(edge.source()));
			disassortativities.set(edge.target().index(), edge.source().index(), disassortativity);
			columnSums[edge.source().index()] += disassortativity;
			edges.next();
		}
		/*
		 * Column normalizes transposed disassortativity matrix.
		 */
		edges.toFirst();
		while(edges.ok()) {
			Edge edge = edges.edge();
			if(columnSums[edge.source().index()] > 0) {
				double normDisassortativity = disassortativities.get(edge.target().index(), edge.source().index());
				normDisassortativity /= columnSums[edge.source().index()];
				disassortativities.set(edge.target().index(), edge.source().index(), normDisassortativity);
			}
			edges.next();
		}
		return disassortativities;
	}
	
	/*
	 * Executes a random walk on the disassortativity matrix
	 * and returns the resulting disassortativity vector.
	 * The vector is initialized with a uniform distribution.
	 */
	protected Vector executeRandomWalk(Matrix disassortativityMatrix) {
		Vector vec1 = new BasicVector(disassortativityMatrix.columns());
		for(int i=0; i<vec1.length(); i++) {
			vec1.set(i, 1.0 / vec1.length());
		}
		Vector vec2 = new BasicVector();
		for(int i=0; !vec1.equals(vec2) && i < RANDOM_WALK_ITERATION_BOUND; i++) {
			vec2 = new BasicVector(vec1);
			vec1 = disassortativityMatrix.multiply(vec1);
			//////////////////////////////////////////// TEST
			// TODO
			System.out.println("vec1 updated: ");
			///////////////////////
		}
		return vec1;
	}
	
	/*
	 * Returns the leadership vector.
	 */
	protected Vector getLeadershipValues(CustomGraph graph, Vector disassortativityVector) {
		Vector leadershipVector = new BasicVector(graph.nodeCount());
		NodeCursor nodes = graph.nodes();
		while(nodes.ok()) {
			Node node = nodes.node();
			double leadershipValue = graph.getWeightedInDegree(node) * disassortativityVector.get(node.index());
			///////////////////////////////////////////////////// TEST
			// TODO
			System.out.println("Node: " + node.index());
			System.out.println("Name: " + graph.getNodeName(node));
			System.out.println("Disassortativity: " + disassortativityVector.get(node.index()));
			System.out.println("Weigted Deg: " + graph.getWeightedInDegree(node));
			System.out.println("Leadership Val: " + leadershipValue);
			System.out.println();
			/////////////////////////////////////
			leadershipVector.set(node.index(), leadershipValue);
			nodes.next();
		}
		return leadershipVector;
	}
	
	/*
	 * Returns the follower degree of each node.
	 */
	protected Map<Node, Double> getFollowerDegrees(CustomGraph graph, Vector leadershipVector) {
		Map<Node, Double> followerMap = new HashMap<Node, Double>();
		NodeCursor nodes = graph.nodes();
		/*
		 * Iterates over all nodes to detect their local leader
		 */
		while(nodes.ok()) {
			Node node = nodes.node();
			NodeCursor successors = node.successors();
			double maxInfluence = Double.NEGATIVE_INFINITY;
			List<Node> leaders = new ArrayList<Node>();
			/*
			 * Checks all successors for possible leader
			 */
			while(successors.ok()) {
				Node successor = successors.node();
				Edge successorEdge = node.getEdgeTo(successor);
				double successorInfluence =
						leadershipVector.get(successor.index()) * graph.getEdgeWeight(successorEdge);
				if(successorInfluence >= maxInfluence) {
					Edge nodeEdge = node.getEdgeFrom(successor);
					/*
					 * Ensures the node itself is not a leader of the successor
					 */
					if(nodeEdge == null || successorInfluence
							> leadershipVector.get(node.index()) * graph.getEdgeWeight(nodeEdge)) {
						if(successorInfluence > maxInfluence) {
							/*
							 * Other nodes have lower influence
							 */
							leaders.clear();
						}
						leaders.add(successor);
						maxInfluence = successorInfluence;
					}
				}
				successors.next();
			}
			if(!leaders.isEmpty()) {
				double followerDegree = 0;
				for(Node leader : leaders) {
					if(followerMap.containsKey(leader)) {
						followerDegree = followerMap.get(leader);
					}
					followerMap.put(leader, followerDegree += maxInfluence / leaders.size());
				}
			}
			nodes.next();
		}
		return followerMap;
	}
	
	/*
	 * Returns a list of global leaders
	 */
	protected List<Node> getGlobalLeaders(Map<Node, Double> followerMap) {
		double averageFollowerDegree = 0;
		for(Double followerDegree : followerMap.values()) {
			averageFollowerDegree += followerDegree;
		}
		averageFollowerDegree /= followerMap.size();
		List<Node> globalLeaders = new ArrayList<Node>();
		for(Map.Entry<Node, Double> entry : followerMap.entrySet()) {
			if(entry.getValue() >= averageFollowerDegree) {
				globalLeaders.add(entry.getKey());
			}
		}
		return globalLeaders;
	}
	
	/*
	 * Executes the label propagation phase
	 */
	protected Cover labelPropagationPhase(CustomGraph graph, List<Node> leaders) {
		/*
		 * Executes the label propagation until all nodes are assigned to at least one community
		 */
		int iterationCount=0;
		Map<Node, Map<Node, Integer>> communities = new HashMap<Node, Map<Node, Integer>>();
		do{
			communities.clear();
			iterationCount++;
			for(Node leader : leaders) {
				Map<Node, Integer> communityMemberships 
						= executeLabelPropagation(graph, leader, 1-iterationCount*profitabilityDelta);
				communities.put(leader, communityMemberships);
			}
		} while(1-iterationCount*profitabilityDelta > 0 && !areAllNodesAssigned(graph, communities));
		return getMembershipDegrees(graph, communities);
	}
	
	/*
	 * Executes the label propagation for a single leader
	 */
	protected Map<Node, Integer> executeLabelPropagation(CustomGraph graph, Node leader, double profitabilityThreshold) {
		Map<Node, Integer> memberships = new HashMap<Node, Integer>();
		int previousMemberCount;
		int iterationCount = 0;
		/*
		 * Iterates as long as new members assume the behavior.
		 */
		do {
			iterationCount++;
			previousMemberCount = memberships.size();
			Set<Node> predecessors = getBehaviorPredecessors(graph, memberships, leader);
			Iterator<Node> nodeIt = predecessors.iterator();
			/*
			 * Checks for each predecessor of the leader behavior nodes whether it assumes the new behavior.
			 */
			while(nodeIt.hasNext()) {
				Node node = nodeIt.next();
				double profitability = 0;
				NodeCursor nodeSuccessors = node.successors();
				while(nodeSuccessors.ok()) {
					Node nodeSuccessor = nodeSuccessors.node();
					if(nodeSuccessor.equals(leader) || memberships.containsKey(nodeSuccessor)) {
						profitability++;
					}
					nodeSuccessors.next();
				}
				if(profitability / nodeSuccessors.size() > profitabilityThreshold) {
					memberships.put(node, iterationCount);
				}
			}

		} while (memberships.size() > previousMemberCount);
		return memberships;
	}
	
	/*
	 * Returns all predecessors of the nodes which adopted the leader's behavior (and the leader itself)
	 * for the label propagation. Any key node of the membership map is regarded as a node which adopted
	 * leader behavior.
	 */
	protected Set<Node> getBehaviorPredecessors(CustomGraph graph, Map<Node, Integer> memberships, Node leader) {
		Set<Node> neighbors = new HashSet<Node>();
		NodeCursor leaderPredecessors = leader.predecessors();		
		while(leaderPredecessors.ok()) {
			Node leaderPredecessor = leaderPredecessors.node();
			if(!memberships.containsKey(leaderPredecessor)) {
				neighbors.add(leaderPredecessor);
			}
			leaderPredecessors.next();
		}
		for(Node member : memberships.keySet()) {
			NodeCursor memberPredecessors = member.predecessors();
			while(memberPredecessors.ok()) {
				Node memberPredecessor = memberPredecessors.node();
				if(!memberPredecessor.equals(leader) && !memberships.containsKey(memberPredecessor)) {
					neighbors.add(memberPredecessor);
				}
				memberPredecessors.next();
			}
		}
		return neighbors;
	}

	/*
	 * Returns TRUE when each node has been assigned to at least one community
	 * in the label propagation phase, and FALSE otherwise.
	 */
	protected boolean areAllNodesAssigned(CustomGraph graph, Map<Node, Map<Node, Integer>>communities) {
		boolean allNodesAreAssigned = true;
		NodeCursor nodes = graph.nodes();
		while(nodes.ok()) {
			boolean nodeIsAssigned = false;
			Node node = nodes.node();
			for(Map.Entry<Node, Map<Node, Integer>> entry : communities.entrySet()) {
				if(entry.getValue().containsKey(node)) {
					nodeIsAssigned = true;
					break;
				}
			}
			if(!nodeIsAssigned) {
				allNodesAreAssigned = false;
				break;
			}
			nodes.next();
		}
		return allNodesAreAssigned;
	}
	
	/*
	 * Returns a cover containing the membership degrees of all nodes, calculated from the iteration
	 * counts in the communities map.
	 */
	protected Cover getMembershipDegrees(CustomGraph graph, Map<Node, Map<Node, Integer>> communities) {
		Matrix membershipMatrix = new Basic2DMatrix(graph.nodeCount(), communities.size());
		int communityIndex = 0;
		for(Node leader : communities.keySet()) {
			membershipMatrix.set(leader.index(), communityIndex, 1.0);
			for(Map.Entry<Node, Integer> entry : communities.get(leader).entrySet()) {
				double membershipDegree = 1.0 / Math.pow(entry.getValue(), 2);
				membershipMatrix.set(entry.getKey().index(), communityIndex, membershipDegree);
			}
			communityIndex++;
		}
		Cover cover = new Cover(graph, membershipMatrix);
		return cover;
	}
}