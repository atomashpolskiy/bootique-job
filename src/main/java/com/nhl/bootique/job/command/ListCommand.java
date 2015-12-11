package com.nhl.bootique.job.command;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.nhl.bootique.command.CommandOutcome;
import com.nhl.bootique.command.OptionTriggeredCommand;
import com.nhl.bootique.job.Job;
import com.nhl.bootique.jopt.Options;

import joptsimple.OptionParser;

public class ListCommand extends OptionTriggeredCommand {

	private static final Logger LOGGER = LoggerFactory.getLogger(ListCommand.class);

	private static final String LIST_OPTION = "list";

	private Provider<Set<Job>> jobsProvider;

	@Inject
	public ListCommand(Provider<Set<Job>> jobsProvider) {
		this.jobsProvider = jobsProvider;
	}

	@Override
	public void configOptions(OptionParser parser) {
		parser.accepts(getOption(), "Lists all jobs available in the app");
	}

	@Override
	protected String getOption() {
		return LIST_OPTION;
	}

	@Override
	protected CommandOutcome doRun(Options options) {

		Collection<Job> jobs = jobsProvider.get();
		if (jobs.isEmpty()) {
			return CommandOutcome.failed(1, "No jobs are available.");
		}

		LOGGER.info("Available jobs:");

		// TODO: sort jobs by name for more readable output

		jobs.forEach(j -> {
			Optional<String> params = j.getMetadata().getParameters().stream().map(p -> {

				StringBuilder buffer = new StringBuilder();

				buffer.append(p.getName()).append(":").append(p.getTypeName());

				if (p.getDefaultValue() != null) {
					buffer.append("=").append(p.getDefaultValue());
				}

				return buffer.toString();
			}).reduce((s1, s2) -> s1 + ", " + s2);

			if (params.isPresent()) {
				LOGGER.info(String.format("     - %s(%s)", j.getMetadata().getName(), params.get()));
			} else {
				LOGGER.info(String.format("     - %s", j.getMetadata().getName()));
			}
		});

		return CommandOutcome.succeeded();
	}
}