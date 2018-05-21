package data;

import javafx.geometry.Point2D;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * This class specifies how an algorithm will expect the dataset to be. It is
 * provided as a rudimentary structure only, and does not include many of the
 * sanity checks and other requirements of the use cases. As such, you can
 * completely write your own class to represent a set of data instances as long
 * as the algorithm can read from and write into two {@link java.util.Map}
 * objects representing the name-to-label map and the name-to-location (i.e.,
 * the x,y values) map. These two are the {@link DataSet#labels} and
 * {@link DataSet#locations} maps in this class.
 *
 * @author Ritwik Banerjee
 */
public class DataSet {

	public static class InvalidDataNameException extends Exception {

		private static final String NAME_ERROR_MSG = "All data instance names must start with the @ character.";

		public InvalidDataNameException(String name) {
			super(String.format("Invalid name '%s'." + NAME_ERROR_MSG, name));
		}
	}

	private static String nameFormatCheck(String name) throws InvalidDataNameException {
		if (!name.startsWith("@")) {
			throw new InvalidDataNameException(name);
		}
		return name;
	}

	private static Point2D locationOf(String locationString) {
		String[] coordinateStrings = locationString.trim().split(",");
		return new Point2D(Double.parseDouble(coordinateStrings[0]), Double.parseDouble(coordinateStrings[1]));
	}

	private Map<String, String> labels;
	private Map<String, String> originalLabels;
	private Map<String, Point2D> locations;

	private Double[] sortedXValues;
	private Double[] sortedYValues;
	/**
	 * Creates an empty dataset.
	 */
	public DataSet() {
		labels = new LinkedHashMap<>();
		originalLabels = new LinkedHashMap<>();
		locations = new LinkedHashMap<>();
	}

	public Map<String, String> getLabels() {
		return labels;
	}

	public Map<String, String> getOriginalLabels(){
		return originalLabels;
	}

	public Map<String, Point2D> getLocations() {
		return locations;
	}

	public void updateLabel(String instanceName, String newlabel) {
		if (labels.get(instanceName) == null) {
			throw new NoSuchElementException();
		}
		labels.put(instanceName, newlabel);
	}

	private void addInstance(String tsdLine) throws InvalidDataNameException {
		String[] arr = tsdLine.split("\t");
		labels.put(nameFormatCheck(arr[0]), arr[1]);
		originalLabels.put(nameFormatCheck(arr[0]), arr[1]);
		locations.put(arr[0], locationOf(arr[2]));
	}

	public void sortValues(){
		Map<Double, Double> xMap = new LinkedHashMap<>();
		Map<Double, Double> yMap = new LinkedHashMap<>();

		locations.values().forEach(point -> {
			double xValue = point.getX();
			double yValue = point.getY();

			xMap.put(xValue, xValue);
			yMap.put(yValue, yValue);
		});

		sortedXValues = xMap.values().toArray(new Double[0]);
		Arrays.sort(sortedXValues);

		sortedYValues = yMap.values().toArray(new Double[0]);
		Arrays.sort(sortedYValues);
	}

	/**
	 * Identifies the minimum x value
	 * @precondition data must be sorted
	 * @return minimum x of data
	 */
	public double getMinX() {
		return sortedXValues[0].doubleValue();
	}

	/**
	 * Identifies the maximum x value
	 * @precondition data must be sorted
	 * @return maximum x of data
	 */
	public double getMaxX() {
		return sortedXValues[sortedXValues.length - 1].doubleValue();
	}

	/**
	 * Identifies the minimum y value
	 * @precondition data must be sorted
	 * @return minimum y of data
	 */
	public double getMinY() {
		return sortedYValues[0].doubleValue();
	}

	/**
	 * Identifies the maximum y value
	 * @precondition data must be sorted
	 * @return maximum y of data
	 */
	public double getMaxY() {
		return sortedYValues[sortedYValues.length - 1].doubleValue();
	}

	public static DataSet fromTSDFile(Path tsdFilePath) throws IOException {
		DataSet dataset = new DataSet();
		Files.lines(tsdFilePath).forEach(line -> {
			try {
				dataset.addInstance(line);
			} catch (InvalidDataNameException e) {
				e.printStackTrace();
			}
		});
		return dataset;
	}

	public static DataSet fromText(String text) {
		List lines = Arrays.asList(text.split("\n"));
		DataSet dataset = new DataSet();
		lines.forEach(line -> {
			try {
				dataset.addInstance(line.toString());
			} catch (InvalidDataNameException e) {
				e.printStackTrace();
			}
		});
		return dataset;
	}
}
