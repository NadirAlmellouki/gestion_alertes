package FST.MST_RSI.PFA.directory.application.dto;

import java.util.ArrayList;
import java.util.List;

public record ReferentialImportReport(
        int entitiesImported,
        int polesImported,
        int domainsImported,
        int solutionsImported,
        int solutionsActiveImported,
        int solutionsInactiveImported,
        int personsImported,
        int personsUpdated,
        int personsRejected,
        int assignmentsImported,
        int assignmentsRejected,
        int rowsRejected,
        String message,
        List<String> rejectionSamples
) {
    public ReferentialImportReport {
        rejectionSamples = rejectionSamples == null ? List.of() : List.copyOf(rejectionSamples);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private int entitiesImported;
        private int polesImported;
        private int domainsImported;
        private int solutionsImported;
        private int solutionsActiveImported;
        private int solutionsInactiveImported;
        private int personsImported;
        private int personsUpdated;
        private int personsRejected;
        private int assignmentsImported;
        private int assignmentsRejected;
        private int rowsRejected;
        private final List<String> rejectionSamples = new ArrayList<>();

        public Builder entitiesImported(int v) { entitiesImported = v; return this; }
        public Builder polesImported(int v) { polesImported = v; return this; }
        public Builder domainsImported(int v) { domainsImported = v; return this; }
        public Builder solutionsImported(int v) { solutionsImported = v; return this; }
        public Builder solutionsActiveImported(int v) { solutionsActiveImported = v; return this; }
        public Builder solutionsInactiveImported(int v) { solutionsInactiveImported = v; return this; }
        public Builder personsImported(int v) { personsImported = v; return this; }
        public Builder personsUpdated(int v) { personsUpdated = v; return this; }
        public Builder personsRejected(int v) { personsRejected = v; return this; }
        public Builder assignmentsImported(int v) { assignmentsImported = v; return this; }
        public Builder assignmentsRejected(int v) { assignmentsRejected = v; return this; }
        public Builder rowsRejected(int v) { rowsRejected = v; return this; }
        public int rowsRejected() { return rowsRejected; }
        public int personsImported() { return personsImported; }
        public int personsUpdated() { return personsUpdated; }
        public int assignmentsImported() { return assignmentsImported; }
        public int assignmentsRejected() { return assignmentsRejected; }
        public Builder addRejectionSample(String sample) {
            if (rejectionSamples.size() < 20) {
                rejectionSamples.add(sample);
            }
            return this;
        }

        public ReferentialImportReport build(String message) {
            return new ReferentialImportReport(
                    entitiesImported,
                    polesImported,
                    domainsImported,
                    solutionsImported,
                    solutionsActiveImported,
                    solutionsInactiveImported,
                    personsImported,
                    personsUpdated,
                    personsRejected,
                    assignmentsImported,
                    assignmentsRejected,
                    rowsRejected,
                    message,
                    rejectionSamples
            );
        }
    }
}
