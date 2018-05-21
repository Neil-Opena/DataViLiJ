package dataprocessors;

import actions.AppActions;
import algorithms.Algorithm;
import algorithms.AlgorithmTypes;
import algorithms.Classifier;
import algorithms.Clusterer;
import data.Config;
import data.DataSet;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

import vilij.propertymanager.PropertyManager;
import vilij.components.DataComponent;
import vilij.templates.ApplicationTemplate;
import ui.AppUI;
import static settings.AppPropertyTypes.*;

/**
 * This is the concrete application-specific implementation of the data
 * component defined by the Vilij framework.
 *
 * @author Ritwik Banerjee
 * @see DataComponent
 */
public class AppData implements DataComponent {

	private TSDProcessor processor;
	private ApplicationTemplate applicationTemplate;
	private AppUI appUI;
	private AppActions appActions;
	private PropertyManager manager;

	/**
	 * current data that the application has access to
	 */
	private DataSet data;
	private Set labels;
	private double minX;
	private double maxX;
	private double minY;
	private double maxY;

	//For classification algorithms
	private ArrayList<Classifier> classificationAlgorithms;
	private ArrayList<Config> classificationConfigurations;
	private double lineMinY;
	private double lineMaxY; //variables used to store temp y values 
	private XYChart.Series<Number, Number> line;

	//For clustering algorithms
	private ArrayList<Clusterer> clusteringAlgorithms;
	private ArrayList<Config> clusteringConfigurations;

	private AlgorithmTypes algorithmType;
	private int algorithmIndex;
	private Algorithm algorithmToRun; //current algorithm queued up to run
	private Config configuration;

	private boolean isRunning; //test if algorithm is running
	private boolean fromFile;

	public AppData(ApplicationTemplate applicationTemplate) {
		this.processor = new TSDProcessor();
		this.applicationTemplate = applicationTemplate;
		this.algorithmIndex = -1;

		appUI = (AppUI) applicationTemplate.getUIComponent();
		appActions = (AppActions) applicationTemplate.getActionComponent();
		manager = applicationTemplate.manager;

		loadAlgorithms();
	}

	public AppData(){
		//default constructor, mainly used for testing
	}

	@Override
	public void loadData(Path dataFilePath) {
		clear();

		try {
			String fileData = getFileText(dataFilePath.toFile());
			try {
				processor.processString(fileData);
				data = DataSet.fromTSDFile(dataFilePath);

				labels = new LinkedHashSet(data.getLabels().values());
				int numInstances = data.getLocations().size();

				if (numInstances > 10) {
					appUI.setTextAreaText(getTopTen(fileData));
				} else {
					appUI.setTextAreaText(fileData);
				}

				checkLabels();
				String path = dataFilePath.toString();
				appUI.displayInfo(numInstances, path);
				appUI.setUpAlgorithmTypes(labels.size());
				fromFile = true;
				displayOriginalData();
				appUI.enableScreenShotButton();
			} catch (Exception e) {
				//FILE NOT VALID
			}

		} catch (FileNotFoundException e) {
			appActions.showErrorDialog(manager.getPropertyValue(FILE_NOT_FOUND_TITLE.name()), manager.getPropertyValue(FILE_NOT_FOUND_MESSAGE.name()));
		}
	}

	/**
	 * Load data from a given string input
	 *
	 * @param dataString String representation of the data to be loaded
	 */
	public void loadData(String dataString) {
		fromFile = false;
		data = DataSet.fromText(dataString);
		labels = new LinkedHashSet(data.getLabels().values());
		checkLabels();
		appUI.displayInfo(data.getLocations().size(), null);
		displayOriginalData();
		appUI.enableScreenShotButton();
	}

	@Override
	public void saveData(Path dataFilePath) {
		try {
			String toSave = appUI.getSavedText();

			File file = dataFilePath.toFile();

			FileWriter writer = new FileWriter(file);
			writer.append(toSave);
			writer.close();
		} catch (IOException e) {
			appActions.showErrorDialog(manager.getPropertyValue(IO_ERROR_TITLE.name()), manager.getPropertyValue(IO_SAVE_ERROR_MESSAGE.name()));
		} catch (NullPointerException e) {
			//save cancelled
		}
	}

