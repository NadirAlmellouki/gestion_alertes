package FST.MST_RSI.PFA.classification.domain.port;

import FST.MST_RSI.PFA.classification.domain.model.SolutionContext;

import java.util.List;
import java.util.Optional;

/**
 * Abstraction over the référentiel applicatif (PostgreSQL en production, JSON en test).
 */
public interface BusinessContextPort {

    List<SolutionContext> findCandidates(String... searchTerms);

    Optional<String> findPsiBySolutionName(String solutionName);
}
