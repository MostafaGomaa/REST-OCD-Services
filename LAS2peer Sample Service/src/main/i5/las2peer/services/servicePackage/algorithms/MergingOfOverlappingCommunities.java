package i5.las2peer.services.servicePackage.algorithms;

import i5.las2peer.services.servicePackage.graph.Cover;
import i5.las2peer.services.servicePackage.graph.CustomGraph;
import i5.las2peer.services.servicePackage.graph.GraphType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.la4j.matrix.Matrix;
import org.la4j.matrix.sparse.CCSMatrix;

import y.base.Node;
import y.base.NodeCursor;

public class MergingOfOverlappingCommunities implements OcdAlgorithm {

	@Override
	public Map<String, String> getParameters() {
		return new HashMap<String, String>();
	}
	
	@Override
	public AlgorithmType getAlgorithmType() {
		return AlgorithmType.MERGING_OF_OVERLAPPING_COMMUNITIES_ALGORITHM;
	}
	
	@Override
	public Set<GraphType> compatibleGraphTypes() {
		Set<GraphType> compatibleTypes = new HashSet<GraphType>();
		compatibleTypes.add(GraphType.WEIGHTED);
		return compatibleTypes;
	}
	
	@Override
	public Cover detectOverlappingCommunities(CustomGraph graph) {
		Map<Node, Set<Node>> activeCommunities = new HashMap<Node, Set<Node>>();
		Map<Node, Set<Node>> unactiveCommunities = new HashMap<Node, Set<Node>>();
		Map<Node, Node> deactivatedBy = new HashMap<Node, Node>();
		Map<Node, Map<Node, Double>> inclusionAlphas = new HashMap<Node, Map<Node, Double>>();
		Map<Node, Double> alphaBounds = new HashMap<Node, Double>();
		Map<Node, Set<Node>> communityNeighbors = new HashMap<Node, Set<Node>>();
		Map<Node, Double> nodeDegrees = new HashMap<Node, Double>();
		Map<Node, Map<Node, Double>> internalNeighborDegrees = new HashMap<Node, Map<Node, Double>>();
		Map<Node, Double> communityDegrees = new HashMap<Node, Double>();
		Map<Node, Double> internalCommunityDegrees = new HashMap<Node, Double>();
		Set<Node> mainCommunities = new HashSet<Node>();
		Set<Node> inclusionNodes = new HashSet<Node>();
		init(graph, activeCommunities, inclusionAlphas, alphaBounds, communityNeighbors, nodeDegrees, internalNeighborDegrees,
				communityDegrees, internalCommunityDegrees);
		Set<Node> deactivatedCommunities = new HashSet<Node>();
		double maxAlpha;
		double alpha;
		Node communityId;
		int minCommunitySize;
		while(!activeCommunities.isEmpty()) {
			minCommunitySize = Integer.MAX_VALUE;
			for(Set<Node> community : activeCommunities.values()) {
				if(community.size() < minCommunitySize) {
					minCommunitySize = community.size();
				}
			}
			deactivatedCommunities.clear();
			for(Map.Entry<Node, Set<Node>> entry : activeCommunities.entrySet()) {
				if(entry.getValue().size() == minCommunitySize) {
					maxAlpha = 0;
					inclusionNodes.clear();
					communityId = entry.getKey();
					for(Node neighbor : communityNeighbors.get(communityId)) {
						alpha = calculateInclusionAlpha(internalCommunityDegrees.get(communityId), communityDegrees.get(communityId),
								internalNeighborDegrees.get(communityId).get(neighbor), nodeDegrees.get(neighbor));
						if(alpha >= maxAlpha) {
							if(alpha > maxAlpha) {
								maxAlpha = alpha;
								inclusionNodes.clear();
							}
							inclusionNodes.add(neighbor);
						}
					}
					if(!inclusionNodes.isEmpty()) {
						updateCommunity(graph, communityId, inclusionNodes, maxAlpha, activeCommunities, communityNeighbors, nodeDegrees, internalNeighborDegrees,
								communityDegrees, internalCommunityDegrees, inclusionAlphas, alphaBounds);
					}
					if(didDeactivate(graph, communityId, activeCommunities, unactiveCommunities, deactivatedBy)) {
						deactivatedCommunities.add(communityId);
						if(unactiveCommunities.size() > graph.nodeCount() - Math.log(graph.nodeCount())) {
							mainCommunities.add(communityId);
						}
					}
				}
			}
			for(Node deactivedId : deactivatedCommunities) {
				activeCommunities.remove(deactivedId);
			}
		}
		double resolutionAlpha = determineResolutionAlpha(graph, inclusionAlphas, deactivatedBy, mainCommunities);
		Matrix memberships = determineMembershipMatrix(graph, unactiveCommunities, deactivatedBy, inclusionAlphas, resolutionAlpha);
		return new Cover(graph, memberships);
	}
	
