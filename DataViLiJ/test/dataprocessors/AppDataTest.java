/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dataprocessors;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.AfterClass;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author neil1
 */
public class AppDataTest {
	
	public AppDataTest() {
	}
	
	@BeforeClass
	public static void setUpClass() {
	}
	
	@AfterClass
	public static void tearDownClass() {
	}

	/**
	 * The requirement is to test whether the data entered in the text area is being saved to the destination .tsd file. 
	 * That is, you can store the data internally in a string, and test saving that string to the destination file.
	 * 
	 * Saving data from the text-area in the UI to a .tsd file.
	 * 
	 * For this test, a string is tested and checked if the file contains
	 * the saved data
	 */
	@Test
	public void testSaveData() throws IOException {
		System.out.println("saveData");
		AppData instance = new AppData();
		
		Path current = Paths.get(".").toAbsolutePath();
		Path destination = current.resolve("data-vilij/resources/data/test_valid.tsd");

		String toSave = "@a	label1	1,1";
		instance.saveDataTest(toSave, destination);

		StringBuilder newData = new StringBuilder();
		Files.lines(destination).forEach(line -> {
			newData.append(line + "\n");
		});

		assertTrue(newData.toString().contains(toSave));

	}
	
}
