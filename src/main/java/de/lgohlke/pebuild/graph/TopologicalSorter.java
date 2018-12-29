package de.lgohlke.pebuild.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

class TopologicalSorter {

    static Collection<Job> sort(Collection<Job> jobs) {

        Collection<Job> sortedJobs = new ArrayList<>();

        Map<Job, Set<Job>> waitList = new LinkedHashMap<>();

        for (Job j : jobs) {
            waitList.put(j,
                         j.getWaitForJobs());
        }

        while (!waitList.isEmpty()) {
            List<Job> sortedCandidates = new ArrayList<>();
            waitList.forEach((j, waits) -> {
                if (waits.isEmpty()) {
                    sortedCandidates.add(j);
                }
            });

            // remove all zero waits
            sortedCandidates.forEach(waitList::remove);

            waitList.forEach((j, waits) -> {
                Set<Job> waitForJobsCopy = new HashSet<>(waits);
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