	private Matrix determineMembershipMatrix(CustomGraph graph, Map<Node, Set<Node>> unactiveCommunities, Map<Node, Node> deactivatedBy, Map<Node, Map<Node, Double>> inclusionAlphas, double resolutionAlpha) {
		Map<Node, Integer> originalCommunitySizes = new HashMap<Node, Integer>();
		for(Map.Entry<Node, Set<Node>> entry : unactiveCommunities.entrySet()) {
			originalCommunitySizes.put(entry.getKey(), entry.getValue().size());
			Iterator<Node> memberIt = entry.getValue().iterator();
			while(memberIt.hasNext()) {
				if(resolutionAlpha > inclusionAlphas.get(entry.getKey()).get(memberIt.next())) {
					memberIt.remove();
				}
			}
		}
		Iterator<Map.Entry<Node, Set<Node>>> entryIt = unactiveCommunities.entrySet().iterator();
		Map.Entry<Node, Set<Node>> entry;
		Node deactivatorId;
		while(entryIt.hasNext()) {
			entry = entryIt.next();
			deactivatorId = deactivatedBy.get(entry.getKey());
			if(deactivatorId != null && ( unactiveCommunities.get(deactivatorId) == null  ||
					unactiveCommunities.get(deactivatorId).size() >= originalCommunitySizes.get(entry.getKey()) ) ) {
				entryIt.remove();
			}
		}
		Matrix memberships = new CCSMatrix(graph.nodeCount(), unactiveCommunities.size());
		int communityIndex = 0;
		for(Set<Node> community : unactiveCommunities.values()) {
			for(Node member : community) {
				memberships.set(member.index(), communityIndex, 1);
			}
			communityIndex++;
		}
		return memberships;
	}
	
	private double determineResolutionAlpha(CustomGraph graph, Map<Node, Map<Node, Double>> inclusionAlphas,
			Map<Node, Node> deactivatedBy, Set<Node> mainCommunities) {	
		TreeSet<Double> alphaSequence = determineUnitAlphaSequence(inclusionAlphas);
		double resolutionAlpha = 3 * calculateSingleSequenceResolutionAlpha(alphaSequence);
		for(Node communityId : mainCommunities) {
			alphaSequence = determineCommunityAlphaSequence(graph, inclusionAlphas, deactivatedBy, communityId);
			resolutionAlpha += calculateSingleSequenceResolutionAlpha(alphaSequence) / mainCommunities.size();
		}
		double normalizationCoefficient = 3d;
		if(!mainCommunities.isEmpty()) {
			normalizationCoefficient++;
		}
		return resolutionAlpha / normalizationCoefficient;
	}
	
	private double calculateSingleSequenceResolutionAlpha(TreeSet<Double> alphaSequence) {
		double resolutionAlpha;
		if(alphaSequence.size() <= 2) {
			resolutionAlpha = alphaSequence.first();
		}
		else {
			double maxPlateauSize = 0;
			double plateauSize;
			double lastAlpha;
			double alpha;
			Iterator<Double> it = alphaSequence.descendingIterator();
			it.next();
			lastAlpha = it.next();
			resolutionAlpha = lastAlpha;
			while(it.hasNext()) {
				alpha = it.next();
				plateauSize = 1/alpha - 1/lastAlpha;
				if(plateauSize > maxPlateauSize) {
					maxPlateauSize = plateauSize;
					resolutionAlpha = lastAlpha;
				}
				lastAlpha = alpha;
			}
		}
		return resolutionAlpha;
	}
	
	private TreeSet<Double> determineUnitAlphaSequence(Map<Node, Map<Node, Double>> inclusionAlphas) {
		TreeSet<Double> alphaSequence = new TreeSet<Double>();
		for(Map<Node, Double> alphaMap : inclusionAlphas.values()) {
			for(Double alpha : alphaMap.values()) {
				alphaSequence.add(alpha);
			}
		}
		return alphaSequence;
	}
	
