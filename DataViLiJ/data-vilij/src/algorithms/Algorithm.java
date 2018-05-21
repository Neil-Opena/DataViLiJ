/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package algorithms;

/**
 * This interface provides a way to run an algorithm
 * on a thread as a {@link java.lang.Runnable}object.
 * @author Ritwik Banerjee
 */
public interface Algorithm extends Runnable {

    int getMaxIterations();

    int getUpdateInterval();

    boolean tocontinue();

    void startAlgorithm();

    void continueAlgorithm();

    void stopAlgorithm();

    String getName();
}
