/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ui;

import algorithms.AlgorithmTypes;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author neil1
 */
public class AppUITest {

	public AppUITest() {
	}

	private String invalidInput = "One or more inputs have invalid data.\nPlease input integers";
	private String negativeIterations = "Number of max iterations cannot be negative";
	private String negativeUpdateInterval = "Update interval cannot be negative";
	private String invalidIntervalIterations = "Update interval cannot be greater than the number of max iterations";
	private String negativeLabels = "The number of labels cannot be negative";
	private String largeLabels = "The number of labels cannot exceed the number of data points";


	private String checkInput(AlgorithmTypes type, String testMaxIterations, String testUpdateInterval, String testNumLabels, int numInstances) {
		// no negative values or letters
		String message = "";
		try {
			int maxIterations = Integer.parseInt(testMaxIterations);
			int updateInterval = Integer.parseInt(testUpdateInterval);
			if (maxIterations < 0) {
				message = negativeIterations;
			} else if (updateInterval < 0) {
				message = negativeUpdateInterval;
			} else if (updateInterval > maxIterations) {
				message = invalidIntervalIterations;
			} else if (type.equals(AlgorithmTypes.CLUSTERING)) {
				int numLabels = Integer.parseInt(testNumLabels);
				if (numLabels < 1) {
					message = negativeLabels;
				} else if (numLabels > numInstances) {
					message = largeLabels;
				}
			}
		} catch (NumberFormatException e) {
			message = invalidInput;
		}
		return message;
	}

	/**
	 * Run configuration values for classification and clustering. Again,
	 * the tests must include boundary value analyses.
	 *
	 * code has been copied from the check input method since it is an inner
	 * class and cannot be called outside the AppUI class
	 * 
	 * Clustering will be mainly used to test the input such that the number of labels 
	 * is also tested
	 * 
	 * The number of instances is assummed to be 5 for this test.
	 * 
	 * 1. checkInput(type, "a", "b", "c", 5) - non numeric values used
	 * 2. checkInput(type, "1", "2", "3", 5) - update interval greater than iteration
	 * 3. checkInput(type, "-1", "2", "3", 5) - negative iterations
	 * 4. checkInput(type, "1", "-2", "3", 5) - negative update interval
	 * 5. checkInput(type, "1", "2", "-3", 5) - negative number of labels
	 * 6. checkInput(type, "2", "2", "6", 5) - label number larger than number of instances
	 * 7. checkInput(type, "2", "2", "3", 5) - standard case
	 */
	@Test
	public void testConfigurationValues() {
		AlgorithmTypes type = AlgorithmTypes.CLUSTERING;
		int numInstances = 5;
		String result;

		result = checkInput(type, "a", "b", "c", 5);
		assertEquals(result, invalidInput);

		result = checkInput(type, "1", "2", "3", 5);
		assertEquals(result, invalidIntervalIterations);

		result = checkInput(type, "-1", "2", "3", 5);
		assertEquals(result, negativeIterations);

		result = checkInput(type, "1", "-2", "3", 5);
		assertEquals(result, negativeUpdateInterval);

		result = checkInput(type, "2", "2", "-3", 5);
		assertEquals(result, negativeLabels);

		result = checkInput(type, "2", "2", "6", 5);
		assertEquals(result, largeLabels);

		result = checkInput(type, "2", "2", "3", 5);
		assertEquals(result, "");
		
	}

}
