package de.lgohlke.pebuild.graph;

import de.lgohlke.pebuild.StepExecutor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

class TopologicalSorter {

    static Collection<StepExecutor> sort(Collection<StepExecutor> jobs) {

        Collection<StepExecutor> sortedJobs = new ArrayList<>();

        Map<StepExecutor, Set<StepExecutor>> waitList = new LinkedHashMap<>();

        for (StepExecutor j : jobs) {
            waitList.put(j, j.getWaitForJobs());
        }

        while (!waitList.isEmpty()) {
            List<StepExecutor> sortedCandidates = new ArrayList<>();
            waitList.forEach((j, waits) -> {
                if (waits.isEmpty()) {
                    sortedCandidates.add(j);
                }
            });

            // remove all zero waits
            sortedCandidates.forEach(waitList::remove);

            waitList.forEach((j, waits) -> {
                Set<StepExecutor> waitForJobsCopy = new HashSet<>(waits);
                if (waitForJobsCopy.removeAll(sortedCandidates)) {
                    waitList.put(j, waitForJobsCopy);
                }
            });

            sortedJobs.addAll(sortedCandidates);
            sortedCandidates.clear();
        }

        return sortedJobs;
    }
}
