package io.bootique.job.runtime;

import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import io.bootique.BQCoreModule;
import io.bootique.ConfigModule;
import io.bootique.command.Command;
import io.bootique.config.ConfigurationFactory;
import io.bootique.env.Environment;
import io.bootique.job.Job;
import io.bootique.job.command.ExecCommand;
import io.bootique.job.command.ListCommand;
import io.bootique.job.command.ScheduleCommand;
import io.bootique.job.lock.LocalLockHandler;
import io.bootique.job.lock.LockHandler;
import io.bootique.job.lock.LockType;
import io.bootique.job.lock.zookeeper.ZkClusterLockHandler;
import io.bootique.job.scheduler.Scheduler;
import io.bootique.job.scheduler.SchedulerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class JobModule extends ConfigModule {

	private Collection<Class<? extends Job>> jobTypes = new HashSet<>();

	/**
	 * @param binder
	 *            DI binder passed to the Module that invokes this method.
	 * @since 0.11
	 * @return returns a {@link Multibinder} for contributed jobs.
	 */
	public static Multibinder<Job> contributeJobs(Binder binder) {
		return Multibinder.newSetBinder(binder, Job.class);
	}

	public JobModule() {
	}

	public JobModule(String configPrefix) {
		super(configPrefix);
	}

	@Override
	protected String defaultConfigPrefix() {
		// main config sets up Scheduler , so renaming default config prefix
		return "scheduler";
	}

	@SafeVarargs
	public final JobModule jobs(Class<? extends Job>... jobTypes) {
		Arrays.asList(jobTypes).forEach(jt -> this.jobTypes.add(jt));
		return this;
	}

	@Override
	public void configure(Binder binder) {

		Multibinder<Command> commandBinder = BQCoreModule.contributeCommands(binder);
		commandBinder.addBinding().to(ExecCommand.class).in(Singleton.class);
		commandBinder.addBinding().to(ListCommand.class).in(Singleton.class);
		commandBinder.addBinding().to(ScheduleCommand.class).in(Singleton.class);

		// trigger extension points creation and provide default contributions

		Multibinder<Job> jobBinder = JobModule.contributeJobs(binder);
		jobTypes.forEach(jt -> jobBinder.addBinding().to(jt).in(Singleton.class));

		MapBinder<LockType, LockHandler> lockHandlers = MapBinder.newMapBinder(binder, LockType.class,
				LockHandler.class);
		lockHandlers.addBinding(LockType.local).to(LocalLockHandler.class);
		lockHandlers.addBinding(LockType.clustered).to(ZkClusterLockHandler.class);
	}

	@Provides
	protected Scheduler createScheduler(Set<Job> jobs, Environment environment, Map<LockType, LockHandler> jobRunners,
										ConfigurationFactory configFactory) {
		return configFactory.config(SchedulerFactory.class, configPrefix).createScheduler(jobs, environment,
				configFactory, jobRunners);
	}
}
