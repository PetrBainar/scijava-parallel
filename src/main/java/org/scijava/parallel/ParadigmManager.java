package org.scijava.parallel;

import org.scijava.plugin.SingletonPlugin;

public interface ParadigmManager
	extends SingletonPlugin
{

	Class<? extends ParallelizationParadigm> getSupportedParadigmType();

	boolean isProfileSupported(ParallelizationParadigmProfile profile);


	ParallelizationParadigmProfile createProfile(
		String name);

	Boolean editProfile(ParallelizationParadigmProfile profile);

	void prepareParadigm(ParallelizationParadigmProfile profile,
		ParallelizationParadigm paradigm);

	void shutdownIfPossible(ParallelizationParadigmProfile profile);
}
