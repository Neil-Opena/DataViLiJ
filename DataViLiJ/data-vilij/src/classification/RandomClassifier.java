package classification;

import algorithms.Classifier;
import data.DataSet;
import dataprocessors.AppData;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Ritwik Banerjee
 */
public class RandomClassifier extends Classifier {

	private static final Random RAND = new Random();

	@SuppressWarnings("FieldCanBeLocal")
	// this mock classifier doesn't actually use the data, but a real classifier will
	private DataSet dataset;
	private final Thread algorithm;
	private final AppData appData;

	private final int maxIterations;
	private final int updateInterval;

	// currently, this value does not change after instantiation
	private final AtomicBoolean tocontinue;
	private final boolean isContinuous; //value that does not change

	@Override
	public int getMaxIterations() {
		return maxIterations;
	}

	@Override
	public int getUpdateInterval() {
		return updateInterval;
	}

	@Override
	public final boolean tocontinue() {
		return tocontinue.get();
	}

	public RandomClassifier(DataSet dataset,
		int maxIterations,
		int updateInterval,
		boolean tocontinue, AppData appData) {
		this.dataset = dataset;
		this.maxIterations = maxIterations;
		this.updateInterval = updateInterval;
		algorithm = new Thread(this);
		algorithm.setName(this.getClass().toString());

		this.tocontinue = new AtomicBoolean(tocontinue);
		this.isContinuous = tocontinue;
		this.appData = appData;
	}

	@Override
	public void run() {
		// time for original chart to show
		try {
			Thread.sleep(750);
		} catch (InterruptedException ex) {
			return;
		}

		int i;
		for (i = 1; i <= maxIterations && !Thread.interrupted(); i++) {
			int xCoefficient = new Long(-1 * Math.round((2 * RAND.nextDouble() - 0) * 10)).intValue(); //change 0 to 1 for original implementation
			int yCoefficient = 10;
			int constant = RAND.nextInt(11);

			// this is the real output of the classifier
			output = Arrays.asList(xCoefficient, yCoefficient, constant);
			appData.showCurrentIteration(i);
			
			// everything below is just for internal viewing of how the output is changing
			// in the final project, such changes will be dynamically visible in the UI
			if (i % updateInterval == 0) {
				//System.out.printf("Iteration number %d: ", i); //
				//flush();
				appData.updateChart(i);
				if (!isContinuous) {
					appData.enableRun();
					tocontinue.set(false);
					while (!tocontinue()) { //wait until play is clicked
						if (Thread.interrupted()) {
							return;
						}
					}
					appData.disableRun();
				}
			}
			if (i > maxIterations * .6 && RAND.nextDouble() < 0.05) {
				//System.out.printf("Iteration number %d: ", i);
				//flush();
				break;
			}
			try {
				Thread.sleep(750);
			} catch (InterruptedException ex) {
				return;
			}
		}
		//System.out.printf("Iteration number %d: ", i);
		//flush();

		if(i-1 == maxIterations){
			appData.completeAlgorithm(); //algorithm exhausted all iterations
			appData.updateChart(maxIterations); //show last update
		}else{
			appData.autocompleteAlgorithm(); //algorithm terminated by itself
			appData.updateChart(i);
		}
	}

	@Override
	public String toString() {
		return "[" + this.getClass() + ": maxIterations=" + maxIterations + ", updateInterval=" + updateInterval + ", tocontinue=" + tocontinue + "]";
	}

	// for internal viewing only
	protected void flush() {
		System.out.printf("%d\t%d\t%d%n", output.get(0), output.get(1), output.get(2));
	}

	@Override
	public void startAlgorithm() {
		algorithm.start();
	}

	@Override
	public void continueAlgorithm() {
		tocontinue.set(true);
	}

	@Override
	public void stopAlgorithm() {
		algorithm.interrupt();
	}

	@Override
	public String getName() {
		return "Random Classifier";
	}
}
