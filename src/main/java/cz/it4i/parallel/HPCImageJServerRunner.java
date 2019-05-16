
package cz.it4i.parallel;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import cz.it4i.parallel.ClusterJobLauncher.Job;

public class HPCImageJServerRunner extends AbstractImageJServerRunner {

	private List< Integer > ports;

	private final HPCSettings settings;

	private Job job;

	private ClusterJobLauncher launcher;

	public HPCImageJServerRunner(HPCSettings settings, boolean shutdownOnClose)
	{
		super(shutdownOnClose);
		this.settings = settings;
		this.ports = Collections.emptyList();
	}

	public Job getJob() {
		return job;
	}

	@Override
	public int getNCores() {
		return settings.getNcpus();
	}

	@Override
	public void shutdown() {
		if (job != null) {
			job.stop();
		}
	}

	@Override
	public void close() {
		super.close();
		launcher.close();
	}

	@Override
	protected void doStartImageJServer() throws IOException {
		launcher = Routines.supplyWithExceptionHandling(
			() -> new ClusterJobLauncher(settings.getHost(), settings.getUserName(), settings.getKeyFile().toString(),
				settings.getKeyFilePassword()));
		final String arguments = getParameters().stream().collect(Collectors
			.joining(" "));
		if (settings.getJobID() != null) {
			job = launcher.getSubmittedJob(settings.getJobID());
		}
		else {
			job = launcher.submit(settings.getRemoteDirectory(), settings
				.getCommand(), arguments, settings.getNodes(), settings.getNcpus());
		}
		ports = job.createTunnels(8080, 8080);
	}

	@Override
	public List< Integer > getPorts()
	{
		return ports;
	}


}
