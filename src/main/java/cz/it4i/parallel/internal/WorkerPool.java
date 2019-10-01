
package cz.it4i.parallel.internal;

import java.io.Closeable;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

class WorkerPool implements Closeable {

	private final BlockingQueue<ParallelWorker> availableWorkers;

	public WorkerPool() {
		availableWorkers = new ArrayBlockingQueue<>(1024);
	}

	public void addWorker(final ParallelWorker worker) {
		availableWorkers.add(worker);
	}

	public ParallelWorker takeFreeWorker() throws InterruptedException {
		return availableWorkers.take();
	}

	@Override
	public void close() {
		Iterator<ParallelWorker> iter = availableWorkers.iterator();
		while (iter.hasNext()) {
			try (ParallelWorker worker = iter.next()) {
				iter.remove();
			}
		}
	}

	public int size() {
		return availableWorkers.size();
	}
}
