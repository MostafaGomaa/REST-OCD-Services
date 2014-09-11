package i5.las2peer.services.ocd.metrics;

import i5.las2peer.services.ocd.graphs.Cover;
import i5.las2peer.services.ocd.graphs.CustomGraph;
import i5.las2peer.services.ocd.graphs.GraphProcessor;
import i5.las2peer.services.ocd.utils.ExecutionStatus;

public class OcdMetricExecutor {
	
	public void executeKnowledgeDrivenMeasure(Cover cover, Cover groundTruth, KnowledgeDrivenMeasure metric) throws MetricException {
		CustomGraph graph = cover.getGraph();
		CustomGraph graphCopy = new CustomGraph(graph);
		Cover coverCopy = new Cover(graphCopy, cover.getMemberships());
		Cover groundTruthCopy = new Cover(graphCopy, groundTruth.getMemberships());
		metric.measure(coverCopy, groundTruthCopy);
		MetricLog calculatedLog = coverCopy.getMetrics().get(0);
		MetricLog log = new MetricLog(calculatedLog.getType(), calculatedLog.getValue(), calculatedLog.getParameters(), cover);
		log.setStatus(ExecutionStatus.COMPLETED);
		cover.addMetric(log);
	}
	
	public void executeStatisticalMeasure(Cover cover, StatisticalMeasure metric) throws MetricException {
		GraphProcessor processor = new GraphProcessor();
		CustomGraph graph = cover.getGraph();
		CustomGraph graphCopy = new CustomGraph(graph);
		processor.makeCompatible(graphCopy, metric.compatibleGraphTypes());
		Cover coverCopy = new Cover(graphCopy, cover.getMemberships());
		metric.measure(coverCopy);
		MetricLog calculatedLog = coverCopy.getMetrics().get(0);
		MetricLog log = new MetricLog(calculatedLog.getType(), calculatedLog.getValue(), calculatedLog.getParameters(), cover);
		log.setStatus(ExecutionStatus.COMPLETED);
		cover.addMetric(log);
	}

}