	public void saveDataTest(String toSave, Path dataFilePath){
		try{
			File file = dataFilePath.toFile();

			FileWriter writer = new FileWriter(file);
			writer.append(toSave);
			writer.close();
		}catch(IOException e){
			appActions.showErrorDialog(manager.getPropertyValue(IO_ERROR_TITLE.name()), manager.getPropertyValue(IO_SAVE_ERROR_MESSAGE.name()));
		}catch(NullPointerException e){
			//save cancelled
		}
	}

	@Override
	public void clear() {
		processor.clear();
		appUI.getChart().getData().clear();
		algorithmType = null;
		algorithmIndex = -1;
		algorithmToRun = null;
		configuration = null;
		data = null;
		fromFile = false;
	}

	private void displayOriginalData() {
		data.sortValues();

		NumberAxis xAxis = (NumberAxis) appUI.getChart().getXAxis();
		NumberAxis yAxis = (NumberAxis) appUI.getChart().getYAxis();
		appUI.getChart().getData().clear();
		xAxis.setAutoRanging(false);
		yAxis.setAutoRanging(false);

		Set<String> labels = new LinkedHashSet<>(data.getOriginalLabels().values());
		for (String label : labels) {
			XYChart.Series<Number, Number> series = new XYChart.Series<>();
			series.setName(label);
			data.getOriginalLabels().entrySet().stream().filter(entry -> entry.getValue().equals(label)).forEach(entry -> {
				Point2D point = data.getLocations().get(entry.getKey());
				String name = entry.getKey();
				series.getData().add(new XYChart.Data<>(point.getX(), point.getY(), name));
			});
			appUI.getChart().getData().add(series);
		}

		minX = data.getMinX();
		maxX = data.getMaxX();
		minY = data.getMinY();
		maxY = data.getMaxY();

		double xTicks = getTickMarks(maxX, minX);
		double yTicks = getTickMarks(maxY, minY);

		xAxis.setLowerBound(minX - xTicks);
		xAxis.setUpperBound(maxX + xTicks);
		yAxis.setLowerBound(minY - yTicks);
		yAxis.setUpperBound(maxY + yTicks);

		xAxis.setTickUnit(xTicks);
		yAxis.setTickUnit(yTicks);

		appUI.displayInstanceNames();
	}

	/**
	 * Displays the updated data in the chart
	 */
	private void displayData() {
		appUI.getChart().getData().clear();
		Set<String> labels = new LinkedHashSet<>(data.getLabels().values());
		for (String label : labels) {
			XYChart.Series<Number, Number> series = new XYChart.Series<>();
			series.setName(label);
			data.getLabels().entrySet().stream().filter(entry -> entry.getValue().equals(label)).forEach(entry -> {
				Point2D point = data.getLocations().get(entry.getKey());
				String name = entry.getKey();
				series.getData().add(new XYChart.Data<>(point.getX(), point.getY(), name));
			});
			appUI.getChart().getData().add(series);
		}
	}

	/**
	 * Determines the best possible tick marks according to range of data
	 *
	 * @param max maximum value of axis data
	 * @param min minimum value of axis data
	 * @return best possible tick mark
	 *
	 * StackOverflow was used to help determine this simple algorithm
	 */
	private double getTickMarks(double max, double min) {
		double range = max - min;
		double power = Math.log10(range);
		double orderOfMagnitude = Math.pow(10, power);

		return orderOfMagnitude / 10;
	}

	/**
	 * Gets the labels of the current data
	 *
	 * @return Set of data labels
	 */
	public Set getLabels() {
		return labels;
	}

	/**
	 * Indicates whether the given data is loaded from a file
	 *
	 * @return true if data is from a file
	 */
	public boolean isFromFile() {
		return fromFile;
	}

