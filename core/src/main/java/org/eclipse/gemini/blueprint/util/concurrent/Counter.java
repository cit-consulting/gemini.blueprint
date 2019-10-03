/******************************************************************************
 * Copyright (c) 2006, 2010 VMware Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution. 
 * The Eclipse Public License is available at 
 * http://www.eclipse.org/legal/epl-v10.html and the Apache License v2.0
 * is available at http://www.opensource.org/licenses/apache2.0.php.
 * You may elect to redistribute this code under either of these licenses. 
 * 
 * Contributors:
 *   VMware Inc.
 *****************************************************************************/

package org.eclipse.gemini.blueprint.util.concurrent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Simple counting class which can be incremented or decremented in a
 * synchronized manner. This class can be used as a synchronization mechanism
 * between threads mainly though {@link #waitForZero(long)} method.
 * 
 * The main usage of the class is to allow a master thread, to know when other
 * threads (slaves) have passed a certain point in execution.
 * 
 * <p/> As opposed to a Barrier or a Semaphore, this class should be used only
 * with 1 waiting thread (a master) and any number of slave threads.
 * 
 * <pre style="code">
 * Thread 1:
 *  counter.increment();
 *  thread2.start();
 *  counter.increment();
 *  thread3.start();
 *   
 *  // wait 1 second for other threads to complete
 *  counter.waitForZero(1000);
 * 
 * Thread 2:
 *  // do some work
 *  counter.decrement();
 * 
 * Thread 3:
 *  // do some work
 *  counter.decrement();
 * 
 * </pre>
 * 
 * <p/> Mainly for usage inside the framework. All methods are thread-safe
 * however for the master/slave pattern, synchronized blocks are recommended as
 * multiple operations have to be executed at once.
 * 
 * @author Costin Leau
 * 
 */
public class Counter {

	private int counter = 0;

	private static final Log log = LogFactory.getLog(Counter.class);

	private final String name;


	/**
	 * Create counter with a given name.
	 * 
	 * @param name counter name
	 */
	public Counter(String name) {
		this.name = name;
	}

	/**
	 * Increment the counter value.
	 */
	public synchronized void increment() {
		counter++;
		if (log.isTraceEnabled())
			log.trace("counter [" + name + "] incremented to " + counter);
	}

	/**
	 * Decrement the counter value.
	 */
	public synchronized void decrement() {
		counter--;
		if (log.isTraceEnabled())
			log.trace("counter [" + name + "] decremented to " + counter);
		notifyAll();
	}

	public synchronized boolean decrementAndWait(long timeToWait) {
		decrement();
		if (counter > 0)
			return waitForZero(timeToWait);
		return true;
	}

	/**
	 * Check if the counter value is zero.
	 * 
	 * @return true if value is equal or below zero, false otherwise.
	 */
	public synchronized boolean isZero() {
		return is(0);
	}

	public synchronized boolean is(int value) {
		return counter == value;
	}

	/**
	 * Return the counter value.
	 * 
	 * @return the counter value.
	 */
	public synchronized int getValue() {
		return counter;
	}

	public synchronized String toString() {
		return "" + counter;
	}

	/**
	 * Specialized method which waits for 0. Identical to waitFor(0, waitTime).
	 * 
	 * @see #waitFor(int, long)
	 * @param waitTime
	 * @return true if the waiting timed out, false otherwise
	 */
	public synchronized boolean waitForZero(long waitTime) {
		return waitFor(0, waitTime);
	}

	/**
	 * Wait maximum the given amount of time, for the counter to reach the given
	 * value. This mechanism relies on {@link Object#wait(long)} and
	 * {@link Object#notify()} mechanism to work appropriately. Please see the
	 * class javadoc for more info.
	 * 
	 * <p/> This method will stop waiting and return true if the thread is
	 * interrupted.
	 * 
	 * @param value the value to wait for
	 * @param waitTime the time (in miliseconds) to wait for zero value
	 * @return true if the waiting timed out, false otherwise
	 */
	public synchronized boolean waitFor(int value, long waitTime) {
		boolean timedout = false;
		long remainingTime = waitTime;
		long startTime = System.currentTimeMillis();

		while (counter > value && !timedout) {
			// start waiting
			try {
				this.wait(remainingTime);
				// compute the remaining time
				remainingTime = waitTime - (System.currentTimeMillis() - startTime);
				timedout = remainingTime <= 0;
			}
			catch (InterruptedException ex) {
				timedout = true;
			}
		}

		return timedout;
	}
}