	private TreeSet<Double> determineCommunityAlphaSequence(CustomGraph graph, Map<Node, Map<Node, Double>> inclusionAlphas,
			Map<Node, Node> deactivatedBy, Node communityId) {
		TreeSet<Double> alphaSequence = new TreeSet<Double>();
		NodeCursor nodes = graph.nodes();
		Node node;
		Node currentCommunityId;
		while(nodes.ok()) {
			node = nodes.node();
			currentCommunityId = communityId;
			while(!inclusionAlphas.get(currentCommunityId).containsKey(node)) {
				currentCommunityId = deactivatedBy.get(currentCommunityId);
			}
			alphaSequence.add(inclusionAlphas.get(currentCommunityId).get(node));
			nodes.next();
		}
		return alphaSequence;
	}
	
	private boolean didDeactivate(CustomGraph graph, Node communityId, Map<Node, Set<Node>> activeCommunities,
			Map<Node, Set<Node>> unactiveCommunities, Map<Node, Node> deactivatedBy) {
		Set<Node> community = activeCommunities.get(communityId);
		Iterator<Map.Entry<Node, Set<Node>>> entryIt = activeCommunities.entrySet().iterator();
		Map.Entry<Node, Set<Node>> entry;
		Node deactivatorId = null;
		while(entryIt.hasNext()) {
			entry = entryIt.next();
			if(entry.getKey() != communityId) {
				if(!deactivatedBy.containsKey(entry.getKey()) && entry.getValue().equals(community)) {
					deactivatorId = entry.getKey();
					break;
				}
			}
		}
		if(deactivatorId != null || community.size() == graph.nodeCount()) {
			unactiveCommunities.put(communityId, community);
			if(deactivatorId != null) {
				deactivatedBy.put(communityId, deactivatorId);
			}
			return true;
		}
		else {
			return false;
		}
	}
	
	private double calculateInclusionAlpha(double internalCommunityDegree, double totalCommunityDegree, double internalNeighborDegree, double totalNeighborDegree) {
		double alpha = Math.log10(internalCommunityDegree + 2*internalNeighborDegree + 1) - Math.log10(internalCommunityDegree + 1);
		alpha /= Math.log10(totalCommunityDegree + totalNeighborDegree) - Math.log10(totalCommunityDegree);
		return alpha;
	}
	
	/*
	 * Updates all community parameters after new nodes were added a community.
	 * @param graph The graph being analyzed.
	 * @param communityId The id node of the community the new nodes were added to.
	 * @param inclusionNodes The nodes added to the community.
	 * @param inclusionAlpha The inclusion alpha of the new nodes.
	 * @param communities A mapping from each community index node to the community members.
	 * @param communityNeighbors A mapping from each community index node to the community neighbors.
	 * @param nodeDegrees A mapping from each node to its weighted degree.
	 * @param internalNeighborDegrees A mapping from each community index node to the weighted internal degree.
	 * @param communityDegrees A mapping from each community index node to the weighted total community degree.
	 * @param internalCommunityDegrees A mapping from each community index node to the weighted internal community degree.
	 * @param inclusionAlphas A mapping from each community index node to the community member inclusion alphas.
	 * @param alphaBounds A mapping from each community index node to the upper bound for inclusion alphas.
	 */
	private void updateCommunity(CustomGraph graph, Node communityId, Set<Node> inclusionNodes, double inclusionAlpha, Map<Node, Set<Node>> communities,
			Map<Node, Set<Node>> communityNeighbors, Map<Node, Double> nodeDegrees, Map<Node, Map<Node, Double>> internalNeighborDegrees,
			Map<Node, Double> communityDegrees, Map<Node, Double> internalCommunityDegrees, Map<Node, Map<Node, Double>> inclusionAlphas, Map<Node, Double> alphaBounds) {
		double internalCommunityDegree;
		double totalCommunityDegree;
		NodeCursor successors;
		Node neighbor;
		double internalNeighborDegree;
		if(inclusionAlpha > alphaBounds.get(communityId)) {
			inclusionAlpha = alphaBounds.get(communityId);
		}
		else {
			alphaBounds.put(communityId, inclusionAlpha);
		}
		for(Node inclusionNode : inclusionNodes) {
			/*
			 * Update of community values.
			 */
			communityNeighbors.get(communityId).remove(inclusionNode);
			internalCommunityDegree = internalCommunityDegrees.get(communityId);
			internalCommunityDegree += 2 * internalNeighborDegrees.get(communityId).get(inclusionNode);
			internalCommunityDegrees.put(communityId, internalCommunityDegree);
			internalNeighborDegrees.get(communityId).remove(inclusionNode);
			totalCommunityDegree = communityDegrees.get(communityId);
			totalCommunityDegree += nodeDegrees.get(inclusionNode);
			communityDegrees.put(communityId, totalCommunityDegree);
			inclusionAlphas.get(communityId).put(inclusionNode, inclusionAlpha);
			communities.get(communityId).add(inclusionNode);
			successors = inclusionNode.successors();
			/*
			 * Neighborhood update.
			 */
			while(successors.ok()) {
				neighbor = successors.node();
				if(communityNeighbors.get(communityId).contains(neighbor)) {
					internalNeighborDegree = internalNeighborDegrees.get(communityId).get(neighbor);
					internalNeighborDegree += graph.getEdgeWeight(inclusionNode.getEdgeTo(neighbor));
					internalNeighborDegrees.get(communityId).put(neighbor, internalNeighborDegree);
				}
				else if(!communities.get(communityId).contains(neighbor)) {
					communityNeighbors.get(communityId).add(neighbor);
					internalNeighborDegree = graph.getEdgeWeight(inclusionNode.getEdgeTo(neighbor));
					internalNeighborDegrees.get(communityId).put(neighbor, internalNeighborDegree);
				}
				successors.next();
			}	
		}
	}
	