	/**
	 * Checks whether the input String is valid based on the tsd
	 * requirements
	 *
	 * @param toCheck the text from the tsd file
	 * @return null if data is valid, else returns a message of the error
	 */
	public String validateText(String toCheck) {
		try {
			processor.clear(); // clear current data in processor
			processor.processString(toCheck);
			return null;
		} catch (Exception e) {
			String message = e.getMessage();
			if (message.length() < 9) {
				message = message + applicationTemplate.manager.getPropertyValue(INVALID_DATA_MESSAGE.name());
			}
			return message;
		}
	}

	/**
	 * Check whether the text within a file is valid based on the tsd
	 * requirements
	 *
	 * @param file the File to be checked
	 * @return null if data is valid, else return a message of the error
	 */
	public String validateText(File file) {
		try {
			String fileData = getFileText(file);

			return validateText(fileData);
		} catch (FileNotFoundException e) {
			appActions.showErrorDialog(manager.getPropertyValue(FILE_NOT_FOUND_TITLE.name()), manager.getPropertyValue(FILE_NOT_FOUND_MESSAGE.name()));
			return e.getMessage();
		}

	}

	/**
	 * Return the current algorithm to run type
	 *
	 * @return
	 */
	public AlgorithmTypes getAlgorithmType() {
		return algorithmType;
	}

	public int getAlgorithmIndex() {
		return algorithmIndex;
	}

	private void loadAlgorithms() {
		/*
		One way to satisfy the requirement concerning the loading of algorithms 
		would be to assume that any algorithms to be used by the application would 
		have class files located in a particular resource directory. 
		Upon application startup, this directory would be scanned to retrieve a list 
		of names of the class files located there, and each class file would be loaded 
		and instantiated using reflection. Once instantiated, the name of each algorithm 
		would be obtained, either by just using the base name of the class file, or else 
		by ensuring that each algorithm class supplies a "getName" or similar method that 
		can be used to get the user-friendly name of the algorithm. These names can then 
		be registered with the user interface so that the user will see a complete list 
		of the available algorithms, without any having been hard-coded.
		 */
		Path current = Paths.get(".").toAbsolutePath();
		File classificationDirectory = current.resolve("data-vilij/src/classification").toFile();
		File clusteringDirectory = current.resolve("data-vilij/src/clustering").toFile();

		classificationAlgorithms = new ArrayList<>();
		classificationConfigurations = new ArrayList<>();
		Arrays.asList(classificationDirectory.list()).forEach(algorithm -> {
			String className = "classification." + algorithm.split("\\.")[0];
			try {
				Class algorithmClass = Class.forName(className);
				Constructor constructor = algorithmClass.getConstructors()[0];

				Classifier algorithmInstance = (Classifier) constructor.newInstance(null, -1, -1, false, this);
				classificationAlgorithms.add(algorithmInstance);

				//Add temporary configurations
				classificationConfigurations.add(new Config());

			} catch (Exception ex) { 
				appActions.showErrorDialog("Algorithm Loading Error", "Something went wrong with loading the algorithm");
			}
		});

		clusteringAlgorithms = new ArrayList<>();
		clusteringConfigurations = new ArrayList<>();
		Arrays.asList(clusteringDirectory.list()).forEach(algorithm -> {
			String className = "clustering." + algorithm.split("\\.")[0];
			try {
				Class algorithmClass = Class.forName(className);
				Constructor constructor = algorithmClass.getConstructors()[0];

				Clusterer algorithmInstance = (Clusterer) constructor.newInstance(null, -1, -1, -1, false, this);
				clusteringAlgorithms.add(algorithmInstance);

				//Add temporary configurations
				clusteringConfigurations.add(new Config());

			} catch (Exception ex) { 
				appActions.showErrorDialog("Algorithm Loading Error", "Something went wrong with loading the algorithm");
			}
		});
	}

