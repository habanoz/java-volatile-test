import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class TestVolatile {
	private static final Logger logger = org.slf4j.LoggerFactory.getLogger(TestVolatile.class);
	static final int testCount = 100;
	static final int execCount = 5;
	private static final boolean multipleVariables = false;
	private static final int nReadThreads = 1;
	private static final int TEST_SIZE = 1_000_000;
	private static final String CANNOT_BE_NULL = "cannot be null";
	private static final boolean WRITE_ENABLED = false;

	public static void main(String[] args) throws ExecutionException, InterruptedException {
		Configurator.setRootLevel(Level.ERROR);

		// warm up
		doTest();
		reset();

		logger.error("Test started");


		for (int i = 0; i < execCount; i++) {
			doTest();
			logger.error("Test {} completd", i);
		}

		logger.error("Shared Read \n{}", sharedVariablesRead.stream().map(Object::toString).collect(Collectors.joining(",")));
		logger.error("Shared Write \n{}", sharedVariablesWrite.stream().map(Object::toString).collect(Collectors.joining(",")));

		logger.error("Synchronized Read \n{}", testSharedSynchronizedVariablesRead.stream().map(Object::toString).collect(Collectors.joining(",")));
		logger.error("Synchronized Write \n{}", testSharedSynchronizedVariablesWrite.stream().map(Object::toString).collect(Collectors.joining(",")));

		logger.error("RW Synchronized Read \n{}", testSharedRWSynchronizedVariablesRead.stream().map(Object::toString).collect(Collectors.joining(",")));
		logger.error("RW Synchronized Write \n{}", testSharedRWSynchronizedVariablesWrite.stream().map(Object::toString).collect(Collectors.joining(",")));

		logger.error("Volatile Read \n{}", testSharedVolatileVariablesRead.stream().map(Object::toString).collect(Collectors.joining(",")));
		logger.error("Volatile Write \n{}", testSharedVolatileVariablesWrite.stream().map(Object::toString).collect(Collectors.joining(",")));

		logger.error("Atomic Read \n{}", testAtomicIntegerRead.stream().map(Object::toString).collect(Collectors.joining(",")));
		logger.error("Atomic Write \n{}", testAtomicIntegerWrite.stream().map(Object::toString).collect(Collectors.joining(",")));

		logger.error("Shared Read {}", sharedVariablesRead.stream().mapToLong(l -> l).sum());
		logger.error("Shared Write {}", sharedVariablesWrite.stream().mapToLong(l -> l).sum());

		logger.error("Synchronized Read {}", testSharedSynchronizedVariablesRead.stream().mapToLong(l -> l).sum());
		logger.error("Synchronized Write {}", testSharedSynchronizedVariablesWrite.stream().mapToLong(l -> l).sum());

		logger.error("RW Synchronized Read {}", testSharedRWSynchronizedVariablesRead.stream().mapToLong(l -> l).sum());
		logger.error("RW  Synchronized Write {}", testSharedRWSynchronizedVariablesWrite.stream().mapToLong(l -> l).sum());

		logger.error("Volatile Read {}", testSharedVolatileVariablesRead.stream().mapToLong(l -> l).sum());
		logger.error("Volatile Write {}", testSharedVolatileVariablesWrite.stream().mapToLong(l -> l).sum());

		logger.error("Atomic Read {}", testAtomicIntegerRead.stream().mapToLong(l -> l).sum());
		logger.error("Atomic Write {}", testAtomicIntegerWrite.stream().mapToLong(l -> l).sum());
	}

	private static void reset() {
		sharedVariablesRead.clear();
		sharedVariablesWrite.clear();
		testSharedSynchronizedVariablesRead.clear();
		testSharedSynchronizedVariablesWrite.clear();
		testSharedRWSynchronizedVariablesRead.clear();
		testSharedRWSynchronizedVariablesWrite.clear();
		testSharedVolatileVariablesRead.clear();
		testSharedVolatileVariablesWrite.clear();
		testAtomicIntegerRead.clear();
		testAtomicIntegerWrite.clear();
	}


	static ArrayList<Long> sharedVariablesRead = new ArrayList<>(testCount * execCount);
	static ArrayList<Long> sharedVariablesWrite = new ArrayList<>(testCount * execCount);
	static ArrayList<Long> testSharedSynchronizedVariablesRead = new ArrayList<>(testCount * execCount);
	static ArrayList<Long> testSharedSynchronizedVariablesWrite = new ArrayList<>(testCount * execCount);
	static ArrayList<Long> testSharedRWSynchronizedVariablesRead = new ArrayList<>(testCount * execCount);
	static ArrayList<Long> testSharedRWSynchronizedVariablesWrite = new ArrayList<>(testCount * execCount);
	static ArrayList<Long> testSharedVolatileVariablesRead = new ArrayList<>(testCount * execCount);
	static ArrayList<Long> testSharedVolatileVariablesWrite = new ArrayList<>(testCount * execCount);
	static ArrayList<Long> testAtomicIntegerRead = new ArrayList<>(testCount * execCount);
	static ArrayList<Long> testAtomicIntegerWrite = new ArrayList<>(testCount * execCount);


	private static int sharedInteger = 0;
	private static int sharedInteger2 = 0;
	private static volatile int sharedVolatileInteger = 0;
	private static volatile int sharedVolatileInteger2 = 0;
	private static AtomicInteger atomicInteger = new AtomicInteger();
	private static AtomicInteger atomicInteger2 = new AtomicInteger();

	private static Lock lock = new ReentrantLock();
	private static ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

	private static void doTest() throws ExecutionException, InterruptedException {
		for (int i = 0; i < testCount; i++) {
			long[] durations = testReadWrite(nReadThreads, () -> {
				if (sharedInteger < 0 && multipleVariables && sharedInteger2 < 0)
					throw new IllegalArgumentException(CANNOT_BE_NULL);
			}, () -> {
				sharedInteger++;
				if (multipleVariables) sharedInteger2++;
			});

			sharedVariablesRead.add(TimeUnit.NANOSECONDS.toMillis(durations[0]));
			sharedVariablesWrite.add(TimeUnit.NANOSECONDS.toMillis(durations[1]));
		}


		for (int i = 0; i < testCount; i++) {
			long[] durations = testReadWrite(nReadThreads,
					() -> doInLock(lock, () -> {
						if (sharedInteger < 0 && multipleVariables && sharedInteger2 < 0)
							throw new IllegalArgumentException(CANNOT_BE_NULL);
					})
					, () -> doInLock(lock, () -> {
						sharedInteger++;
						if (multipleVariables) sharedInteger2++;
					}));

			testSharedSynchronizedVariablesRead.add(TimeUnit.NANOSECONDS.toMillis(durations[0]));
			testSharedSynchronizedVariablesWrite.add(TimeUnit.NANOSECONDS.toMillis(durations[1]));
		}

		for (int i = 0; i < testCount; i++) {
			long[] durations = testReadWrite(nReadThreads,
					() -> doInLock(readWriteLock.readLock(), () -> {
						if (sharedInteger < 0 && multipleVariables && sharedInteger2 < 0)
							throw new IllegalArgumentException(CANNOT_BE_NULL);
					})
					, () -> doInLock(readWriteLock.writeLock(), () -> {
						sharedInteger++;
						if (multipleVariables) sharedInteger2++;
					}));

			testSharedRWSynchronizedVariablesRead.add(TimeUnit.NANOSECONDS.toMillis(durations[0]));
			testSharedRWSynchronizedVariablesWrite.add(TimeUnit.NANOSECONDS.toMillis(durations[1]));
		}

		for (int i = 0; i < testCount; i++) {
			long[] durations = testReadWrite(nReadThreads, () -> {
				if (sharedVolatileInteger < 0 && multipleVariables && sharedVolatileInteger2 < 0)
					throw new IllegalArgumentException(CANNOT_BE_NULL);
			}, () -> {
				sharedVolatileInteger++;
				if (multipleVariables) sharedVolatileInteger2++;
			});

			testSharedVolatileVariablesRead.add(TimeUnit.NANOSECONDS.toMillis(durations[0]));
			testSharedVolatileVariablesWrite.add(TimeUnit.NANOSECONDS.toMillis(durations[1]));
		}


		for (int i = 0; i < testCount; i++) {
			long[] durations = testReadWrite(nReadThreads, () -> {
				if (atomicInteger.get() < 0 && multipleVariables && atomicInteger2.get() < 0)
					throw new IllegalArgumentException(CANNOT_BE_NULL);
			}, () -> {
				atomicInteger.addAndGet(1);
				if (multipleVariables)
					atomicInteger2.addAndGet(1);
			});

			testAtomicIntegerRead.add(TimeUnit.NANOSECONDS.toMillis(durations[0]));
			testAtomicIntegerWrite.add(TimeUnit.NANOSECONDS.toMillis(durations[1]));
		}
	}

	private static void doInLock(Lock lock, Runnable runnable) {
		lock.lock();  // block until condition holds
		try {
			runnable.run();
		} finally {
			lock.unlock();
		}
	}

	private static long[] testReadWrite(int nReadThreads, Runnable readRunnable, Runnable writeRunnable) throws ExecutionException, InterruptedException {
		ExecutorService executorService = new ForkJoinPool();
		List<Future<Long>> readFutures = getFutures(nReadThreads, readRunnable, executorService);

		List<Future<Long>> writeFutures = Collections.emptyList();

		if (writeRunnable != null && WRITE_ENABLED)
			writeFutures = getFutures(1, writeRunnable, executorService);

		long readTotal = 0;
		for (Future<Long> readFuture : readFutures) {
			readTotal += readFuture.get();
		}

		long writeTotal = 0;
		for (Future<Long> writeFuture : writeFutures) {
			writeTotal += writeFuture.get();
		}

		executorService.shutdown();

		return new long[]{readTotal, writeTotal};
	}

	private static List<Future<Long>> getFutures(int nThreads, Runnable runnable, ExecutorService executorService) {
		List<Future<Long>> readFutures = new ArrayList<>();

		for (int x = 0; x < nThreads; x++) {
			Callable<Long> callable = () -> {
				final long start = System.nanoTime();
				for (int i = 0; i < TEST_SIZE; i++) {
					runnable.run();
				}
				return System.nanoTime() - start;
			};
			Future<Long> future = executorService.submit(callable);

			readFutures.add(future);
		}
		return readFutures;
	}
}
