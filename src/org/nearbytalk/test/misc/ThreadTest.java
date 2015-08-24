package org.nearbytalk.test.misc;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Collection;

import org.eclipse.jetty.util.ConcurrentHashSet;

public class ThreadTest {

	public static class SingleTest {
		public void singleTest() throws Exception{}
		
		public void singleTest(int threadIndex,int threadNumber) throws Exception{
			singleTest();
		}

		public void singleTest(int threadIndex,int threadNumber,int loopCounter,int loopNumber) throws Exception{
			singleTest(threadIndex,threadNumber);
		}

		public void threadLeaveCallback(){
			
		}
	}

	static public Collection<Exception> run(final int threadNumber,
			final int loopNumber, final int sleepMs,final SingleTest singleTest)
			throws InterruptedException {

		Thread allThreads[] = new Thread[threadNumber];

		final ConcurrentHashSet<Exception> errors=new ConcurrentHashSet<Exception>();

		for (int i = 0; i < threadNumber; ++i) {
			
			final int threadIndex=i;

			allThreads[i] = new Thread() {

				@Override
				public void run() {

					try {
						for (int j = 0; j < loopNumber; j++) {

							singleTest.singleTest(threadIndex,threadNumber,j,loopNumber);

							sleep(sleepMs);

						}
					} catch (InterruptedException e) {
						errors.add(e);
					} catch (Exception e) {
						errors.add(e);
					}
					
					singleTest.threadLeaveCallback();
				}

			};
		}

		for (Thread thread : allThreads) {

			
			thread.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
				
				@Override
				public void uncaughtException(Thread t, Throwable e) {
					
					e.printStackTrace();
					errors.add(new Exception(e));
				}
			});

			thread.start();
		}
		for (Thread thread : allThreads) {

			thread.join();
		}

		return errors;
	}

}
