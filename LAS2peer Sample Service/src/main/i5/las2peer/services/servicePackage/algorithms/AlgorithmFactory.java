package i5.las2peer.services.servicePackage.algorithms;

import i5.las2peer.services.servicePackage.algorithms.SpeakerListenerLabelPropagationHelpers.ListenerRuleCommand;
import i5.las2peer.services.servicePackage.algorithms.SpeakerListenerLabelPropagationHelpers.SpeakerRuleCommand;

/**
 *	A singleton factory for producing concrete
 * instances of the algorithms.
 *
 * @author Sebastian
 *
 */
public class AlgorithmFactory {
	
	/*
	 * The unique factory instance
	 */
    static private AlgorithmFactory algorithmFactory;
 
    private AlgorithmFactory() {
    }    
 
    /**
     * The getter for the unique factory instance.
     */
    public static AlgorithmFactory getAlgorithmFactory() {
		if (algorithmFactory == null) {
			if (algorithmFactory == null) {
				algorithmFactory = new AlgorithmFactory();
		    }
		}
		return algorithmFactory;
    }
    
    /**
     * Creates an instance of the Speaker Listener Label Propagation Algorithm
     */
    public SpeakerListenerLabelPropagationAlgorithm getSpeakerListenerLabelPropagationAlgorithm(int memorySize, double proabilityThreshold, SpeakerRuleCommand speakerRule, ListenerRuleCommand listenerRule) {
    	return new SpeakerListenerLabelPropagationAlgorithm(memorySize, proabilityThreshold, speakerRule, listenerRule);
    }
}