	/*
	 * Initializes all parameters other than graph for the algorithm execution.
	 * @param graph The graph being analyzed.
	 * @param communities A mapping from each community index node to the community members.
	 * @param inclusionAlphas A mapping from each community index node to the community member inclusion alphas.
	 * @param alphaBounds A mapping from each community index node to the upper bound for inclusion alphas.
	 * @param communityNeighbors A mapping from each community index node to the community neighbors.
	 * @param weightedNodeDegrees A mapping from each node to its weighted node degree.
	 * @param internalWeightedNeighborDegrees A mapping from each community index node to the weighted internal degrees
	 * of the community neighbors.
	 * @param weightedCommunityDegrees A mapping from each community index node to the weighted total community degree.
	 * @param internalWeightedCommunityDegrees A mapping from each community index node to the weighted internal community degree.
	 */
	private void init(CustomGraph graph, Map<Node, Set<Node>> communities, Map<Node, Map<Node, Double>> inclusionAlphas,
			Map<Node, Double> alphaBounds, Map<Node, Set<Node>> communityNeighbors, Map<Node, Double> weightedNodeDegrees,
			Map<Node, Map<Node, Double>> internalWeightedNeighborDegrees, Map<Node, Double> weightedCommunityDegrees,
			Map<Node, Double> internalWeightedCommunityDegrees) {
		NodeCursor nodes = graph.nodes();
		NodeCursor successors;
		Node node;
		Node neighbor;
		double edgeWeight;
		Set<Node> neighbors;
		Map<Node, Double> internalWeightedCommunityNeighborDegrees;
		Set<Node> communityMembers;
		Map<Node, Double> communityAlphas;
		double weightedDegree;
		while(nodes.ok()) {
			node = nodes.node();
			weightedDegree = 0;
			successors = node.successors();
			neighbors = new HashSet<Node>();
			internalWeightedCommunityNeighborDegrees = new HashMap<Node, Double>();
			while(successors.ok()) {
				neighbor = successors.node();
				neighbors.add(neighbor);
				edgeWeight = graph.getEdgeWeight(node.getEdgeTo(neighbor));
				weightedDegree += edgeWeight;
				internalWeightedCommunityNeighborDegrees.put(neighbor, edgeWeight);
				successors.next();
			}
			weightedNodeDegrees.put(node, weightedDegree);
			weightedCommunityDegrees.put(node, weightedDegree);
			internalWeightedCommunityDegrees.put(node, 0d);
			communityNeighbors.put(node, neighbors);
			internalWeightedNeighborDegrees.put(node, internalWeightedCommunityNeighborDegrees);
			communityMembers = new HashSet<Node>();
			communityMembers.add(node);
			communities.put(node, communityMembers);
			communityAlphas = new HashMap<Node, Double>();
			communityAlphas.put(node, Double.POSITIVE_INFINITY);
			inclusionAlphas.put(node, communityAlphas);
			alphaBounds.put(node, Double.POSITIVE_INFINITY);
			nodes.next();
		}
	}
	
	
}
