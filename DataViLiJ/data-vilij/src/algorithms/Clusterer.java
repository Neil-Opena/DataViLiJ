package algorithms;

/**
 * An abstract class for classification algorithms. The output for these
 * algorithms is a straight line, as described in Appendix C of the software
 * requirements specification (SRS). The {@link #output} is defined with
 * extensibility in mind.
 *
 * @author Ritwik Banerjee
 */
/**
 * @author Ritwik Banerjee
 */
public abstract class Clusterer implements Algorithm {

	protected final int numberOfClusters;

	public int getNumberOfClusters() {
		return numberOfClusters;
	}

	public Clusterer(int k) {
		if (k < 2) {
			k = 2;
		} else if (k > 4) {
			k = 4;
		}
		numberOfClusters = k;
	}
}
