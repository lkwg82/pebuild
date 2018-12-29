package de.lgohlke.pebuild.graph;

import de.lgohlke.pebuild.JobTriggerHandler;
import de.lgohlke.pebuild.StepExecutor;
import de.lgohlke.pebuild.graph.validators.CycleValidator;
import de.lgohlke.pebuild.graph.validators.ReferencedJobMissingValidator;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TransferQueue;
import java.util.stream.Collectors;

@Slf4j
public class ExecutionGraph implements JobTriggerHandler {

    private final Map<StepExecutor, Collection<StepExecutor>> waitList = new ConcurrentHashMap<>();

    @Getter
    private final Collection<StepExecutor> jobs;
    @Getter
    private Duration timeout;

    private final Map<String, StepExecutor> jobNameMap;
    private final CountDownLatch toBeCompletedLatch;

    private final TransferQueue<StepExecutor.TimeContext> timeContextChannel = new LinkedTransferQueue<>();

    private ExecutorService executorService = Executors.newFixedThreadPool(5);

    private ExecutionGraph(Collection<StepExecutor> jobs, Duration timeout) {
        this.jobs = jobs;
        this.timeout = timeout;

        toBeCompletedLatch = new CountDownLatch(jobs.size());
        Map<String, StepExecutor> map = jobs.stream()
                                            .map(j -> new AbstractMap.SimpleEntry<>(j.getName(), j))
                                            .collect(Collectors.toMap(
                                                    AbstractMap.SimpleEntry::getKey,
                                                    AbstractMap.SimpleEntry::getValue));
        jobNameMap = new ConcurrentHashMap<>(map);
    }

    @Override
    public String toString() {
        return jobs.stream()
                   .map(j -> "(" + j + ")")
                   .collect(Collectors.joining("->"));
    }

    @SneakyThrows
    public void execute() {
        createTimeContextListener();
        createWaitList();

        synchronized (this) {
            startNextJobs();
        }
        waitAtMaximumTillTimeout();
        shutDown();
    }

    private void createTimeContextListener() {
        executorService.submit(() -> {
            Logger log = LoggerFactory.getLogger(ExecutionGraph.class.getName() + ":timeContextChannel");
            log.info("started");
//            String reportDir = Configuration.REPORT_DIRECTORY.value();

            do {
                try {
                    StepExecutor.TimeContext timeContext = timeContextChannel.take();
                    log.info("received:{}", timeContext);
                } catch (InterruptedException e) {
                    log.error(e.getMessage(), e);
                }
            } while (!executorService.isTerminated());
            log.info("finished");
        });
    }

    private void createWaitList() {
        waitList.clear();
        jobs.forEach(j -> waitList.put(j, j.getWaitForJobs()));
        log.info("waitList: " + waitList);
    }

    private void shutDown() {
        log.info("shutdown");
        executorService.shutdown();
        try {
            executorService.awaitTermination(10, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.error("failed to terminate executorService", e);
        }
    }

    private void waitAtMaximumTillTimeout() throws InterruptedException {
        long milliseconds = timeout.getSeconds() * 1000 + timeout.getNano() / 1_000_000;
        toBeCompletedLatch.await(milliseconds, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onComplete(String jobName, StepExecutor.TimeContext timeContext) {
        log.info("oncomplete: " + jobName);
        toBeCompletedLatch.countDown();

        timeContextChannel.offer(timeContext);

        Optional<StepExecutor> jobOptional = Optional.ofNullable(jobNameMap.get(jobName));

        synchronized (this) {
            jobOptional.ifPresent(job -> {
                waitList.forEach((j, waitForJobs) -> {
                    if (waitForJobs.remove(job)) {
                        log.info("removed '" + job + "' from waitlist of " + j);
                    }
                });
            });

            startNextJobs();
        }
    }

    private void startNextJobs() {
        List<StepExecutor> removalFromWaitlist = new ArrayList<>();

        waitList.forEach((j, waitForJobs) -> {
            if (waitForJobs.isEmpty()) {
                Runnable runnable = () -> j.execute();
                executorService.submit(runnable);
                removalFromWaitlist.add(j);
            }
        });

        removalFromWaitlist.forEach(waitList::remove);
        removalFromWaitlist.clear();
    }

    public static class Builder {
        private final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(10);

        private Collection<StepExecutor> stepExecutors = new HashSet<>();
        private Duration timeout = DEFAULT_TIMEOUT;

        public Builder addJob(StepExecutor executor) {
            if (stepExecutors.add(executor)) {
                return this;
            }
            throw new DuplicateJobException("tried to add duplicated executor: " + executor);
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        void sort() {
            stepExecutors = new LinkedList<>(TopologicalSorter.sort(stepExecutors));
        }

        void validate() {
            ReferencedJobMissingValidator.validate(stepExecutors);
            CycleValidator.validate(stepExecutors);
        }

        public ExecutionGraph build() {

            validate();
            sort();

            return new ExecutionGraph(stepExecutors, timeout);
        }

        class DuplicateJobException extends RuntimeException {
            DuplicateJobException(String message) {
                super(message);
            }
        }
    }
}
