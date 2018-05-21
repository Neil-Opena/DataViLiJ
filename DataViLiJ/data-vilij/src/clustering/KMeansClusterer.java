/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package clustering;

import algorithms.Clusterer;
import data.DataSet;
import dataprocessors.AppData;
import javafx.geometry.Point2D;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Ritwik Banerjee
 */
public class KMeansClusterer extends Clusterer {

	private final Thread algorithm;
	private final AppData appData;

	private DataSet dataset;
	private List<Point2D> centroids;

	private final int maxIterations;
	private final int updateInterval;

	private final boolean isContinuous; //value that does not change
	private final AtomicBoolean tocontinue;
	private final AtomicBoolean continueIterations; //variable to hanlde non continuous requirement

	public KMeansClusterer(DataSet dataset, int maxIterations, int updateInterval, int numberOfClusters, boolean tocontinue, AppData appData) {
		super(numberOfClusters);
		this.dataset = dataset;
		this.maxIterations = maxIterations;
		this.updateInterval = updateInterval;
		this.tocontinue = new AtomicBoolean(false);

		algorithm = new Thread(this);
		algorithm.setName(getName());

		this.isContinuous = tocontinue;
		this.continueIterations = new AtomicBoolean(tocontinue);
		this.appData = appData;
	}

	@Override
	public int getMaxIterations() {
		return maxIterations;
	}

	@Override
	public int getUpdateInterval() {
		return updateInterval;
	}

	@Override
	public boolean tocontinue() {
		return tocontinue.get();
	}

	@Override
	public void run() {
		// time for original chart to show
		try {
			Thread.sleep(750);
		} catch (InterruptedException ex) {
			return;
		}

		try{
			initializeCentroids();
		}catch(IndexOutOfBoundsException ex){
			//this occurs when the variable i is out of bounds
			//do nothing
		}

		int iteration = 0;
		while (!Thread.interrupted() && iteration++ < maxIterations & tocontinue.get()) {
			appData.showCurrentIteration(iteration);
			assignLabels();
			recomputeCentroids();
			if (iteration % updateInterval == 0) {
				appData.updateChart(iteration);
				if (!isContinuous) {
					appData.enableRun();
					continueIterations.set(false);
					while (!continueIterations.get()) { //wait until play is clicked
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

		if (iteration - 1 == maxIterations) {
			appData.completeAlgorithm(); //algorithm exhausted all iterations
			appData.updateChart(maxIterations); //show the last update
		} else {
			appData.autocompleteAlgorithm(); //algorithm terminated by itself
			appData.updateChart(iteration);
		}

	}

	private void initializeCentroids() throws IndexOutOfBoundsException{
		Set<String> chosen = new HashSet<>();
		List<String> instanceNames = new ArrayList<>(dataset.getOriginalLabels().keySet()); //modified so that it gets original labels instead
		Random r = new Random();
		while (chosen.size() < numberOfClusters) {
			int i = r.nextInt(instanceNames.size());
			while (chosen.contains(instanceNames.get(i))) {
				++i;
			}
			chosen.add(instanceNames.get(i));
		}
		centroids = chosen.stream().map(name -> dataset.getLocations().get(name)).collect(Collectors.toList());
		tocontinue.set(true);
	}

	private void assignLabels() {
		dataset.getLocations().forEach((instanceName, location) -> {
			double minDistance = Double.MAX_VALUE;
			int minDistanceIndex = -1;
			for (int i = 0; i < centroids.size(); i++) {
				double distance = computeDistance(centroids.get(i), location);
				if (distance < minDistance) {
					minDistance = distance;
					minDistanceIndex = i;
				}
			}
			dataset.getLabels().put(instanceName, Integer.toString(minDistanceIndex));
		});
	}

	private void recomputeCentroids() {
		tocontinue.set(false);
		IntStream.range(0, numberOfClusters).forEach(i -> {
			AtomicInteger clusterSize = new AtomicInteger();
			Point2D sum = dataset.getLabels() //this is the modied labels
				.entrySet()
				.stream()
				.filter(entry -> i == Integer.parseInt(entry.getValue()))
				.map(entry -> dataset.getLocations().get(entry.getKey()))
				.reduce(new Point2D(0, 0), (p, q) -> {
					clusterSize.incrementAndGet();
					return new Point2D(p.getX() + q.getX(), p.getY() + q.getY());
				});
			Point2D newCentroid = new Point2D(sum.getX() / clusterSize.get(), sum.getY() / clusterSize.get());
			if (!newCentroid.equals(centroids.get(i))) {
				centroids.set(i, newCentroid);
				tocontinue.set(true);
			}
		});
	}

	private static double computeDistance(Point2D p, Point2D q) {
		return Math.sqrt(Math.pow(p.getX() - q.getX(), 2) + Math.pow(p.getY() - q.getY(), 2));
	}

	@Override
	public void startAlgorithm() {
		algorithm.start();
	}

	@Override
	public void continueAlgorithm() {
		continueIterations.set(true);
	}

	@Override
	public void stopAlgorithm() {
		algorithm.interrupt();
	}

	@Override
	public String getName() {
		return "KMeans Clusterer";
	}

}
