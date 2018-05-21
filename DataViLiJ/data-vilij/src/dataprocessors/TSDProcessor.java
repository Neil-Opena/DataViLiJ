package dataprocessors;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import javafx.scene.chart.XYChart;
import javafx.geometry.Point2D;

import static settings.AppPropertyTypes.*;
import vilij.propertymanager.PropertyManager;

/**
 * The data files used by this data visualization applications follow a
 * tab-separated format, where each data point is named, labeled, and has a
 * specific location in the 2-dimensional X-Y plane. This class handles the
 * parsing and processing of such data. It also handles exporting the data to a
 * 2-D plot.
 * <p>
 * A sample file in this format has been provided in the application's
 * <code>resources/data</code> folder.
 *
 * @author Ritwik Banerjee
 * @see XYChart
 */
public final class TSDProcessor {

	private PropertyManager manager;

	public static class InvalidDataNameException extends Exception {

		private static final String NAME_ERROR_MSG = "All data instance names must start with the @ character.";

		public InvalidDataNameException(String name) {
			super(String.format("Invalid name '%s'. " + NAME_ERROR_MSG, name));
		}
	}

	public static class DuplicateNameException extends Exception {

		private static final String MSG = "Duplicate name = ";

		public DuplicateNameException(String name) {
			super(MSG + name);
		}
	}

	private Map<String, String> dataLabels;
	private Map<String, Point2D> dataPoints;

	public TSDProcessor() {
		dataLabels = new LinkedHashMap<>();
		dataPoints = new LinkedHashMap<>();
		manager = PropertyManager.getManager();
	}

	/**
	 * Processes the data and populated two {@link Map} objects with the
	 * data.
	 *
	 * @param tsdString the input data provided as a single {@link String}
	 * @throws Exception if the input string does not follow the
	 * <code>.tsd</code> data format
	 */
	public void processString(String tsdString) throws Exception {
		AtomicBoolean hadAnError = new AtomicBoolean(false);
		StringBuilder errorMessage = new StringBuilder();
		Stream.of(tsdString.split("\n"))
			.map(line -> Arrays.asList(line.split("\t")))
			.forEach(list -> {
				try {
					String name = checkedname(list.get(0));
					String label = list.get(1);
					String[] pair = list.get(2).split(",");
					Point2D point = new Point2D(Double.parseDouble(pair[0]), Double.parseDouble(pair[1]));
					if (dataPoints.containsKey(name)) {
						throw new DuplicateNameException(name);
					}
					dataLabels.put(name, label);
					dataPoints.put(name, point);
				} catch (Exception e) {
					errorMessage.setLength(0);
					errorMessage.append(manager.getPropertyValue(LINE.name()) + (dataPoints.size() + 1) + ": ");
					if (e instanceof InvalidDataNameException || e instanceof DuplicateNameException) {
						errorMessage.append(e.getMessage());
					}
					hadAnError.set(true);
				}
			});
		if (errorMessage.length() > 0) {
			throw new Exception(errorMessage.toString());
		}
	}

	/**
	 * Exports the data to the specified 2-D chart.
	 *
	 * @param chart the specified chart
	 */
	void toChartData(XYChart<Number, Number> chart) {
		chart.getData().clear();
		chart.getXAxis().setAutoRanging(true);
		chart.getYAxis().setAutoRanging(true);
		Set<String> labels = new LinkedHashSet<>(dataLabels.values());
		for (String label : labels) {
			XYChart.Series<Number, Number> series = new XYChart.Series<>();
			series.setName(label);
			dataLabels.entrySet().stream().filter(entry -> entry.getValue().equals(label)).forEach(entry -> {
				Point2D point = dataPoints.get(entry.getKey());
				String name = entry.getKey();
				series.getData().add(new XYChart.Data<>(point.getX(), point.getY(), name));
			});
			chart.getData().add(series);
		}
	}

	/**
	 *
	 * @return
	 */
	public List getDataPoints() {
		return Arrays.asList(dataPoints.values().toArray());
	}

	/**
	 *
	 */
	void clear() {
		dataPoints.clear();
		dataLabels.clear();
	}

	/**
	 *
	 * @return
	 */
	public List getLabelList() {
		return Arrays.asList(dataLabels.values().toArray());
	}

	/**
	 *
	 * @param name
	 * @return
	 * @throws dataprocessors.TSDProcessor.InvalidDataNameException
	 */
	private String checkedname(String name) throws InvalidDataNameException {
		if (!name.startsWith("@")) {
			throw new InvalidDataNameException(name);
		}
		return name;
	}
}
