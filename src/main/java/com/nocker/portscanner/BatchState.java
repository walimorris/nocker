package com.nocker.portscanner;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The BatchState class represents the state of a batch operation.
 * It is responsible for managing and tracking the status of individual
 * tasks within a batch, supporting task submission, completion, and sealing.
 *
 * This class uses synchronization mechanisms such as an AtomicInteger,
 * CountDownLatch, and concurrent collections to ensure thread safety.
 *
 * Key features:
 * - Tracks the number of tasks currently in progress (in-flight tasks).
 * - Allows submitting tasks to the batch for tracking purposes.
 * - Signals when all tasks in the batch have completed.
 * - Can be sealed to stop accepting new tasks.
 */
public class BatchState {
    final AtomicInteger inFlight = new AtomicInteger(0);
    public final CountDownLatch done = new CountDownLatch(1);
    public final List<Future<?>> futures = new CopyOnWriteArrayList<>();
    volatile boolean sealed = false;

    public void onSubmit(Future<?> future) {
        if (sealed) {
            throw new IllegalStateException("Batch already sealed");
        }
        inFlight.incrementAndGet();
        futures.add(future);
    }

    public void onComplete() {
        if (inFlight.decrementAndGet() == 0) {
            done.countDown();
        }
    }

    public void seal() {
        sealed = true;
        if (inFlight.get() == 0) {
            done.countDown();
        }
    }
}
