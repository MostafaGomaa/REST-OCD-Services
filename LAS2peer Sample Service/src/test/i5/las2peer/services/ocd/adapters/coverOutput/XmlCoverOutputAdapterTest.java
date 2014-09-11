package i5.las2peer.services.ocd.adapters.coverOutput;

import i5.las2peer.services.ocd.adapters.AdapterException;
import i5.las2peer.services.ocd.adapters.coverOutput.CoverOutputAdapter;
import i5.las2peer.services.ocd.adapters.coverOutput.DefaultXmlCoverOutputAdapter;
import i5.las2peer.services.ocd.algorithms.AlgorithmLog;
import i5.las2peer.services.ocd.algorithms.AlgorithmType;
import i5.las2peer.services.ocd.graphs.Cover;
import i5.las2peer.services.ocd.graphs.CustomGraph;
import i5.las2peer.services.ocd.graphs.GraphType;
import i5.las2peer.services.ocd.metrics.MetricLog;
import i5.las2peer.services.ocd.metrics.MetricType;
import i5.las2peer.services.ocd.testsUtil.OcdTestConstants;

import java.awt.Color;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import org.junit.Test;
import org.la4j.matrix.Matrix;
import org.la4j.matrix.sparse.CCSMatrix;

import y.base.Edge;
import y.base.Node;

public class XmlCoverOutputAdapterTest {

	private static final String userName = "coverPersistenceUser";
	private static final String graphName = "coverPersistenceGraph";
	private static final String coverName = "coverPersistenceCover";

	
	@Test
	public void test() throws AdapterException, IOException {
		CustomGraph graph = new CustomGraph();
		graph.setUserName(userName);
		graph.setName(graphName);
		Node nodeA = graph.createNode();
		Node nodeB = graph.createNode();
		Node nodeC = graph.createNode();
		graph.setNodeName(nodeA, "A");
		graph.setNodeName(nodeB, "B");
		graph.setNodeName(nodeC, "C");
		Edge edgeAB = graph.createEdge(nodeA, nodeB);
		graph.setEdgeWeight(edgeAB, 5);
		Edge edgeBC = graph.createEdge(nodeB, nodeC);
		graph.setEdgeWeight(edgeBC, 2.5);
		Matrix memberships = new CCSMatrix(3, 2);
		memberships.set(0, 0, 1);
		memberships.set(1, 0, 0.5);
		memberships.set(1, 1, 0.5);
		memberships.set(2, 1, 1);
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("param1", "val1");
		params.put("param2", "val2");
		AlgorithmLog algo = new AlgorithmLog(AlgorithmType.UNDEFINED, params, new HashSet<GraphType>());
		Cover cover = new Cover(graph, memberships);
		cover.setAlgorithm(algo);
		cover.setName(coverName);
		cover.setCommunityColor(1, Color.BLUE);
		cover.setCommunityName(1, "Community1");
		MetricLog metric = new MetricLog(MetricType.EXECUTION_TIME, 3.55, params, cover);
		cover.addMetric(metric);
		CoverOutputAdapter adapter = new DefaultXmlCoverOutputAdapter();
		adapter.setWriter(new FileWriter(OcdTestConstants.testXmlCoverOutputPath));
		adapter.writeCover(cover);
	}

}