package i5.las2peer.services.servicePackage.metrics;

import i5.las2peer.services.servicePackage.graph.Cover;
import i5.las2peer.services.servicePackage.graph.GraphType;

import java.util.Set;

/**
 * A common interface for statistical overlapping community detection measures.
 * @author Sebastian
 *
 */
public interface StatisticalMeasure {

	/**
	 * Calculates the metric value of a cover.
	 * @param cover The cover which is evaluated. It must also contain the corresponding graph.
	 * @return The metric value.
	 */
	public void measure(Cover cover) throws MetricException;
	
	/**
	 * Returns the graph types which are compatible for a metric.
	 * @return The compatible graph types.
	 */
	 public Set<GraphType> compatibleGraphTypes();
	
}
