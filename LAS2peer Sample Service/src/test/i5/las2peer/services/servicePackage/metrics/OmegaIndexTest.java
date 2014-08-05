package i5.las2peer.services.servicePackage.metrics;

import static org.junit.Assert.assertEquals;
import i5.las2peer.services.servicePackage.algorithms.OcdAlgorithm;
import i5.las2peer.services.servicePackage.algorithms.RandomWalkLabelPropagationAlgorithm;
import i5.las2peer.services.servicePackage.algorithms.utils.OcdAlgorithmException;
import i5.las2peer.services.servicePackage.graph.Cover;
import i5.las2peer.services.servicePackage.graph.CustomGraph;
import i5.las2peer.services.servicePackage.testsUtil.OcdTestCoverFactory;
import i5.las2peer.services.servicePackage.testsUtil.OcdTestGraphFactory;

import org.junit.Test;
import org.la4j.matrix.Matrix;
import org.la4j.matrix.sparse.CCSMatrix;

public class OmegaIndexTest {

	/*
	 * Tests SLPA result on sawmill.
	 */
	@Test
	public void testOnSawmillRawLpa() throws Exception {
		CustomGraph graph = OcdTestGraphFactory.getSawmillGraph();
		OcdAlgorithm algo = new RandomWalkLabelPropagationAlgorithm();
		Cover cover = algo.detectOverlappingCommunities(graph);
		cover.filterMembershipsbyThreshold(0.15);
		KnowledgeDrivenMeasure metric = new OmegaIndex();
		Cover groundTruth = OcdTestCoverFactory.getSawmillGroundTruth();
		OcdMetricExecutor executor = new OcdMetricExecutor();
		executor.executeKnowledgeDrivenMeasure(cover, groundTruth, metric);
		System.out.println("Sawmill SLPA");
		System.out.println(cover);
	}
	
	/*
	 * Tests ground truth as output cover on sawmill.
	 */
	@Test
	public void testOnSawmillGroundTruth() throws Exception {
		KnowledgeDrivenMeasure metric = new OmegaIndex();
		Cover cover = OcdTestCoverFactory.getSawmillGroundTruth();
		metric.measure(cover, cover);
		System.out.println("Sawmill Ground Truth");
		System.out.println(cover);
		assertEquals(1.0, cover.getMetric(MetricType.OMEGA_INDEX).getValue(), 0.001);
	}

	/*
	 * Tests for two covers with a known result.
	 */
	@Test
	public void testWithKnownResult() throws OcdAlgorithmException, MetricException {
		CustomGraph graph = new CustomGraph();
		for(int i=0; i<11; i++) {
			graph.createNode();
		}
		Matrix memberships1 = new CCSMatrix(11, 2);
		memberships1.set(0, 0, 1);
		memberships1.set(1, 0, 0.8);
		memberships1.set(2, 0, 0.3);
		memberships1.set(3, 0, 1);
		memberships1.set(4, 0, 1);
		memberships1.set(6, 0, 0.05);
		memberships1.set(7, 0, 1);
		memberships1.set(9, 0, 0.5);
		memberships1.set(1, 1, 0.2);
		memberships1.set(2, 1, 0.7);
		memberships1.set(5, 1, 1);
		memberships1.set(6, 1, 0.95);
		memberships1.set(8, 1, 1);
		memberships1.set(9, 1, 0.5);
		memberships1.set(10, 1, 1);
		Cover cover = new Cover(graph, memberships1);
		Matrix memberships2 = new CCSMatrix(11, 2);
		memberships2.set(0, 0, 1);
		memberships2.set(1, 0, 1);
		memberships2.set(3, 0, 1);
		memberships2.set(4, 0, 0.6);
		memberships2.set(7, 0, 1);
		memberships2.set(9, 0, 0.45);
		memberships2.set(2, 1, 1);
		memberships2.set(4, 1, 1);
		memberships2.set(5, 1, 1);
		memberships2.set(6, 1, 1);
		memberships2.set(8, 1, 1);
		memberships2.set(9, 1, 1);
		memberships2.set(10, 1, 1);
		Cover groundTruth = new Cover(graph, memberships2);
		KnowledgeDrivenMeasure metric = new OmegaIndex();
		metric.measure(cover, groundTruth);
		assertEquals(0.313, cover.getMetric(MetricType.OMEGA_INDEX).getValue(), 0.01);
		System.out.println("Known Result");
		System.out.println(cover);
	}
	
}
