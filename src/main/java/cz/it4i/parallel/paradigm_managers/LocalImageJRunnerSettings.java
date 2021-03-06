package cz.it4i.parallel.paradigm_managers;

import lombok.Builder;
import lombok.Getter;


public class LocalImageJRunnerSettings extends RunnerSettings
{

	private static final long serialVersionUID = 4430991592979282602L;

	@Getter
	private String fijiExecutable;

	@Builder
	private LocalImageJRunnerSettings(String fiji)
	{
		super(true);
		fijiExecutable = fiji;
	}

}
