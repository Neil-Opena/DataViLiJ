/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * @author neil1
 */
public class Config {
	private final int maxIterations;
    	private final int updateInterval;
	private final int numLabels;

    	private final AtomicBoolean tocontinue;

	public Config(){
		this(-1, -1, false, -1);
	}

	public Config(int maxIterations, int updateInterval, boolean toContinue){
		this(maxIterations, updateInterval, toContinue, -1);
	}

	public Config(int maxIterations, int updateInterval, boolean toContinue, int numLabels){
		this.maxIterations = maxIterations;
		this.updateInterval = updateInterval;
		this.tocontinue = new AtomicBoolean(toContinue);
		this.numLabels = numLabels;
	}

	public int getMaxIterations(){
		return maxIterations;
	}

	public int getUpdateInterval(){
		return updateInterval;
	}

	public int getNumLabels(){
		return numLabels;
	}

	public boolean getToContinue(){
		return tocontinue.get();
	}

	@Override
	public String toString(){
		return "[maxIterations=" + maxIterations + ", updateInterval=" + updateInterval + ", toContinue=" + tocontinue + ", numLabels=" + numLabels +"]";
	}
}
