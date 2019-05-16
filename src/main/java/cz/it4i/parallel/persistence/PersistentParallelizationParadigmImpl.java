package cz.it4i.parallel.persistence;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.scijava.parallel.ParallelizationParadigm;
import org.scijava.parallel.PersistentParallelizationParadigm;

import cz.it4i.parallel.ImageJServerParadigm.Host;
import cz.it4i.parallel.plugins.RequestBrokerServiceCallCommand;
import cz.it4i.parallel.plugins.RequestBrokerServiceGetResultCommand;
import cz.it4i.parallel.plugins.RequestBrokerServiceInitCommand;
import cz.it4i.parallel.plugins.RequestBrokerServicePurgeCommand;
import lombok.AllArgsConstructor;
import lombok.Data;



public class PersistentParallelizationParadigmImpl implements
	PersistentParallelizationParadigm
{

	static final String INPUTS = "inputs";
	static final String MODULE_ID = "moduleId";
	static final String REQUEST_IDS = "requestIDs";
	static final String RESULTS = "results";

	enum CompletableFutureIDCases implements CompletableFutureID {
			UNKNOWN;
	}

	private final ParallelizationParadigm paradigm;


	private final Map<CompletableFuture<Map<String, Object>>, Serializable> futures2id =
		new HashMap<>();
	private final Map<Serializable, CompletableFuture<Map<String, Object>>> id2futures =
		new HashMap<>();


	private List<Host> hosts;

	public PersistentParallelizationParadigmImpl() {
		this(null);
	}

	public static PersistentParallelizationParadigm addPersistencyToParadigm(
		ParallelizationParadigm paradigm, List<Host> hosts)
	{
		PersistentParallelizationParadigmImpl result =
			new PersistentParallelizationParadigmImpl(paradigm);
		result.setHosts(hosts);
		result.initRemoteParallelizationParadigm();
		return result;
	}

	@Override
	public void init() {
	}

	@Override
	public void close() {
		paradigm.close();
	}

	@Override
	public List<CompletableFuture<Map<String, Object>>> runAllAsync(
		String commandTypeName, List<Map<String, Object>> inputs)
	{
		if (hosts == null) {
			throw new IllegalStateException();
		}
		Map<String, Object> inputForExecution = new HashMap<>();
		inputForExecution.put(MODULE_ID, commandTypeName);
		inputForExecution.put(INPUTS, inputs);

		@SuppressWarnings("unchecked")
		List<Serializable> result = (List<Serializable>) paradigm
			.runAll(RequestBrokerServiceCallCommand.class.getCanonicalName(),
				Collections
				.singletonList(inputForExecution)).get(0).get(REQUEST_IDS);

		return result.stream().map(this::getFuture4FutureID).collect(Collectors
			.toList());
	}


	@Override
	public List<CompletableFutureID> getIDs(
		List<CompletableFuture<Map<String, Object>>> futures)
	{
		return futures.stream().map(this::getFutureID4Future).collect(Collectors
			.toList());
	}

	@Override
	public List<CompletableFuture<Map<String, Object>>> getByIDs(
		List<CompletableFutureID> ids)
	{
		return ids.stream().map(this::getFuture4FutureID).collect(Collectors
			.toList());
	}

	@Override
	public void purge(List<CompletableFutureID> ids) {
		final Map<String, Object> inputForExecution = new HashMap<>();
		synchronized (this) {
			ids.stream().forEach(this::removeFutureID);
		}
		inputForExecution.put(REQUEST_IDS, new LinkedList<>(ids));
		try {
			paradigm.runAllAsync(RequestBrokerServicePurgeCommand.class
				.getCanonicalName(), Collections.singletonList(inputForExecution)).get(
					0).get();

		}
		catch (InterruptedException | ExecutionException exc) {
			throw new RuntimeException(exc);
		}
	}

	@Override
	public Collection<CompletableFuture<Map<String, Object>>> getAll() {

		throw new UnsupportedOperationException();
//			return (Collection<CompletableFuture<Map<String, Object>>>) paradigm
//				.runAllAsync(RequestBrokerServiceGetAllCommand.class.getCanonicalName(),
//					Collections.emptyList()).get(0).get().get(REQUEST_IDS);

	}

	private PersistentParallelizationParadigmImpl(
		ParallelizationParadigm paradigmParam)
	{
		paradigm = paradigmParam;
	}

	private void setHosts(List<Host> hosts) {
		this.hosts = hosts;
	}

	private void initRemoteParallelizationParadigm() {
		Map<String, Object> inputForExecution = new HashMap<>();
		inputForExecution.put("names", hosts.stream().map(Host::getName).collect(
			Collectors.toList()));
		inputForExecution.put("ncores", hosts.stream().map(Host::getNCores)
			.collect(Collectors.toList()));
		paradigm.runAll(
				RequestBrokerServiceInitCommand.class.getCanonicalName(),
			Collections.singletonList(inputForExecution));
	
	}

	@SuppressWarnings("unchecked")
	private synchronized CompletableFuture<Map<String, Object>>
		getFuture4FutureID(Serializable requestID)
	{
		if (requestID instanceof PComletableFutureID) {
			requestID = ((PComletableFutureID) requestID).getInnerId();

		}
		if (id2futures.containsKey(requestID)) {
			return id2futures.get(requestID);
		}

		if (requestID == CompletableFutureIDCases.UNKNOWN) {
			return CompletableFuture.supplyAsync(() -> {
				throw new IllegalStateException();
			});
		}
		final Map<String, Object> inputForExecution = new HashMap<>();
		inputForExecution.put(REQUEST_IDS, new LinkedList<>(Collections.singleton(
			requestID)));
		CompletableFuture<Map<String, Object>> resultFuture = paradigm.runAllAsync(
			RequestBrokerServiceGetResultCommand.class.getCanonicalName(), Collections
				.singletonList(inputForExecution)).get(0).thenApply(
					result -> ((List<Map<String, Object>>) result.get(RESULTS)).get(0));

		id2futures.put(requestID, resultFuture);
		futures2id.put(resultFuture, requestID);
		return resultFuture;

	}

	private synchronized CompletableFutureID getFutureID4Future(
		CompletableFuture<Map<String, Object>> future)
	{
		if (futures2id.containsKey(future)) {
			return PComletableFutureID.getFutureID(futures2id.get(future));
		}
		return CompletableFutureIDCases.UNKNOWN;
	}

	private void removeFutureID(Object futureID) {
		if (futureID == CompletableFutureIDCases.UNKNOWN) {
			return;
		}
		if (futureID instanceof CompletableFutureID) {
			CompletableFutureID pId = (CompletableFutureID) futureID;
			if (id2futures.containsKey(pId)) {
				CompletableFuture<Map<String, Object>> future = id2futures.get(pId);
				id2futures.remove(pId);
				futures2id.remove(future);
			}
		}
		else {
			throw new IllegalArgumentException("Unsupported type " + futureID);
		}
	}

	@Data
	@AllArgsConstructor
	private static class PComletableFutureID implements CompletableFutureID {

		final Serializable innerId;

		static CompletableFutureID getFutureID(Serializable id) {
			if (id instanceof CompletableFutureID) {
				return (CompletableFutureID) id;
			}
			return new PComletableFutureID(id);
		}
	}
}