	/**
	 * FIXME
	 */
	private void setUpAlgorithm() {
		if (algorithmType.equals(AlgorithmTypes.CLASSIFICATION)) {
			//go to list, get algorithm based on index && replace
			Classifier toReplace = classificationAlgorithms.get(algorithmIndex);
			Constructor constructor = toReplace.getClass().getConstructors()[0];
			try {
				algorithmToRun = (Algorithm) constructor.newInstance(data, configuration.getMaxIterations(), configuration.getUpdateInterval(), configuration.getToContinue(), this);
			} catch (Exception ex) { 
				appActions.showErrorDialog("Algorithm Loading Error", "Something went wrong with loading the algorithm");
			} 

			classificationAlgorithms.set(algorithmIndex, (Classifier) algorithmToRun);
		} else {
			//go to list, get algorithm based on index && replace
			Clusterer toReplace = clusteringAlgorithms.get(algorithmIndex);
			Constructor constructor = toReplace.getClass().getConstructors()[0];

			try {
				algorithmToRun = (Algorithm) constructor.newInstance(data, configuration.getMaxIterations(), configuration.getUpdateInterval(), configuration.getNumLabels(), configuration.getToContinue(), this);
			} catch (Exception ex) { 
				appActions.showErrorDialog("Algorithm Loading Error", "Something went wrong with loading the algorithm");
			} 
		}
	}

	/**
	 * Starts the algorithm and performs all necessary UI changes to
	 * indicate that the algorithm is running
	 */
	public void startAlgorithm() {
		displayOriginalData();
		setUpAlgorithm();
		if (algorithmToRun instanceof Classifier) {
			initLine();
		}

		algorithmToRun.startAlgorithm();

		appUI.disableRun();
		appUI.disableBackButton();
		appUI.disableAlgorithmChanges();
		isRunning = true;

		if (!fromFile) {
			//user created data
			appUI.disableEditToggle();
		}

		Platform.runLater(() -> appUI.showAlgorithmRunWindow());
	}

	/**
	 * Updates the current display of the line with the current output
	 * values
	 */
	public void updateChart(int iteration) {
		Platform.runLater(() -> {
			if (algorithmType.equals(AlgorithmTypes.CLASSIFICATION)) {
				XYChart.Series line = (appUI.getChart().getData().get(appUI.getChart().getData().size() - 1));
				XYChart.Data min = (XYChart.Data) line.getData().get(0);
				XYChart.Data max = (XYChart.Data) line.getData().get(1);
				min.setYValue(lineMinY);
				max.setYValue(lineMaxY);
				showDisplayedIteration(iteration, produceLineEquation());
				checkDisplayedLine((double) min.getYValue(), (double) max.getYValue());
			} else {
				displayData();
				showDisplayedIteration(iteration);
			}
		});
	}

	private void showDisplayedIteration(int iteration){
		Platform.runLater(() -> appUI.appendAlgorithmRunWindow("Displayed iteration " + iteration));
	}

	private void showDisplayedIteration(int iteration, String equation){
		Platform.runLater(() -> appUI.appendAlgorithmRunWindow("Displayed iteration " + iteration + equation));
	}

	/**
	 * Checks if the current displayed line lies in the bounds of the chart
	 * The parameters are the endpoints of the chart
	 *
	 * @param minY the minimum y value of the line
	 * @param maxY the maximum y value of the line
	 */
	private void checkDisplayedLine(double minY, double maxY) {
		double yLower = ((NumberAxis) appUI.getChart().getYAxis()).getLowerBound();
		double yUpper = ((NumberAxis) appUI.getChart().getYAxis()).getUpperBound();

		if (minY < yLower && maxY < yLower) {
			Platform.runLater(() -> appUI.appendAlgorithmRunWindow("Line not in chart bounds - direction: South"));
		} else if (minY > yUpper && maxY > yUpper) {
			Platform.runLater(() -> appUI.appendAlgorithmRunWindow("Line not in chart bounds - direction: North"));
		}

		/*
		If the line does not intersect the display window, 
		an appropriate action might be to provide some sort of 
		visual indication as to the direction in which the line lies, 
		relative to the displayed rectangle.
		 */
	}

