/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package clustering;

import algorithms.Clusterer;
import data.DataSet;
import dataprocessors.AppData;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * @author Neil Opena
 */
public class RandomClustering extends Clusterer { //problem with CLuster

	private static final Random RAND = new Random();

	@SuppressWarnings("FieldCanBeLocal")
	private DataSet dataset;
	private List<String> labels;

	private final Thread algorithm;
	private final AppData appData;

	private final int maxIterations;
	private final int updateInterval;

	// currently, this value does not change after instantiation
	private AtomicBoolean tocontinue;
	private boolean isContinuous; //value that does not change

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

	public RandomClustering(DataSet dataset, int maxIterations, int updateInterval, int numberOfClusters, boolean tocontinue, AppData appData) {
		super(numberOfClusters);
		this.dataset = dataset;
		this.maxIterations = maxIterations;
		this.updateInterval = updateInterval;

		algorithm = new Thread(this);
		algorithm.setName(getName());

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
		initializeLabels();
		int iteration = 0;
		while (iteration++ < maxIterations && !Thread.interrupted()) {
			appData.showCurrentIteration(iteration);
			assignLabels();
			if(iteration % updateInterval == 0){
				appData.updateChart(iteration);
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
			try {
				Thread.sleep(750);
			} catch (InterruptedException ex) {
				return;
			}
		}

		if(iteration-1 == maxIterations){
			appData.completeAlgorithm(); //algorithm exhausted all iterations
			appData.updateChart(maxIterations);
		}else{
			appData.autocompleteAlgorithm(); //algorithm terminated by itself
			appData.updateChart(iteration);
		}
	}

	private void initializeLabels(){
		labels = new ArrayList<>();
		for(int i = 0; i < numberOfClusters; i++){
			labels.add("" + i);
		}
	}

	private void assignLabels() {
		Random random = new Random();
		dataset.getLocations().forEach((instanceName, location) -> {
			int labelIndex = random.nextInt(labels.size());
			dataset.getLabels().put(instanceName, labels.get(labelIndex));
		});
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
		return "Random Clustering";
	}
}
