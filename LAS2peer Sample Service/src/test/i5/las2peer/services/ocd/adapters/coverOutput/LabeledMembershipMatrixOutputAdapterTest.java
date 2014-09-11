package i5.las2peer.services.ocd.adapters.coverOutput;

import i5.las2peer.services.ocd.adapters.AdapterException;
import i5.las2peer.services.ocd.algorithms.OcdAlgorithm;
import i5.las2peer.services.ocd.algorithms.SpeakerListenerLabelPropagationAlgorithm;
import i5.las2peer.services.ocd.algorithms.SskAlgorithm;
import i5.las2peer.services.ocd.algorithms.utils.OcdAlgorithmException;
import i5.las2peer.services.ocd.graphs.Cover;
import i5.las2peer.services.ocd.graphs.CustomGraph;
import i5.las2peer.services.ocd.testsUtil.OcdTestConstants;
import i5.las2peer.services.ocd.testsUtil.OcdTestGraphFactory;

import java.io.FileWriter;
import java.io.IOException;

import org.junit.Test;

public class LabeledMembershipMatrixOutputAdapterTest {

	@Test
	public void test() throws OcdAlgorithmException, IOException, AdapterException, InterruptedException {
		CustomGraph graph = OcdTestGraphFactory.getSawmillGraph();
		OcdAlgorithm algo = new SskAlgorithm();
		Cover cover = algo.detectOverlappingCommunities(graph);
		System.out.println(cover.toString());
		CoverOutputAdapter adapter = new LabeledMembershipMatrixCoverOutputAdapter(new FileWriter(OcdTestConstants.sawmillLabeledMembershipMatrixOutputPath));
		adapter.writeCover(cover);
	}
	
	@Test
	public void testOnDolphin() throws OcdAlgorithmException, IOException, AdapterException, InterruptedException {
		CustomGraph graph = OcdTestGraphFactory.getDolphinsGraph();
		OcdAlgorithm algo = new SpeakerListenerLabelPropagationAlgorithm();
		Cover cover = algo.detectOverlappingCommunities(graph);
		System.out.println(cover.toString());
		CoverOutputAdapter adapter = new LabeledMembershipMatrixCoverOutputAdapter(new FileWriter(OcdTestConstants.sawmillLabeledMembershipMatrixOutputPath + "Dolphinswhat"));
		adapter.writeCover(cover);
	}

}
