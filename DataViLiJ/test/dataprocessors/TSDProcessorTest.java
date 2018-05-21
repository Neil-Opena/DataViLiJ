/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dataprocessors;

import org.junit.Test;

/**
 * Parsing a single line of data in the TSD format to create an instance object. This must include tests for suitable boundary values.
 * @author Neil Opena
 */
public class TSDProcessorTest {
	
	public TSDProcessorTest() {
	}
	
	/*
	Tab-Separated Data (TSD) Format specification
	1. A file in this format must have the ".tsd" extension
	2. Each line (including the last line)of such a file must end with '\n' as the newline character
	3. Each line must consist of exactly three components separated by '\t'. The individual components are
		a. Instance name, which must start with '@'
		b. Label name, which may be null
		c. Spatial location in the x-y plane as a pair of comma-separated numeric values. The values must be no more specific than 2 decimal places, and there must not be any whitespace between them.
	4. There must not be any empty line before the end of file.
	5. There must not be any duplicate instance names. It is possible for two separate instances to have the same spatial location, however. 
	
	*/

	/**
	 * Test of processString method, of class TSDProcessor.
	 * Parsing a single line of data in the TSD format to create an instance object.
	 * This must include tests for suitable boundary values.
	 * 
	 * Strings tested and reason:
	 * 
	 * 1. tsdString = "@a	label1	1,1" (Standard case)
	 * 2. tsdString = "@b	null	3,3" (Standard case - null label)
	 * 3. tsdString = "name	label1	1,1"; (Invalid instance name - does not start with '@')
	 * 4. tsdString = "@a	label1	2,2"; (Duplicate instance name (a))
	 * 5. tsdString = "@c	label1	x,y"; (Non-numerical values for x-y location)
	 * 6. tsdString = "@d	label1	3.12,4.15"; (Standard case - 2 decimal place)
	 * 7. tsdString = ""; (Empty line)
	 */
	@Test (expected = Exception.class)
	public void testProcessString() throws Exception {
		System.out.println("processString");
		TSDProcessor instance = new TSDProcessor();

		String tsdString;

		tsdString = "@a	label1	1,1";
		System.out.println("testing String=" + tsdString);
		instance.processString(tsdString);

		tsdString = "@b	null	3,3";
		System.out.println("testing String=" + tsdString);
		instance.processString(tsdString);

		tsdString = "name	label1	1,1";
		System.out.println("testing String=" + tsdString);
		instance.processString(tsdString);

		tsdString = "@a	label1	2,2"; 
		System.out.println("testing String=" + tsdString);
		instance.processString(tsdString);

		tsdString = "@c	label1	x,y";
		System.out.println("testing String=" + tsdString);
		instance.processString(tsdString);

		tsdString = "@d	label1	3.12,4.15";
		System.out.println("testing String=" + tsdString);
		instance.processString(tsdString);

		tsdString = "";
		System.out.println("testing String=" + tsdString);
		instance.processString(tsdString);
	}

}
