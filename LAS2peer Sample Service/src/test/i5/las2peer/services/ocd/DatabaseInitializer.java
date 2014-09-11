package i5.las2peer.services.ocd;

import i5.las2peer.services.ocd.adapters.AdapterException;
import i5.las2peer.services.ocd.algorithms.BinarySearchRandomWalkLabelPropagationAlgorithm;
import i5.las2peer.services.ocd.algorithms.OcdAlgorithm;
import i5.las2peer.services.ocd.algorithms.OcdAlgorithmExecutor;
import i5.las2peer.services.ocd.algorithms.SpeakerListenerLabelPropagationAlgorithm;
import i5.las2peer.services.ocd.algorithms.utils.OcdAlgorithmException;
import i5.las2peer.services.ocd.graphs.Cover;
import i5.las2peer.services.ocd.graphs.CoverId;
import i5.las2peer.services.ocd.graphs.CustomGraph;
import i5.las2peer.services.ocd.graphs.CustomGraphId;
import i5.las2peer.services.ocd.metrics.ExtendedModularity;
import i5.las2peer.services.ocd.metrics.MetricException;
import i5.las2peer.services.ocd.metrics.OcdMetricExecutor;
import i5.las2peer.services.ocd.metrics.StatisticalMeasure;
import i5.las2peer.services.ocd.testsUtil.OcdTestGraphFactory;
import i5.las2peer.services.ocd.utils.RequestHandler;

import java.io.FileNotFoundException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;

/**
 * This class is designed for database initialization through jUnit tests.
 * All tests will be executed when running the 'database' target of the ant build file.
 */
public class DatabaseInitializer {

	private RequestHandler requestHandler = new RequestHandler();
	
	public DatabaseInitializer() {
		RequestHandler.setPersistenceUnit("test");
	}
	
	private final String username = "User";
	
	public CustomGraphId createGraph(CustomGraph graph) throws AdapterException, FileNotFoundException, ParserConfigurationException {
		graph.setUserName(username);
		EntityManager em = requestHandler.getEntityManager();
		EntityTransaction tx = em.getTransaction();
		try {
			tx.begin();
			em.persist(graph);
			tx.commit();
		} catch( RuntimeException e ) {
			if( tx != null && tx.isActive() ) {
				tx.rollback();
			}
			throw e;
		}
		em.close();
		return new CustomGraphId(graph.getId(), username);
	}
	
	public CoverId createRealWorldCover(OcdAlgorithm algorithm, CustomGraphId gId, String name, List<StatisticalMeasure> statMetrics) throws OcdAlgorithmException, InterruptedException, MetricException {
		EntityManager em = requestHandler.getEntityManager();
		EntityTransaction tx = em.getTransaction();
		CustomGraph graph;
		Cover cover;
		try {
			tx.begin();
			graph = em.find(CustomGraph.class, gId);
			OcdAlgorithmExecutor algoExecutor = new OcdAlgorithmExecutor();
			cover = algoExecutor.execute(graph, algorithm, 0);
			OcdMetricExecutor metricExecutor = new OcdMetricExecutor();
			for(StatisticalMeasure metric : statMetrics) {
				metricExecutor.executeStatisticalMeasure(cover, metric);
			}
			cover.setName(name);
			em.persist(cover);
			tx.commit();
		} catch( RuntimeException e ) {
			if( tx != null && tx.isActive() ) {
				tx.rollback();
			}
			throw e;
		}
		em.close();
		return new CoverId(cover.getId(), gId);
	}
	
	@Test
	public void initDatabase() throws AdapterException, FileNotFoundException, ParserConfigurationException, OcdAlgorithmException, MetricException, InterruptedException {
		CustomGraph graph = OcdTestGraphFactory.getAperiodicTwoCommunitiesGraph();
		CustomGraphId aperiodicId = createGraph(graph);
		graph = OcdTestGraphFactory.getSawmillGraph();
		CustomGraphId sawmillId = createGraph(graph);
		graph = OcdTestGraphFactory.getDolphinsGraph();
		CustomGraphId dolphinsId = createGraph(graph);
		graph = OcdTestGraphFactory.getDirectedAperiodicTwoCommunitiesGraph();
		CustomGraphId directedAperiodicId = createGraph(graph);
		DecimalFormat df = new DecimalFormat("00");
		for(int i=0; i<10; i++) {
			graph = OcdTestGraphFactory.getMiniServiceTestGraph();
			graph.setName(graph.getName() + " " + df.format(i));
			createGraph(graph);
		}
		List<StatisticalMeasure> statMetrics = new ArrayList<StatisticalMeasure>();
		statMetrics.add(new ExtendedModularity());
		CoverId slpaAperiodicId = createRealWorldCover(new SpeakerListenerLabelPropagationAlgorithm(), aperiodicId, "SLPA on ATC", statMetrics);
		CoverId slpaSawmill = createRealWorldCover(new SpeakerListenerLabelPropagationAlgorithm(), sawmillId, "SLPA on Sawmill", new ArrayList<StatisticalMeasure>());
		CoverId rawlpaSawmill = createRealWorldCover(new BinarySearchRandomWalkLabelPropagationAlgorithm(), sawmillId, "RAW LPA on Sawmill", statMetrics);
	}

}