	/**
	 * Shows the current iteration number to the user
	 *
	 * @param iteration current iteration
	 */
	public void showCurrentIteration(int iteration) {
		StringBuilder infoBuilder = new StringBuilder();
		infoBuilder.append(String.format("Iteration number %d", iteration));
		if (algorithmToRun instanceof Classifier) {
			infoBuilder.append(produceLineEquation());
		}

		Platform.runLater(() -> updateIteration(iteration, infoBuilder.toString()));
	}

	private String produceLineEquation() {
		List<Integer> output = ((Classifier) algorithmToRun).getOutput();
		int a = output.get(0);
		int b = output.get(1);
		int c = output.get(2);

		Platform.runLater(() -> {
			XYChart.Series line = (appUI.getChart().getData().get(appUI.getChart().getData().size() - 1));
			XYChart.Data min = (XYChart.Data) line.getData().get(0);
			XYChart.Data max = (XYChart.Data) line.getData().get(1);
			//ax + by + c = 0
			// y = (-c -ax) / b
			double minX = (double) min.getXValue();
			lineMinY = (-c - (a * minX)) / b;

			double maxX = (double) max.getXValue();
			lineMaxY = (-c - (a * maxX)) / b;
		});
		return ": " + output.get(0) + "x + " + output.get(1) + "y + " + output.get(2) + " = 0 ";
	}

	/**
	 * Initiates the line that will be displayed for classification
	 * algorithms
	 */
	private void initLine() {
		/*
		Modify chart after traversing data to avoid ConcurrentModificationException
		 */
		Platform.runLater(() -> {
			XYChart.Series potentialLine = (XYChart.Series) appUI.getChart().getData().get(appUI.getChart().getData().size() - 1);
			if (potentialLine.getName().equals(AlgorithmTypes.CLASSIFICATION.toString())) {
				appUI.getChart().getData().remove(potentialLine);
			}
		});
		line = new XYChart.Series<>();
		line.setName(AlgorithmTypes.CLASSIFICATION.toString());

		line.getData().add(new XYChart.Data(minX, ((NumberAxis) appUI.getChart().getYAxis()).getLowerBound()));
		line.getData().add(new XYChart.Data(maxX, ((NumberAxis) appUI.getChart().getYAxis()).getLowerBound()));

		Platform.runLater(() -> {
			appUI.getChart().getData().add(line);
			line.getNode().getStyleClass().add("line");
			((XYChart.Data) line.getData().get(0)).getNode().getStyleClass().add("hide-symbol");
			((XYChart.Data) line.getData().get(1)).getNode().getStyleClass().add("hide-symbol");
		});
	}

	/**
	 * Updates the display of the app UI algorithm run window based on the
	 * current iteration
	 *
	 * @param iteration current iteration
	 * @param info additional information to display
	 */
	private void updateIteration(int iteration, String info) {
		double percent = ((double) iteration) / configuration.getMaxIterations();
		appUI.updateAlgorithmRunWindow(percent, info);
	}

	/**
	 * Method used to indicate that the user wants to stop the algorithm
	 * before it has completed
	 */
	public void stopAlgorithm() {
		algorithmStopped();
		algorithmToRun.stopAlgorithm();
	}

	/*So your code is ready to handle the two situations (i) the maximum number of iterations are exhausted, and the algorithm is terminated, or (ii) the algorithm terminates by itself even though the maximum number of iterations has not been reached.
	*/

	public void autocompleteAlgorithm(){
		algorithmStopped();
		Platform.runLater(() -> appActions.showErrorDialog("Completed", "Algorithm has successfully completed and terminated itself"));
	}

	/**
	 * Method used to indicate that the algorithm has successfully finished (the maximum number of iterations are exhausted)
	 */
	public void completeAlgorithm() {
		algorithmStopped();
		Platform.runLater(() -> appActions.showErrorDialog("Completed", "Algorithm has successfully exhausted the maximum number of iterations"));
	}

