package de.lgohlke.ci.graph;

import de.lgohlke.ci.graph.validators.CycleValidator;
import de.lgohlke.ci.graph.validators.ReferencedJobMissingValidator;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.stream.Collectors;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ExecutionGraph {
    private final Collection<Job> jobs;

    @Override
    public String toString() {
        return jobs.stream()
                   .map(j -> "(" + j + ")")
                   .collect(Collectors.joining("->"));
    }

    public static class Builder {
        private Collection<Job> jobs = new HashSet<>();

        public Builder addJob(Job job) {
            if (jobs.add(job)) {
                return this;
            }
            throw new DuplicateJobException("tried to add duplicated job: " + job);
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

            return new ExecutionGraph(jobs);
        }

        class DuplicateJobException extends RuntimeException {
            DuplicateJobException(String message) {
                super(message);
            }
        }
    }
}
