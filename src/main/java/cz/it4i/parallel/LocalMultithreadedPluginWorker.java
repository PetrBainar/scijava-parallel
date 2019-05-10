
package cz.it4i.parallel;

import io.scif.services.DatasetIOService;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.scijava.Context;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalMultithreadedPluginWorker implements ParallelWorker {

	@Parameter
	private CommandService commandService;

	@Parameter
	private DatasetIOService datasetIOService;

	private final static Logger log = LoggerFactory.getLogger(
		cz.it4i.parallel.LocalMultithreadedPluginWorker.class);

	@Parameter
	private Context context;

	public LocalMultithreadedPluginWorker() {
		new Context().inject(this);
	}


	@Override
	public Map<String, Object> executeCommand(
		final String commandTypeName, final Map<String, ?> map)
	{

		// Create a new Object-typed input map
		final Map<String, Object> inputMap = new HashMap<>();
		inputMap.putAll(map);

		// Execute command and return outputs
		try {
			return commandService.run(commandTypeName, true, inputMap).get().getOutputs();
		}
		catch (InterruptedException | ExecutionException e) {
			log.error(e.getMessage(), e);
		}

		return null;
	}
}
