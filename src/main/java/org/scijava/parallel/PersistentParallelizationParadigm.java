/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2019 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this project.
 ******************************************************************************/
package org.scijava.parallel;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Extension of {@link ParallelizationParadigm} that allows send request,
 * disconnect, save/load requests IDs, reconnect and get results.
 * 
 * @author Jan Kožusznik
 */
public interface PersistentParallelizationParadigm extends
	ParallelizationParadigm
{

	public interface CompletableFutureID extends Serializable {

	}

	List<CompletableFutureID> getIDs(
		List<CompletableFuture<Map<String, Object>>> future);

	List<CompletableFuture<Map<String, Object>>> getByIDs(
		List<CompletableFutureID> ids);

	void purge(List<CompletableFutureID> ids);

	Collection<CompletableFuture<Map<String, Object>>> getAll();

}
