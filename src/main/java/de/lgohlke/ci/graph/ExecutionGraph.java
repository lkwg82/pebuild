package de.lgohlke.ci.graph;

import de.lgohlke.ci.JobTriggerHandler;
import de.lgohlke.ci.graph.validators.CycleValidator;
import de.lgohlke.ci.graph.validators.ReferencedJobMissingValidator;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class ExecutionGraph implements JobTriggerHandler {

    private final Map<Job, Collection<Job>> waitList = new ConcurrentHashMap<>();

    @Getter
    private final Collection<Job> jobs;
    @Getter
    private Duration timeout;

    private final Map<String, Job> jobNameMap;
    private final CountDownLatch toBeCompletedLatch;

    private ExecutorService executorService = Executors.newFixedThreadPool(5);

    private ExecutionGraph(Collection<Job> jobs, Duration timeout) {
        this.jobs = jobs;
        this.timeout = timeout;

        toBeCompletedLatch = new CountDownLatch(jobs.size());
        Map<String, Job> map = jobs.stream()
                                   .map(j -> new AbstractMap.SimpleEntry<>(j.getName(), j))
                                   .collect(Collectors.toMap(
                                           AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
        jobNameMap = new ConcurrentHashMap<>(map);
    }

    @Override
    public String toString() {
        return jobs.stream()
                   .map(j -> "(" + j + ")")
                   .collect(Collectors.joining("->"));
    }

    @SneakyThrows
    void execute() {
        createWaitList();

        synchronized (this) {
            startNextJobs();
        }
        waitAtMaximumTillTimeout();
        shutDown();
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
    public void onComplete(String jobName) {
        log.info("oncomplete: " + jobName);
        toBeCompletedLatch.countDown();

        Optional<Job> jobOptional = Optional.ofNullable(jobNameMap.get(jobName));

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
        List<Job> removalFromWaitlist = new ArrayList<>();

        waitList.forEach((j, waitForJobs) -> {
            if (waitForJobs.isEmpty()) {
                Runnable runnable = () -> j.getExecutor()
                                           .execute();
                executorService.submit(runnable);
                removalFromWaitlist.add(j);
            }
        });

        removalFromWaitlist.forEach(waitList::remove);
        removalFromWaitlist.clear();
    }

    public static class Builder {
        private final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(10);

        private Collection<Job> jobs = new HashSet<>();
        private Duration timeout = DEFAULT_TIMEOUT;

        public Builder addJob(Job job) {
            if (jobs.add(job)) {
                return this;
            }
            throw new DuplicateJobException("tried to add duplicated job: " + job);
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        void sort() {
            jobs = new LinkedList<>(TopologicalSorter.sort(jobs));
        }

        void validate() {
            ReferencedJobMissingValidator.validate(jobs);
            CycleValidator.validate(jobs);
        }

        public ExecutionGraph build() {

            validate();
            sort();

            return new ExecutionGraph(jobs, timeout);
        }

        class DuplicateJobException extends RuntimeException {
            DuplicateJobException(String message) {
                super(message);
            }
        }
    }
}
