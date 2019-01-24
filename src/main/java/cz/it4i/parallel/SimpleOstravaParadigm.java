
package cz.it4i.parallel;

import com.google.common.collect.Streams;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.scijava.command.CommandService;
import org.scijava.parallel.ParallelizationParadigm;
import org.scijava.plugin.Parameter;
import org.scijava.thread.ThreadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SimpleOstravaParadigm implements ParallelizationParadigm {

	private static final Logger log = LoggerFactory.getLogger(
		cz.it4i.parallel.SimpleOstravaParadigm.class);

	protected WorkerPool workerPool;

	@Parameter
	private ThreadService threadService;

	@Parameter
	private CommandService commandService;
	
	private ExecutorService executorService;

	// -- SimpleOstravaParadigm methods --

	abstract protected void initWorkerPool();

	// -- ParallelizationParadigm methods --

	@Override
	public void init() {
		workerPool = new WorkerPool();
		initWorkerPool();
		executorService = Executors.newFixedThreadPool(workerPool.size(),
			threadService);
	}

	@Override
	public List<Map<String, Object>> runAllCommands(List<String> commands,
		List<Map<String, Object>> parameters)
	{
		List<CompletableFuture<Map<String, Object>>> futures = runAllCommandsAsync(
			commands, parameters);

		return futures.stream().map(f -> {
			try {
				return f.get();
			}
			catch (InterruptedException | ExecutionException exc) {
				log.error("f.get", exc);
				throw new RuntimeException(exc);
			}
		}).collect(Collectors.toList());
	}

	@Override
	public List<CompletableFuture<Map<String, Object>>> runAllCommandsAsync(
		List<String> commands, List<Map<String, Object>> parameters)
	{
		
		List<CompletableFuture<Map<String, Object>>> futures = Streams.zip(commands.stream(), parameters.stream(), new BiFunction<String, Map<String, Object>, CompletableFuture<Map<String, Object>>>() {
		
			@Override
			public CompletableFuture<Map<String, Object>> apply(String command,
				Map<String, Object> params)
			{
				return CompletableFuture.supplyAsync(new Supplier<Map<String, Object>>() {

					@Override
					public Map<String, Object> get() {
						try {
							ParallelWorker pw = workerPool.takeFreeWorker();
							try {
								ParameterProcessor parameterProcessor = constructParameterProcessor(pw, command);
										return parameterProcessor.processOutput(pw.executeCommand(
											command, parameterProcessor.processInputs(params)));
									} finally {
								workerPool.addWorker(pw);
							}
						}
						catch (InterruptedException exc) {
							log.error(exc.getMessage(), exc);
							throw new RuntimeException(exc);
						}
					}
				}, executorService);
			}
		}).collect(Collectors.toList());
		return futures;
	}
	
	
	@Override
	public void close() {
		workerPool.close();
		executorService.shutdown();
		threadService.dispose();
	}

	abstract protected ParameterProcessor constructParameterProcessor(
		ParallelWorker pw, String command);

}
