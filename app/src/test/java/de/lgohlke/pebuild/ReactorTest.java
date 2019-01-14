package de.lgohlke.pebuild;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static reactor.core.scheduler.Schedulers.elastic;
import static reactor.core.scheduler.Schedulers.parallel;

@Slf4j
public class ReactorTest {
    @Test
    @SneakyThrows
    void demo() {

        List<Job> runningJobs = new CopyOnWriteArrayList<>();
        DirectProcessor<Job> allJobs = DirectProcessor.create();

        allJobs
                .timeout(Duration.ofMillis(800))
//                .log(null, Level.WARNING)
                .doOnTerminate(() -> {
                    log.warn("TERMINATE");
                    allJobs.onComplete();
                })
                .parallel(2)
                .runOn(parallel())
                .doOnError(e -> {
                    log.warn("" + e);
                    log.warn("running jobs: " + runningJobs);
                    runningJobs.forEach(Job::cancel);
                })
                .subscribe(job -> runJob(runningJobs, job));

        allJobs.onNext(new JobSuccess());
        allJobs.onNext(new JobInnerTimeout());
        allJobs.onNext(new JobOuterTimeout());

        TimeUnit.MILLISECONDS.sleep(2000);
        System.out.println(allJobs.isTerminated());
    }

    private void runJob(List<Job> runningJobs, Job job) {
        log.warn("adding job: " + job);
        runningJobs.add(job);
        log.warn("running jobs: " + runningJobs);
        job.getResults()
//           .log(null, Level.WARNING)
           .doOnError(e -> {
               log.warn("" + e);
               runningJobs.remove(job);
           })
           .subscribe(s -> log.warn(s));
        log.warn("executing job: {}", job);
        Mono.fromRunnable(job::execute)
            .publishOn(elastic())
            .subscribeOn(elastic())
            .doOnTerminate(() -> {
                log.warn("terminated job:{}", job);
                runningJobs.remove(job);
            })
            .subscribe();
        log.warn("dispatched:{}", job);
    }

    @Test
    void blocking() throws InterruptedException {
        val blocking = Mono.fromRunnable(this::blockingMethod)
                           .subscribeOn(elastic());

        log.warn("subscribe");
        blocking.subscribe();
        log.warn("finished");

        TimeUnit.MILLISECONDS.sleep(200);
        log.warn("end test");
    }

    private void blockingMethod() {
        log.warn("start block");
        try {
            TimeUnit.MILLISECONDS.sleep(1100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.warn("end block");
    }

    @Slf4j
    @EqualsAndHashCode
    static abstract class Job {
        private final AtomicBoolean canceled = new AtomicBoolean(false);
        @Getter
        private final DirectProcessor<String> results = DirectProcessor.create();

        abstract void execute();

        void cancel() {
            if (canceled.getAndSet(true)) {
                return;
            }

            log.warn("cancelation " + getClass().getSimpleName());
            results.onNext("cancelation");
            results.onComplete();
        }

        @Override
        public String toString() {
            return getClass().getSimpleName();
        }
    }

    @Slf4j
    static class JobSuccess extends Job {

        @Override
        @SneakyThrows
        public void execute() {
            getResults().onNext("start " + getClass().getSimpleName());
            TimeUnit.MILLISECONDS.sleep(300);
            getResults().onNext("finish " + getClass().getSimpleName());
            getResults().onComplete();
        }
    }

    @Slf4j
    static class JobInnerTimeout extends Job {

        @Override
        @SneakyThrows
        public void execute() {
            getResults().onNext("start " + getClass().getSimpleName());
            TimeUnit.MILLISECONDS.sleep(100);
            getResults().onError(new TimeoutException(getClass().getSimpleName()));
        }
    }

    @Slf4j
    static class JobOuterTimeout extends Job {
        private final CountDownLatch latch = new CountDownLatch(1);
        private final AtomicBoolean canceled = new AtomicBoolean(false);

        @Override
        void cancel() {
            super.cancel();
            latch.countDown();
            canceled.set(true);
        }

        @Override
        @SneakyThrows
        public void execute() {
            getResults().onNext("start " + getClass().getSimpleName());
            latch.await(1800, TimeUnit.MILLISECONDS);
            if (canceled.get()) {
                log.warn("canceled");
            } else {
                log.warn("finish");
                getResults().onNext("finish " + getClass().getSimpleName());
                getResults().onComplete();
            }
        }
    }
}
