/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package algorithms;

/**
 *
 * @author neil1
 */
public enum AlgorithmTypes {
	CLASSIFICATION, CLUSTERING;

	public String toString(){
		switch(this){
			case CLASSIFICATION : 
				return "Classification";
			case CLUSTERING :
				return "Clustering";
		}
		return null;
	}
}