	/**
	 * Method used to reset the user interface that no algorithm is running
	 */
	private void algorithmStopped() {
		isRunning = false;
		enableRun();
		appUI.enableEditToggle();
		appUI.enableBackButton();
		appUI.enableAlgorithmChanges();
		//should reset progress indicator
		Platform.runLater(() -> {
			appUI.closeAlgorithmRunWindow();
		});
	}

	/**
	 * Enables the run button in the user interface Also displays the
	 * instances names to the user
	 */
	public void enableRun() {
		appUI.enableRun();
		appUI.displayInstanceNames();
	}

	/**
	 * Disables the run button in the user interface
	 */
	public void disableRun() {
		appUI.disableRun();
	}

	/**
	 * Indicates if an algorithm is currently running
	 *
	 * @return true if algorithm is running, false otherwise
	 */
	public boolean isRunning() {
		return isRunning;
	}

	/**
	 * Continue the algorithm
	 */
	public void continueAlgorithm() {
		algorithmToRun.continueAlgorithm();
	}

	/**
	 * Returns the number of classification algorithms available
	 *
	 * @return number of classification algorithms
	 */
	public int getNumClassificationAlgorithms() {
		return classificationAlgorithms.size();
	}

	/**
	 * Returns the number of clustering algorithms available
	 *
	 * @return number of clustering algorithms
	 */
	public int getNumClusteringAlgorithms() {
		return clusteringAlgorithms.size();
	}

	/**
	 * Sets the algorithm to run
	 *
	 * @param index of the algorithm to be run by the application
	 */
	public void setAlgorithmToRun(int index) {
		algorithmIndex = index;
	}

	public String getAlgorithmName(AlgorithmTypes type, int index){
		Algorithm algorithm;
		if(type.equals(AlgorithmTypes.CLASSIFICATION)){
			algorithm = classificationAlgorithms.get(index);
		}else{
			algorithm = clusteringAlgorithms.get(index);
		}

		return algorithm.getName();
	}

	/**
	 * Set the type of algorithm to be run
	 *
	 * @param type of algorithm from AlgorithmTypes enum
	 */
	public void setAlgorithmType(AlgorithmTypes type) {
		algorithmType = type;
	}

	/**
	 * Sets the configuration of the current selected algorithm
	 *
	 * @param config configuration to be set
	 */
	public void setConfiguration(int index) {
		if (algorithmType.equals(AlgorithmTypes.CLASSIFICATION)) {
			configuration = classificationConfigurations.get(index);
		} else {
			configuration = clusteringConfigurations.get(index);
		}
	}

	public void modifyConfiguration(int index, Config config) {
		if (algorithmType.equals(AlgorithmTypes.CLASSIFICATION)) {
			classificationConfigurations.set(index, config);
		} else {
			clusteringConfigurations.set(index, config);
		}
	}

	/*
	when user clicks algorithm --> it sets the algorithm to run
	if there's no configuration --> can't actually run
	 */
	/**
	 * Returns the text within a given file
	 *
	 * @param file file to extract the text
	 * @return string representation of the text inside the file
	 * @throws FileNotFoundException file is not found
	 */
	private String getFileText(File file) throws FileNotFoundException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		Stream<String> dataLines = reader.lines();

		StringBuilder builder = new StringBuilder();

		dataLines.forEach(line -> {
			builder.append(line + "\n");
		});

		return builder.toString();
	}

	/**
	 * Returns the string representation of the first ten lines
	 *
	 * @param data full text of a tsd file
	 * @return te top ten lines of the text
	 */
	private String getTopTen(String data) {
		List temp = Arrays.asList(data.split("\n"));

		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < 10; i++) {
			builder.append(temp.get(i) + "\n");
		}
		return builder.toString();
	}

	/**
	 * Removes the null label from the labels set
	 */
	private void checkLabels() {
		labels.remove(manager.getPropertyValue(NULL.name()));
	}

}
